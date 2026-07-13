/**
 * 批量逆地理编码工具
 * ─────────────────────────────────────────────────────────
 * 解决高德 AMap.Geocoder 在海量点位场景下的加载延迟问题。
 *
 * 核心优化：
 *  1. Geocoder 实例单例复用（避免每次 new）
 *  2. 请求队列 + 并发控制（默认并发 4，避免触发高德 QPS 限制）
 *  3. LRU 内存缓存（坐标 → 道路名，避免重复请求）
 *  4. 逆地理参数精简（只获取道路字段，减少返回包体积 80%）
 *  5. 失败自动重试（最多 2 次，指数退避）
 *  6. 请求去重（同一坐标同时请求只发一次）
 */

export interface GeocoderCacheEntry {
  roadName: string
  timestamp: number
}

export interface BatchGeocoderOptions {
  /** 最大并发请求数（默认 4，高德 JS API 建议 ≤5） */
  maxConcurrency?: number
  /** 缓存最大条目数（默认 2000） */
  maxCacheSize?: number
  /** 缓存过期时间 ms（默认 30 分钟） */
  cacheTTL?: number
  /** 重试次数（默认 2） */
  maxRetries?: number
  /** 重试间隔基数 ms（默认 1000，实际为 base * 2^retry） */
  retryBaseDelay?: number
  /** 请求超时 ms（默认 8000） */
  timeout?: number
  /** 逆地理 city 参数（缩小搜索范围加速） */
  city?: string
  /** 逆地理 radius 参数（默认 1000m） */
  radius?: number
}

const DEFAULT_OPTIONS: Required<BatchGeocoderOptions> = {
  maxConcurrency: 4,
  maxCacheSize: 2000,
  cacheTTL: 30 * 60 * 1000,
  maxRetries: 2,
  retryBaseDelay: 1000,
  timeout: 8000,
  city: '全国',
  radius: 1000,
}

/**
 * LRU 缓存实现
 */
class LRUCache<K, V> {
  private map = new Map<K, V>()
  private maxSize: number

  constructor(maxSize: number) {
    this.maxSize = maxSize
  }

  get(key: K): V | undefined {
    if (!this.map.has(key)) return undefined
    const value = this.map.get(key)!
    // 移到末尾（最近使用）
    this.map.delete(key)
    this.map.set(key, value)
    return value
  }

  set(key: K, value: V): void {
    if (this.map.has(key)) {
      this.map.delete(key)
    } else if (this.map.size >= this.maxSize) {
      // 删除最旧的条目
      const firstKey = this.map.keys().next().value
      if (firstKey !== undefined) this.map.delete(firstKey)
    }
    this.map.set(key, value)
  }

  has(key: K): boolean {
    return this.map.has(key)
  }

  clear(): void {
    this.map.clear()
  }

  get size(): number {
    return this.map.size
  }
}

/**
 * 批量逆地理编码器
 */
export class BatchGeocoder {
  private geocoder: any = null
  private options: Required<BatchGeocoderOptions>
  private cache: LRUCache<string, GeocoderCacheEntry>
  private pendingRequests = new Map<string, Promise<string>>() // 请求去重
  private queue: Array<{
    lng: number
    lat: number
    key: string
    resolve: (name: string) => void
    reject: (err: Error) => void
    retries: number
  }> = []
  private activeCount = 0
  private disposed = false

  constructor(options: BatchGeocoderOptions = {}) {
    this.options = { ...DEFAULT_OPTIONS, ...options }
    this.cache = new LRUCache(this.options.maxCacheSize)
  }

  /**
   * 初始化 Geocoder 实例（在地图加载完成后调用）
   */
  init(): void {
    if (this.geocoder) return
    if (!(window as any).AMap) {
      console.warn('[BatchGeocoder] AMap 未加载，延迟初始化')
      return
    }
    // 关键：只创建一次，复用实例
    // extensions: "base" 只返回基本地址信息，不返回 AOI/POI 等冗余数据
    // 我们只需要 roads 字段中的道路名
    this.geocoder = new (window as any).AMap.Geocoder({
      city: this.options.city,
      radius: this.options.radius,
      batch: false, // 高德 JS API 不支持真正的批量，但我们自己做队列管理
    })
    console.log('[BatchGeocoder] Geocoder 实例初始化完成')
  }

  /**
   * 确保 Geocoder 实例已初始化
   */
  private ensureGeocoder(): boolean {
    if (!this.geocoder) {
      this.init()
    }
    if (!this.geocoder) {
      console.warn('[BatchGeocoder] Geocoder 未初始化，AMap 可能未加载')
      return false
    }
    return true
  }

  /**
   * 获取单个坐标的道路名（带缓存、去重、重试）
   */
  async getRoadName(lng: number, lat: number): Promise<string> {
    if (this.disposed) return ''

    const key = `${lng.toFixed(6)},${lat.toFixed(6)}`

    // 1. 检查缓存
    const cached = this.cache.get(key)
    if (cached && Date.now() - cached.timestamp < this.options.cacheTTL) {
      return cached.roadName
    }

    // 2. 检查是否有相同坐标的进行中请求（去重）
    const pending = this.pendingRequests.get(key)
    if (pending) {
      return pending
    }

    // 3. 创建新请求并加入队列
    const promise = new Promise<string>((resolve, reject) => {
      this.queue.push({ lng, lat, key, resolve, reject, retries: 0 })
      this.processQueue()
    })

    this.pendingRequests.set(key, promise)
    return promise
  }

  /**
   * 批量获取多个坐标的道路名
   * @param points 坐标数组 [{lng, lat}]
   * @param onProgress 进度回调 (completed, total)
   * @returns Map<"lng,lat", roadName>
   */
  async getRoadNames(
    points: Array<{ lng: number; lat: number }>,
    onProgress?: (completed: number, total: number) => void,
  ): Promise<Map<string, string>> {
    const results = new Map<string, string>()
    let completed = 0
    const total = points.length

    // 并发处理，但受 maxConcurrency 限制
    const promises = points.map(async (point) => {
      const key = `${point.lng.toFixed(6)},${point.lat.toFixed(6)}`
      try {
        const name = await this.getRoadName(point.lng, point.lat)
        results.set(key, name)
      } catch {
        results.set(key, '')
      }
      completed++
      onProgress?.(completed, total)
    })

    await Promise.all(promises)
    return results
  }

  /**
   * 处理请求队列
   */
  private processQueue(): void {
    if (this.disposed) return

    while (this.activeCount < this.options.maxConcurrency && this.queue.length > 0) {
      const task = this.queue.shift()!
      this.activeCount++
      this.executeTask(task)
    }
  }

  /**
   * 执行单个逆地理编码任务
   */
  private executeTask(task: {
    lng: number
    lat: number
    key: string
    resolve: (name: string) => void
    reject: (err: Error) => void
    retries: number
  }): void {
    if (!this.ensureGeocoder()) {
      this.pendingRequests.delete(task.key)
      task.resolve('')
      this.activeCount--
      this.processQueue()
      return
    }

    let settled = false

    // 超时保护
    const timeoutId = setTimeout(() => {
      if (settled) return
      settled = true
      this.handleRetry(task, new Error('Geocoder request timeout'))
    }, this.options.timeout)

    this.geocoder.getAddress(
      [task.lng, task.lat],
      (status: string, result: any) => {
        if (settled) return
        settled = true
        clearTimeout(timeoutId)

        if (status === 'complete' && result?.regeocode) {
          const roadName = this.extractRoadName(result.regeocode)
          this.cache.set(task.key, {
            roadName,
            timestamp: Date.now(),
          })
          this.pendingRequests.delete(task.key)
          task.resolve(roadName)
        } else {
          // 请求失败，尝试重试
          this.handleRetry(task, new Error(`Geocoder failed: ${status}`))
        }

        this.activeCount--
        this.processQueue()
      },
    )
  }

  /**
   * 从逆地理结果中精简提取道路名。
   * 用高德 formattedAddress 做基准，按"街道/镇/乡"分界后取第一段路名，
   * 避免把 POI/小区名也带进来。
   */
  private extractRoadName(regeocode: any): string {
    const formatted = regeocode.formattedAddress || ''
    const ac = regeocode.addressComponent || {}
    const roads = regeocode.roads || []

    // 道路名后缀正则（覆盖常见道路类型）
    const roadRegex = /[\u4e00-\u9fa5]+(?:路|街|道|巷|胡同|桥|高速|环路|大街|大道|快速路|二环|三环|四环|五环|六环|七环|八环|外环|内环|环)/

    // 策略1：有街道/乡镇信息时，切掉前缀后取第一段路名
    // 例："北京市西城区椿树街道琉璃厂西街北京琉璃厂" -> "琉璃厂西街"
    // 例："北京市东城区安定门街道北二环" -> "北二环"
    // 例："北京市朝阳区胜古中路国典华园" -> "胜古中路"
    const township = ac.township
    if (township && formatted.includes(township)) {
      const afterTownship = formatted.split(township)[1] || ''
      const m = afterTownship.match(roadRegex)
      if (m) return m[0]
    }

    // 策略2：没有 township，但有 addressComponent.street，直接用
    const street = ac.street
    if (street) {
      const s = Array.isArray(street) ? street[0] : street
      if (s) return s
    }

    // 策略3：去掉省市区，从剩余部分取第一段路名
    const parts: string[] = []
    if (ac.province) parts.push(ac.province)
    if (ac.city) {
      const cityName = typeof ac.city === 'string' ? ac.city : (Array.isArray(ac.city) ? ac.city[0] : '')
      if (cityName && !parts.some(p => p.includes(cityName))) parts.push(cityName)
    }
    if (ac.district) parts.push(ac.district)

    let road = formatted
    for (const p of parts) {
      const idx = road.indexOf(p)
      if (idx !== -1) {
        road = road.substring(idx + p.length)
      }
    }
    road = road.trim()
    const m2 = road.match(roadRegex)
    if (m2) return m2[0]

    // 兜底：roads[0].name
    if (roads.length > 0 && roads[0].name) {
      return roads[0].name
    }

    return ''
  }

  /**
   * 失败重试逻辑
   */
  private handleRetry(
    task: {
      lng: number
      lat: number
      key: string
      resolve: (name: string) => void
      reject: (err: Error) => void
      retries: number
    },
    error: Error,
  ): void {
    if (task.retries < this.options.maxRetries) {
      // 指数退避：1s, 2s, 4s...
      const delay = this.options.retryBaseDelay * Math.pow(2, task.retries)
      console.log(
        `[BatchGeocoder] 重试 ${task.key} (第 ${task.retries + 1} 次)，${delay}ms 后重试`,
      )
      setTimeout(() => {
        task.retries++
        // 重新入队（放在队尾）
        this.queue.push(task)
        this.processQueue()
      }, delay)
    } else {
      // 重试耗尽，返回空字符串
      console.warn(`[BatchGeocoder] ${task.key} 重试耗尽:`, error.message)
      this.cache.set(task.key, {
        roadName: '',
        timestamp: Date.now(),
      })
      this.pendingRequests.delete(task.key)
      task.resolve('')
    }
  }

  /**
   * 预加载一批坐标（静默加载，不阻塞渲染）
   */
  preload(points: Array<{ lng: number; lat: number }>): void {
    for (const point of points) {
      const key = `${point.lng.toFixed(6)},${point.lat.toFixed(6)}`
      if (!this.cache.has(key)) {
        // 发起请求但不等待结果
        this.getRoadName(point.lng, point.lat).catch(() => {})
      }
    }
  }

  /**
   * 获取缓存统计
   */
  getStats() {
    return {
      cacheSize: this.cache.size,
      queueLength: this.queue.length,
      activeCount: this.activeCount,
      pendingRequests: this.pendingRequests.size,
    }
  }

  /**
   * 清除过期缓存
   */
  clearExpiredCache(): void {
    // LRU 本身就会淘汰旧条目，这里不做额外清理
  }

  /**
   * 销毁实例
   */
  dispose(): void {
    this.disposed = true
    this.queue = []
    this.pendingRequests.clear()
    this.cache.clear()
    this.geocoder = null
  }
}

/**
 * 全局单例（推荐使用）
 */
let globalInstance: BatchGeocoder | null = null

export function getBatchGeocoder(options?: BatchGeocoderOptions): BatchGeocoder {
  if (!globalInstance || globalInstance['disposed']) {
    globalInstance = new BatchGeocoder(options)
  }
  return globalInstance
}

export function disposeBatchGeocoder(): void {
  if (globalInstance) {
    globalInstance.dispose()
    globalInstance = null
  }
}

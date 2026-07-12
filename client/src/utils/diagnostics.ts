/**
 * 道路健康度数据一致性诊断工具
 *
 * 使用方式：在浏览器 DevTools Console 中执行
 *   import('./src/utils/diagnostics.ts').then(m => m.runDiagnostics())
 *
 * 或者直接在页面组件中调用：
 *   import { runDiagnostics } from '@/utils/diagnostics'
 *   runDiagnostics()
 */

interface DiagnosticResult {
  section: string
  status: 'pass' | 'warn' | 'fail'
  detail: string
}

interface RoadItem {
  roadId: number
  roadName: string
  totalCount: number
  highCount: number
  mediumCount: number
  lowCount: number
  overallSeverity: string
  diseasePoints: DiseasePoint[]
  centerLng?: number
  centerLat?: number
  _roadNameReady?: boolean
}

interface DiseasePoint {
  lng: number
  lat: number
  severity: string
  damageType: string
  detectionTime?: string
  workOrderNo?: string
}

/**
 * 运行完整诊断
 */
export async function runDiagnostics(): Promise<DiagnosticResult[]> {
  const results: DiagnosticResult[] = []

  console.group('%c 道路健康度数据一致性诊断 %c',
    'background:#4361ee;color:#fff;padding:4px 8px;border-radius:4px',
    'background:transparent')

  // ---- 1. 后端数据源诊断 ----
  console.log('%c[1/5] 检查后端 roadsWithDisease 接口数据...', 'font-weight:bold;color:#4361ee')
  try {
    const res = await fetch('/api/map/roads-with-disease')
    const json = await res.json()
    const roads: RoadItem[] = json.data || []

    const unknownRoads = roads.filter(r => !r.roadName || r.roadName === '未知道路')
    const knownRoads = roads.filter(r => r.roadName && r.roadName !== '未知道路')
    const negativeIdRoads = roads.filter(r => r.roadId < 0)
    const zeroIdRoads = roads.filter(r => r.roadId === 0)

    console.table({
      '道路总数': roads.length,
      '已知道路 (有roadName)': knownRoads.length,
      '未知道路 (roadName为空/未知道路)': unknownRoads.length,
      'roadId=0 (未关联)': zeroIdRoads.length,
      'roadId<0 (前端动态分组)': negativeIdRoads.length,
    })

    if (unknownRoads.length > 0) {
      console.warn('%c⚠ 后端返回 %d 条"未知道路"',
        'color:#f59e0b;font-weight:bold', unknownRoads.length)
      console.log('未知道路详情:', unknownRoads.map(r => ({
        roadId: r.roadId,
        totalCount: r.totalCount,
        centerLng: r.centerLng,
        centerLat: r.centerLat,
        diseasePointCount: r.diseasePoints?.length || 0,
        firstDpCoord: r.diseasePoints?.[0]
          ? `${r.diseasePoints[0].lng},${r.diseasePoints[0].lat}`
          : 'N/A',
      })))
      results.push({
        section: '后端数据源',
        status: 'warn',
        detail: `后端返回 ${unknownRoads.length} 条"未知道路"（共 ${roads.length} 条），需要前端逆地理兜底`,
      })
    } else {
      console.log('%c✓ 后端所有道路已关联 road 表', 'color:#22c55e')
      results.push({
        section: '后端数据源',
        status: 'pass',
        detail: `所有 ${roads.length} 条道路均已关联 road 表`,
      })
    }

    // 检查病害点总数
    let totalDps = 0
    for (const road of roads) {
      totalDps += road.diseasePoints?.length || 0
    }
    console.log(`病害点总数: ${totalDps}`)

  } catch (e) {
    console.error('接口调用失败:', e)
    results.push({
      section: '后端数据源',
      status: 'fail',
      detail: `API /api/map/roads-with-disease 调用失败: ${e}`,
    })
  }

  // ---- 2. 数据库 road_id 关联诊断 ----
  console.log('%c[2/5] 检查数据库 road_id 关联状态...', 'font-weight:bold;color:#4361ee')
  try {
    const taskRes = await fetch('/api/map/statistics')
    const taskJson = await taskRes.json()
    const stats = taskJson.data || {}

    console.table({
      '标记总数': stats.totalMarkers || 0,
      '新增标记': stats.newMarkers || 0,
      '已修复': stats.repairedCount || 0,
      '待修复': stats.pendingRepair || 0,
      '严重(HIGH)': stats.highCount || 0,
      '中等(MEDIUM)': stats.mediumCount || 0,
      '轻微(LOW)': stats.lowCount || 0,
    })
  } catch (e) {
    console.warn('统计接口调用失败:', e)
  }

  // ---- 3. 前端逆地理缓存诊断 ----
  console.log('%c[3/5] 检查前端逆地理缓存状态...', 'font-weight:bold;color:#4361ee')
  const win = window as any
  if (win.__batchGeocoder) {
    const stats = win.__batchGeocoder.getStats()
    console.table({
      '缓存条目数': stats.cacheSize,
      '活跃请求数': stats.activeCount,
      '队列长度': stats.queueLength,
      '已处理总数': stats.totalProcessed,
      '成功率': stats.totalProcessed > 0
        ? `${((stats.totalProcessed - stats.totalFailed) / stats.totalProcessed * 100).toFixed(1)}%`
        : 'N/A',
    })
    results.push({
      section: '逆地理缓存',
      status: stats.cacheSize > 0 ? 'pass' : 'warn',
      detail: `缓存 ${stats.cacheSize} 条，已处理 ${stats.totalProcessed} 个请求`,
    })
  } else {
    console.warn('%c⚠ batchGeocoder 未挂载到 window.__batchGeocoder', 'color:#f59e0b')
    results.push({
      section: '逆地理缓存',
      status: 'warn',
      detail: 'batchGeocoder 实例未暴露到 window，无法检查缓存状态',
    })
  }

  // ---- 4. 前端 roadDiseaseData 响应式数据诊断 ----
  console.log('%c[4/5] 检查前端响应式数据状态...', 'font-weight:bold;color:#4361ee')
  if (win.__roadDiseaseData) {
    const rd = win.__roadDiseaseData
    const unknownCount = rd.filter((r: RoadItem) => !r.roadName || r.roadName === '未知道路').length
    const resolvedCount = rd.filter((r: RoadItem) => r.roadName && r.roadName !== '未知道路').length
    console.table({
      'roadDiseaseData 条目数': rd.length,
      '已解析路名': resolvedCount,
      '仍未知道路': unknownCount,
      'roadNameResolved': win.__roadNameResolved ? 'true' : 'false',
    })

    if (unknownCount > 0) {
      console.warn('%c⚠ 前端仍有 %d 条"未知道路"未被逆地理兜底', 'color:#f59e0b', unknownCount)
      results.push({
        section: '前端数据状态',
        status: 'warn',
        detail: `roadDiseaseData 中仍有 ${unknownCount} 条"未知道路"，逆地理兜底可能失败`,
      })
    } else {
      results.push({
        section: '前端数据状态',
        status: 'pass',
        detail: `所有 ${rd.length} 条道路已解析路名`,
      })
    }
  } else {
    console.warn('%c⚠ roadDiseaseData 未挂载到 window', 'color:#f59e0b')
    results.push({
      section: '前端数据状态',
      status: 'warn',
      detail: 'roadDiseaseData 未暴露到 window，无法检查',
    })
  }

  // ---- 5. 高德逆地理 API 可用性诊断 ----
  console.log('%c[5/5] 检查高德逆地理 API 可用性...', 'font-weight:bold;color:#4361ee')
  if (win.AMap && win.AMap.Geocoder) {
    console.log('%c✓ AMap.Geocoder 可用', 'color:#22c55e')
    results.push({
      section: '高德 API',
      status: 'pass',
      detail: 'AMap.Geocoder 已加载',
    })
  } else if (win.AMap) {
    console.warn('%c⚠ AMap 已加载但 Geocoder 未初始化', 'color:#f59e0b')
    results.push({
      section: '高德 API',
      status: 'warn',
      detail: 'AMap 已加载但 Geocoder 未初始化，需调用 batchGeocoder.init()',
    })
  } else {
    console.error('%c✗ AMap 未加载，请检查高德地图 JS API 引入', 'color:#ef4444')
    results.push({
      section: '高德 API',
      status: 'fail',
      detail: 'AMap 未加载',
    })
  }

  // ---- 汇总 ----
  console.log('%c---- 诊断汇总 ----', 'font-weight:bold;font-size:14px')
  const failCount = results.filter(r => r.status === 'fail').length
  const warnCount = results.filter(r => r.status === 'warn').length
  const passCount = results.filter(r => r.status === 'pass').length

  console.log(`通过: ${passCount} | 警告: ${warnCount} | 失败: ${failCount}`)
  for (const r of results) {
    const icon = r.status === 'pass' ? '✓' : r.status === 'warn' ? '⚠' : '✗'
    const color = r.status === 'pass' ? '#22c55e' : r.status === 'warn' ? '#f59e0b' : '#ef4444'
    console.log(`%c${icon} [${r.section}] ${r.detail}`, `color:${color}`)
  }

  console.groupEnd()
  return results
}

/**
 * 快速校验：检查指定坐标的逆地理缓存命中情况
 */
export function checkGeocodeCache(points: Array<{ lng: number; lat: number }>): void {
  const win = window as any
  if (!win.__batchGeocoder) {
    console.warn('batchGeocoder 未挂载到 window')
    return
  }

  const stats = win.__batchGeocoder.getStats()
  let hit = 0
  let miss = 0

  console.group('逆地理缓存命中检查')
  for (const p of points) {
    const key = `${p.lng.toFixed(6)},${p.lat.toFixed(6)}`
    const cached = stats.cacheKeys?.has(key)
    if (cached) {
      hit++
    } else {
      miss++
      console.log(`未命中: ${key}`)
    }
  }
  console.log(`命中: ${hit} | 未命中: ${miss} | 命中率: ${(hit / (hit + miss) * 100).toFixed(1)}%`)
  console.groupEnd()
}

/**
 * 导出：在 DataScreen.vue 的 onMounted 中挂载调试对象到 window
 * 用法：在 initMap() 或 onMounted 中添加：
 *   (window as any).__batchGeocoder = batchGeocoder
 *   (window as any).__roadDiseaseData = roadDiseaseData
 *   (window as any).__roadNameResolved = roadNameResolved
 */

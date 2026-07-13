/**
 * DiseaseMockData.ts
 * ===================
 * 病害标记线模拟数据 (RoadDiseaseSummaryResponse[])
 *
 * 后端对接说明：
 * ──────────────────────────────────────────────────────
 * 接口： GET /map/roads-with-disease
 * 返回： ApiResponse<RoadDiseaseSummaryResponse[]>
 *
 * RoadDiseaseSummaryResponse 结构：
 * {
 *   roadId: number
 *   roadName: string
 *   centerLng: number
 *   centerLat: number
 *   pathPoints: number[][]    // 道路路径经纬度坐标数组 [[lng,lat], ...]
 *   totalCount: number
 *   highCount: number
 *   mediumCount: number
 *   lowCount: number
 *   overallSeverity: string   // HIGH | MEDIUM | LOW
 *   diseasePoints: DiseasePoint[]
 * }
 *
 * DiseasePoint 结构：
 * {
 *   taskId: number
 *   lng: number
 *   lat: number
 *   damageType: string       // POTHOLE(重度破损) | CRACK(中度裂缝) | MARKING_DAMAGE(细微裂纹)
 *   severity: string         // HIGH(严重) | MEDIUM(中等) | LOW(轻微)
 *   confidence: number
 *   detectionTime: string
 *   address: string
 *   workOrderNo: string
 *   area?: number            // 破损面积 (m²)
 * }
 *
 * 颜色映射规则：
 *   POTHOLE / 重度破损 → #ef4444 (红色)    → 线宽：HIGH=2px, MEDIUM=1.5px, LOW=1px
 *   CRACK   / 中度裂缝 → #ec4899 (粉色)
 *   MARKING_DAMAGE / 细微裂纹 → #3b82f6 (蓝色)
 */

import type { RoadDiseaseSummaryResponse, DiseasePoint } from '@/types'

/** 生成某个经纬度附近的随机点 */
function jitter(lng: number, lat: number, range: number = 0.0008): [number, number] {
  return [
    lng + (Math.random() - 0.5) * range,
    lat + (Math.random() - 0.5) * range,
  ]
}

/** 随机病害类型 */
function randomDamageType(): string {
  const types = ['POTHOLE', 'CRACK', 'MARKING_DAMAGE']
  return types[Math.floor(Math.random() * types.length)]
}

/** 随机严重等级（带权重） */
function randomSeverity(): string {
  const r = Math.random()
  if (r < 0.35) return 'LOW'
  if (r < 0.7) return 'MEDIUM'
  return 'HIGH'
}

/** 生成一段道路路径（贝塞尔曲线采样得到的经纬度点） */
function generateRoadPath(
  startLng: number, startLat: number,
  endLng: number, endLat: number,
  numPoints: number = 20,
  curveOffset: number = 0.002
): number[][] {
  const points: number[][] = []
  for (let i = 0; i < numPoints; i++) {
    const t = i / (numPoints - 1)
    // 线性插值 + 正弦扰动模拟道路弯曲
    const lng = startLng + (endLng - startLng) * t + Math.sin(t * Math.PI * 3) * curveOffset
    const lat = startLat + (endLat - startLat) * t + Math.cos(t * Math.PI * 2) * curveOffset * 0.6
    points.push([lng, lat])
  }
  return points
}

/** 生成一条道路上的一组病害点 */
function generateDiseasePoints(
  roadPath: number[][],
  count: number,
  roadName: string
): DiseasePoint[] {
  const points: DiseasePoint[] = []
  for (let i = 0; i < count; i++) {
    // 在道路路径上随机选取位置
    const segIdx = Math.floor(Math.random() * (roadPath.length - 1))
    const t = Math.random()
    const p1 = roadPath[segIdx]
    const p2 = roadPath[segIdx + 1]
    const lng = p1[0] + (p2[0] - p1[0]) * t + (Math.random() - 0.5) * 0.0006
    const lat = p1[1] + (p2[1] - p1[1]) * t + (Math.random() - 0.5) * 0.0005
    const damageType = randomDamageType()
    const severity = randomSeverity()
    points.push({
      taskId: 1000 + i,
      lng, lat,
      damageType,
      severity,
      confidence: 0.75 + Math.random() * 0.24,
      detectionTime: '2026-07-' + String(1 + Math.floor(Math.random() * 10)).padStart(2, '0') + ' ' +
                     String(8 + Math.floor(Math.random() * 10)).padStart(2, '0') + ':' +
                     String(Math.floor(Math.random() * 60)).padStart(2, '0'),
      address: roadName + ' K' + (Math.floor(Math.random() * 50) + 1) + '+' + Math.floor(Math.random() * 100),
      workOrderNo: i % 3 === 0 ? 'WO-2026' + String(10000 + i) : '',
      area: Math.round((0.5 + Math.random() * 4.5) * 100) / 100,
    })
  }
  return points
}

/**
 * 生成 6 条主要道路的病害数据（北京城区路网模拟）
 * 坐标范围：东城区/朝阳区 116.38~116.48, 39.90~39.97
 */
export function generateMockDiseaseData(): RoadDiseaseSummaryResponse[] {
  const roads: RoadDiseaseSummaryResponse[] = []

  // 道路 1：长安街（东段）
  const path1 = generateRoadPath(116.385, 39.907, 116.460, 39.908, 25, 0.001)
  const dp1 = generateDiseasePoints(path1, 15, '长安街')
  roads.push({
    roadId: 1, roadName: '长安街', centerLng: 116.422, centerLat: 39.907, pathPoints: path1,
    totalCount: dp1.length, highCount: dp1.filter(d => d.severity === 'HIGH').length,
    mediumCount: dp1.filter(d => d.severity === 'MEDIUM').length,
    lowCount: dp1.filter(d => d.severity === 'LOW').length,
    overallSeverity: dp1.some(d => d.severity === 'HIGH') ? 'HIGH' : 'MEDIUM',
    diseasePoints: dp1,
  })

  // 道路 2：二环路（东二环段）
  const path2 = generateRoadPath(116.430, 39.915, 116.435, 39.960, 22, 0.0015)
  const dp2 = generateDiseasePoints(path2, 20, '东二环路')
  roads.push({
    roadId: 2, roadName: '东二环路', centerLng: 116.433, centerLat: 39.938, pathPoints: path2,
    totalCount: dp2.length, highCount: dp2.filter(d => d.severity === 'HIGH').length,
    mediumCount: dp2.filter(d => d.severity === 'MEDIUM').length,
    lowCount: dp2.filter(d => d.severity === 'LOW').length,
    overallSeverity: 'HIGH',
    diseasePoints: dp2,
  })

  // 道路 3：三环路（东三段）
  const path3 = generateRoadPath(116.445, 39.920, 116.450, 39.965, 20, 0.0012)
  const dp3 = generateDiseasePoints(path3, 25, '东三环路')
  roads.push({
    roadId: 3, roadName: '东三环路', centerLng: 116.448, centerLat: 39.942, pathPoints: path3,
    totalCount: dp3.length, highCount: dp3.filter(d => d.severity === 'HIGH').length,
    mediumCount: dp3.filter(d => d.severity === 'MEDIUM').length,
    lowCount: dp3.filter(d => d.severity === 'LOW').length,
    overallSeverity: 'MEDIUM',
    diseasePoints: dp3,
  })

  // 道路 4：朝阳路
  const path4 = generateRoadPath(116.400, 39.920, 116.480, 39.925, 18, 0.0018)
  const dp4 = generateDiseasePoints(path4, 18, '朝阳路')
  roads.push({
    roadId: 4, roadName: '朝阳路', centerLng: 116.440, centerLat: 39.923, pathPoints: path4,
    totalCount: dp4.length, highCount: dp4.filter(d => d.severity === 'HIGH').length,
    mediumCount: dp4.filter(d => d.severity === 'MEDIUM').length,
    lowCount: dp4.filter(d => d.severity === 'LOW').length,
    overallSeverity: 'MEDIUM',
    diseasePoints: dp4,
  })

  // 道路 5：建国路
  const path5 = generateRoadPath(116.395, 39.910, 116.478, 39.912, 20, 0.0008)
  const dp5 = generateDiseasePoints(path5, 12, '建国路')
  roads.push({
    roadId: 5, roadName: '建国路', centerLng: 116.436, centerLat: 39.911, pathPoints: path5,
    totalCount: dp5.length, highCount: dp5.filter(d => d.severity === 'HIGH').length,
    mediumCount: dp5.filter(d => d.severity === 'MEDIUM').length,
    lowCount: dp5.filter(d => d.severity === 'LOW').length,
    overallSeverity: 'LOW',
    diseasePoints: dp5,
  })

  // 道路 6：通惠河北路
  const path6 = generateRoadPath(116.410, 39.900, 116.475, 39.896, 16, 0.0022)
  const dp6 = generateDiseasePoints(path6, 10, '通惠河北路')
  roads.push({
    roadId: 6, roadName: '通惠河北路', centerLng: 116.442, centerLat: 39.898, pathPoints: path6,
    totalCount: dp6.length, highCount: dp6.filter(d => d.severity === 'HIGH').length,
    mediumCount: dp6.filter(d => d.severity === 'MEDIUM').length,
    lowCount: dp6.filter(d => d.severity === 'LOW').length,
    overallSeverity: 'LOW',
    diseasePoints: dp6,
  })

  return roads
}

/**
 * 后端返回 JSON 示例（直接粘贴可用）
 * 
 * {
 *   "code": 200,
 *   "message": "success",
 *   "data": [
 *     {
 *       "roadId": 1,
 *       "roadName": "长安街",
 *       "centerLng": 116.422,
 *       "centerLat": 39.907,
 *       "pathPoints": [[116.385,39.907],[116.390,39.907],...],
 *       "totalCount": 15,
 *       "highCount": 5,
 *       "mediumCount": 7,
 *       "lowCount": 3,
 *       "overallSeverity": "HIGH",
 *       "diseasePoints": [
 *         {
 *           "taskId": 1001,
 *           "lng": 116.388,
 *           "lat": 39.907,
 *           "damageType": "POTHOLE",
 *           "severity": "HIGH",
 *           "confidence": 0.92,
 *           "detectionTime": "2026-07-05 09:30",
 *           "address": "长安街 K12+300",
 *           "workOrderNo": "WO-20260001",
 *           "area": 3.25
 *         }
 *       ]
 *     }
 *   ]
 * }
 */

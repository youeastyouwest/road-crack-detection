/**
 * RoadHeatmapMockData.ts
 */

import type { RoadDiseaseSummaryResponse, DiseasePoint } from '@/types'

export const SEVERITY_WEIGHTS: Record<string, number> = {
  LOW: 0.25, MEDIUM: 0.50, HIGH: 0.75, CRITICAL: 1.0,
}

export const HEAT_COLORS: Record<string, string> = {
  LOW: '#FDE047', MEDIUM: '#F97316', HIGH: '#DC2626', CRITICAL: '#7F1D1D',
}

export function weightToColor(weight: number): string {
  const c = Math.max(0, Math.min(1, weight))
  if (c <= 0.25) return lerpColor('#f8fafc', '#FDE047', c / 0.25)
  if (c <= 0.5) return lerpColor('#FDE047', '#F97316', (c - 0.25) / 0.25)
  if (c <= 0.75) return lerpColor('#F97316', '#DC2626', (c - 0.5) / 0.25)
  return lerpColor('#DC2626', '#7F1D1D', (c - 0.75) / 0.25)
}

function lerpColor(c1: string, c2: string, t: number): string {
  const r1 = parseInt(c1.slice(1, 3), 16); const g1 = parseInt(c1.slice(3, 5), 16); const b1 = parseInt(c1.slice(5, 7), 16)
  const r2 = parseInt(c2.slice(1, 3), 16); const g2 = parseInt(c2.slice(3, 5), 16); const b2 = parseInt(c2.slice(5, 7), 16)
  return 'rgb(' + Math.round(r1 + (r2 - r1) * t) + ',' + Math.round(g1 + (g2 - g1) * t) + ',' + Math.round(b1 + (b2 - b1) * t) + ')'
}

function randomDamageType(): string {
  return ['POTHOLE','CRACK','MARKING_DAMAGE','ROAD_SPILL'][Math.floor(Math.random() * 4)]
}

function generateRoadPath(sLng: number, sLat: number, eLng: number, eLat: number, n: number = 25, off: number = 0.002): number[][] {
  const pts: number[][] = []
  for (let i = 0; i < n; i++) {
    const t = i / (n - 1)
    pts.push([Math.round((sLng + (eLng - sLng) * t + Math.sin(t * Math.PI * 3.5) * off) * 1e6) / 1e6, Math.round((sLat + (eLat - sLat) * t + Math.cos(t * Math.PI * 2.5) * off * 0.5) * 1e6) / 1e6])
  }
  return pts
}

function generateDiseasePoints(path: number[][], count: number, name: string, bias: number = 0): DiseasePoint[] {
  const pts: DiseasePoint[] = []
  for (let i = 0; i < count; i++) {
    const si = Math.floor(Math.random() * (path.length - 1))
    const t = Math.random()
    const sr = Math.random() + bias
    let sev: string
    if (sr < 0.25) sev = 'LOW'
    else if (sr < 0.55) sev = 'MEDIUM'
    else if (sr < 0.85) sev = 'HIGH'
    else sev = 'CRITICAL'
    const d = Math.floor(Math.random() * 10) + 1
    const h = 8 + Math.floor(Math.random() * 10)
    const m = Math.floor(Math.random() * 60)
    pts.push({ taskId: 2000 + i, lng: Math.round((path[si][0] + (path[si+1][0] - path[si][0]) * t + (Math.random() - 0.5) * 0.0004) * 1e6) / 1e6, lat: Math.round((path[si][1] + (path[si+1][1] - path[si][1]) * t + (Math.random() - 0.5) * 0.0004) * 1e6) / 1e6, damageType: randomDamageType(), severity: sev, confidence: 0.75 + Math.random() * 0.24, detectionTime: '2026-07-0' + d + ' ' + h + ':' + m, address: name + ' K' + (Math.floor(Math.random() * 50) + 1) + '+' + Math.floor(Math.random() * 100), workOrderNo: i % 3 === 0 ? 'WO-2026' + (20000 + i) : '' })
  }
  return pts
}

export interface RoadSegmentSummary {
  roadId: number; roadName: string; totalCount: number
  lowCount: number; mediumCount: number; highCount: number; criticalCount: number
  avgWeight: number; damageTypeBreakdown: Record<string, number>
}

export function generateRoadSummaries(roads: RoadDiseaseSummaryResponse[]): RoadSegmentSummary[] {
  return roads.map(r => {
    const dps = r.diseasePoints || []
    const bd: Record<string, number> = {}
    let tw = 0, lw = 0, md = 0, hg = 0, cr = 0
    dps.forEach(d => {
      if (d.severity === 'LOW') lw++; else if (d.severity === 'MEDIUM') md++; else if (d.severity === 'HIGH') hg++; else if (d.severity === 'CRITICAL') cr++
      tw += (SEVERITY_WEIGHTS[d.severity] || 0.25)
      bd[d.damageType] = (bd[d.damageType] || 0) + 1
    })
    return { roadId: r.roadId, roadName: r.roadName, totalCount: dps.length, lowCount: lw, mediumCount: md, highCount: hg, criticalCount: cr, avgWeight: dps.length > 0 ? tw / dps.length : 0, damageTypeBreakdown: bd }
  })
}

export function generateHeatmapMockData(): RoadDiseaseSummaryResponse[] {
  const roads: RoadDiseaseSummaryResponse[] = []
  const p1 = generateRoadPath(116.380, 39.907, 116.465, 39.908, 30, 0.0008)
  roads.push({ roadId: 1, roadName: '长安街', centerLng: 116.422, centerLat: 39.9075, pathPoints: p1, totalCount: 12, highCount: 2, mediumCount: 4, lowCount: 6, overallSeverity: 'MEDIUM', diseasePoints: generateDiseasePoints(p1, 12, '长安街', -0.1) })
  const p2 = generateRoadPath(116.430, 39.912, 116.435, 39.965, 28, 0.0012)
  roads.push({ roadId: 2, roadName: '东二环路', centerLng: 116.433, centerLat: 39.938, pathPoints: p2, totalCount: 18, highCount: 5, mediumCount: 7, lowCount: 6, overallSeverity: 'HIGH', diseasePoints: generateDiseasePoints(p2, 18, '东二环路', 0) })
  const p3 = generateRoadPath(116.445, 39.918, 116.450, 39.968, 26, 0.0010)
  roads.push({ roadId: 3, roadName: '东三环路', centerLng: 116.448, centerLat: 39.943, pathPoints: p3, totalCount: 22, highCount: 8, mediumCount: 8, lowCount: 6, overallSeverity: 'MEDIUM', diseasePoints: generateDiseasePoints(p3, 22, '东三环路', 0.15) })
  const p4 = generateRoadPath(116.395, 39.920, 116.482, 39.926, 22, 0.0020)
  roads.push({ roadId: 4, roadName: '朝阳路', centerLng: 116.438, centerLat: 39.923, pathPoints: p4, totalCount: 15, highCount: 3, mediumCount: 5, lowCount: 7, overallSeverity: 'LOW', diseasePoints: generateDiseasePoints(p4, 15, '朝阳路', -0.2) })
  const p5 = generateRoadPath(116.390, 39.910, 116.480, 39.913, 24, 0.0006)
  roads.push({ roadId: 5, roadName: '建国路', centerLng: 116.435, centerLat: 39.9115, pathPoints: p5, totalCount: 10, highCount: 1, mediumCount: 3, lowCount: 6, overallSeverity: 'LOW', diseasePoints: generateDiseasePoints(p5, 10, '建国路', -0.3) })
  const p6 = generateRoadPath(116.405, 39.898, 116.478, 39.894, 20, 0.0025)
  roads.push({ roadId: 6, roadName: '通惠河北路', centerLng: 116.441, centerLat: 39.896, pathPoints: p6, totalCount: 14, highCount: 5, mediumCount: 5, lowCount: 4, overallSeverity: 'MEDIUM', diseasePoints: generateDiseasePoints(p6, 14, '通惠河北路', 0.1) })
  const p7 = generateRoadPath(116.415, 39.948, 116.445, 39.955, 20, 0.0030)
  roads.push({ roadId: 7, roadName: '北二环路', centerLng: 116.430, centerLat: 39.951, pathPoints: p7, totalCount: 20, highCount: 9, mediumCount: 6, lowCount: 5, overallSeverity: 'HIGH', diseasePoints: generateDiseasePoints(p7, 20, '北二环路', 0.3) })
  const p8 = generateRoadPath(116.468, 39.930, 116.475, 39.970, 22, 0.0018)
  roads.push({ roadId: 8, roadName: '东四环北路', centerLng: 116.472, centerLat: 39.950, pathPoints: p8, totalCount: 25, highCount: 12, mediumCount: 8, lowCount: 5, overallSeverity: 'HIGH', diseasePoints: generateDiseasePoints(p8, 25, '东四环北路', 0.35) })
  return roads
}

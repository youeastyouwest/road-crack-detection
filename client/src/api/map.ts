import http from "./index"
import type { ApiResponse, MapMarkerResponse, RoadDiseaseSummaryResponse, MapMarkerDetailResponse, MapStatisticsResponse, MapTrendPointResponse, MapDamageTypeRatioResponse, DamageType, SeverityLevel } from "@/types"

export const mapApi = {
  markers(params?: { damageType?: DamageType; severityLevel?: SeverityLevel; status?: string; hasWorkOrder?: boolean; onlyWithCoordinates?: boolean; keyword?: string }) {
    return http.get<ApiResponse<MapMarkerResponse[]>>("/map/markers", { params })
  },
  markerDetail(markerId: number) {
    return http.get<ApiResponse<MapMarkerDetailResponse>>("/map/markers/" + markerId)
  },
  statistics() {
    return http.get<ApiResponse<MapStatisticsResponse>>("/map/statistics")
  },
  trend(days?: number) {
    return http.get<ApiResponse<MapTrendPointResponse[]>>("/map/trend", { params: { days: days || 7 } })
  },
  roadsWithDisease() {
    return http.get<ApiResponse<RoadDiseaseSummaryResponse[]>>("/map/roads-with-disease")
  },

  damageTypes() {
    return http.get<ApiResponse<MapDamageTypeRatioResponse[]>>("/map/damage-types")
  },
}

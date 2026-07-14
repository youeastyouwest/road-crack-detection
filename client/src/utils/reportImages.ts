export function resolveReportImageUrl(url?: string): string {
  if (!url) return ""

  const trimmed = url.trim()
  if (!trimmed) return ""

  if (trimmed.startsWith("/api/file/download/")) {
    return "/uploads/" + trimmed.replace("/api/file/download/", "")
  }

  if (trimmed.startsWith("/api/")) {
    return `${window.location.origin}${trimmed}`
  }

  return trimmed
}

export function splitReportImageUrls(urlStr?: string): string[] {
  if (!urlStr) return []
  return urlStr
    .split(",")
    .map(url => resolveReportImageUrl(url))
    .filter(Boolean)
}

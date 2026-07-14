const AUTH_STORAGE_KEYS = [
  "accessToken",
  "refreshToken",
  "userId",
  "username",
  "realName",
  "roles",
] as const

type AuthStorageKey = typeof AUTH_STORAGE_KEYS[number]

function hasWindow() {
  return typeof window !== "undefined"
}

function getSessionStorage() {
  return hasWindow() ? window.sessionStorage : null
}

function getLegacyStorage() {
  return hasWindow() ? window.localStorage : null
}

export function migrateLegacyAuthStorage() {
  const sessionStorage = getSessionStorage()
  const legacyStorage = getLegacyStorage()
  if (!sessionStorage || !legacyStorage) return

  AUTH_STORAGE_KEYS.forEach((key) => {
    if (sessionStorage.getItem(key) !== null) return
    const legacyValue = legacyStorage.getItem(key)
    if (legacyValue !== null) {
      sessionStorage.setItem(key, legacyValue)
      legacyStorage.removeItem(key)
    }
  })
}

export function getAuthItem(key: AuthStorageKey) {
  migrateLegacyAuthStorage()
  const sessionStorage = getSessionStorage()
  if (sessionStorage) {
    const value = sessionStorage.getItem(key)
    if (value !== null) return value
  }
  return getLegacyStorage()?.getItem(key) ?? null
}

export function setAuthItem(key: AuthStorageKey, value: string) {
  const sessionStorage = getSessionStorage()
  const legacyStorage = getLegacyStorage()
  sessionStorage?.setItem(key, value)
  legacyStorage?.removeItem(key)
}

export function clearAuthStorage() {
  const sessionStorage = getSessionStorage()
  const legacyStorage = getLegacyStorage()
  AUTH_STORAGE_KEYS.forEach((key) => {
    sessionStorage?.removeItem(key)
    legacyStorage?.removeItem(key)
  })
}

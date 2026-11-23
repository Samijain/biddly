/**
 * The current access token, in memory only: never localStorage, where any injected script could copy it.
 * A page reload forgets it; features/auth/session.ts gets a new one with the refresh cookie.
 */
let accessToken: string | null = null

export function getAccessToken(): string | null {
  return accessToken
}

export function setAccessToken(token: string | null): void {
  accessToken = token
}

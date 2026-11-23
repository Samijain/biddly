import { setAccessToken } from '../../lib/accessToken'
import * as authApi from './api'
import type { Account, LoginInput, TokenResponse } from './types'

export type AuthStatus = 'loading' | 'signedIn' | 'signedOut'

export interface AuthState {
  status: AuthStatus
  account: Account | null
}

/** Refresh this long before the access token expires, so requests never carry an expired one. */
const REFRESH_MARGIN_MS = 60_000

let state: AuthState = { status: 'loading', account: null }
const listeners = new Set<() => void>()
let refreshTimer: ReturnType<typeof setTimeout> | undefined
let refreshInFlight: Promise<void> | null = null

export function getAuthState(): AuthState {
  return state
}

export function subscribe(listener: () => void): () => void {
  listeners.add(listener)
  return () => listeners.delete(listener)
}

function setState(next: AuthState) {
  state = next
  listeners.forEach((listener) => listener())
}

function startSession(response: TokenResponse) {
  setAccessToken(response.accessToken)
  setState({ status: 'signedIn', account: response.account })
  clearTimeout(refreshTimer)
  const delay = Math.max(new Date(response.expiresAt).getTime() - Date.now() - REFRESH_MARGIN_MS, 0)
  refreshTimer = setTimeout(() => void refreshSession(), delay)
}

function endSession() {
  setAccessToken(null)
  clearTimeout(refreshTimer)
  setState({ status: 'signedOut', account: null })
}

/**
 * Swaps the refresh cookie for a new access token. Only one refresh runs at a time: the server rotates the
 * cookie, so a second request with the old one would look like a stolen token and end the whole session.
 * (React's StrictMode runs effects twice in development, which is exactly that.)
 */
export function refreshSession(): Promise<void> {
  refreshInFlight ??= authApi
    .refresh()
    .then(startSession, endSession)
    .finally(() => {
      refreshInFlight = null
    })
  return refreshInFlight
}

/** On page load: a refresh cookie from an earlier visit signs the user straight back in. */
export function restoreSession(): Promise<void> {
  return state.status === 'loading' ? refreshSession() : Promise.resolve()
}

export async function signIn(input: LoginInput): Promise<Account> {
  const response = await authApi.login(input)
  startSession(response)
  return response.account
}

/** Signed out locally even if the server can't be reached; the cookie then simply expires. */
export async function signOut(): Promise<void> {
  try {
    await authApi.logout()
  } finally {
    endSession()
  }
}

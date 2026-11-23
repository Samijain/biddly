import { http } from '../../lib/http'
import type { Account, LoginInput, RegisterInput, TokenResponse } from './types'

export async function register(input: RegisterInput): Promise<Account> {
  return http<Account>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(input), anonymous: true })
}

export async function login(input: LoginInput): Promise<TokenResponse> {
  return http<TokenResponse>('/api/v1/auth/login', { method: 'POST', body: JSON.stringify(input), anonymous: true })
}

/** The browser sends the HttpOnly refresh cookie by itself; JavaScript never sees it. */
export async function refresh(): Promise<TokenResponse> {
  return http<TokenResponse>('/api/v1/auth/refresh', { method: 'POST', anonymous: true })
}

export async function logout(): Promise<void> {
  return http<void>('/api/v1/auth/logout', { method: 'POST', anonymous: true })
}

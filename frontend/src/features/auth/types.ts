/** Mirrors com.biddly.auth.dto.AccountResponse. */
export interface Account {
  id: number
  displayName: string
  email: string
  role: 'USER' | 'ADMIN'
  createdAt: string
}

/** Mirrors com.biddly.auth.dto.RegisterRequest. */
export interface RegisterInput {
  displayName: string
  email: string
  password: string
}

/** Mirrors com.biddly.auth.dto.LoginRequest. */
export interface LoginInput {
  email: string
  password: string
}

/** Mirrors com.biddly.auth.dto.TokenResponse. The refresh token is in an HttpOnly cookie, never in here. */
export interface TokenResponse {
  accessToken: string
  tokenType: 'Bearer'
  expiresAt: string
  account: Account
}

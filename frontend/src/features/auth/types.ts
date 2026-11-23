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

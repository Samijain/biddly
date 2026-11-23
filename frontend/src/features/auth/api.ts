import { http } from '../../lib/http'
import type { Account, RegisterInput } from './types'

export async function register(input: RegisterInput): Promise<Account> {
  return http<Account>('/api/v1/auth/register', { method: 'POST', body: JSON.stringify(input) })
}

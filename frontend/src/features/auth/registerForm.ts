import { z } from 'zod'

import type { RegisterInput } from './types'

/** BCrypt reads at most 72 bytes (R28). "₹" or most Hindi letters take 3 bytes each in UTF-8. */
const MAX_PASSWORD_BYTES = 72

function utf8Bytes(value: string): number {
  return new TextEncoder().encode(value).length
}

/** Client-side validation gives instant feedback. The server still re-validates everything. */
export const registerFormSchema = z
  .object({
    displayName: z.string().trim().min(1, 'Required').max(100, 'At most 100 characters'),
    email: z.string().trim().pipe(z.email('Enter a valid email')),
    password: z
      .string()
      .min(8, 'At least 8 characters')
      .refine((p) => utf8Bytes(p) <= MAX_PASSWORD_BYTES, 'Too long: at most 72 bytes (symbols like ₹ count as 3)'),
    confirmPassword: z.string(),
  })
  .refine((v) => v.password === v.confirmPassword, {
    path: ['confirmPassword'],
    message: "Passwords don't match",
  })

export type RegisterFormValues = z.infer<typeof registerFormSchema>

export const emptyRegisterForm: RegisterFormValues = { displayName: '', email: '', password: '', confirmPassword: '' }

export function toRegisterInput(values: RegisterFormValues): RegisterInput {
  return { displayName: values.displayName, email: values.email, password: values.password }
}

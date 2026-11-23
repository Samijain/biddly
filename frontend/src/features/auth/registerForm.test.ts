import { registerFormSchema, toRegisterInput } from './registerForm'

const valid = {
  displayName: 'Priya Shah',
  email: 'priya@example.com',
  password: 'correct-horse-42',
  confirmPassword: 'correct-horse-42',
}

function errorsFor(values: typeof valid): Record<string, string> {
  const result = registerFormSchema.safeParse(values)
  if (result.success) return {}
  return Object.fromEntries(result.error.issues.map((issue) => [issue.path.join('.'), issue.message]))
}

describe('registerFormSchema', () => {
  it('accepts a valid form and trims the name and email', () => {
    const result = registerFormSchema.parse({ ...valid, displayName: '  Priya Shah ', email: ' priya@example.com ' })

    expect(toRegisterInput(result)).toEqual({ displayName: 'Priya Shah', email: 'priya@example.com', password: 'correct-horse-42' })
  })

  it('needs a name, a real email and at least 8 characters', () => {
    expect(errorsFor({ ...valid, displayName: '   ', email: 'not-an-email', password: 'short', confirmPassword: 'short' })).toEqual({
      displayName: 'Required',
      email: 'Enter a valid email',
      password: 'At least 8 characters',
    })
  })

  it('counts the password limit in bytes, like BCrypt', () => {
    const rupees = '₹'.repeat(25)

    expect(errorsFor({ ...valid, password: rupees, confirmPassword: rupees }).password).toMatch(/72 bytes/)
    expect(errorsFor({ ...valid, password: 'a'.repeat(72), confirmPassword: 'a'.repeat(72) })).toEqual({})
  })

  it('needs the two passwords to match', () => {
    expect(errorsFor({ ...valid, confirmPassword: 'something-else' })).toEqual({ confirmPassword: "Passwords don't match" })
  })
})

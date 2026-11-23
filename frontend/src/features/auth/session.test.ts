import type { Account, TokenResponse } from './types'

const account: Account = { id: 7, displayName: 'Priya Shah', email: 'priya@example.com', role: 'USER', createdAt: '2026-11-23T10:00:00Z' }
const credentials = { email: 'priya@example.com', password: 'correct-horse-42' }

function jsonResponse(status: number, body?: unknown): Response {
  return new Response(body === undefined ? null : JSON.stringify(body), { status, headers: { 'Content-Type': 'application/json' } })
}

/** A new one per call: a Response body can only be read once, and the expiry has to move with the clock. */
function signedIn(): Response {
  const tokens: TokenResponse = { accessToken: 'access-1', tokenType: 'Bearer', expiresAt: new Date(Date.now() + 15 * 60_000).toISOString(), account }
  return jsonResponse(200, tokens)
}

function unauthorized(code: string): Response {
  return jsonResponse(401, { code, message: 'No', timestamp: '', fieldErrors: {} })
}

/** session.ts keeps module state, so every test imports a fresh copy. */
async function freshModules() {
  vi.resetModules()
  const session = await import('./session')
  const { http } = await import('../../lib/http')
  return { session, http }
}

function headersOf(call: unknown[] | undefined): Record<string, string> {
  return ((call?.[1] as RequestInit | undefined)?.headers ?? {}) as Record<string, string>
}

describe('session', () => {
  const fetchMock = vi.fn<typeof fetch>()

  beforeEach(() => {
    vi.useFakeTimers({ shouldAdvanceTime: true })
    fetchMock.mockReset()
    vi.stubGlobal('fetch', fetchMock)
  })

  afterEach(() => {
    vi.useRealTimers()
    vi.unstubAllGlobals()
  })

  it('signs in, then sends the access token with later requests', async () => {
    const { session, http } = await freshModules()
    fetchMock.mockResolvedValueOnce(signedIn()).mockResolvedValueOnce(jsonResponse(200, []))

    await session.signIn(credentials)
    await http('/api/v1/auctions')

    expect(session.getAuthState()).toEqual({ status: 'signedIn', account })
    expect(headersOf(fetchMock.mock.calls[1])['Authorization']).toBe('Bearer access-1')
  })

  it('refreshes only once when asked twice at the same moment', async () => {
    const { session } = await freshModules()
    fetchMock.mockImplementation(async () => signedIn())

    await Promise.all([session.restoreSession(), session.restoreSession(), session.refreshSession()])

    expect(fetchMock).toHaveBeenCalledTimes(1)
    expect(session.getAuthState().status).toBe('signedIn')
  })

  it('never sends the access token to the auth endpoints', async () => {
    const { session } = await freshModules()
    fetchMock.mockImplementation(async () => signedIn())

    await session.signIn(credentials)
    await session.refreshSession()

    expect(headersOf(fetchMock.mock.calls[1])['Authorization']).toBeUndefined()
  })

  it('ends up signed out when there is no session to restore', async () => {
    const { session, http } = await freshModules()
    fetchMock.mockResolvedValueOnce(unauthorized('INVALID_REFRESH_TOKEN')).mockResolvedValueOnce(jsonResponse(200, []))

    await session.restoreSession()
    await http('/api/v1/auctions')

    expect(session.getAuthState()).toEqual({ status: 'signedOut', account: null })
    expect(headersOf(fetchMock.mock.calls[1])['Authorization']).toBeUndefined()
  })

  it('refreshes by itself a minute before the access token expires', async () => {
    const { session } = await freshModules()
    fetchMock.mockImplementation(async () => signedIn())
    await session.signIn(credentials)

    await vi.advanceTimersByTimeAsync(14 * 60_000)

    expect(fetchMock.mock.calls.map((call) => call[0])).toEqual(['/api/v1/auth/login', '/api/v1/auth/refresh'])
  })

  it('signs out locally even if the server cannot be reached', async () => {
    const { session } = await freshModules()
    fetchMock.mockResolvedValueOnce(signedIn()).mockRejectedValueOnce(new TypeError('Failed to fetch'))
    await session.signIn(credentials)

    await session.signOut().catch(() => undefined)

    expect(session.getAuthState().status).toBe('signedOut')
  })
})

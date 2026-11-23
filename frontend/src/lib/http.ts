import { getAccessToken } from './accessToken'

/** Mirrors com.biddly.common.ApiError on the backend. */
export interface ApiErrorBody {
  code: string
  message: string
  timestamp: string
  fieldErrors: Record<string, string>
}

export class ApiError extends Error {
  readonly status: number
  readonly code: string
  readonly fieldErrors: Record<string, string>

  constructor(status: number, body: ApiErrorBody) {
    super(body.message)
    this.name = 'ApiError'
    this.status = status
    this.code = body.code
    this.fieldErrors = body.fieldErrors ?? {}
  }
}

function isApiErrorBody(value: unknown): value is ApiErrorBody {
  return typeof value === 'object' && value !== null && 'code' in value && 'message' in value
}

export interface HttpInit extends RequestInit {
  /**
   * Leave out the access token. The auth endpoints use the refresh cookie instead, and an expired token in the
   * header would make Spring Security answer 401 before the refresh endpoint even runs.
   */
  anonymous?: boolean
}

/**
 * A thin wrapper around fetch. fetch only rejects on network failure, never on 4xx/5xx,
 * so HTTP errors have to be turned into exceptions here.
 */
export async function http<T>(path: string, init?: HttpInit): Promise<T> {
  const { anonymous, ...requestInit } = init ?? {}
  const token = anonymous ? null : getAccessToken()
  let response: Response
  try {
    response = await fetch(path, {
      ...requestInit,
      headers: {
        'Content-Type': 'application/json',
        Accept: 'application/json',
        ...(token ? { Authorization: `Bearer ${token}` } : {}),
        ...requestInit.headers,
      },
    })
  } catch {
    throw new ApiError(0, {
      code: 'NETWORK_ERROR',
      message: 'Cannot reach the Biddly server. Is the backend running?',
      timestamp: new Date().toISOString(),
      fieldErrors: {},
    })
  }

  const body: unknown = response.status === 204 ? undefined : await response.json().catch(() => undefined)

  if (!response.ok) {
    throw new ApiError(
      response.status,
      isApiErrorBody(body)
        ? body
        : {
            code: 'HTTP_' + response.status,
            message: 'Unexpected server response.',
            timestamp: new Date().toISOString(),
            fieldErrors: {},
          },
    )
  }
  return body as T
}

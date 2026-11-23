import { useMutation } from '@tanstack/react-query'
import { useSyncExternalStore } from 'react'

import { register } from './api'
import { getAuthState, signIn, signOut, subscribe } from './session'

export function useRegister() {
  return useMutation({ mutationFn: register })
}

/** Who is signed in. Re-renders whenever the session starts, refreshes or ends. */
export function useAuth() {
  return useSyncExternalStore(subscribe, getAuthState)
}

export function useSignIn() {
  return useMutation({ mutationFn: signIn })
}

export function useSignOut() {
  return useMutation({ mutationFn: signOut })
}

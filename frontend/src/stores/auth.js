import { reactive } from 'vue'

import { getMeApi, loginApi, logoutApi, registerApi } from '../api'
import {
  clearStoredSession,
  readStoredSession,
  writeStoredSession,
} from '../api/http'

const stored = readStoredSession()

export const authState = reactive({
  token: stored.token,
  user: stored.user,
  restoring: true,
})

let restorePromise = null

function applyUser(user) {
  authState.user = user
  writeStoredSession(authState.token, user)
}

function applySession(token, user) {
  authState.token = token
  authState.user = user
  writeStoredSession(token, user)
}

export function clearSession() {
  authState.token = ''
  authState.user = null
  clearStoredSession()
}

export async function restoreSession() {
  if (!restorePromise) {
    restorePromise = (async () => {
      authState.restoring = true
      if (authState.token) {
        try {
          applyUser(await getMeApi())
        } catch (error) {
          if (error.status === 401) {
            clearSession()
          }
        }
      }
      authState.restoring = false
    })()
  }
  return restorePromise
}

export async function login(credentials) {
  const result = await loginApi(credentials)
  applySession(result.token, {
    id: result.userId,
    username: result.username,
  })
  return result
}

export async function register(profile) {
  return registerApi(profile)
}

export async function logout() {
  try {
    await logoutApi()
  } catch {
    // A local logout should still work when the token is already invalid.
  } finally {
    clearSession()
  }
}

export function isLoggedIn() {
  return Boolean(authState.token)
}

export function currentUser() {
  return authState.user
}

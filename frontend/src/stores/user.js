import { defineStore } from 'pinia'
import { ref, computed } from 'vue'
import { loginApi, registerApi, registerNationalApi, registerProvinceApi, registerSchoolApi, logoutApi, getMeApi, teacherLoginApi } from '../api/auth'

const ADMIN_ROLES = ['ADMIN', 'SUPER_ADMIN', 'UNIT_ADMIN', 'COLLEGE_ADMIN', 'SCHOOL_ADMIN', 'PROVINCE_ADMIN', 'NATIONAL_ADMIN']

function isAdminRole(role) {
  return role && ADMIN_ROLES.includes(role)
}

function getTokenKey(role) {
  return isAdminRole(role) ? 'admin_authToken' : 'user_authToken'
}

function getUserKey(role) {
  return isAdminRole(role) ? 'admin_user' : 'user_user'
}

function readStoredUser() {
  const adminRaw = sessionStorage.getItem('admin_user')
  const userRaw = sessionStorage.getItem('user_user')
  if (adminRaw) return JSON.parse(adminRaw)
  if (userRaw) return JSON.parse(userRaw)
  return null
}

function readStoredToken() {
  return sessionStorage.getItem('admin_authToken') || sessionStorage.getItem('user_authToken')
}

function clearAllSessions() {
  sessionStorage.removeItem('admin_user')
  sessionStorage.removeItem('admin_authToken')
  sessionStorage.removeItem('user_user')
  sessionStorage.removeItem('user_authToken')
}

export const useUserStore = defineStore('user', () => {
  const user = ref(readStoredUser())
  const isLoggedIn = computed(() => user.value !== null && readStoredToken() !== null)
  const isAdmin = computed(() => isAdminRole(user.value?.role))
  const token = computed(() => {
    if (!user.value) return null
    return sessionStorage.getItem(getTokenKey(user.value.role))
  })

  function saveSession(userData) {
    // Clear the opposite session to avoid cross-contamination
    if (isAdminRole(userData.role)) {
      sessionStorage.removeItem('user_user')
      sessionStorage.removeItem('user_authToken')
      sessionStorage.setItem('admin_user', JSON.stringify(userData))
      sessionStorage.setItem('admin_authToken', userData.token)
    } else {
      sessionStorage.removeItem('admin_user')
      sessionStorage.removeItem('admin_authToken')
      sessionStorage.setItem('user_user', JSON.stringify(userData))
      sessionStorage.setItem('user_authToken', userData.token)
    }
    user.value = userData
  }

  async function login(username, password, role) {
    const res = await loginApi(username, password, role)
    saveSession(res.data)
    return res.data
  }

  async function loginTeacher(username, password) {
    const res = await teacherLoginApi(username, password)
    saveSession(res.data)
    return res.data
  }

  function markPasswordChanged() {
    if (user.value) {
      const updated = { ...user.value, mustChangePassword: false }
      saveSession(updated)
    }
  }

  async function register(data) {
    const res = await registerApi(data)
    saveSession(res.data)
    return res.data
  }

  async function registerNational(data) {
    const res = await registerNationalApi(data)
    saveSession(res.data)
    return res.data
  }

  async function registerProvince(data) {
    const res = await registerProvinceApi(data)
    saveSession(res.data)
    return res.data
  }

  async function registerSchool(data) {
    const res = await registerSchoolApi(data)
    saveSession(res.data)
    return res.data
  }

  async function fetchMe() {
    try {
      const res = await getMeApi()
      const currentToken = token.value
      const userData = { ...res.data, token: currentToken }
      saveSession(userData)
    } catch {
      clearAllSessions()
      user.value = null
    }
  }

  async function logout() {
    clearAllSessions()
    user.value = null
    try { await logoutApi() } catch {}
  }

  return { user, isLoggedIn, isAdmin, token, login, loginTeacher, markPasswordChanged, register, registerNational, registerProvince, registerSchool, logout, fetchMe }
})
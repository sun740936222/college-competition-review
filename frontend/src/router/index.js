import { createRouter, createWebHashHistory } from 'vue-router'
import { useUserStore } from '../stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('../views/auth/LoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('../views/auth/RegisterView.vue'),
    meta: { guest: true }
  },
  {
    path: '/admin/login',
    name: 'AdminLogin',
    component: () => import('../views/auth/AdminLoginView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register/national',
    name: 'RegisterNational',
    component: () => import('../views/auth/RegisterNationalView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register/province',
    name: 'RegisterProvince',
    component: () => import('../views/auth/RegisterProvinceView.vue'),
    meta: { guest: true }
  },
  {
    path: '/register/school',
    name: 'RegisterSchool',
    component: () => import('../views/auth/RegisterSchoolView.vue'),
    meta: { guest: true }
  },
  {
    path: '/profile/complete',
    name: 'ProfileComplete',
    component: () => import('../views/auth/ProfileCompleteView.vue'),
    meta: { auth: true }
  },
  {
    path: '/',
    component: () => import('../components/layout/AppLayout.vue'),
    meta: { auth: true },
    children: [
      { path: '', name: 'Home', component: () => import('../views/HomeView.vue') },
      { path: 'projects', name: 'ProjectList', component: () => import('../views/projects/ProjectList.vue') },
      { path: 'projects/create', name: 'ProjectCreate', component: () => import('../views/projects/ProjectCreate.vue') },
      { path: 'projects/:id', name: 'ProjectDetail', component: () => import('../views/projects/ProjectDetail.vue') },
      { path: 'projects/:id/edit', name: 'ProjectEdit', component: () => import('../views/projects/ProjectEdit.vue') },
      { path: 'training/topics', name: 'TrainingTopics', component: () => import('../views/training/TopicList.vue') },
      { path: 'training/projects', name: 'TrainingProjects', component: () => import('../views/training/ProjectList.vue') },
      { path: 'training/midterm', name: 'TrainingMidterm', component: () => import('../views/training/ReportList.vue'), meta: { stage: 'MIDTERM', stagePath: 'midterm', title: '中期检查' } },
      { path: 'training/conclusion', name: 'TrainingConclusion', component: () => import('../views/training/ReportList.vue'), meta: { stage: 'CONCLUSION', stagePath: 'conclusion', title: '结题验收' } },
      { path: 'training/inbox', name: 'TrainingInbox', component: () => import('../views/training/ApprovalWorkspace.vue') }
    ]
  },
  {
    path: '/admin',
    component: () => import('../views/admin/AdminLayout.vue'),
    meta: { auth: true, admin: true },
    children: [
      { path: '', redirect: '/admin/events' },
      { path: 'events', name: 'AdminEvents', component: () => import('../views/admin/EventManage.vue'), meta: { noSuperAdmin: true } },
      { path: 'orgs', name: 'AdminOrgs', component: () => import('../views/admin/OrgManage.vue') },
      { path: 'projects', name: 'AdminProjects', component: () => import('../views/admin/ProjectReview.vue'), meta: { noSuperAdmin: true } },
      { path: 'judges', name: 'AdminJudges', component: () => import('../views/admin/JudgeManage.vue'), meta: { noSuperAdmin: true } },
      { path: 'criteria', name: 'AdminCriteria', component: () => import('../views/admin/ScoreCriteriaManage.vue'), meta: { noSuperAdmin: true } },
      { path: 'allocation', name: 'AdminAllocation', component: () => import('../views/admin/JudgeAllocation.vue'), meta: { noSuperAdmin: true } },
      { path: 'scores', name: 'AdminScores', component: () => import('../views/admin/ScoreSummary.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/batches', name: 'SrtpBatches', component: () => import('../views/admin/srtp/BatchManage.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/experts', name: 'SrtpExperts', component: () => import('../views/admin/srtp/ExpertManage.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/teachers', name: 'SrtpTeachers', component: () => import('../views/admin/srtp/TeacherManage.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/topics', name: 'SrtpTopics', component: () => import('../views/admin/srtp/TopicReview.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/projects', name: 'SrtpProjects', component: () => import('../views/admin/srtp/ProjectReview.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/midterm', name: 'SrtpMidterm', component: () => import('../views/admin/srtp/ReportReview.vue'), meta: { noSuperAdmin: true, stagePath: 'midterm', title: '中期' } },
      { path: 'srtp/conclusion', name: 'SrtpConclusion', component: () => import('../views/admin/srtp/ReportReview.vue'), meta: { noSuperAdmin: true, stagePath: 'conclusion', title: '结题' } },
      { path: 'srtp/fields', name: 'SrtpFields', component: () => import('../views/admin/srtp/CustomFieldManage.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/tracking', name: 'SrtpTracking', component: () => import('../views/admin/srtp/TrackingBoard.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/stats', name: 'SrtpStats', component: () => import('../views/admin/srtp/CategoryStats.vue'), meta: { noSuperAdmin: true } },
      { path: 'srtp/promotion', name: 'SrtpPromotion', component: () => import('../views/admin/srtp/ProjectRecommend.vue'), meta: { noSuperAdmin: true } }
    ]
  },
  {
    path: '/judge',
    component: () => import('../views/judge/JudgeLayout.vue'),
    meta: { auth: true, judge: true },
    children: [
      { path: '', name: 'JudgeProjects', component: () => import('../views/judge/JudgeProjectList.vue') },
      { path: 'projects/:id', name: 'JudgeProjectReview', component: () => import('../views/judge/JudgeProjectReview.vue'), props: true }
    ]
  }
]

const router = createRouter({
  history: createWebHashHistory(),
  routes
})

router.onError((error) => {
  console.error('路由错误:', error)
  if (error.message?.includes('dynamically imported module')) {
    window.location.reload()
  }
})

// ============================================================
// 域名判断：cs 子域名 → 参赛者/评委端，gly 子域名 → 管理员端
// localhost / 127.0.0.1 开发环境不做域名限制，两端路由均可访问
// ============================================================
function isContestDomain() {
  const h = window.location.hostname
  return h.startsWith('cs.')
}

function isAdminDomain() {
  return window.location.hostname.startsWith('gly.')
}

// 域名级路由守卫：限制每个子域名只能访问自己负责的路由
router.beforeEach((to) => {
  if (isContestDomain()) {
    // cs 域名：禁止访问管理端页面（/admin/*，但不包括 /admin/login）
    if (to.path.startsWith('/admin') && to.path !== '/admin/login') {
      return '/login'
    }
    // / 路由由认证守卫统一处理未登录拦截，无需域名守卫重复判断
  }
  if (isAdminDomain()) {
    // gly 域名：禁止访问参赛者/评委页面，/ 重定向到管理员登录
    if (to.path === '/' || (!to.path.startsWith('/admin') && to.path !== '/login' && !to.path.startsWith('/register') && to.path !== '/profile/complete')) {
      return '/admin/login'
    }
  }
})

// 认证与权限守卫
router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.auth && !userStore.isLoggedIn) {
    if (to.meta.admin) {
      next('/admin/login')
    } else {
      next('/login')
    }
  } else if (to.meta.guest && userStore.isLoggedIn) {
    if (to.path === '/admin/login') {
      const role = userStore.user?.role
      if (role === 'ADMIN' || role === 'SUPER_ADMIN' || role === 'UNIT_ADMIN') {
        if (userStore.user?.orgLevel === 'SUPER_ADMIN') {
          next('/admin/orgs')
        } else {
          next('/admin')
        }
      } else {
        next()
      }
    } else if (to.path === '/login') {
      const role = userStore.user?.role
      if (role && role !== 'ADMIN' && role !== 'SUPER_ADMIN' && role !== 'UNIT_ADMIN') {
        // On cs subdomain, / is the login page itself — avoid infinite loop
        if (isContestDomain()) {
          next()
        } else {
          next('/')
        }
      } else {
        next()
      }
    } else {
      next('/')
    }
  } else if (to.meta.admin && userStore.isLoggedIn) {
    const role = userStore.user?.role
    if (role !== 'ADMIN' && role !== 'SUPER_ADMIN' && role !== 'UNIT_ADMIN') {
      next('/admin/login')
    } else if (to.meta.noSuperAdmin && userStore.user?.orgLevel === 'SUPER_ADMIN') {
      // SUPER_ADMIN only has access to org management
      next('/admin/orgs')
    } else {
      next()
    }
  } else if (to.meta.judge && userStore.isLoggedIn) {
    const role = userStore.user?.role
    if (role !== 'JUDGE') {
      next('/login')
    } else {
      next()
    }
  } else {
    next()
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false, title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/views/Layout.vue'),
    children: [
      {
        path: '',
        redirect: '/items'
      },
      {
        path: 'items',
        name: 'ItemList',
        component: () => import('@/views/ItemList.vue'),
        meta: { title: '物品列表' }
      },
      {
        path: 'items/:id',
        name: 'ItemDetail',
        component: () => import('@/views/ItemDetail.vue'),
        meta: { title: '物品详情' }
      },
      {
        path: 'publish',
        name: 'ItemPublish',
        component: () => import('@/views/ItemPublish.vue'),
        meta: { title: '发布物品', requiresAuth: true }
      },
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人中心', requiresAuth: true }
      },
      {
        path: 'admin/users',
        name: 'AdminUser',
        component: () => import('@/views/admin/UserManage.vue'),
        meta: { title: '用户管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/categories',
        name: 'AdminCategory',
        component: () => import('@/views/admin/CategoryManage.vue'),
        meta: { title: '分类管理', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/audit',
        name: 'AdminAudit',
        component: () => import('@/views/admin/ItemAudit.vue'),
        meta: { title: '物品审核', requiresAuth: true, requiresAdmin: true }
      },
      {
        path: 'admin/logs',
        name: 'AdminLog',
        component: () => import('@/views/admin/OperationLog.vue'),
        meta: { title: '操作日志', requiresAuth: true, requiresAdmin: true }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  document.title = to.meta.title ? `${to.meta.title} - 校园失物招领系统` : '校园失物招领系统'
  
  if (to.meta.requiresAuth && !userStore.isLoggedIn) {
    next('/login')
  } else if (to.meta.requiresAdmin && !userStore.isAdmin) {
    next('/')
  } else {
    next()
  }
})

export default router

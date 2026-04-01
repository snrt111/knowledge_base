import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    // 登录和注册页面
    {
      path: '/login',
      name: 'Login',
      component: () => import('@/views/LoginView.vue'),
      meta: { title: '登录', requiresAuth: false }
    },
    {
      path: '/register',
      name: 'Register',
      component: () => import('@/views/RegisterView.vue'),
      meta: { title: '注册', requiresAuth: false }
    },
    // 主布局
    {
      path: '/',
      component: Layout,
      redirect: '/chat',
      meta: { requiresAuth: true },
      children: [
        {
          path: 'chat',
          name: 'Chat',
          component: () => import('@/views/ChatView.vue'),
          meta: { title: 'AI对话', icon: 'ChatDotRound' }
        },
        {
          path: 'knowledge',
          name: 'Knowledge',
          component: () => import('@/views/KnowledgeView.vue'),
          meta: { title: '知识库管理', icon: 'Collection' }
        },
        {
          path: 'documents',
          name: 'Documents',
          component: () => import('@/views/DocumentsView.vue'),
          meta: { title: '文档管理', icon: 'Document' }
        }
      ]
    }
  ]
})

// 路由守卫
router.beforeEach((to, from, next) => {
  const requiresAuth = to.matched.some(record => record.meta.requiresAuth !== false)
  const token = localStorage.getItem('token')
  
  if (requiresAuth && !token) {
    // 需要认证但没有token，跳转到登录页
    next('/login')
  } else if ((to.path === '/login' || to.path === '/register') && token) {
    // 已登录用户不能访问登录和注册页
    next('/')
  } else {
    next()
  }
})

export default router

import { createRouter, createWebHistory } from 'vue-router'
import Layout from '@/layouts/MainLayout.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: Layout,
      redirect: '/chat',
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

export default router

import { createRouter, createWebHistory } from 'vue-router'
import Layout from '../components/Layout.vue'
import HomeView from '../views/HomeView.vue'

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes: [
    {
      path: '/',
      component: Layout,
      redirect: '/dashboard',
      children: [
        {
          path: 'dashboard',
          name: 'dashboard',
          component: HomeView,
          meta: { title: 'Dashboard' },
        },
        {
          path: 'workflows',
          name: 'workflows',
          component: () => import('../views/WorkflowListView.vue'),
          meta: { title: '工作流列表' },
        },
        {
          path: 'workflows/create',
          name: 'workflow-create',
          component: () => import('../views/WorkflowEditorView.vue'),
          meta: { title: '创建工作流' },
        },
        {
          path: 'workflows/:id/edit',
          name: 'workflow-edit',
          component: () => import('../views/WorkflowEditorView.vue'),
          meta: { title: '编辑工作流' },
        },
        {
          path: 'executions',
          name: 'executions',
          component: () => import('../views/ExecutionListView.vue'),
          meta: { title: '执行记录' },
        },
      ],
    },
    {
      path: '/:pathMatch(.*)*',
      name: 'not-found',
      component: () => import('../views/NotFoundView.vue'),
    },
  ],
})

export default router

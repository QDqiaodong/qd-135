import { createRouter, createWebHistory } from 'vue-router';

const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: '/',
      name: 'Dashboard',
      component: () => import('@/pages/Dashboard.vue'),
    },
    {
      path: '/tools',
      name: 'ToolManagement',
      component: () => import('@/pages/ToolManagement.vue'),
    },
    {
      path: '/tools/create',
      name: 'CreateTool',
      component: () => import('@/pages/ToolForm.vue'),
    },
    {
      path: '/tools/:id',
      name: 'EditTool',
      component: () => import('@/pages/ToolForm.vue'),
    },
    {
      path: '/inheritors',
      name: 'InheritorManagement',
      component: () => import('@/pages/InheritorManagement.vue'),
    },
    {
      path: '/inheritors/create',
      name: 'CreateInheritor',
      component: () => import('@/pages/InheritorForm.vue'),
    },
    {
      path: '/inheritors/:id',
      name: 'EditInheritor',
      component: () => import('@/pages/InheritorForm.vue'),
    },
    {
      path: '/projects',
      name: 'ProjectManagement',
      component: () => import('@/pages/ProjectManagement.vue'),
    },
    {
      path: '/projects/create',
      name: 'CreateProject',
      component: () => import('@/pages/ProjectForm.vue'),
    },
    {
      path: '/projects/:id',
      name: 'EditProject',
      component: () => import('@/pages/ProjectForm.vue'),
    },
    {
      path: '/binding',
      name: 'AssociationBinding',
      component: () => import('@/pages/AssociationBinding.vue'),
    },
    {
      path: '/traceability',
      name: 'TraceabilityQuery',
      component: () => import('@/pages/TraceabilityQuery.vue'),
    },
  ],
});

export default router;
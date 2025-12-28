import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router';
import FirstPage from '../views/FirstPage.vue';
import ChatPage from '../views/ChatPage.vue';

const routes: Array<RouteRecordRaw> = [
  {
    path: '/',
    name: 'FirstPage',
    component: FirstPage
  },
  {
    path: '/chat/:roomId',
    name: 'ChatPage',
    component: ChatPage
  }
];

const router = createRouter({
  history: createWebHistory(),
  routes
});

export default router;

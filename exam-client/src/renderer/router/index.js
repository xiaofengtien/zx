import Vue from 'vue'
import Router from 'vue-router'
import Login from '../views/Login.vue'
import PaperSelect from '../views/PaperSelect.vue'
import ExamPaper from '../views/ExamPaper.vue'
import OperationTips from '../views/OperationTips.vue'
import Notes from '../views/Notes.vue'
import Ready from '../views/Ready.vue'
import Broadcast from '../views/Broadcast.vue'
import SectionList from '../views/SectionList.vue'
import ExamResult from '../views/ExamResult.vue'
import VolumeComplete from '../views/VolumeComplete.vue'
import Intermission from '../views/Intermission.vue'

Vue.use(Router)

export default new Router({
  routes: [
    {
      path: '/',
      redirect: '/login'
    },
    {
      path: '/login',
      name: 'Login',
      component: Login
    },
    {
      path: '/paper-select',
      name: 'PaperSelect',
      component: PaperSelect,
      meta: { requiresAuth: true }
    },
    {
      path: '/operation-tips',
      name: 'OperationTips',
      component: OperationTips,
      meta: { requiresAuth: true }
    },
    {
      path: '/notes',
      name: 'Notes',
      component: Notes,
      meta: { requiresAuth: true }
    },
    {
      path: '/ready',
      name: 'Ready',
      component: Ready,
      meta: { requiresAuth: true }
    },
    {
      path: '/broadcast',
      name: 'Broadcast',
      component: Broadcast,
      meta: { requiresAuth: true }
    },
    {
      path: '/section-list',
      name: 'SectionList',
      component: SectionList,
      meta: { requiresAuth: true }
    },
    {
      path: '/exam',
      name: 'ExamPaper',
      component: ExamPaper,
      meta: { requiresAuth: true }
    },
    {
      path: '/exam-result',
      name: 'ExamResult',
      component: ExamResult,
      meta: { requiresAuth: true }
    },
    {
      path: '/volume-complete',
      name: 'VolumeComplete',
      component: VolumeComplete,
      meta: { requiresAuth: true }
    },
    {
      path: '/intermission',
      name: 'Intermission',
      component: Intermission,
      meta: { requiresAuth: true }
    }
  ]
})


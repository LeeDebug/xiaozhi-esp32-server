import Vue from 'vue'
import VueRouter from 'vue-router'
import routeProgress from '../utils/routeProgress'

Vue.use(VueRouter)

const routes = [
  {
    path: '/',
    name: 'welcome',
    component: function () {
      return import('../views/login.vue')
    }
  },
  {
    path: '/role-config',
    name: 'RoleConfig',
    component: function () {
      return import('../views/roleConfig.vue')
    }
  },
  {
    path: '/voice-print',
    name: 'VoicePrint',
    component: function () {
      return import('../views/VoicePrint.vue')
    }
  },
  {
    path: '/login',
    name: 'login',
    component: function () {
      return import('../views/login.vue')
    }
  },
  {
    path: '/home',
    name: 'home',
    component: function () {
      return import('../views/home.vue')
    }
  },
  {
    path: '/register',
    name: 'Register',
    component: function () {
      return import('../views/register.vue')
    }
  },
  {
    path: '/retrieve-password',
    name: 'RetrievePassword',
    component: function () {
      return import('../views/retrievePassword.vue')
    }
  },
  // 设备管理页面路由
  {
    path: '/device-management',
    name: 'DeviceManagement',
    component: function () {
      return import('../views/DeviceManagement.vue')
    }
  },
  // 设备分析看板（设备地域分布地图）
  {
    path: '/device-analytics',
    name: 'DeviceAnalyticsDashboard',
    component: function () {
      return import('../views/DeviceAnalyticsDashboard.vue')
    }
  },
  // 会话记录页面路由（支持 ?macAddress=xxx 从设备管理页带入筛选）
  {
    path: '/chat-history',
    name: 'ChatHistoryRecords',
    component: function () {
      return import('../views/ChatHistoryRecords.vue')
    }
  },
  // 会话分析看板（支持按设备 / 智能体筛选）
  {
    path: '/chat-analytics',
    name: 'ChatAnalytics',
    component: function () {
      return import('../views/ChatAnalytics.vue')
    }
  },
  // 会话内容分析（词云）
  {
    path: '/chat-wordcloud',
    name: 'ChatWordCloud',
    component: function () {
      return import('../views/ChatWordCloud.vue')
    }
  },
  // 会话内容分析丨高频方向（按地区 / 智能体 / 设备统计提问方向，支持大模型总结）
  {
    path: '/chat-direction',
    name: 'ChatDirection',
    component: function () {
      return import('../views/ChatDirection.vue')
    }
  },
  // 添加用户管理路由
  {
    path: '/user-management',
    name: 'UserManagement',
    component: function () {
      return import('../views/UserManagement.vue')
    }
  },
  // 个人信息页面路由（右上角用户下拉进入）
  {
    path: '/user-profile',
    name: 'UserProfile',
    component: function () {
      return import('../views/UserProfile.vue')
    }
  },
  {
    path: '/model-config',
    name: 'ModelConfig',
    component: function () {
      return import('../views/ModelConfig.vue')
    }
  },
  {
    path: '/params-management',
    name: 'ParamsManagement',
    component: function () {
      return import('../views/ParamsManagement.vue')
    }
  },
  {
    path: '/knowledge-base-management',
    name: 'KnowledgeBaseManagement',
    component: function () {
      return import('../views/KnowledgeBaseManagement.vue')
    }
  },
  {
    path: '/server-side-management',
    name: 'ServerSideManager',
    component: function () {
      return import('../views/ServerSideManager.vue')
    }
  },
  {
    path: '/ota-management',
    name: 'OtaManagement',
    component: function () {
      return import('../views/OtaManagement.vue')
    }
  },
  {
    path: '/voice-resource-management',
    name: 'VoiceResourceManagement',
    component: function () {
      return import('../views/VoiceResourceManagement.vue')
    }
  },
  {
    path: '/voice-clone-management',
    name: 'VoiceCloneManagement',
    component: function () {
      return import('../views/VoiceCloneManagement.vue')
    }
  },
  {
    path: '/dict-management',
    name: 'DictManagement',
    component: function () {
      return import('../views/DictManagement.vue')
    }
  },
  {
    path: '/provider-management',
    name: 'ProviderManagement',
    component: function () {
      return import('../views/ProviderManagement.vue')
    }
  },
  // 添加默认角色管理路由
  {
    path: '/agent-template-management',
    name: 'AgentTemplateManagement',
    component: function () {
      return import('../views/AgentTemplateManagement.vue')
    }
  },
  // 添加模板快速配置路由
  {
    path: '/template-quick-config',
    name: 'TemplateQuickConfig',
    component: function () {
      return import('../views/TemplateQuickConfig.vue')
    }
  },
  // 功能配置页面路由
  {
    path: '/feature-management',
    name: 'FeatureManagement',
    component: function () {
      return import('../views/FeatureManagement.vue')
    }
  },
  // 替换词管理
  {
    path: '/replacement-word-management',
    name: 'ReplacementWordManagement',
    component: function () {
      return import('../views/ReplacementWordManagement.vue')
    }
  },
  // 通讯录管理页面路由
  {
    path: '/address-book-management',
    name: 'AddressBookManagement',
    component: function () {
      return import('../views/AddressBookManagement.vue')
    }
  },
  // AI数据分析 - 下发配置
  {
    path: '/data-push-config',
    name: 'DataPushConfig',
    component: function () {
      return import('../views/DataPushConfig.vue')
    }
  },
  // AI数据分析 - 数据预览
  {
    path: '/data-preview',
    name: 'DataPreview',
    component: function () {
      return import('../views/DataPreview.vue')
    }
  },
  // AI数据分析 - 数据预览看板
  {
    path: '/data-preview-dashboard',
    name: 'DataPreviewDashboard',
    component: function () {
      return import('../views/DataPreviewDashboard.vue')
    }
  },
  // AI数据分析 - 数据分析
  {
    path: '/data-analysis',
    name: 'DataAnalysis',
    component: function () {
      return import('../views/DataAnalysis.vue')
    }
  },
  // AI数据分析 - 数据预测
  {
    path: '/data-prediction',
    name: 'DataPrediction',
    component: function () {
      return import('../views/DataPrediction.vue')
    }
  },
]
const router = new VueRouter({
  base: process.env.VUE_APP_PUBLIC_PATH || '/',
  routes
})

// 全局处理重复导航，改为刷新页面
const originalPush = VueRouter.prototype.push
VueRouter.prototype.push = function push(location) {
  return originalPush.call(this, location).catch(err => {
    if (err.name === 'NavigationDuplicated') {
      // 如果是重复导航，刷新页面
      window.location.reload()
    } else {
      // 其他错误正常抛出
      throw err
    }
  })
}

// 匿名可访问的路由白名单
const anonRouteNames = new Set(['login', 'Register', 'RetrievePassword', 'welcome'])

// 路由守卫
router.beforeEach((to, from, next) => {
  // 路由切换时显示顶部进度条（同路由仅 query 变化时不显示）
  if (to.path !== from.path) {
    routeProgress.start()
  }
  const token = localStorage.getItem('token');
  const isAnonRoute = anonRouteNames.has(to.name);

  if (isAnonRoute && token) {
    next({ name: 'home', replace: true })
    return
  }

  if (!isAnonRoute && !token) {
    next({
      name: 'login',
      replace: true
    })
    return
  }

  next()
})

router.afterEach(() => {
  routeProgress.done()
})

// 懒加载 chunk 失败等导航错误时也要收起进度条
router.onError(() => {
  routeProgress.done()
})

export default router

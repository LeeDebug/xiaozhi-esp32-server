<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationBarTitleText": "智能体",
    "navigationStyle": "custom"
  }
}
</route>

<script lang="ts" setup>
import { onLoad } from '@dcloudio/uni-app'
import { computed, onMounted, ref } from 'vue'
import CustomTabs from '@/components/custom-tabs/index.vue'
import { t } from '@/i18n'
import ChatHistory from '@/pages/chat-history/index.vue'
import DeviceManagement from '@/pages/device/index.vue'
import VoiceprintManagement from '@/pages/voiceprint/index.vue'
import { useConfigStore } from '@/store'
import AgentEdit from './edit.vue'

defineOptions({
  name: 'AgentIndex',
})

// 获取屏幕边界到安全区域距离
let safeAreaInsets: any
let systemInfo: any

// #ifdef MP-WEIXIN
systemInfo = uni.getWindowInfo()
safeAreaInsets = systemInfo.safeArea
  ? {
      top: systemInfo.safeArea.top,
      right: systemInfo.windowWidth - systemInfo.safeArea.right,
      bottom: systemInfo.windowHeight - systemInfo.safeArea.bottom,
      left: systemInfo.safeArea.left,
    }
  : null
// #endif

// #ifndef MP-WEIXIN
systemInfo = uni.getSystemInfoSync()
safeAreaInsets = systemInfo.safeAreaInsets
// #endif

// 智能体ID
const currentAgentId = ref('default')

// 当前 tab
const currentTab = ref('agent-config')

// 刷新和加载状态
const refreshing = ref(false)
const refresherEnabled = ref(false)

// 子组件引用
const deviceRef = ref()
const chatRef = ref()
const voiceprintRef = ref()

// 更新刷新器状态
function updateRefresherEnabled(value: boolean) {
  refresherEnabled.value = value
}

const configStore = useConfigStore()

// Tab 配置
// 声纹识别被服务端「功能管理」明确关闭时隐藏该 tab；未下发配置时默认显示，保持原有行为。
const tabList = computed(() => {
  const tabs = [
    {
      label: t('agent.roleConfig'),
      value: 'agent-config',
      icon: '/static/tabbar/robot.png',
      activeIcon: '/static/tabbar/robot_activate.png',
    },
    {
      label: t('agent.deviceManagement'),
      value: 'device-management',
      icon: '/static/tabbar/device.png',
      activeIcon: '/static/tabbar/device_activate.png',
    },
    {
      label: t('agent.chatHistory'),
      value: 'chat-history',
      icon: '/static/tabbar/chat.png',
      activeIcon: '/static/tabbar/chat_activate.png',
    },
  ]
  if (configStore.isFeatureEnabled('voiceprintRecognition')) {
    tabs.push({
      label: t('agent.voiceprintManagement'),
      value: 'voiceprint-management',
      icon: '/static/tabbar/microphone.png',
      activeIcon: '/static/tabbar/microphone_activate.png',
    })
  }
  return tabs
})

// 返回上一页
function goBack() {
  uni.navigateBack()
}

// 处理 tab 切换
function handleTabChange(item: any) {
  console.log('Tab changed:', item)
}

// 下拉刷新
async function onRefresh() {
  // 角色编辑页面不需要刷新
  if (currentTab.value === 'agent-config') {
    return
  }

  refreshing.value = true

  try {
    switch (currentTab.value) {
      case 'device-management':
        if (deviceRef.value?.refresh) {
          await deviceRef.value.refresh()
        }
        break
      case 'chat-history':
        if (chatRef.value?.refresh) {
          await chatRef.value.refresh()
        }
        break
      case 'voiceprint-management':
        if (voiceprintRef.value?.refresh) {
          await voiceprintRef.value.refresh()
        }
        break
    }
  }
  catch (error) {
    console.error('刷新失败:', error)
  }
  finally {
    refreshing.value = false
  }
}

// 触底加载更多
async function onLoadMore() {
  // 只有会话记录需要加载更多
  if (currentTab.value === 'chat-history' && chatRef.value?.loadMore) {
    await chatRef.value.loadMore()
  }
}

watch(() => currentTab.value, (newTab) => {
  updateRefresherEnabled(newTab !== 'agent-config')
})

// 当前 tab 被隐藏（例如功能开关被关闭）时回到第一个 tab，避免显示空白面板
watch(tabList, (list) => {
  if (!list.some(item => item.value === currentTab.value)) {
    currentTab.value = list[0].value
  }
})

// 接收页面参数
onLoad((options) => {
  if (options?.agentId) {
    currentAgentId.value = options.agentId
    console.log('接收到智能体ID:', options.agentId)
  }
})

onMounted(async () => {
  // 页面初始化
})
</script>

<template>
  <view class="h-screen flex flex-col bg-[#f5f7fb]">
    <!-- 导航栏 -->
    <wd-navbar :title="t('agent.pageTitle')" safe-area-inset-top>
      <template #left>
        <wd-icon name="arrow-left" size="18" @click="goBack" />
      </template>
    </wd-navbar>

    <!-- 自定义 Tabs -->
    <CustomTabs
      v-model="currentTab"
      :tab-list="tabList"
      @change="handleTabChange"
    />

    <!-- 主内容滚动区域 -->
    <scroll-view
      scroll-y
      :style="{ height: `calc(100vh - ${safeAreaInsets?.top || 0}px - 180rpx)` }"
      class="box-border flex-1 bg-[#f5f7fb]"
      enable-back-to-top
      :refresher-enabled="refresherEnabled"
      :refresher-triggered="refreshing"
      @refresherrefresh="onRefresh"
      @scrolltolower="onLoadMore"
    >
      <!-- Tab 内容 -->
      <view class="flex-1">
        <AgentEdit
          v-if="currentTab === 'agent-config'"
          :agent-id="currentAgentId"
        />
        <DeviceManagement
          v-else-if="currentTab === 'device-management'"
          ref="deviceRef"
          :agent-id="currentAgentId"
        />
        <ChatHistory
          v-else-if="currentTab === 'chat-history'"
          ref="chatRef"
          :agent-id="currentAgentId"
        />
        <VoiceprintManagement
          v-else-if="currentTab === 'voiceprint-management'"
          ref="voiceprintRef"
          :agent-id="currentAgentId"
          @update-refresher-enabled="updateRefresherEnabled"
        />
      </view>
    </scroll-view>
  </view>
</template>

<style scoped>
</style>

<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationStyle": "custom",
    "navigationBarTitleText": "会话记录"
  }
}
</route>

<script lang="ts" setup>
import type { ChatFilterAgentOption, ChatFilterDeviceOption, ChatRecordQuery, ChatSessionRecord } from '@/api/chat-history/types'
import { onLoad } from '@dcloudio/uni-app'
import { computed, onMounted, ref } from 'vue'
import useZPaging from 'z-paging/components/z-paging/js/hooks/useZPaging.js'
import { getChatRecordFilterOptions, getChatSessionRecords } from '@/api/chat-history/chat-history'
import { t } from '@/i18n'
import { chinaRecentRange } from '@/utils/chinaTime'
import { hasOwner, ownerDisplayName } from '@/utils/ownerDisplay'
import { toast } from '@/utils/toast'

defineOptions({
  name: 'ChatRecordsPage',
})

const pagingRef = ref()
useZPaging(pagingRef)

const sessionList = ref<ChatSessionRecord[]>([])

// 筛选选项
const agentOptions = ref<ChatFilterAgentOption[]>([])
const deviceOptions = ref<ChatFilterDeviceOption[]>([])

// 当前生效的筛选条件
const filters = ref({
  macAddress: '',
  agentId: '',
  chatType: '' as '' | 1 | 2 | 3,
  keyword: '',
})
// 快捷时间范围：null 表示全部时间
const recentDays = ref<number | null>(null)

// 筛选弹窗里的临时状态，点「确定」才写回 filters
const showFilterPopup = ref(false)
const draft = ref({ macAddress: '', agentId: '', chatType: '' as '' | 1 | 2 | 3 })
const keywordInput = ref('')

const quickRanges = computed(() => [
  { key: 1, label: t('chatRecords.today') },
  { key: 7, label: t('chatRecords.last7Days') },
  { key: 30, label: t('chatRecords.last30Days') },
  { key: 0, label: t('chatRecords.allTime') },
])

const roleOptions = computed(() => [
  { value: '' as const, label: t('chatRecords.allRoles') },
  { value: 1 as const, label: t('chatRecords.roleUser') },
  { value: 2 as const, label: t('chatRecords.roleAgent') },
  { value: 3 as const, label: t('chatRecords.roleTool') },
])

// 只有出现其他用户的数据时才展示归属标签，默认「只看自己」时保持界面简洁
const showOwnerTags = computed(() => {
  return sessionList.value.some(row => row.isOwn === false)
    || agentOptions.value.some(item => item.isOwn === false)
    || deviceOptions.value.some(item => item.isOwn === false)
})

const activeFilterCount = computed(() => {
  let count = 0
  if (filters.value.macAddress)
    count++
  if (filters.value.agentId)
    count++
  if (filters.value.chatType !== '')
    count++
  return count
})

// 弹窗里「设备」选项：选了智能体时只展示该智能体下的设备
const visibleDeviceOptions = computed(() => {
  if (!draft.value.agentId)
    return deviceOptions.value
  return deviceOptions.value.filter(item => item.agentId === draft.value.agentId)
})

function buildQuery(page: number, limit: number): ChatRecordQuery {
  const query: ChatRecordQuery = { page, limit }
  if (filters.value.macAddress)
    query.macAddress = filters.value.macAddress
  if (filters.value.agentId)
    query.agentId = filters.value.agentId
  if (filters.value.chatType !== '')
    query.chatType = filters.value.chatType
  const keyword = filters.value.keyword.trim()
  if (keyword)
    query.keyword = keyword
  const range = chinaRecentRange(recentDays.value)
  if (range) {
    query.startTime = range.startTime
    query.endTime = range.endTime
  }
  return query
}

// z-paging 查询列表
async function queryList(pageNo: number, pageSize: number) {
  try {
    const response = await getChatSessionRecords(buildQuery(pageNo, pageSize))
    pagingRef.value.complete(response.list || [])
  }
  catch (error) {
    console.error('获取会话记录失败:', error)
    pagingRef.value.complete(false)
    toast.error(t('chatRecords.loadFailed'))
  }
}

async function loadFilterOptions() {
  try {
    const options = await getChatRecordFilterOptions()
    agentOptions.value = options.agents || []
    deviceOptions.value = options.devices || []
  }
  catch (error) {
    console.error('获取筛选选项失败:', error)
    toast.error(t('chatRecords.filterFailed'))
  }
}

function reloadList() {
  pagingRef.value?.reload()
}

function selectQuickRange(key: number) {
  recentDays.value = key === 0 ? null : key
  reloadList()
}

function handleSearch() {
  filters.value.keyword = keywordInput.value
  reloadList()
}

function handleClearKeyword() {
  keywordInput.value = ''
  filters.value.keyword = ''
  reloadList()
}

function openFilterPopup() {
  draft.value = {
    macAddress: filters.value.macAddress,
    agentId: filters.value.agentId,
    chatType: filters.value.chatType,
  }
  showFilterPopup.value = true
}

function selectDraftAgent(agentId: string) {
  draft.value.agentId = draft.value.agentId === agentId ? '' : agentId
  // 切换智能体后，不属于该智能体的设备选择需要清除
  if (draft.value.macAddress) {
    const device = deviceOptions.value.find(item => item.macAddress === draft.value.macAddress)
    if (draft.value.agentId && device && device.agentId !== draft.value.agentId)
      draft.value.macAddress = ''
  }
}

function selectDraftDevice(macAddress: string) {
  draft.value.macAddress = draft.value.macAddress === macAddress ? '' : macAddress
}

function selectDraftRole(value: '' | 1 | 2 | 3) {
  draft.value.chatType = value
}

function applyFilters() {
  filters.value.macAddress = draft.value.macAddress
  filters.value.agentId = draft.value.agentId
  filters.value.chatType = draft.value.chatType
  showFilterPopup.value = false
  reloadList()
}

function resetFilters() {
  filters.value = { macAddress: '', agentId: '', chatType: '', keyword: '' }
  keywordInput.value = ''
  recentDays.value = null
  draft.value = { macAddress: '', agentId: '', chatType: '' }
  showFilterPopup.value = false
  reloadList()
}

function agentLabel(item: ChatFilterAgentOption) {
  const base = item.agentName || item.agentId || t('chatRecords.unnamedAgent')
  return showOwnerTags.value && hasOwner(item) && item.isOwn === false
    ? `${base} · ${ownerDisplayName(item)}`
    : base
}

function deviceLabel(item: ChatFilterDeviceOption) {
  const base = item.alias || t('chatRecords.unnamedDevice')
  const tail = item.macAddress ? ` (${item.macAddress})` : ''
  return `${base}${tail}`
}

function sessionTitle(row: ChatSessionRecord) {
  return row.sessionTitle || row.startedAtText || row.createdAtText || t('chatRecords.noTitle')
}

function openSession(row: ChatSessionRecord) {
  if (!row.sessionId || !row.agentId)
    return
  uni.navigateTo({
    url: `/pages/chat-history/detail?sessionId=${encodeURIComponent(row.sessionId)}&agentId=${encodeURIComponent(row.agentId)}`,
  })
}

function goBack() {
  uni.navigateBack()
}

// 支持从首页 / 设备页带 agentId / macAddress 进入并预筛选
onLoad((options) => {
  if (options?.agentId)
    filters.value.agentId = decodeURIComponent(options.agentId)
  if (options?.macAddress)
    filters.value.macAddress = decodeURIComponent(options.macAddress)
})

onMounted(() => {
  uni.setNavigationBarTitle({
    title: t('chatRecords.title'),
  })
  loadFilterOptions()
})
</script>

<template>
  <view class="min-h-screen bg-[#f5f7fb]">
    <wd-navbar :title="t('chatRecords.title')" safe-area-inset-top left-arrow placeholder fixed @click-left="goBack" />

    <z-paging
      ref="pagingRef" v-model="sessionList" :refresher-enabled="true" :auto-show-back-to-top="true"
      :default-page-size="10" :empty-view-text="t('chatRecords.noRecords')" empty-view-img=""
      :use-page-scroll="false" @query="queryList"
    >
      <template #top>
        <view class="bg-white px-[24rpx] pb-[16rpx] pt-[16rpx]">
          <!-- 关键词搜索 + 筛选入口 -->
          <view class="flex items-center gap-[16rpx]">
            <view class="flex-1 overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
              <wd-input
                v-model="keywordInput" type="text" clearable :maxlength="50"
                :placeholder="t('chatRecords.keywordPlaceholder')"
                custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
                input-class="text-[28rpx] text-[#232338]"
                @confirm="handleSearch" @clear="handleClearKeyword"
              />
            </view>
            <view
              class="relative rounded-[16rpx] bg-[#336cff] px-[28rpx] py-[18rpx] text-[26rpx] text-white"
              @click="handleSearch"
            >
              {{ t('chatRecords.search') }}
            </view>
            <view
              class="relative border border-[#336cff] rounded-[16rpx] bg-white px-[24rpx] py-[18rpx] text-[26rpx] text-[#336cff]"
              @click="openFilterPopup"
            >
              {{ t('chatRecords.filter') }}
              <text v-if="activeFilterCount > 0">
                ·{{ activeFilterCount }}
              </text>
            </view>
          </view>

          <!-- 快捷时间范围 -->
          <scroll-view scroll-x class="mt-[16rpx] whitespace-nowrap">
            <view class="inline-flex gap-[16rpx]">
              <view
                v-for="item in quickRanges" :key="item.key"
                class="chip" :class="{ 'chip-active': (item.key === 0 ? recentDays === null : recentDays === item.key) }"
                @click="selectQuickRange(item.key)"
              >
                {{ item.label }}
              </view>
            </view>
          </scroll-view>
          <text class="mt-[12rpx] block text-[22rpx] text-[#9d9ea3]">
            {{ t('chatRecords.chinaTime') }}
          </text>
        </view>
      </template>

      <view class="p-[24rpx]">
        <view
          v-for="row in sessionList" :key="row.sessionId"
          class="mb-[20rpx] border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[28rpx]"
          style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
          @click="openSession(row)"
        >
          <view class="flex items-center justify-between">
            <text class="flex-1 truncate text-[30rpx] text-[#232338] font-semibold">
              {{ sessionTitle(row) }}
            </text>
            <text class="ml-[16rpx] text-[24rpx] text-[#9d9ea3]">
              {{ t('chatRecords.messageCount', { count: row.chatCount }) }}
            </text>
          </view>

          <view class="mt-[12rpx] flex flex-wrap items-center gap-[12rpx]">
            <text class="text-[24rpx] text-[#65686f]">
              {{ row.agentName || row.agentId || t('chatRecords.unnamedAgent') }}
            </text>
            <text class="text-[24rpx] text-[#9d9ea3]">
              · {{ row.deviceAlias || row.macAddress || t('chatRecords.unnamedDevice') }}
            </text>
            <text v-if="row.hasAudio" class="tag tag-audio">
              {{ t('chatRecords.audio') }}
            </text>
            <text v-if="showOwnerTags && hasOwner(row)" class="tag" :class="row.isOwn === false ? 'tag-other' : 'tag-mine'">
              {{ row.isOwn === false ? t('common.ownerOther') : t('common.ownerMine') }} · {{ ownerDisplayName(row) }}
            </text>
          </view>

          <text v-if="row.summary" class="line-clamp-2 mt-[12rpx] block text-[26rpx] text-[#65686f]">
            {{ row.summary }}
          </text>

          <text class="mt-[12rpx] block text-[22rpx] text-[#9d9ea3]">
            {{ row.createdAtText || '-' }}
          </text>
        </view>
      </view>
    </z-paging>

    <!-- 筛选弹窗 -->
    <wd-popup v-model="showFilterPopup" position="bottom" custom-style="border-radius: 24rpx 24rpx 0 0;">
      <view class="p-[32rpx]">
        <text class="mb-[24rpx] block text-[32rpx] text-[#232338] font-bold">
          {{ t('chatRecords.filter') }}
        </text>

        <scroll-view scroll-y class="filter-scroll">
          <text class="filter-title">
            {{ t('chatRecords.agent') }}
          </text>
          <view class="mb-[24rpx] flex flex-wrap gap-[16rpx]">
            <view
              v-for="item in agentOptions" :key="item.agentId"
              class="chip" :class="{ 'chip-active': draft.agentId === item.agentId }"
              @click="selectDraftAgent(item.agentId)"
            >
              {{ agentLabel(item) }}
            </view>
            <text v-if="agentOptions.length === 0" class="text-[24rpx] text-[#9d9ea3]">
              {{ t('chatRecords.allAgents') }}
            </text>
          </view>

          <text class="filter-title">
            {{ t('chatRecords.device') }}
          </text>
          <view class="mb-[24rpx] flex flex-wrap gap-[16rpx]">
            <view
              v-for="item in visibleDeviceOptions" :key="item.macAddress"
              class="chip" :class="{ 'chip-active': draft.macAddress === item.macAddress }"
              @click="selectDraftDevice(item.macAddress)"
            >
              {{ deviceLabel(item) }}
            </view>
            <text v-if="visibleDeviceOptions.length === 0" class="text-[24rpx] text-[#9d9ea3]">
              {{ t('chatRecords.allDevices') }}
            </text>
          </view>

          <text class="filter-title">
            {{ t('chatRecords.role') }}
          </text>
          <view class="mb-[24rpx] flex flex-wrap gap-[16rpx]">
            <view
              v-for="item in roleOptions" :key="String(item.value)"
              class="chip" :class="{ 'chip-active': draft.chatType === item.value }"
              @click="selectDraftRole(item.value)"
            >
              {{ item.label }}
            </view>
          </view>
        </scroll-view>

        <view class="mt-[8rpx] flex gap-[16rpx]">
          <wd-button
            type="default"
            custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-white border-[#eeeeee] text-[#65686f]"
            @click="resetFilters"
          >
            {{ t('chatRecords.reset') }}
          </wd-button>
          <wd-button
            type="primary"
            custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-[#336cff] border-none"
            @click="applyFilters"
          >
            {{ t('common.confirm') }}
          </wd-button>
        </view>

        <view style="height: env(safe-area-inset-bottom);" />
      </view>
    </wd-popup>
  </view>
</template>

<style lang="scss" scoped>
.chip {
  padding: 12rpx 24rpx;
  font-size: 24rpx;
  color: #65686f;
  background: #f5f7fb;
  border: 1rpx solid #eeeeee;
  border-radius: 999rpx;
}

.chip-active {
  color: #336cff;
  background: #eef3ff;
  border-color: #336cff;
}

.filter-scroll {
  max-height: 55vh;
}

.filter-title {
  display: block;
  margin-bottom: 16rpx;
  font-size: 26rpx;
  font-weight: 600;
  color: #232338;
}

.tag {
  padding: 2rpx 14rpx;
  font-size: 20rpx;
  border-radius: 8rpx;
}

.tag-audio {
  color: #336cff;
  background: #eef3ff;
}

.tag-mine {
  color: #2f9e44;
  background: #ebfbee;
}

.tag-other {
  color: #e8590c;
  background: #fff4e6;
}
</style>

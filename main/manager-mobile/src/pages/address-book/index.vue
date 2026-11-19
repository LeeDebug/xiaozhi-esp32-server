<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationStyle": "custom",
    "navigationBarTitleText": "通讯录"
  }
}
</route>

<script lang="ts" setup>
import type { AddressBookItem } from '@/api/device/address-book'
import type { Device } from '@/api/device/types'
import { onLoad } from '@dcloudio/uni-app'
import { computed, ref } from 'vue'
import { getAgentList } from '@/api/agent/agent'
import { getAddressBookList, updateAddressBookAlias, updateAddressBookPermission } from '@/api/device/address-book'
import { getBindDevices } from '@/api/device/device'
import { t } from '@/i18n'
import { toast } from '@/utils/toast'

defineOptions({
  name: 'AddressBookPage',
})

// MAC 不区分大小写，所有比较都先统一转小写
function normalizeMac(mac: string | null | undefined) {
  return (mac || '').toLowerCase()
}

const macAddress = ref('')
const deviceAlias = ref('')
const loading = ref(false)

// 候选目标设备（当前用户自己的其他设备）
const candidates = ref<Device[]>([])
// 当前设备的通讯录条目，按对方 MAC（小写）索引
const bookMap = ref<Record<string, AddressBookItem>>({})
// 正在保存权限的对方 MAC，用于防止重复点击
const savingMacs = ref<string[]>([])

// 别名编辑
const editingTarget = ref('')
const aliasDraft = ref('')
const savingAlias = ref(false)

const rows = computed(() => {
  return candidates.value.map((device) => {
    const key = normalizeMac(device.macAddress)
    const book = bookMap.value[key]
    return {
      key,
      device,
      alias: book?.alias || '',
      hasPermission: book?.hasPermission === true,
    }
  })
})

const permittedCount = computed(() => rows.value.filter(row => row.hasPermission).length)

function displayName(row: { device: Device, alias: string }) {
  return row.alias || row.device.alias || row.device.macAddress
}

async function loadCandidates() {
  // 候选只取当前用户自己的设备。超管开启「查看全部数据」后列表里会混入他人设备，
  // 跟 web 端一致，不允许把他人设备当作呼叫目标。
  const agents = (await getAgentList()).filter(agent => agent.isOwn !== false)
  const lists = await Promise.all(agents.map(agent => getBindDevices(agent.id)))
  const own = lists.flat().filter(device => device.isOwn !== false)
  const self = normalizeMac(macAddress.value)
  const seen = new Set<string>()
  candidates.value = own.filter((device) => {
    const key = normalizeMac(device.macAddress)
    if (!key || key === self || seen.has(key))
      return false
    seen.add(key)
    return true
  })
}

async function loadAddressBook() {
  const list = await getAddressBookList(macAddress.value)
  const map: Record<string, AddressBookItem> = {}
  for (const item of list || []) {
    map[normalizeMac(item.targetMac)] = item
  }
  bookMap.value = map
}

async function loadAll() {
  if (!macAddress.value)
    return
  try {
    loading.value = true
    await Promise.all([loadCandidates(), loadAddressBook()])
  }
  catch (error) {
    console.error('获取通讯录失败:', error)
    toast.error(t('addressBook.loadFailed'))
  }
  finally {
    loading.value = false
  }
}

async function togglePermission(row: { key: string, device: Device, hasPermission: boolean }, checked: boolean) {
  if (savingMacs.value.includes(row.key))
    return
  const previous = bookMap.value[row.key]
  // 先乐观更新界面，失败再回滚
  bookMap.value = {
    ...bookMap.value,
    [row.key]: {
      macAddress: macAddress.value,
      targetMac: row.device.macAddress,
      alias: previous?.alias ?? null,
      hasPermission: checked,
    },
  }
  savingMacs.value = [...savingMacs.value, row.key]
  try {
    await updateAddressBookPermission({
      macAddress: macAddress.value,
      targetMac: row.device.macAddress,
      hasPermission: checked,
    })
  }
  catch (error) {
    // 保存本身失败：回滚到修改前的状态
    console.error('保存权限失败:', error)
    const next = { ...bookMap.value }
    if (previous)
      next[row.key] = previous
    else
      delete next[row.key]
    bookMap.value = next
    toast.error(t('addressBook.saveFailed'))
    savingMacs.value = savingMacs.value.filter(key => key !== row.key)
    return
  }

  // 保存已成功。从服务端重新读取（首次开启权限时服务端会自动创建条目并分配别名）。
  // 刷新失败不能回滚，否则界面会和服务端已生效的权限不一致；保留乐观结果即可。
  try {
    await loadAddressBook()
  }
  catch (error) {
    console.error('刷新通讯录失败:', error)
  }
  finally {
    savingMacs.value = savingMacs.value.filter(key => key !== row.key)
  }
}

function startEditAlias(row: { key: string, alias: string, device: Device }) {
  editingTarget.value = row.key
  aliasDraft.value = row.alias || row.device.alias || ''
}

function cancelEditAlias() {
  editingTarget.value = ''
  aliasDraft.value = ''
}

async function saveAlias(row: { key: string, device: Device }) {
  if (savingAlias.value)
    return
  const alias = aliasDraft.value.trim()
  if (!alias) {
    toast.warning(t('addressBook.aliasRequired'))
    return
  }
  savingAlias.value = true
  try {
    await updateAddressBookAlias({
      macAddress: macAddress.value,
      targetMac: row.device.macAddress,
      alias,
    })
  }
  catch (error) {
    // 保存失败：保留编辑框和草稿，让用户可以重试
    console.error('保存别名失败:', error)
    savingAlias.value = false
    return
  }

  // 保存已成功，先关闭编辑框，避免用户重复提交导致追加多余的重名后缀。
  cancelEditAlias()
  try {
    // 服务端会对重名自动追加数字后缀，必须重新拉取而不是直接使用本地输入值
    await loadAddressBook()
  }
  catch (error) {
    console.error('刷新通讯录失败:', error)
  }
  finally {
    savingAlias.value = false
  }
}

function goBack() {
  uni.navigateBack()
}

onLoad((options) => {
  macAddress.value = options?.macAddress ? decodeURIComponent(options.macAddress) : ''
  deviceAlias.value = options?.alias ? decodeURIComponent(options.alias) : ''
  loadAll()
})
</script>

<template>
  <view class="min-h-screen bg-[#f5f7fb]">
    <wd-navbar :title="t('addressBook.title')" safe-area-inset-top left-arrow placeholder fixed @click-left="goBack" />

    <view class="p-[24rpx]">
      <!-- 当前设备 -->
      <view
        class="mb-[24rpx] border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[28rpx]"
        style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
      >
        <text class="block text-[32rpx] text-[#232338] font-semibold">
          {{ deviceAlias || macAddress || '-' }}
        </text>
        <text class="mt-[8rpx] block text-[24rpx] text-[#9d9ea3]">
          {{ t('device.macAddress') }}：{{ macAddress || '-' }}
        </text>
        <text class="mt-[8rpx] block text-[24rpx] text-[#65686f]">
          {{ t('addressBook.permittedCount', { count: permittedCount }) }}
        </text>
      </view>

      <view v-if="loading" class="py-[80rpx] text-center text-[26rpx] text-[#9d9ea3]">
        {{ t('addressBook.loading') }}
      </view>

      <view v-else-if="rows.length === 0" class="py-[80rpx] text-center text-[26rpx] text-[#9d9ea3]">
        {{ t('addressBook.empty') }}
      </view>

      <view v-else class="flex flex-col gap-[16rpx]">
        <view
          v-for="row in rows" :key="row.key"
          class="border border-[#eeeeee] rounded-[20rpx] bg-[#fbfbfb] p-[24rpx]"
          style="box-shadow: 0 2rpx 12rpx rgba(0, 0, 0, 0.04);"
        >
          <view class="flex items-center justify-between">
            <view class="mr-[16rpx] min-w-0 flex-1">
              <text class="block truncate text-[30rpx] text-[#232338] font-medium">
                {{ displayName(row) }}
              </text>
              <text class="mt-[4rpx] block text-[22rpx] text-[#9d9ea3]">
                {{ row.device.macAddress }}
              </text>
            </view>
            <wd-switch
              :model-value="row.hasPermission"
              :disabled="savingMacs.includes(row.key)"
              size="24"
              @change="({ value }) => togglePermission(row, value === true)"
            />
          </view>

          <view class="mt-[16rpx]">
            <view v-if="editingTarget !== row.key" class="flex items-center">
              <text class="text-[24rpx] text-[#65686f]">
                {{ t('addressBook.alias') }}：{{ row.alias || '-' }}
              </text>
              <text class="ml-[16rpx] text-[24rpx] text-[#336cff]" @click="startEditAlias(row)">
                {{ t('addressBook.editAlias') }}
              </text>
            </view>

            <view v-else>
              <view class="overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
                <wd-input
                  v-model="aliasDraft" type="text" clearable :maxlength="32"
                  :placeholder="t('addressBook.aliasPlaceholder')"
                  custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
                  input-class="text-[28rpx] text-[#232338]"
                />
              </view>
              <view class="mt-[12rpx] flex gap-[16rpx]">
                <wd-button
                  type="primary" :loading="savingAlias"
                  custom-class="flex-1 h-[64rpx] rounded-[16rpx] text-[24rpx] bg-[#336cff] border-none"
                  @click="saveAlias(row)"
                >
                  {{ t('common.confirm') }}
                </wd-button>
                <wd-button
                  type="default" :disabled="savingAlias"
                  custom-class="flex-1 h-[64rpx] rounded-[16rpx] text-[24rpx] bg-white border-[#eeeeee] text-[#65686f]"
                  @click="cancelEditAlias"
                >
                  {{ t('common.cancel') }}
                </wd-button>
              </view>
            </view>
          </view>
        </view>
      </view>

      <view style="height: env(safe-area-inset-bottom);" />
    </view>
  </view>
</template>

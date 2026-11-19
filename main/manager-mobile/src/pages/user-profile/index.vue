<route lang="jsonc" type="page">
{
  "layout": "default",
  "style": {
    "navigationStyle": "custom",
    "navigationBarTitleText": "个人信息"
  }
}
</route>

<script lang="ts" setup>
import type { UserProfile } from '@/api/auth'
import { computed, onMounted, ref } from 'vue'
import { changePassword, getUserProfile, updateUserNickname } from '@/api/auth'
import { t } from '@/i18n'
import { useUserStore } from '@/store/user'
import { toast } from '@/utils/toast'

defineOptions({
  name: 'UserProfilePage',
})

const userStore = useUserStore()

const loading = ref(false)
const profile = ref<UserProfile | null>(null)

// 昵称编辑
const editingNickname = ref(false)
const nicknameDraft = ref('')
const savingNickname = ref(false)

// 修改密码弹窗
const showPasswordPopup = ref(false)
const savingPassword = ref(false)
const passwordForm = ref({
  password: '',
  newPassword: '',
  confirmPassword: '',
})

const displayName = computed(() => {
  return profile.value?.displayName || profile.value?.nickname || profile.value?.username || '-'
})

const roleText = computed(() => {
  return profile.value?.superAdmin === 1 ? t('userProfile.superAdmin') : t('userProfile.normalUser')
})

const statusText = computed(() => {
  return profile.value?.status === 0 ? t('userProfile.statusDisabled') : t('userProfile.statusNormal')
})

const scopeText = computed(() => {
  return profile.value?.canViewAllUserData ? t('userProfile.scopeAll') : t('userProfile.scopeOwn')
})

// 后端的 Date 经过 WebMvcConfig 序列化为北京时间字符串，这里直接展示，不再做时区换算
function formatDate(value: string | null | undefined) {
  return value || '-'
}

async function loadProfile() {
  try {
    loading.value = true
    profile.value = await getUserProfile()
  }
  catch (error) {
    console.error('获取个人信息失败:', error)
    toast.error(t('userProfile.loadFailed'))
  }
  finally {
    loading.value = false
  }
}

function startEditNickname() {
  nicknameDraft.value = profile.value?.nickname || ''
  editingNickname.value = true
}

function cancelEditNickname() {
  editingNickname.value = false
  nicknameDraft.value = ''
}

async function saveNickname() {
  if (savingNickname.value)
    return
  const nickname = nicknameDraft.value.trim()
  if (nickname.length > 64) {
    toast.warning(t('userProfile.nicknamePlaceholder'))
    return
  }
  try {
    savingNickname.value = true
    const latest = await updateUserNickname(nickname)
    profile.value = latest
    // 同步到本地 store，避免其他页面显示旧昵称
    userStore.setUserInfo({
      ...userStore.userInfo,
      nickname: latest.nickname || '',
    })
    editingNickname.value = false
  }
  catch (error) {
    console.error('修改昵称失败:', error)
  }
  finally {
    savingNickname.value = false
  }
}

function openPasswordPopup() {
  passwordForm.value = { password: '', newPassword: '', confirmPassword: '' }
  showPasswordPopup.value = true
}

function closePasswordPopup() {
  if (savingPassword.value)
    return
  showPasswordPopup.value = false
}

async function submitPassword() {
  if (savingPassword.value)
    return
  const { password, newPassword, confirmPassword } = passwordForm.value
  if (!password) {
    toast.warning(t('userProfile.oldPasswordRequired'))
    return
  }
  if (!newPassword) {
    toast.warning(t('userProfile.newPasswordRequired'))
    return
  }
  if (newPassword !== confirmPassword) {
    toast.warning(t('userProfile.passwordNotMatch'))
    return
  }
  if (newPassword === password) {
    toast.warning(t('userProfile.passwordSameAsOld'))
    return
  }

  try {
    savingPassword.value = true
    await changePassword({ password, newPassword })
    toast.success(t('userProfile.passwordChanged'))
    showPasswordPopup.value = false
    // 后端修改密码后会使 token 失效，必须清除本地登录态并重新登录
    setTimeout(() => {
      doLogout()
    }, 1200)
  }
  catch (error) {
    console.error('修改密码失败:', error)
  }
  finally {
    savingPassword.value = false
  }
}

function goBack() {
  uni.navigateBack()
}

async function doLogout() {
  await userStore.logout()
  uni.reLaunch({ url: '/pages/login/index' })
}

function handleLogout() {
  uni.showModal({
    title: t('userProfile.logout'),
    content: t('userProfile.logoutConfirm'),
    confirmText: t('common.confirm'),
    cancelText: t('common.cancel'),
    success: (res) => {
      if (res.confirm) {
        doLogout()
      }
    },
  })
}

onMounted(() => {
  uni.setNavigationBarTitle({
    title: t('userProfile.title'),
  })
  loadProfile()
})
</script>

<template>
  <view class="min-h-screen bg-[#f5f7fb]">
    <wd-navbar :title="t('userProfile.title')" safe-area-inset-top left-arrow placeholder fixed @click-left="goBack" />

    <view class="p-[24rpx]">
      <!-- 头部卡片 -->
      <view
        class="mb-[24rpx] flex items-center border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
        style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
      >
        <view class="mr-[24rpx] h-[112rpx] w-[112rpx] flex items-center justify-center rounded-full bg-[#336cff] text-[48rpx] text-white font-bold">
          {{ (displayName || '-').slice(0, 1).toUpperCase() }}
        </view>
        <view class="flex-1">
          <text class="block text-[36rpx] text-[#232338] font-bold">
            {{ displayName }}
          </text>
          <text class="mt-[8rpx] block text-[24rpx] text-[#9d9ea3]">
            {{ roleText }}
          </text>
        </view>
      </view>

      <!-- 基本信息 -->
      <view
        class="mb-[24rpx] border border-[#eeeeee] rounded-[24rpx] bg-[#fbfbfb] p-[32rpx]"
        style="box-shadow: 0 4rpx 20rpx rgba(0, 0, 0, 0.06);"
      >
        <view class="profile-row">
          <text class="profile-label">
            {{ t('userProfile.username') }}
          </text>
          <text class="profile-value">
            {{ profile?.username || '-' }}
          </text>
        </view>

        <view class="profile-row">
          <text class="profile-label">
            {{ t('userProfile.nickname') }}
          </text>
          <view v-if="!editingNickname" class="flex items-center">
            <text class="profile-value">
              {{ profile?.nickname || profile?.username || '-' }}
            </text>
            <text class="ml-[16rpx] text-[26rpx] text-[#336cff]" @click="startEditNickname">
              {{ t('userProfile.editNickname') }}
            </text>
          </view>
        </view>

        <view v-if="editingNickname" class="mb-[16rpx]">
          <view class="overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
            <wd-input
              v-model="nicknameDraft" type="text" clearable :maxlength="64"
              :placeholder="t('userProfile.nicknamePlaceholder')"
              custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
              input-class="text-[28rpx] text-[#232338]"
            />
          </view>
          <text class="mt-[8rpx] block text-[24rpx] text-[#9d9ea3]">
            {{ t('userProfile.nicknameTip') }}
          </text>
          <view class="mt-[16rpx] flex gap-[16rpx]">
            <wd-button
              type="primary" :loading="savingNickname"
              custom-class="flex-1 h-[72rpx] rounded-[16rpx] text-[26rpx] bg-[#336cff] border-none"
              @click="saveNickname"
            >
              {{ t('common.confirm') }}
            </wd-button>
            <wd-button
              type="default" :disabled="savingNickname"
              custom-class="flex-1 h-[72rpx] rounded-[16rpx] text-[26rpx] bg-white border-[#eeeeee] text-[#65686f]"
              @click="cancelEditNickname"
            >
              {{ t('common.cancel') }}
            </wd-button>
          </view>
        </view>

        <view class="profile-row">
          <text class="profile-label">
            {{ t('userProfile.status') }}
          </text>
          <text class="profile-value">
            {{ statusText }}
          </text>
        </view>

        <view class="profile-row">
          <text class="profile-label">
            {{ t('userProfile.createDate') }}
          </text>
          <text class="profile-value">
            {{ formatDate(profile?.createDate) }}
          </text>
        </view>

        <view class="profile-row">
          <text class="profile-label">
            {{ t('userProfile.deviceCount') }}
          </text>
          <text class="profile-value">
            {{ profile?.deviceCount ?? '-' }}
          </text>
        </view>

        <view class="profile-row">
          <text class="profile-label">
            {{ t('userProfile.agentCount') }}
          </text>
          <text class="profile-value">
            {{ profile?.agentCount ?? '-' }}
          </text>
        </view>

        <view class="profile-row !border-b-0">
          <text class="profile-label">
            {{ t('userProfile.dataScope') }}
          </text>
          <text class="profile-value">
            {{ scopeText }}
          </text>
        </view>
      </view>

      <!-- 操作 -->
      <view class="flex flex-col gap-[16rpx]">
        <wd-button
          type="primary"
          custom-class="h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-[#336cff] border-none"
          @click="openPasswordPopup"
        >
          {{ t('userProfile.changePassword') }}
        </wd-button>
        <wd-button
          type="default"
          custom-class="h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-white border-[#eeeeee] text-[#ff6b6b]"
          @click="handleLogout"
        >
          {{ t('userProfile.logout') }}
        </wd-button>
      </view>

      <view style="height: env(safe-area-inset-bottom);" />
    </view>

    <!-- 修改密码弹窗 -->
    <wd-popup v-model="showPasswordPopup" position="bottom" custom-style="border-radius: 24rpx 24rpx 0 0;" @close="closePasswordPopup">
      <view class="p-[32rpx]">
        <text class="mb-[24rpx] block text-[32rpx] text-[#232338] font-bold">
          {{ t('userProfile.changePassword') }}
        </text>

        <view class="mb-[16rpx] overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
          <wd-input
            v-model="passwordForm.password" type="text" clearable show-password :maxlength="64"
            :placeholder="t('userProfile.oldPassword')"
            custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
            input-class="text-[28rpx] text-[#232338]"
          />
        </view>
        <view class="mb-[16rpx] overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
          <wd-input
            v-model="passwordForm.newPassword" type="text" show-password clearable :maxlength="64"
            :placeholder="t('userProfile.newPassword')"
            custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
            input-class="text-[28rpx] text-[#232338]"
          />
        </view>
        <view class="mb-[24rpx] overflow-hidden border border-[#eeeeee] rounded-[16rpx] bg-[#f5f7fb]">
          <wd-input
            v-model="passwordForm.confirmPassword" type="text" show-password clearable :maxlength="64"
            :placeholder="t('userProfile.confirmPassword')"
            custom-class="!border-none !bg-transparent h-[64rpx] px-[24rpx] items-center"
            input-class="text-[28rpx] text-[#232338]"
          />
        </view>

        <view class="flex gap-[16rpx]">
          <wd-button
            type="primary" :loading="savingPassword"
            custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-[#336cff] border-none"
            @click="submitPassword"
          >
            {{ t('common.confirm') }}
          </wd-button>
          <wd-button
            type="default" :disabled="savingPassword"
            custom-class="flex-1 h-[88rpx] rounded-[20rpx] text-[28rpx] font-semibold bg-white border-[#eeeeee] text-[#65686f]"
            @click="closePasswordPopup"
          >
            {{ t('common.cancel') }}
          </wd-button>
        </view>

        <view style="height: env(safe-area-inset-bottom);" />
      </view>
    </wd-popup>
  </view>
</template>

<style lang="scss" scoped>
.profile-row {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 24rpx 0;
  border-bottom: 1rpx solid #f0f0f0;
}

.profile-label {
  font-size: 28rpx;
  color: #65686f;
}

.profile-value {
  font-size: 28rpx;
  color: #232338;
  font-weight: 500;
}
</style>

import { http } from '@/http/request/alova'

// 登录接口数据类型
export interface LoginData {
  username: string
  password: string
  captchaId: string
  areaCode?: string
  mobile?: string
}

// 登录响应数据类型
export interface LoginResponse {
  token: string
  expire: number
  clientHash: string
}

// 验证码响应数据类型
export interface CaptchaResponse {
  captchaId: string
  captchaImage: string
}

// 获取验证码
export function getCaptcha(uuid: string) {
  return http.Get<string>('/user/captcha', {
    params: { uuid },
    meta: {
      ignoreAuth: true,
      toast: false,
    },
  })
}

// 用户登录
export function login(data: LoginData) {
  return http.Post<LoginResponse>('/user/login', data, {
    meta: {
      ignoreAuth: true,
      toast: true,
    },
  })
}

// 用户信息响应数据类型
export interface UserInfo {
  id: number
  username: string
  realName: string
  email: string
  mobile: string
  status: number
  superAdmin: number
  nickname?: string | null
}

// 个人资料响应数据类型（对应后端 UserProfileVO）
export interface UserProfile {
  id: number
  username: string
  nickname: string | null
  displayName: string
  superAdmin: number
  status: number
  createDate: string | null
  updateDate: string | null
  deviceCount: number | null
  agentCount: number | null
  canViewAllUserData: boolean
}

// 服务端功能开关（对应 pub-config 里 systemWebMenu.features，与 manager-web 的 featureManager 一致）
export type FeatureKey
  = | 'voiceprintRecognition'
    | 'voiceClone'
    | 'knowledgeBase'
    | 'mcpAccessPoint'
    | 'vad'
    | 'asr'
    | 'addressBook'

export interface FeatureItem {
  name?: string
  enabled?: boolean
  description?: string
}

export interface SystemWebMenu {
  features?: Partial<Record<FeatureKey, FeatureItem>>
  groups?: Record<string, string[]>
}

// 公共配置响应数据类型
export interface PublicConfig {
  enableMobileRegister: boolean
  version: string
  year: string
  allowUserRegister: boolean
  mobileAreaList: Array<{
    name: string
    key: string
  }>
  beianIcpNum: string
  beianGaNum: string
  name: string
  sm2PublicKey: string
  // 服务端未配置该参数时不会返回此字段
  systemWebMenu?: SystemWebMenu | string | null
}

// 获取用户信息
export function getUserInfo() {
  return http.Get<UserInfo>('/user/info', {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

// 获取当前用户完整资料
export function getUserProfile() {
  return http.Get<UserProfile>('/user/profile', {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

// 修改当前用户昵称，返回最新资料
export function updateUserNickname(nickname: string) {
  return http.Put<UserProfile>('/user/profile', { nickname }, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

// 修改当前用户密码（后端修改成功后会使 token 失效，需重新登录）
export function changePassword(data: { password: string, newPassword: string }) {
  return http.Put('/user/change-password', data, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

// 获取公共配置
export function getPublicConfig() {
  return http.Get<PublicConfig>('/user/pub-config', {
    meta: {
      ignoreAuth: true,
      toast: false,
    },
  })
}

// 注册数据类型
export interface RegisterData {
  username: string
  password: string
  captchaId: string
  areaCode: string
  mobile: string
  mobileCaptcha: string
}

// 发送短信验证码
export function sendSmsCode(data: {
  phone: string
  captcha: string
  captchaId: string
}) {
  return http.Post('/user/smsVerification', data, {
    meta: {
      ignoreAuth: true,
      toast: false,
    },
  })
}

// 用户注册
export function register(data: RegisterData) {
  return http.Post('/user/register', data, {
    meta: {
      ignoreAuth: true,
      toast: true,
    },
  })
}

// 忘记密码数据类型
export interface ForgotPasswordData {
  phone: string
  code: string
  password: string
  captchaId: string
}

// 忘记密码（找回密码）
export function retrievePassword(data: ForgotPasswordData) {
  return http.Put('/user/retrieve-password', data, {
    meta: {
      ignoreAuth: true,
      toast: true,
    },
  })
}

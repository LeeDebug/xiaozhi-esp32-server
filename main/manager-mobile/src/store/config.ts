import type { FeatureKey, PublicConfig, SystemWebMenu } from '@/api/auth'
import { defineStore } from 'pinia'
import { ref } from 'vue'
import { getPublicConfig } from '@/api/auth'

// 初始化状态
const initialConfigState: PublicConfig = {
  enableMobileRegister: false,
  version: '',
  year: '',
  allowUserRegister: false,
  mobileAreaList: [],
  beianIcpNum: '',
  beianGaNum: '',
  sm2PublicKey: '',
  name: import.meta.env.VITE_APP_TITLE,
}

export const useConfigStore = defineStore(
  'config',
  () => {
    // 定义全局配置
    const config = ref<PublicConfig>({ ...initialConfigState })

    // 设置配置信息
    const setConfig = (val: PublicConfig) => {
      config.value = val
    }

    // 获取公共配置
    const fetchPublicConfig = async () => {
      try {
        const configData = await getPublicConfig()
        console.log(configData)

        setConfig(configData)
        return configData
      }
      catch (error) {
        console.error('获取公共配置失败:', error)
        throw error
      }
    }

    // 重置配置
    const resetConfig = () => {
      config.value = { ...initialConfigState }
    }

    // 解析服务端功能开关（systemWebMenu 可能是对象，也可能是 JSON 字符串）
    function parseSystemWebMenu(): SystemWebMenu | null {
      const raw = config.value?.systemWebMenu
      if (!raw)
        return null
      if (typeof raw === 'string') {
        try {
          return JSON.parse(raw) as SystemWebMenu
        }
        catch {
          return null
        }
      }
      return raw
    }

    /**
     * 某个功能是否对当前用户可见。
     * 关键约定：服务端没有下发配置、配置里没有该功能、或 enabled 未明确为 false 时，一律视为「启用」，
     * 这样老版本服务端或配置缺失时，手机端已有入口不会丢失（不同于 web 的「默认关闭」，避免回归）。
     */
    const isFeatureEnabled = (key: FeatureKey): boolean => {
      const feature = parseSystemWebMenu()?.features?.[key]
      if (!feature)
        return true
      return feature.enabled !== false
    }

    return {
      config,
      setConfig,
      fetchPublicConfig,
      resetConfig,
      isFeatureEnabled,
    }
  },
  {
    persist: {
      key: 'config',
      serializer: {
        serialize: state => JSON.stringify(state.config),
        deserialize: value => ({ config: JSON.parse(value) }),
      },
    },
  },
)

import { http } from '@/http/request/alova'

/**
 * 设备通讯录条目（对应后端 DeviceAddressBookEntity）。
 * macAddress 是本设备，targetMac 是对方设备，alias 是「我对对方的称呼」。
 * 注意：MAC 不区分大小写，比较时必须先统一转小写。
 */
export interface AddressBookItem {
  macAddress: string
  targetMac: string
  alias: string | null
  hasPermission: boolean | null
}

/** 获取某台设备的通讯录列表 */
export function getAddressBookList(macAddress: string) {
  return http.Get<AddressBookItem[]>(`/device/address-book/${encodeURIComponent(macAddress)}`, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
    cacheFor: {
      expire: 0,
    },
  })
}

/**
 * 更新「我对对方的称呼」。
 * 服务端会对重名自动追加数字后缀，因此保存后必须重新拉取列表，不要直接信任本地输入值。
 */
export function updateAddressBookAlias(data: { macAddress: string, targetMac: string, alias: string }) {
  return http.Put<null>('/device/address-book/alias', data, {
    meta: {
      ignoreAuth: false,
      toast: true,
    },
  })
}

/** 更新「是否允许该设备呼叫对方」的权限 */
export function updateAddressBookPermission(data: { macAddress: string, targetMac: string, hasPermission: boolean }) {
  return http.Put<null>('/device/address-book/permission', data, {
    meta: {
      ignoreAuth: false,
      toast: false,
    },
  })
}

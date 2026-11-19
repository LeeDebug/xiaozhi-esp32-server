/**
 * 数据归属展示名的统一口径（与 manager-web 的 ownerDisplay.js 保持一致）。
 *
 * 后端始终返回 ownerUsername（原始登录账号），并额外返回 ownerNickname（可为空的昵称）。
 * 所有「我的 / 其他用户 · XXX」标签都必须经过这里。
 * 昵称允许重复，因此它只用于展示，绝不可作为身份标识或鉴权依据。
 */
export interface OwnerInfo {
  ownerUsername?: string | null
  ownerNickname?: string | null
  isOwn?: boolean | null
}

export function ownerDisplayName(row?: OwnerInfo | null): string {
  if (!row)
    return ''
  const nickname = row.ownerNickname
  if (nickname != null && String(nickname).trim()) {
    return String(nickname).trim()
  }
  return row.ownerUsername || ''
}

export function hasOwner(row?: OwnerInfo | null): boolean {
  return !!ownerDisplayName(row)
}

"""按声纹（说话人）控制本轮可用的工具。

配置由 manager-api 按「设备 × 声纹」下发到 voiceprint_permission：

    {
      "enabled": true,
      "unknownPermissions": ["chat"],          # 未识别出声纹时的权限
      "defaultPermissions": ["chat", "device_read"],  # 声纹没有单独配置时的权限
      "unmatchedToolPolicy": "deny",            # 没有被任何权限项覆盖的工具: allow / deny
      "alwaysAllowTools": ["handle_exit_intent"],
      "items": [{"code": "device_read", "name": "查设备信息", "tools": ["self_*get*"]}],
      "speakers": {"<声纹id>": ["chat", "device_read"]}
    }

工具名用 fnmatch 通配（不区分大小写），按 items 顺序取第一个命中的权限项，
因此更具体的权限项（如 TTL 设备）要排在通用权限项前面。权限码 all 表示全部权限。
"""

import fnmatch
from typing import Any, Dict, Iterable, List, NamedTuple, Optional, Set

ALL_PERMISSION = "all"
# 虚拟工具与退出意图任何人都可以用，否则受限用户连「再见」都说不了
BUILTIN_ALWAYS_ALLOW = {"direct_answer", "handle_exit_intent"}


class PermissionItem(NamedTuple):
    code: str
    name: str
    patterns: List[str]


def _codes(value: Any) -> Set[str]:
    if value is None:
        return set()
    if isinstance(value, str):
        value = value.split(",")
    if not isinstance(value, (list, tuple, set)):
        return set()
    return {str(v).strip() for v in value if str(v).strip()}


class VoiceprintPermissionPolicy:
    def __init__(self, config: Optional[dict]):
        config = config if isinstance(config, dict) else {}
        enabled = config.get("enabled", False)
        self.enabled = enabled is True or str(enabled).strip().lower() == "true"
        self.unknown_permissions = _codes(config.get("unknownPermissions"))
        self.default_permissions = _codes(config.get("defaultPermissions"))
        self.allow_unmatched = (
            str(config.get("unmatchedToolPolicy", "deny")).strip().lower() == "allow"
        )
        self.always_allow = {t.lower() for t in BUILTIN_ALWAYS_ALLOW} | {
            t.lower() for t in _codes(config.get("alwaysAllowTools"))
        }
        self.items: List[PermissionItem] = []
        for item in config.get("items") or []:
            if not isinstance(item, dict):
                continue
            code = str(item.get("code") or "").strip()
            if not code:
                continue
            patterns = [
                str(p).strip().lower() for p in (item.get("tools") or []) if str(p).strip()
            ]
            self.items.append(
                PermissionItem(code, str(item.get("name") or code), patterns)
            )
        speakers = config.get("speakers") or {}
        self.speaker_permissions: Dict[str, Set[str]] = (
            {str(k): _codes(v) for k, v in speakers.items()}
            if isinstance(speakers, dict)
            else {}
        )

    @staticmethod
    def _speaker_id(speaker) -> Optional[str]:
        speaker_id = getattr(speaker, "speaker_id", None)
        if isinstance(speaker_id, str) and speaker_id.strip():
            return speaker_id.strip()
        return None

    def permissions_for(self, speaker) -> Set[str]:
        speaker_id = self._speaker_id(speaker)
        if speaker_id is None:
            return self.unknown_permissions
        return self.speaker_permissions.get(speaker_id, self.default_permissions)

    def required_item(self, tool_name: str) -> Optional[PermissionItem]:
        name = (tool_name or "").lower()
        for item in self.items:
            if any(fnmatch.fnmatchcase(name, p) for p in item.patterns):
                return item
        return None

    def is_allowed(self, tool_name: str, speaker) -> bool:
        if not self.enabled:
            return True
        if (tool_name or "").lower() in self.always_allow:
            return True
        permissions = self.permissions_for(speaker)
        if ALL_PERMISSION in permissions:
            return True
        item = self.required_item(tool_name)
        if item is None:
            return self.allow_unmatched
        return item.code in permissions

    def filter_functions(self, functions: Iterable[dict], speaker) -> List[dict]:
        """只把有权限的工具交给 LLM，模型就不会去调用没权限的工具。"""
        functions = list(functions or [])
        if not self.enabled:
            return functions
        return [
            f
            for f in functions
            if self.is_allowed(((f or {}).get("function") or {}).get("name", ""), speaker)
        ]

    def deny_message(self, tool_name: str, speaker) -> str:
        if self._speaker_id(speaker) is None:
            return "抱歉，我没能识别出你的声纹，暂时不能帮你执行这个操作。"
        item = self.required_item(tool_name)
        if item is None:
            return "抱歉，你没有执行这个操作的权限。"
        return f"抱歉，你没有「{item.name}」权限，不能执行这个操作。"

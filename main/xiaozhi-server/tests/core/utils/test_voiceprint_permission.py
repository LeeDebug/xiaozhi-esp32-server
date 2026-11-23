"""声纹权限：按说话人过滤工具并拦截越权调用。"""
from core.utils.voiceprint_permission import VoiceprintPermissionPolicy
from core.utils.voiceprint_provider import SpeakerIdentification

CONFIG = {
    "enabled": True,
    "unknownPermissions": ["chat"],
    "defaultPermissions": ["chat", "device_read"],
    "unmatchedToolPolicy": "deny",
    "items": [
        {"code": "chat", "name": "闲聊", "tools": ["get_weather"]},
        {"code": "ttl_control", "name": "操作TTL设备", "tools": ["*ttl*set*"]},
        {"code": "ttl_read", "name": "查TTL设备信息", "tools": ["*ttl*"]},
        {"code": "device_read", "name": "查设备信息", "tools": ["self_*get*"]},
        {"code": "knowledge_base", "name": "查知识库", "tools": ["search_from_ragflow"]},
    ],
    "speakers": {
        "A": ["device_read"],
        "B": ["ttl_read", "ttl_control"],
        "C": ["knowledge_base"],
        "D": ["all"],
    },
}


def _sp(sid):
    return SpeakerIdentification(sid, sid)


def _fn(name):
    return {"type": "function", "function": {"name": name}}


def test_disabled_allows_everything():
    policy = VoiceprintPermissionPolicy({**CONFIG, "enabled": False})
    assert policy.is_allowed("self_ttl_set_power", None)


def test_unknown_speaker_only_chat():
    policy = VoiceprintPermissionPolicy(CONFIG)
    assert policy.is_allowed("get_weather", None)
    assert not policy.is_allowed("self_get_volume", None)
    assert policy.is_allowed("handle_exit_intent", None)


def test_first_matching_item_wins():
    policy = VoiceprintPermissionPolicy(CONFIG)
    assert policy.required_item("self_ttl_set_power").code == "ttl_control"
    assert policy.required_item("self_ttl_get_temp").code == "ttl_read"
    assert policy.required_item("self_get_volume").code == "device_read"


def test_speaker_permissions():
    policy = VoiceprintPermissionPolicy(CONFIG)
    assert policy.is_allowed("self_get_volume", _sp("A"))
    assert not policy.is_allowed("self_ttl_get_temp", _sp("A"))
    assert policy.is_allowed("self_ttl_set_power", _sp("B"))
    assert not policy.is_allowed("self_get_volume", _sp("C"))
    assert policy.is_allowed("search_from_ragflow", _sp("C"))
    assert policy.is_allowed("anything_unmatched", _sp("D"))


def test_unconfigured_voiceprint_uses_default():
    policy = VoiceprintPermissionPolicy(CONFIG)
    assert policy.is_allowed("self_get_volume", _sp("new"))
    assert not policy.is_allowed("search_from_ragflow", _sp("new"))


def test_unmatched_tool_policy():
    assert not VoiceprintPermissionPolicy(CONFIG).is_allowed("foo", _sp("A"))
    allow = VoiceprintPermissionPolicy({**CONFIG, "unmatchedToolPolicy": "allow"})
    assert allow.is_allowed("foo", _sp("A"))


def test_filter_functions():
    policy = VoiceprintPermissionPolicy(CONFIG)
    names = [
        f["function"]["name"]
        for f in policy.filter_functions(
            [_fn("get_weather"), _fn("self_get_volume"), _fn("search_from_ragflow")],
            _sp("A"),
        )
    ]
    assert names == ["self_get_volume"]


def test_deny_message_mentions_item_name():
    policy = VoiceprintPermissionPolicy(CONFIG)
    assert "操作TTL设备" in policy.deny_message("self_ttl_set_power", _sp("A"))
    assert "声纹" in policy.deny_message("self_ttl_set_power", None)

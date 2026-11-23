import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
    // 获取设备上各声纹用户的权限
    getVoiceprintPermission(deviceId, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/voiceprint-permission/${deviceId}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                failCallback && failCallback(err);
            })
            .networkFail(() => {
                RequestService.reAjaxFun(() => {
                    this.getVoiceprintPermission(deviceId, callback, failCallback);
                });
            }).send();
    },
    // 保存设备上各声纹用户的权限；permissions 为 null 表示恢复默认
    saveVoiceprintPermission(deviceId, payload, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/voiceprint-permission/${deviceId}`)
            .method('PUT')
            .data(payload)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                failCallback && failCallback(err);
            })
            .networkFail(() => {
                RequestService.reAjaxFun(() => {
                    this.saveVoiceprintPermission(deviceId, payload, callback, failCallback);
                });
            }).send();
    },
    // 已绑设备
    getAgentBindDevices(agentId, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/bind/${agentId}`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) {
                    failCallback(err);
                }
            })
            .networkFail((err) => {
                console.error('获取设备列表失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getAgentBindDevices(agentId, callback, failCallback);
                });
            }).send();
    },
    // 当前用户可见的全部设备（不限智能体，设备分析看板使用）
    getAllVisibleDevices(callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/list-all`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) {
                    failCallback(err);
                }
            })
            .networkFail((err) => {
                console.error('获取全部设备列表失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getAllVisibleDevices(callback, failCallback);
                });
            }).send();
    },
    // 解绑设备
    unbindDevice(device_id, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/unbind`)
            .method('POST')
            .data({ deviceId: device_id })
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) {
                    failCallback(err);
                }
            })
            .networkFail((err) => {
                console.error('解绑设备失败:', err);
                RequestService.reAjaxFun(() => {
                    this.unbindDevice(device_id, callback, failCallback);
                });
            }).send();
    },
    // 绑定设备
    bindDevice(agentId, deviceCode, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/bind/${agentId}/${deviceCode}`)
            .method('POST')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) {
                    failCallback(err);
                }
            })
            .networkFail((err) => {
                console.error('绑定设备失败:', err);
                RequestService.reAjaxFun(() => {
                    this.bindDevice(agentId, deviceCode, callback, failCallback);
                });
            }).send();
    },
    updateDeviceInfo(id, payload, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/update/${id}`)
            .method('PUT')
            .data(payload)
            .success((res) => {
                RequestService.clearRequestTime()
                callback(res)
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) {
                    failCallback(err);
                }
            })
            .networkFail((err) => {
                console.error('更新OTA状态失败:', err)
                this.$message.error(err.msg || '更新OTA状态失败')
                RequestService.reAjaxFun(() => {
                    this.updateDeviceInfo(id, payload, callback, failCallback)
                })
            }).send()
    },
    // 手动添加设备
    manualAddDevice(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/manual-add`)
            .method('POST')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) {
                    failCallback(err);
                }
            })
            .networkFail((err) => {
                console.error('手动添加设备失败:', err);
                RequestService.reAjaxFun(() => {
                    this.manualAddDevice(params, callback, failCallback);
                });
            }).send();
    },
    // 获取设备状态
    getDeviceStatus(agentId, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/device/bind/${agentId}`)
            .method('POST')
            .data({}) // 发送空对象作为请求体
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) {
                    failCallback(err);
                }
            })
            .networkFail((err) => {
                console.error('获取设备状态失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getDeviceStatus(agentId, callback, failCallback);
                });
            }).send();
    },
}

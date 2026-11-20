import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
    // 以会话（session_id）为维度的多条件分页查询，页面列表与弹窗左侧共用
    getSessions(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-history/sessions`)
            .method('GET')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) failCallback(err);
            })
            .networkFail((err) => {
                console.error('获取聊天会话失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getSessions(params, callback, failCallback);
                });
            }).send();
    },

    // 单条消息粒度的多条件分页查询（保留供其它场景使用）
    getRecords(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-history/records`)
            .method('GET')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) failCallback(err);
            })
            .networkFail((err) => {
                console.error('获取会话记录失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getRecords(params, callback, failCallback);
                });
            }).send();
    },

    // 会话分析看板聚合统计（支持按设备 / 智能体 / 时间范围筛选）
    getAnalytics(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-history/analytics`)
            .method('GET')
            .data(params)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) failCallback(err);
            })
            .networkFail((err) => {
                console.error('获取会话分析数据失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 设备 / 智能体筛选下拉选项
    getFilterOptions(callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-history/filter-options`)
            .method('GET')
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) failCallback(err);
            })
            .networkFail((err) => {
                console.error('获取会话记录筛选项失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getFilterOptions(callback, failCallback);
                });
            }).send();
    }
};

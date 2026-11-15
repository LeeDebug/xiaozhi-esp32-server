import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
    // 多条件分页查询聊天记录
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
                console.error('获取聊天记录失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getRecords(params, callback, failCallback);
                });
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
                console.error('获取聊天记录筛选项失败:', err);
                RequestService.reAjaxFun(() => {
                    this.getFilterOptions(callback, failCallback);
                });
            }).send();
    }
};

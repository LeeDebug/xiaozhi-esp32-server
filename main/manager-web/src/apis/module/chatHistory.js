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

    // 会话内容分析（词云）：用户 / 智能体消息正文分词后的高频词
    getWordCloud(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-history/word-cloud`)
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
                console.error('获取会话内容词云失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 会话内容分析丨高频方向：按地区 / 智能体 / 设备统计用户提问的高频词
    getChatDirection(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-direction/stat`)
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
                console.error('获取高频方向统计失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 地区筛选下拉：当前可见设备所在的省份
    getChatDirectionProvinces(callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-direction/provinces`)
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
                console.error('获取地区筛选项失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 预览大模型提示词：只组装内容，不调用大模型，不消耗次数
    previewChatDirectionPrompt(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-direction/preview`)
            .method('POST')
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
                console.error('预览大模型提示词失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 通过 AI 大模型总结当前筛选结果的高频方向。大模型调用较慢，超时放宽到 3 分钟
    analyzeChatDirection(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-direction/analyze`)
            .method('POST')
            .data(params)
            .timeout(180000)
            .success((res) => {
                RequestService.clearRequestTime();
                callback(res);
            })
            .fail((err) => {
                RequestService.clearRequestTime();
                if (failCallback) failCallback(err);
            })
            .networkFail((err) => {
                console.error('大模型分析高频方向失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 最近一次大模型分析记录，页面打开时默认展示
    getLatestChatDirectionAnalysis(callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-direction/latest`)
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
                console.error('获取最近一次分析记录失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 一次大模型分析的原始记录：当次提示词、用户问题、模型、耗时、统计快照
    getChatDirectionAnalysisDetail(id, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-direction/history/${encodeURIComponent(id)}`)
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
                console.error('获取分析原始记录失败:', err);
                if (failCallback) failCallback(err);
            }).send();
    },

    // 大模型分析历史记录
    getChatDirectionHistory(params, callback, failCallback) {
        RequestService.sendRequest()
            .url(`${getServiceUrl()}/agent/chat-direction/history`)
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
                console.error('获取分析历史记录失败:', err);
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

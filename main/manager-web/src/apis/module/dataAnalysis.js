import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
  /**
   * 数据预览分页
   */
  preview(params, callback) {
    const queryParams = new URLSearchParams({
      keyword: params.keyword || '',
      metric: params.metric || '',
      page: params.page || 1,
      limit: params.limit || 12,
    }).toString();
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/preview?${queryParams}`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.preview(params, callback);
        });
      }).send();
  },

  /**
   * 数据分析统计
   */
  statistics(callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/statistics`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.statistics(callback);
        });
      }).send();
  },

  /**
   * 指标列表
   */
  metrics(callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/metrics`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.metrics(callback);
        });
      }).send();
  },

  /**
   * 数据预测
   */
  predict(params, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/predict`)
      .method('POST')
      .data(params)
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.predict(params, callback);
        });
      }).send();
  },

  /**
   * 一键生成演示数据
   */
  seed(callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/demo/seed`)
      .method('POST')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.seed(callback);
        });
      }).send();
  },

  /**
   * 设备数据上报落库（公开接口）
   */
  upload(payload, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/upload`)
      .method('POST')
      .data(payload)
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.upload(payload, callback);
        });
      }).send();
  },
};

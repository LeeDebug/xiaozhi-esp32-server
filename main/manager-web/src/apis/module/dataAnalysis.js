import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
  /**
   * 数据预览分页
   */
  preview(params, callback) {
    const queryParams = new URLSearchParams({
      deviceId: params.deviceId || '',
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
   * 数据预览看板聚合统计
   * 注意：无论成功、业务失败还是网络失败都要回调，否则页面 loading 会永远为 true，
   * 图表区既不渲染图表也不渲染空状态，表现为整块空白。
   */
  dashboard(callback) {
    const failResult = (msg) => ({ data: { code: -1, msg: msg || '' } })
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/dashboard`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .fail((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        callback(failResult());
        RequestService.reAjaxFun(() => {
          this.dashboard(callback);
        });
      }).send();
  },

  /**
   * 上报设备列表
   */
  devices(callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/devices`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.devices(callback);
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
   * 一键生成演示数据（管理员）
   * 同样保证失败也回调，避免调用方按钮永久 loading。
   */
  seed(callback) {
    const failResult = (msg) => ({ data: { code: -1, msg: msg || '' } })
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-analysis/demo/seed`)
      .method('POST')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .fail((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        callback(failResult());
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

import { getServiceUrl } from '../api';
import RequestService from '../httpRequest';

export default {
  /**
   * 数据上报配置分页列表
   */
  page(params, callback) {
    const queryParams = new URLSearchParams({
      page: params.page,
      limit: params.limit,
    }).toString();
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-report-config/page?${queryParams}`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.page(params, callback);
        });
      }).send();
  },

  /**
   * 数据上报配置详情
   */
  getById(id, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-report-config/${id}`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.getById(id, callback);
        });
      }).send();
  },

  /**
   * 新增数据上报配置
   */
  create(data, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-report-config`)
      .method('POST')
      .data(data)
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.create(data, callback);
        });
      }).send();
  },

  /**
   * 修改数据上报配置
   */
  update(id, data, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-report-config/${id}`)
      .method('PUT')
      .data(data)
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.update(id, data, callback);
        });
      }).send();
  },

  /**
   * 删除数据上报配置
   */
  remove(id, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-report-config/${id}`)
      .method('DELETE')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.remove(id, callback);
        });
      }).send();
  },

  /**
   * 设为默认/生效配置
   */
  setDefault(id, callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-report-config/${id}/default`)
      .method('POST')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.setDefault(id, callback);
        });
      }).send();
  },

  /**
   * 设备开机拉取的数据上报配置（公开接口）
   */
  current(callback) {
    RequestService.sendRequest()
      .url(`${getServiceUrl()}/data-report-config/current`)
      .method('GET')
      .success((res) => {
        RequestService.clearRequestTime();
        callback(res);
      })
      .networkFail((err) => {
        console.error('请求失败:', err);
        RequestService.reAjaxFun(() => {
          this.current(callback);
        });
      }).send();
  },
};

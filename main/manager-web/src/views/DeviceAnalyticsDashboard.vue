<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="analytics-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('deviceAnalytics.title') }}</h2>
              <span class="page-desc">{{ $t('deviceAnalytics.desc') }}</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-refresh" size="medium" :loading="loading" @click="loadDevices">
                  {{ $t('deviceAnalytics.refresh') }}
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <div v-if="loadError" class="load-error">
              {{ loadError }}
              <el-button type="text" size="mini" @click="loadDevices">{{ $t('deviceAnalytics.retry') }}</el-button>
            </div>
            <div class="map-wrap" v-loading="loading">
              <DeviceGeoMap v-model="regionSelection" :devices="deviceList" fill />
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from '@/apis/api';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import CustomButton from '@/components/CustomButton.vue';
import DeviceGeoMap from '@/components/DeviceGeoMap.vue';

export default {
  name: 'DeviceAnalyticsDashboard',
  components: { HeaderBar, VersionFooter, CustomButton, DeviceGeoMap },
  data() {
    return {
      loading: false,
      loadError: '',
      deviceList: [],
      // 地图选中的地域 { province, city, district }
      regionSelection: {},
    };
  },
  mounted() {
    this.loadDevices();
  },
  methods: {
    loadDevices() {
      this.loading = true;
      this.loadError = '';
      Api.device.getAllVisibleDevices(
        ({ data }) => {
          this.loading = false;
          if (data && data.code === 0) {
            this.deviceList = (data.data || []).map((device) => ({
              device_id: device.id,
              province: device.province || '',
              city: device.city || '',
              district: device.district || '',
              regionSource: device.regionSource || '',
            }));
          } else {
            this.loadError = (data && data.msg) || this.$t('deviceAnalytics.loadFailed');
          }
        },
        (err) => {
          this.loading = false;
          this.loadError = (err && err.msg) || this.$t('deviceAnalytics.loadFailed');
        },
      );
    },
  },
};
</script>

<style lang="scss" scoped>
.welcome {
  min-width: 900px;
  min-height: 506px;
  height: 100vh;
  display: flex;
  flex-direction: column;
  background: var(--sys-surface);
  overflow: hidden;
}

.main-wrapper {
  height: calc(100vh - 63px - 35px);
  padding: 20px 22px 0;
  box-sizing: border-box;
  flex: 1;
  min-height: 0;
}

.content-panel {
  display: flex;
  height: 100%;
  border-radius: 15px;
  border: 1px solid #fff;
  overflow: hidden;
}

.content-area {
  flex: 1;
  min-width: 600px;
  min-height: 0;
  overflow: hidden;
  background: #fff;
  border-radius: 15px;
  display: flex;
  flex-direction: column;
}

.analytics-card {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  background: #fff;
  border: none;
  box-shadow: none;
}

.analytics-card ::v-deep .el-card__body {
  flex: 1;
  min-height: 0;
  display: flex;
  flex-direction: column;
  padding: 22px 26px 20px;
}

.page-header {
  display: flex;
  align-items: baseline;
  gap: 12px;
  flex-wrap: wrap;
}

.page-title {
  font-weight: 500;
  font-size: 24px;
  margin: 0;
}

.page-desc {
  font-size: 13px;
  color: #8a93b8;
}

.header-actions {
  margin-left: auto;
  display: flex;
  gap: 8px;
}

.divider {
  height: 1px;
  background: #f3f1f1;
  margin: 16px 0;
}

.load-error {
  margin-bottom: 8px;
  font-size: 13px;
  color: #e8554e;
}

.map-wrap {
  flex: 1;
  min-height: 0;
}
</style>

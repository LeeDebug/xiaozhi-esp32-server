<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="device-card" shadow="never">
            <div class="operation-header">
              <h2 class="page-title">{{ $t('device.management') }}</h2>
              <div class="right-operations">
                <el-input :placeholder="$t('device.searchPlaceholder')" v-model="searchKeyword" class="search-input"
                  @keyup.enter.native="handleSearch" clearable />
                <CustomButton icon="el-icon-search" type="confirm" @click="handleSearch">{{ $t('device.search') }}</CustomButton>
              </div>
            </div>
            <CustomTable
              ref="deviceTable"
              :data="paginatedDeviceList"
              :columns="tableColumns"
              :loading="loading"
              :loading-text="$t('deviceManagement.loading')"
              :show-selection="true"
              :show-operations="true"
              :operations-label="$t('device.operation')"
              :operations-width="300"
              :total="filteredDeviceList.length"
              :current-page="currentPage"
              :page-size="pageSize"
              :page-size-options="pageSizeOptions"
              @size-change="handlePageSizeChange"
              @page-change="goToPage"
            >
              <template slot="model" slot-scope="scope">
                {{ getFirmwareTypeName(scope.row.model) }}
              </template>
              <template slot="macAddress" slot-scope="scope">
                <MacAddressMask :macAddress="scope.row.macAddress" />
              </template>
              <template slot="deviceStatus" slot-scope="scope">
                <el-tag v-if="scope.row.deviceStatus === 'online'" type="success">{{ $t('device.online') }}</el-tag>
                <el-tag v-else type="danger">{{ $t('device.offline') }}</el-tag>
              </template>
              <template slot="region" slot-scope="scope">
                <span class="region-view" :class="{ 'is-manual': scope.row.regionSource === 'manual' }"
                  @click="openRegionEdit(scope.row)">
                  <i class="el-icon-location-outline"></i>
                  <span v-if="regionText(scope.row)">{{ regionText(scope.row) }}</span>
                  <span v-else class="region-empty">{{ $t('deviceGeo.notLocated') }}</span>
                </span>
              </template>
              <template slot="alias" slot-scope="scope">
                <el-input v-show="scope.row.isEdit" v-model="scope.row.alias" size="mini" maxlength="64" show-word-limit
                  @blur="onAliasBlur(scope.row)" @keyup.enter.native="onAliasEnter(scope.row)" />
                <span v-show="!scope.row.isEdit" class="alias-view">
                  <i class="el-icon-edit" @click="scope.row.isEdit = true" style="cursor: pointer;"></i>
                  <span @click="scope.row.isEdit = true">
                    {{ scope.row.alias || '-' }}
                  </span>
                </span>
              </template>
              <template slot="otaSwitch" slot-scope="scope">
                <el-switch v-model="scope.row.otaSwitch" size="mini" active-color="#13ce66" inactive-color="#ff4949"
                  @change="handleOtaSwitchChange(scope.row)"></el-switch>
              </template>
              <template slot="ownerUsername" slot-scope="scope">
                <el-tag v-if="hasOwner(scope.row)" size="mini"
                  :type="scope.row.isOwn === false ? 'warning' : 'success'">
                  {{ scope.row.isOwn === false ? $t('common.ownerOther') : $t('common.ownerMine') }} · {{ ownerDisplayName(scope.row) }}
                </el-tag>
                <span v-else>-</span>
              </template>
              <template slot="operations" slot-scope="scope">
                <el-button size="mini" type="text" @click="handleUnbind(scope.row.device_id)">
                  {{ $t('device.unbind') }}
                </el-button>
                <el-button v-if="isGenerate(scope.row)" size="mini" type="text" @click="handleGenertor(scope.row)">
                  {{ $t('device.deviceThemeGeneration') }}
                </el-button>
                <el-button size="mini" type="text" @click="handleViewChatHistory(scope.row)">
                  {{ $t('chatRecords.deviceEntry') }}
                </el-button>
                <el-button v-if="voiceprintFeatureEnabled" size="mini" type="text"
                  @click="handleVoiceprintPermission(scope.row)">
                  {{ $t('device.voiceprintPermission') }}
                </el-button>
              </template>
              <template slot="footer-btns">
                <div class="ctrl_btn">
                  <CustomButton :icon="isCurrentPageAllSelected ? 'el-icon-circle-close' : 'el-icon-circle-check'" size="small" @click="handleSelectAll">
                    {{ isCurrentPageAllSelected ? $t('common.deselectAll') : $t('common.selectAll') }}
                  </CustomButton>
                  <CustomButton v-if="currentAgentId" icon="el-icon-plus" type="add" size="small" @click="handleAddDevice">
                    {{ $t('device.bindWithCode') }}
                  </CustomButton>
                  <CustomButton v-if="currentAgentId" icon="el-icon-plus" type="add" size="small" @click="handleManualAddDevice">
                    {{ $t('device.manualAdd') }}
                  </CustomButton>
                  <CustomButton size="small" type="delete" icon="el-icon-delete" @click="deleteSelected">
                    {{ $t('device.unbind') }}
                  </CustomButton>
                </div>
              </template>
            </CustomTable>
          </el-card>
        </div>
      </div>
    </div>

    <el-dialog :title="$t('deviceGeo.editTitle')" :visible.sync="regionDialogVisible" width="460px" append-to-body
      :close-on-click-modal="false">
      <el-form label-width="72px" size="small">
        <el-form-item :label="$t('deviceGeo.province')">
          <el-select v-model="regionForm.province" clearable filterable style="width: 100%"
            :placeholder="$t('deviceGeo.pleaseSelect')" @change="onProvinceChange">
            <el-option v-for="item in provinceOptions" :key="item.adcode" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('deviceGeo.city')">
          <el-select v-model="regionForm.city" clearable filterable style="width: 100%" :disabled="!regionForm.province"
            :loading="regionOptionLoading" :placeholder="$t('deviceGeo.pleaseSelect')" @change="onCityChange">
            <el-option v-for="item in cityOptions" :key="item.adcode" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <el-form-item :label="$t('deviceGeo.district')">
          <el-select v-model="regionForm.district" clearable filterable style="width: 100%"
            :disabled="!regionForm.city" :loading="regionOptionLoading" :placeholder="$t('deviceGeo.pleaseSelect')">
            <el-option v-for="item in districtOptions" :key="item.adcode" :label="item.name" :value="item.name" />
          </el-select>
        </el-form-item>
        <div class="region-tip">{{ $t('deviceGeo.editTip') }}</div>
      </el-form>
      <span slot="footer" class="dialog-footer">
        <el-button size="small" @click="regionDialogVisible = false">{{ $t('button.cancel') }}</el-button>
        <el-button size="small" type="primary" :loading="regionSaving" @click="saveRegion">{{ $t('button.ok') }}</el-button>
      </span>
    </el-dialog>

    <AddDeviceDialog v-if="currentAgentId" :visible.sync="addDeviceDialogVisible" :agent-id="currentAgentId"
      @refresh="reload" />
    <ManualAddDeviceDialog v-if="currentAgentId" :visible.sync="manualAddDeviceDialogVisible"
      :agent-id="currentAgentId" @refresh="reload" />
    <VoicePrintPermissionDialog :visible.sync="permissionDialogVisible" :device-id="permissionDeviceId" />
    <el-footer>
      <version-footer />
    </el-footer>
  </div>
</template>

<script>
import Api from '@/apis/api';
import { ownerDisplayName, hasOwner } from '@/utils/ownerDisplay';
import AddDeviceDialog from "@/components/AddDeviceDialog.vue";
import HeaderBar from "@/components/HeaderBar.vue";
import ManualAddDeviceDialog from "@/components/ManualAddDeviceDialog.vue";
import VersionFooter from "@/components/VersionFooter.vue";
import MacAddressMask from "@/components/MacAddressMask.vue";
import CustomButton from "@/components/CustomButton.vue";
import CustomTable from "@/components/CustomTable.vue";
import VoicePrintPermissionDialog from "@/components/VoicePrintPermissionDialog.vue";
import { isSameName } from '@/utils/deviceRegion.mjs';
import { COUNTRY_CODE, MUNICIPALITY_CODES, loadRegionOptions } from '@/utils/geoLoader';
import {
  compareTimestamps,
  formatCreateDate,
  formatTimestamp,
  hasTimestampValue,
  parseLegacyDate,
  parseTimestamp,
} from '@/utils/deviceTime.mjs';

export default {
  components: {
    HeaderBar,
    AddDeviceDialog,
    ManualAddDeviceDialog,
    VersionFooter,
    MacAddressMask,
    CustomButton,
    CustomTable,
    VoicePrintPermissionDialog,
  },
  data() {
    return {
      regionDialogVisible: false,
      regionSaving: false,
      regionOptionLoading: false,
      regionRow: null,
      regionForm: { province: '', city: '', district: '' },
      provinceOptions: [],
      cityOptions: [],
      districtOptions: [],
      addDeviceDialogVisible: false,
      manualAddDeviceDialogVisible: false,
      selectedDeviceId: '',
      searchKeyword: "",
      activeSearchKeyword: "",
      currentAgentId: this.$route.query.agentId || '',
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      deviceList: [],
      loading: false,
      userApi: null,
      firmwareTypes: [],
      mqttServiceAvailable: false,
      permissionDialogVisible: false,
      permissionDeviceId: '',
    };
  },
  computed: {
    // 声纹权限入口跟随「功能管理 → 声纹识别」开关
    voiceprintFeatureEnabled() {
      return !!this.$store.state.pubConfig?.systemWebMenu?.features?.voiceprintRecognition?.enabled;
    },
    filteredDeviceList() {
      const keyword = this.activeSearchKeyword.toLowerCase();
      if (!keyword) return this.deviceList;
      return this.deviceList.filter(device =>
        (device.model && device.model.toLowerCase().includes(keyword)) ||
        (device.macAddress && device.macAddress.toLowerCase().includes(keyword))
      );
    },

    paginatedDeviceList() {
      const start = (this.currentPage - 1) * this.pageSize;
      const end = start + this.pageSize;
      return this.filteredDeviceList.slice(start, end);
    },
    isCurrentPageAllSelected() {
      return this.paginatedDeviceList.length > 0 &&
        this.paginatedDeviceList.every(device => device.selected);
    },
    // 只有超管开启「查看所有用户数据」并出现其他用户的设备时，才显示归属列
    hasOtherOwnerData() {
      return this.deviceList.some(device => device.isOwn === false);
    },
    tableColumns() {
      const columns = [
        { prop: 'model', label: this.$t('device.model'), align: 'center' },
        { prop: 'firmwareVersion', label: this.$t('device.firmwareVersion'), align: 'center' },
        { prop: 'macAddress', label: this.$t('device.macAddress'), align: 'center' },
        { prop: 'bindTime', label: this.$t('device.bindTime'), align: 'center' },
        { prop: 'lastConversation', label: this.$t('device.lastConversation'), align: 'center' },
      ];
      if (this.mqttServiceAvailable) {
        columns.push({ prop: 'deviceStatus', label: this.$t('device.deviceStatus'), align: 'center' });
      }
      columns.push({ prop: 'region', label: this.$t('deviceGeo.regionColumn'), minWidth: 150, align: 'center' });
      columns.push({ prop: 'alias', label: this.$t('device.alias'), align: 'center' });
      columns.push({ prop: 'otaSwitch', label: this.$t('device.autoUpdate'), align: 'center' });
      if (this.hasOtherOwnerData) {
        columns.push({ prop: 'ownerUsername', label: this.$t('common.ownerColumn'), minWidth: 130, align: 'center' });
      }
      return columns;
    },
  },
  watch: {
    // 同一路由组件会被复用：从带 agentId 的页面用顶部菜单进入不带 agentId 的页面时，
    // mounted 不会再执行，必须监听查询参数才能切换模式
    '$route.query.agentId'(agentId) {
      this.currentAgentId = agentId || '';
      this.currentPage = 1;
      this.deviceList = [];
      this.reload();
    },
  },
  mounted() {
    // 从智能体卡片进入时带 agentId；从顶部菜单进入时没有，展示全部可见设备
    this.reload();
  },
  created() {
    this.getFirmwareTypes()
  },
  methods: {
    ownerDisplayName,
    hasOwner,
    async getFirmwareTypes() {
      try {
        const res = await Api.dict.getDictDataByType('FIRMWARE_TYPE')
        this.firmwareTypes = res.data
      } catch (error) {
        console.error(this.$t('device.getFirmwareTypeFailed') + ':', error)
        this.$message.error(error.message || this.$t('device.getFirmwareTypeFailed'))
      }
    },
    handlePageSizeChange(val) {
      this.pageSize = val;
      this.currentPage = 1;
    },
    regionText(row) {
      return [row.province, row.city, row.district].filter(Boolean).join(' / ');
    },
    // 按名称在选项里找到对应项（省市忽略后缀写法差异，区县严格相等）
    findOption(options, level, name) {
      return options.find(item => isSameName(level, item.name, name));
    },
    async openRegionEdit(row) {
      this.regionRow = row;
      this.regionForm = { province: '', city: '', district: '' };
      this.cityOptions = [];
      this.districtOptions = [];
      this.regionDialogVisible = true;
      this.regionOptionLoading = true;
      try {
        if (!this.provinceOptions.length) {
          this.provinceOptions = await loadRegionOptions(COUNTRY_CODE);
        }
        // 用选项里的规范名称回填，保证下拉能选中
        const province = this.findOption(this.provinceOptions, 'province', row.province);
        if (province) {
          this.regionForm.province = province.name;
          await this.loadCityOptions(province);
          const city = this.findOption(this.cityOptions, 'city', row.city);
          if (city) {
            this.regionForm.city = city.name;
            await this.loadDistrictOptions(province, city);
            const district = this.findOption(this.districtOptions, 'district', row.district);
            if (district) this.regionForm.district = district.name;
          }
        }
      } catch (e) {
        this.$message.error(this.$t('deviceGeo.loadFailed', { code: COUNTRY_CODE }));
      } finally {
        this.regionOptionLoading = false;
      }
    },
    // 直辖市：“市”就是该市自己，区县直接来自省级文件
    async loadCityOptions(province) {
      if (MUNICIPALITY_CODES.has(province.adcode)) {
        this.cityOptions = [{ name: province.name, adcode: province.adcode }];
        return;
      }
      this.cityOptions = await loadRegionOptions(province.adcode).catch(() => []);
    },
    async loadDistrictOptions(province, city) {
      // 直辖市的区县在省级文件里，其他地区在市级文件里
      const code = MUNICIPALITY_CODES.has(province.adcode) ? province.adcode : city.adcode;
      // 省级行政区下直接挂区县的“省直辖县级”（如海南、河南省直辖县）没有市级文件，加载失败时视为没有下级
      this.districtOptions = await loadRegionOptions(code).catch(() => []);
    },
    async onProvinceChange(name) {
      this.regionForm.city = '';
      this.regionForm.district = '';
      this.cityOptions = [];
      this.districtOptions = [];
      const province = this.findOption(this.provinceOptions, 'province', name);
      if (!province) return;
      this.regionOptionLoading = true;
      try {
        await this.loadCityOptions(province);
        // 直辖市只有一个“市”，直接选中并加载区县
        if (this.cityOptions.length === 1 && MUNICIPALITY_CODES.has(province.adcode)) {
          this.regionForm.city = this.cityOptions[0].name;
          await this.loadDistrictOptions(province, this.cityOptions[0]);
        }
      } finally {
        this.regionOptionLoading = false;
      }
    },
    async onCityChange(name) {
      this.regionForm.district = '';
      this.districtOptions = [];
      const province = this.findOption(this.provinceOptions, 'province', this.regionForm.province);
      const city = this.findOption(this.cityOptions, 'city', name);
      if (!province || !city) return;
      this.regionOptionLoading = true;
      try {
        await this.loadDistrictOptions(province, city);
      } finally {
        this.regionOptionLoading = false;
      }
    },
    saveRegion() {
      const row = this.regionRow;
      if (!row || this.regionSaving) return;
      const { province, city, district } = this.regionForm;
      // 三项都为空字符串表示清除手动地域，让后端重新按 IP 解析
      const payload = { province: province || '', city: city || '', district: district || '' };
      this.regionSaving = true;
      this.updateDeviceInfo(row.device_id, payload, (ok, resp) => {
        this.regionSaving = false;
        if (ok) {
          const cleared = !payload.province && !payload.city && !payload.district;
          row.province = payload.province;
          row.city = payload.city;
          row.district = payload.district;
          // 清除后由后端下一次 OTA 按 IP 重新填充，前端先把来源置空
          row.regionSource = cleared ? '' : 'manual';
          this.regionDialogVisible = false;
          this.$message.success(this.$t('deviceGeo.saved'));
        } else {
          this.$message.error((resp && resp.msg) || this.$t('deviceGeo.saveFailed'));
        }
      });
    },
    handleSearch() {
      this.activeSearchKeyword = this.searchKeyword;
      this.currentPage = 1;
    },

    handleSelectAll() {
      const shouldSelectAll = !this.isCurrentPageAllSelected;
      this.paginatedDeviceList.forEach(row => {
        row.selected = shouldSelectAll;
      });
    },

    deleteSelected() {
      const selectedDevices = this.paginatedDeviceList.filter(device => device.selected);
      if (selectedDevices.length === 0) {
        this.$message.warning({
          message: this.$t('device.selectAtLeastOne'),
          showClose: true
        });
        return;
      }

      this.$confirm(this.$t('device.confirmBatchUnbind').replace('{count}', selectedDevices.length), this.$t('message.warning'), {
        confirmButtonText: this.$t('button.ok'),
        cancelButtonText: this.$t('button.cancel'),
        type: 'warning'
      }).then(() => {
        const deviceIds = selectedDevices.map(device => device.device_id);
        this.batchUnbindDevices(deviceIds);
      });
    },
    batchUnbindDevices(deviceIds) {
      const promises = deviceIds.map(id => {
        return new Promise((resolve, reject) => {
          Api.device.unbindDevice(id, ({ data }) => {
            if (data.code === 0) {
              resolve();
            } else {
              reject(data.msg || this.$t('device.bindFailed'));
            }
          });
        });
      });
      Promise.all(promises)
        .then(() => {
          this.$message.success({
            message: this.$t('device.batchUnbindSuccess').replace('{count}', deviceIds.length),
            showClose: true
          });
          this.reload();
        })
        .catch(error => {
          this.$message.error({
            message: error || this.$t('device.batchUnbindError'),
            showClose: true
          });
        });
    },
    handleAddDevice() {
      this.addDeviceDialogVisible = true;
    },
    handleManualAddDevice() {
      this.manualAddDeviceDialogVisible = true;
    },
    submitAlias(row) {
      if (row._submitting) return;

      const text = (row.alias || '').trim();
      if (text.length > 64) {
        this.$message.warning(this.$t('device.aliasTooLong'));
        return;
      }
      if (text === row._originalAlias) {
        return;
      }

      row._submitting = true;
      this.updateDeviceInfo(row.device_id, { alias: text }, (ok, resp) => {
        if (ok) {
          row._originalAlias = text;
          this.$message.success(this.$t('device.aliasSaved'));
        } else {
          row.alias = row._originalAlias;
          this.$message.error(resp.msg || this.$t('device.aliasSaveFailed'));
        }
        row._submitting = false;
      });
    },
    onAliasBlur(row) {
      row.isEdit = false;
      setTimeout(() => {
        this.submitAlias(row);
      }, 100);
    },
    onAliasEnter(row) {
      row.isEdit = false;
      this.submitAlias(row);
    },
    handleUnbind(device_id) {
      this.$confirm(this.$t('device.confirmUnbind'), this.$t('message.warning'), {
        confirmButtonText: this.$t('button.ok'),
        cancelButtonText: this.$t('button.cancel'),
        type: 'warning'
      }).then(() => {
        Api.device.unbindDevice(device_id, ({ data }) => {
          if (data.code === 0) {
            this.$message.success({
              message: this.$t('device.unbindSuccess'),
              showClose: true
            });
            this.reload();
          } else {
            this.$message.error({
              message: data.msg || this.$t('device.unbindFailed'),
              showClose: true
            });
          }
        });
      });
    },
    handleGenertor(row) {
      const pathname = window.location.pathname;
      const basePath = pathname.split('/').slice(0, -1).join('/');
      const url = `${window.location.origin}${basePath}/generator/?deviceId=${row.device_id}`;
      sessionStorage.setItem('devicePath', window.location.href);
      window.location.href = url;
    },
    // 跳转到会话记录页面，并自动按该设备筛选
    handleVoiceprintPermission(row) {
      this.permissionDeviceId = row.device_id;
      this.permissionDialogVisible = true;
    },
    handleViewChatHistory(row) {
      this.$router.push({ path: '/chat-history', query: { macAddress: row.macAddress } });
    },
    goToPage(page) {
      this.currentPage = page;
    },

    // 按当前模式刷新列表：带 agentId 是某个智能体的设备，否则是当前用户可见的全部设备
    reload() {
      if (this.currentAgentId) {
        this.fetchBindDevices(this.currentAgentId);
      } else {
        this.fetchAllDevices();
      }
    },

    // 服务端设备 -> 表格行，两种模式共用，避免两份映射逐渐不一致
    mapDevices(list) {
      return list.map(device => {
        const hasCreateDateTimestamp = hasTimestampValue(device.createDateTimestamp);
        const rawBindTime = hasCreateDateTimestamp
          ? parseTimestamp(device.createDateTimestamp)
          : parseLegacyDate(device.createDate);
        return {
          device_id: device.id,
          model: device.board,
          firmwareVersion: device.appVersion,
          macAddress: device.macAddress,
          bindTime: formatCreateDate(device.createDateTimestamp, device.createDate),
          lastConversation: formatTimestamp(device.lastConnectedAtTimestamp),
          alias: device.alias,
          _originalAlias: device.alias,
          isEdit: false,
          _submitting: false,
          otaSwitch: device.autoUpdate === 1,
          province: device.province || '',
          city: device.city || '',
          district: device.district || '',
          regionSource: device.regionSource || '',
          ownerUsername: device.ownerUsername || '',
          ownerNickname: device.ownerNickname || '',
          isOwn: device.isOwn !== false,
          rawBindTime,
          selected: false,
          deviceStatus: 'offline'
        };
      })
        .sort((firstDevice, secondDevice) => compareTimestamps(
          firstDevice.rawBindTime,
          secondDevice.rawBindTime,
        ));
    },

    fetchBindDevices(agentId) {
      this.loading = true;
      Api.device.getAgentBindDevices(agentId, ({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          this.deviceList = this.mapDevices(data.data);
          this.activeSearchKeyword = "";
          this.searchKeyword = "";

          this.fetchDeviceStatus(agentId);
        } else {
          this.$message.error(data.msg || this.$t('device.getListFailed'));
        }
      });
    },

    // 没有指定智能体时展示当前用户可见的全部设备。
    // 在线状态由按智能体查询的 MQTT 接口提供，这里没有智能体，因此不显示在线状态列。
    fetchAllDevices() {
      this.loading = true;
      this.mqttServiceAvailable = false;
      Api.device.getAllVisibleDevices(
        ({ data }) => {
          this.loading = false;
          if (data.code === 0) {
            this.deviceList = this.mapDevices(data.data || []);
            this.activeSearchKeyword = "";
            this.searchKeyword = "";
          } else {
            this.$message.error(data.msg || this.$t('device.getListFailed'));
          }
        },
        () => {
          this.loading = false;
          this.$message.error(this.$t('device.getListFailed'));
        },
      );
    },

    fetchDeviceStatus(agentId) {
      this.loading = true;
      Api.device.getDeviceStatus(agentId, ({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          try {
            const statusData = JSON.parse(data.data);

            if (statusData && typeof statusData === 'object') {
              this.mqttServiceAvailable = true;
              this.updateDeviceStatusFromResponse(statusData);
            } else {
              this.mqttServiceAvailable = false;
            }
          } catch (error) {
            this.mqttServiceAvailable = false;
          }
        } else {
          this.mqttServiceAvailable = false;
        }
      });
    },

    updateDeviceStatusFromResponse(deviceStatusMap) {
      this.deviceList.forEach(device => {
        const macAddress = device.macAddress ? device.macAddress.replace(/:/g, '_') : 'unknown';
        const groupId = device.model ? device.model.replace(/:/g, '_') : 'GID_default';
        const mqttClientId = `${groupId}@@@${macAddress}@@@${macAddress}`;

        if (deviceStatusMap[mqttClientId]) {
          const statusInfo = deviceStatusMap[mqttClientId];

          let isOnline = false;
          if (statusInfo.isAlive === true) {
            isOnline = true;
          } else if (statusInfo.isAlive === false) {
            isOnline = false;
          } else if (statusInfo.isAlive === null && statusInfo.exists === true) {
            isOnline = true;
          } else {
            isOnline = false;
          }

          device.deviceStatus = isOnline ? 'online' : 'offline';
        } else {
          device.deviceStatus = 'offline';
        }
      });
    },
    getFirmwareTypeName(type) {
      const firmwareType = this.firmwareTypes.find(item => item.key === type)
      return firmwareType ? firmwareType.name : type
    },
    updateDeviceInfo(device_id, payload, callback) {
      return Api.device.updateDeviceInfo(device_id, payload, ({ data }) => {
        callback(data.code === 0, data);
      })
    },
    handleOtaSwitchChange(row) {
      this.updateDeviceInfo(row.device_id, { autoUpdate: row.otaSwitch ? 1 : 0 }, (result, { msg }) => {
        if (result) {
          this.$message.success(row.otaSwitch ? this.$t('device.autoUpdateEnabled') : this.$t('device.autoUpdateDisabled'));
          return;
        }
        row.otaSwitch = !row.otaSwitch
        this.$message.error(msg || this.$t('message.error'))
      })
    },
    isGenerate(row) {
      const version = row.firmwareVersion.replace(/\./g, '');
      return Number(version) >= 200;
    },
  }
};
</script>

<style lang="scss" scoped>
.welcome {
  min-width: 900px;
  min-height: 506px;
  height: 100vh;
  display: flex;
  position: relative;
  flex-direction: column;
  background: var(--sys-surface);
  background-size: cover;
  -webkit-background-size: cover;
  -o-background-size: cover;
  overflow: hidden;
}

.main-wrapper {
  height: calc(100vh - 63px - 35px);
  padding: 20px 22px 0;
  position: relative;
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
}

.operation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 0 16px 0;
}

.page-title {
  font-weight: 500;
  font-size: 24px;
  margin: 0;
}

.right-operations {
  display: flex;
  gap: 10px;
  margin-left: auto;
}

.search-input {
  width: 240px;
}

.content-panel {
  display: flex;
  overflow: hidden;
  height: 100%;
  border-radius: 15px;
  background: transparent;
  border: 1px solid #fff;
}

.content-area {
  flex: 1;
  height: 100%;
  min-width: 600px;
  overflow: auto;
  background-color: white;
  display: flex;
  flex-direction: column;
}

.device-card {
  background: white;
  flex: 1;
  display: flex;
  flex-direction: column;
  border: none;
  box-shadow: none;
  overflow: hidden;

  ::v-deep .el-card__body {
    padding: 14px 20px;
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
  }
}

.ctrl_btn {
  display: flex;
  gap: 8px;
}

:deep(.el-table .el-button--text) {
  color: #7079aa;
}

:deep(.el-table .el-button--text:hover) {
  color: #5a64b5;
}

:deep(.el-icon-edit) {
  color: #7079aa;
  cursor: pointer;
}

:deep(.el-icon-edit:hover) {
  color: #5a64b5;
}

.region-view {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  cursor: pointer;
  color: #2c3354;

  &:hover {
    color: var(--sys-primary);
  }

  // 手动填写的地域用主色标注，和 IP 自动解析的区分开
  &.is-manual {
    color: var(--sys-primary);
  }
}

.region-empty {
  color: #b4bad2;
}

.region-tip {
  padding-left: 72px;
  font-size: 12px;
  line-height: 1.5;
  color: #8a93b8;
}
</style>

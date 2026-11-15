<template>
  <div class="welcome">
    <HeaderBar />

    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="chat-card" shadow="never">
            <div class="operation-header">
              <div class="title-group">
                <h2 class="page-title">{{ $t('chatRecords.title') }}</h2>
                <span class="china-time-badge">{{ $t('chatRecords.chinaTime') }}</span>
              </div>
              <div class="total-hint">{{ $t('chatRecords.totalRecords', { count: total }) }}</div>
            </div>

            <div class="filter-bar">
              <div class="filter-item">
                <span class="filter-label">{{ $t('chatRecords.device') }}</span>
                <el-select v-model="filters.macAddress" filterable clearable class="control-device"
                  :placeholder="$t('chatRecords.allDevices')" @change="handleSearch">
                  <el-option v-for="item in deviceOptions" :key="item.macAddress"
                    :label="deviceLabel(item)" :value="item.macAddress" />
                </el-select>
              </div>

              <div class="filter-item">
                <span class="filter-label">{{ $t('chatRecords.agent') }}</span>
                <el-select v-model="filters.agentId" filterable clearable class="control-agent"
                  :placeholder="$t('chatRecords.allAgents')" @change="handleSearch">
                  <el-option v-for="item in agentOptions" :key="item.agentId"
                    :label="item.agentName || item.agentId" :value="item.agentId" />
                </el-select>
              </div>

              <div class="filter-item">
                <span class="filter-label">{{ $t('chatRecords.role') }}</span>
                <el-select v-model="filters.chatType" clearable class="control-role"
                  :placeholder="$t('chatRecords.allRoles')" @change="handleSearch">
                  <el-option :label="$t('chatRecords.roleUser')" :value="1" />
                  <el-option :label="$t('chatRecords.roleAgent')" :value="2" />
                  <el-option :label="$t('chatRecords.roleTool')" :value="3" />
                </el-select>
              </div>

              <div class="filter-item">
                <span class="filter-label">{{ $t('chatRecords.timeRange') }}</span>
                <el-date-picker v-model="dateRange" type="datetimerange" class="control-time"
                  value-format="yyyy-MM-dd HH:mm:ss" :default-time="['00:00:00', '23:59:59']"
                  range-separator="~" :start-placeholder="$t('chatRecords.timeRange')"
                  :end-placeholder="$t('chatRecords.timeRange')" @change="handleSearch" />
              </div>

              <div class="filter-item">
                <span class="filter-label">{{ $t('chatRecords.keyword') }}</span>
                <el-input v-model="filters.keyword" class="control-keyword" clearable
                  :placeholder="$t('chatRecords.keywordPlaceholder')"
                  @keyup.enter.native="handleSearch" @clear="handleSearch" />
              </div>

              <div class="filter-actions">
                <CustomButton icon="el-icon-search" type="confirm" @click="handleSearch">
                  {{ $t('chatRecords.search') }}
                </CustomButton>
                <CustomButton icon="el-icon-refresh" @click="handleReset">
                  {{ $t('chatRecords.reset') }}
                </CustomButton>
              </div>
            </div>

            <div class="quick-bar">
              <el-button type="text" size="mini" @click="applyQuickRange(0)">{{ $t('chatRecords.today') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(7)">{{ $t('chatRecords.last7Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(30)">{{ $t('chatRecords.last30Days') }}</el-button>
              <el-button type="text" size="mini" @click="applyQuickRange(-1)">{{ $t('chatRecords.allTime') }}</el-button>
              <span class="time-hint">{{ $t('chatRecords.timeHint') }}</span>
            </div>

            <CustomTable
              :data="records"
              :columns="tableColumns"
              :loading="loading"
              :loading-text="$t('deviceManagement.loading')"
              :show-selection="false"
              :show-operations="true"
              :operations-label="$t('chatRecords.operation')"
              :operations-width="140"
              :total="total"
              :current-page="currentPage"
              :page-size="pageSize"
              :page-size-options="pageSizeOptions"
              @size-change="handlePageSizeChange"
              @page-change="goToPage"
            >
              <template slot="macAddress" slot-scope="scope">
                <div class="device-cell">
                  <span class="device-alias">{{ scope.row.deviceAlias || $t('chatRecords.unnamedDevice') }}</span>
                  <MacAddressMask v-if="scope.row.macAddress" :macAddress="scope.row.macAddress" />
                  <span v-else>-</span>
                </div>
              </template>

              <template slot="agentName" slot-scope="scope">
                {{ scope.row.agentName || scope.row.agentId || $t('chatRecords.unnamedAgent') }}
              </template>

              <template slot="sessionTitle" slot-scope="scope">
                <span :title="scope.row.sessionId">{{ sessionLabel(scope.row) }}</span>
              </template>

              <template slot="chatType" slot-scope="scope">
                <el-tag size="mini" :type="roleTagType(scope.row.chatType)">
                  {{ roleText(scope.row.chatType) }}
                </el-tag>
              </template>

              <template slot="content" slot-scope="scope">
                <span class="content-cell" @click="openSession(scope.row)">
                  {{ extractDisplayText(scope.row.content) }}
                </span>
                <span v-if="scope.row.contentTruncated" class="truncated-hint">
                  {{ $t('chatRecords.truncated') }}
                </span>
              </template>

              <template slot="extra" slot-scope="scope">
                <span class="extra-cell">
                  <i v-if="scope.row.hasAudio" class="el-icon-headset" :title="$t('chatRecords.audio')"></i>
                  <span v-if="scope.row.metrics && scope.row.metrics.e2eTotalMs" class="duration-text"
                    :title="$t('chatRecords.duration')">
                    {{ formatDuration(scope.row.metrics.e2eTotalMs) }}
                  </span>
                </span>
              </template>

              <template slot="operations" slot-scope="scope">
                <el-button size="mini" type="text" @click="openSession(scope.row)">
                  {{ $t('chatRecords.viewSession') }}
                </el-button>
              </template>
            </CustomTable>
          </el-card>
        </div>
      </div>
    </div>

    <el-dialog :title="sessionDialogTitle" :visible.sync="sessionVisible" width="760px" append-to-body
      custom-class="session-dialog" top="6vh">
      <div v-loading="sessionLoading" class="session-messages">
        <div v-if="!sessionLoading && sessionMessages.length === 0" class="session-empty">
          {{ $t('chatRecords.sessionEmpty') }}
        </div>
        <div v-for="(message, index) in sessionMessages" :key="message.id || index"
          class="session-message" :class="messageClass(message)">
          <div class="session-message-meta">
            <span>{{ roleText(message.chatType) }}</span>
            <span>{{ message.createdAt }}</span>
          </div>
          <div class="session-message-body">{{ extractDisplayText(message.content) }}</div>
        </div>
      </div>
      <span slot="footer">
        <el-button size="small" type="primary" plain :disabled="!sessionRow || sessionLoading"
          @click="downloadCurrentSession">
          {{ $t('chatHistory.downloadCurrentSession') }}
        </el-button>
        <el-button size="small" @click="sessionVisible = false">{{ $t('button.cancel') }}</el-button>
      </span>
    </el-dialog>

    <el-footer><version-footer /></el-footer>
  </div>
</template>

<script>
import Api from '@/apis/api';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import CustomButton from '@/components/CustomButton.vue';
import CustomTable from '@/components/CustomTable.vue';
import MacAddressMask from '@/components/MacAddressMask.vue';

const CN_OFFSET_MS = 8 * 60 * 60 * 1000;

function pad(value) {
  return String(value).padStart(2, '0');
}

/** 把「中国时间 yyyy-MM-dd HH:mm:ss」字符串按 UTC+8 解析成绝对毫秒 */
function chinaStringToMs(value) {
  if (!value) return null;
  const ms = new Date(String(value).replace(' ', 'T') + '+08:00').getTime();
  return Number.isNaN(ms) ? null : ms;
}

/** 把绝对毫秒格式化成「中国时间 yyyy-MM-dd HH:mm:ss」 */
function msToChinaString(ms) {
  const shifted = new Date(ms + CN_OFFSET_MS);
  return `${shifted.getUTCFullYear()}-${pad(shifted.getUTCMonth() + 1)}-${pad(shifted.getUTCDate())} `
    + `${pad(shifted.getUTCHours())}:${pad(shifted.getUTCMinutes())}:${pad(shifted.getUTCSeconds())}`;
}

/** 中国时间当天 00:00:00 对应的绝对毫秒 */
function chinaStartOfToday() {
  const shifted = new Date(Date.now() + CN_OFFSET_MS);
  return Date.UTC(shifted.getUTCFullYear(), shifted.getUTCMonth(), shifted.getUTCDate()) - CN_OFFSET_MS;
}

export default {
  name: 'ChatHistoryRecords',
  components: { HeaderBar, VersionFooter, CustomButton, CustomTable, MacAddressMask },
  data() {
    return {
      filters: {
        macAddress: '',
        agentId: '',
        chatType: '',
        keyword: ''
      },
      dateRange: [],
      deviceOptions: [],
      agentOptions: [],
      records: [],
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      sessionVisible: false,
      sessionLoading: false,
      sessionMessages: [],
      sessionRow: null
    };
  },
  computed: {
    tableColumns() {
      return [
        { prop: 'createdAtText', label: this.$t('chatRecords.colTime'), width: 160, align: 'center' },
        { prop: 'macAddress', label: this.$t('chatRecords.colDevice'), minWidth: 180, align: 'center' },
        { prop: 'agentName', label: this.$t('chatRecords.colAgent'), minWidth: 120, align: 'center' },
        { prop: 'sessionTitle', label: this.$t('chatRecords.colSession'), minWidth: 130, align: 'center' },
        { prop: 'chatType', label: this.$t('chatRecords.colRole'), width: 100, align: 'center' },
        { prop: 'content', label: this.$t('chatRecords.colContent'), minWidth: 260, align: 'left' },
        { prop: 'extra', label: this.$t('chatRecords.colExtra'), width: 110, align: 'center' }
      ];
    },
    sessionDialogTitle() {
      if (!this.sessionRow) return this.$t('chatRecords.sessionDialogTitle');
      return `${this.$t('chatRecords.sessionDialogTitle')} · ${this.sessionLabel(this.sessionRow)}`;
    }
  },
  watch: {
    '$route.query.macAddress'(value) {
      this.filters.macAddress = value || '';
      this.currentPage = 1;
      this.fetchRecords();
    },
    '$route.query.agentId'(value) {
      this.filters.agentId = value || '';
      this.currentPage = 1;
      this.fetchRecords();
    }
  },
  created() {
    // 设备管理页带 macAddress、首页智能体卡片带 agentId，进入时自动预筛选
    this.filters.macAddress = this.$route.query.macAddress || '';
    this.filters.agentId = this.$route.query.agentId || '';
    this.fetchFilterOptions();
    this.fetchRecords();
  },
  methods: {
    fetchFilterOptions() {
      Api.chatHistory.getFilterOptions(({ data }) => {
        if (data && data.code === 0 && data.data) {
          this.deviceOptions = data.data.devices || [];
          this.agentOptions = data.data.agents || [];
        }
      }, () => {
        this.$message.error(this.$t('chatRecords.filterFailed'));
      });
    },

    buildParams() {
      const params = { page: this.currentPage, limit: this.pageSize };
      if (this.filters.macAddress) params.macAddress = this.filters.macAddress;
      if (this.filters.agentId) params.agentId = this.filters.agentId;
      if (this.filters.chatType !== '' && this.filters.chatType !== null && this.filters.chatType !== undefined) {
        params.chatType = this.filters.chatType;
      }
      if (this.filters.keyword && this.filters.keyword.trim()) {
        params.keyword = this.filters.keyword.trim();
      }
      if (this.dateRange && this.dateRange.length === 2) {
        const start = chinaStringToMs(this.dateRange[0]);
        const end = chinaStringToMs(this.dateRange[1]);
        if (start !== null) params.startTime = start;
        if (end !== null) params.endTime = end;
      }
      return params;
    },

    fetchRecords() {
      this.loading = true;
      Api.chatHistory.getRecords(this.buildParams(), ({ data }) => {
        this.loading = false;
        if (data && data.code === 0 && data.data) {
          this.records = data.data.list || [];
          this.total = data.data.total || 0;
        } else {
          this.records = [];
          this.total = 0;
          this.$message.error((data && data.msg) || this.$t('chatRecords.loadFailed'));
        }
      }, () => {
        this.loading = false;
        this.$message.error(this.$t('chatRecords.loadFailed'));
      });
    },

    handleSearch() {
      this.currentPage = 1;
      this.fetchRecords();
    },

    handleReset() {
      this.filters = { macAddress: '', agentId: '', chatType: '', keyword: '' };
      this.dateRange = [];
      this.currentPage = 1;
      this.fetchRecords();
    },

    applyQuickRange(days) {
      if (days < 0) {
        this.dateRange = [];
      } else {
        const end = Date.now();
        const start = days === 0 ? chinaStartOfToday() : end - days * 24 * 60 * 60 * 1000;
        this.dateRange = [msToChinaString(start), msToChinaString(end)];
      }
      this.currentPage = 1;
      this.fetchRecords();
    },

    handlePageSizeChange(size) {
      this.pageSize = size;
      this.currentPage = 1;
      this.fetchRecords();
    },

    goToPage(page) {
      this.currentPage = page;
      this.fetchRecords();
    },

    openSession(row) {
      this.sessionRow = row;
      this.sessionMessages = [];
      this.sessionVisible = true;
      this.sessionLoading = true;
      Api.agent.getAgentChatHistory(row.agentId, row.sessionId, (res) => {
        this.sessionLoading = false;
        if (res && res.data && res.data.code === 0 && Array.isArray(res.data.data)) {
          this.sessionMessages = res.data.data;
        } else {
          this.$message.error(this.$t('chatRecords.sessionLoadFailed'));
        }
      });
    },

    downloadCurrentSession() {
      if (!this.sessionRow) return;
      Api.agent.getDownloadUrl(this.sessionRow.agentId, this.sessionRow.sessionId, (res) => {
        if (res && res.data && res.data.code === 0 && res.data.data) {
          const uuid = res.data.data;
          window.open(`${Api.getServiceUrl()}/agent/chat-history/download/${uuid}/current`, '_blank');
        } else {
          this.$message.error(this.$t('chatHistory.downloadLinkFailed'));
        }
      });
    },

    deviceLabel(item) {
      return item.alias ? `${item.alias} · ${item.macAddress}` : item.macAddress;
    },

    sessionLabel(row) {
      if (row.sessionTitle) return row.sessionTitle;
      return row.sessionId ? `${row.sessionId.slice(0, 8)}...` : this.$t('chatRecords.noTitle');
    },

    roleText(type) {
      if (Number(type) === 1) return this.$t('chatRecords.roleUser');
      if (Number(type) === 3) return this.$t('chatRecords.roleTool');
      return this.$t('chatRecords.roleAgent');
    },

    roleTagType(type) {
      if (Number(type) === 1) return '';
      if (Number(type) === 3) return 'info';
      return 'success';
    },

    messageClass(message) {
      if (Number(message.chatType) === 1) return 'is-user';
      if (Number(message.chatType) === 3) return 'is-tool';
      return 'is-agent';
    },

    formatDuration(ms) {
      const value = Number(ms || 0);
      return value >= 1000 ? `${(value / 1000).toFixed(2)}s` : `${Math.round(value)}ms`;
    },

    extractDisplayText(content) {
      if (!content) return '';
      try {
        const parsed = JSON.parse(content);
        if (Array.isArray(parsed)) {
          return parsed.map(item => (item && item.text) || '').join('\n');
        }
        if (parsed && typeof parsed === 'object' && parsed.content) {
          return parsed.content;
        }
      } catch (e) {
        // 普通文本
      }
      return content;
    }
  }
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
  overflow: auto;
  background: #fff;
  display: flex;
}

.chat-card {
  flex: 1;
  display: flex;
  ::v-deep .el-card__body {
    padding: 14px 20px;
    display: flex;
    flex-direction: column;
    flex: 1;
    overflow: hidden;
  }
}

.operation-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding-bottom: 12px;
}

.title-group {
  display: flex;
  align-items: center;
  gap: 12px;
}

.page-title {
  font-weight: 500;
  font-size: 24px;
  margin: 0;
}

.china-time-badge {
  font-size: 12px;
  color: var(--sys-primary-text);
  background: var(--sys-primary-bg);
  border: 1px solid var(--sys-primary-border);
  border-radius: 10px;
  padding: 2px 10px;
}

.total-hint {
  font-size: 13px;
  color: #909399;
}

.filter-bar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px 16px;
  padding-bottom: 8px;
  border-bottom: 1px solid #f0f0f0;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 6px;
}

.filter-label {
  font-size: 13px;
  color: #606266;
  white-space: nowrap;
}

.control-device { width: 230px; }
.control-agent { width: 160px; }
.control-role { width: 120px; }
.control-time { width: 340px; }
.control-keyword { width: 200px; }

.filter-actions {
  display: flex;
  gap: 8px;
  margin-left: auto;
}

.quick-bar {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 4px 0 8px;
}

.time-hint {
  margin-left: 12px;
  font-size: 12px;
  color: #a4a7ad;
}

.device-cell {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.device-alias {
  max-width: 110px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.content-cell {
  cursor: pointer;
  color: #3d4566;
  &:hover { color: var(--sys-primary); }
}

.truncated-hint {
  color: #a4a7ad;
  font-size: 12px;
  margin-left: 4px;
}

.extra-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--sys-primary-text);
}

.duration-text { font-size: 12px; }

.session-messages {
  max-height: 62vh;
  overflow-y: auto;
  padding: 4px 8px;
}

.session-empty {
  text-align: center;
  color: #909399;
  padding: 40px 0;
}

.session-message {
  margin-bottom: 12px;
  padding: 8px 12px;
  border-radius: 8px;
  background: #f6f7f9;
  &.is-user { background: var(--sys-primary-bg-soft); }
  &.is-tool { background: #f2f3f5; font-family: monospace; }
}

.session-message-meta {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #909399;
  margin-bottom: 4px;
}

.session-message-body {
  font-size: 13px;
  line-height: 1.6;
  white-space: pre-wrap;
  word-break: break-all;
}
</style>

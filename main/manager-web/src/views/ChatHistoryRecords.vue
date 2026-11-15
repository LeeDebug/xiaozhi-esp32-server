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
              <div class="total-hint">{{ $t('chatRecords.totalSessions', { count: total }) }}</div>
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
                    :label="agentLabel(item)" :value="item.agentId" />
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
              :operations-width="100"
              :operations-fixed="true"
              :total="total"
              :current-page="currentPage"
              :page-size="pageSize"
              :page-size-options="pageSizeOptions"
              @size-change="handlePageSizeChange"
              @page-change="goToPage"
            >
              <template slot="createdAtText" slot-scope="scope">
                <div class="time-cell">
                  <div>{{ scope.row.createdAtText }}</div>
                  <div class="time-sub">{{ $t('chatRecords.startAt') }} {{ scope.row.startedAtText }}</div>
                </div>
              </template>

              <template slot="sessionTitle" slot-scope="scope">
                <span class="session-cell" :title="scope.row.sessionId" @click="openSession(scope.row)">
                  {{ sessionLabel(scope.row) }}
                </span>
              </template>

              <template slot="macAddress" slot-scope="scope">
                <div class="device-cell">
                  <span class="device-alias">{{ scope.row.deviceAlias || $t('chatRecords.unnamedDevice') }}</span>
                  <MacAddressMask v-if="scope.row.macAddress" :macAddress="scope.row.macAddress" />
                  <span v-else>-</span>
                </div>
              </template>

              <template slot="agentName" slot-scope="scope">
                <div class="agent-cell">
                  <span>{{ scope.row.agentName || scope.row.agentId || $t('chatRecords.unnamedAgent') }}</span>
                  <!-- 超管开启全库可见后，标注该会话属于自己还是其他用户 -->
                  <el-tag v-if="showOwnerTags && hasOwner(scope.row)" size="mini"
                    :type="scope.row.isOwn === false ? 'warning' : 'success'">
                    {{ scope.row.isOwn === false ? $t('common.ownerOther') : $t('common.ownerMine') }} · {{ ownerDisplayName(scope.row) }}
                  </el-tag>
                </div>
              </template>

              <template slot="chatCount" slot-scope="scope">
                <span class="count-badge">{{ scope.row.chatCount || 0 }}</span>
              </template>

              <template slot="summary" slot-scope="scope">
                <span class="summary-cell" :title="scope.row.summary" @click="openSession(scope.row)">
                  {{ scope.row.summary || '-' }}
                </span>
              </template>

              <template slot="extra" slot-scope="scope">
                <span class="extra-cell">
                  <i v-if="scope.row.hasAudio" class="el-icon-headset" :title="$t('chatRecords.audio')"></i>
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

    <!-- 沿用旧会话记录弹窗的左右布局；左侧列表改为当前查询结果 + 当前设备 -->
    <chat-history-dialog
      v-if="dialogVisible"
      :visible="dialogVisible"
      records-mode
      :query="dialogQuery"
      :mac-address="dialogMacAddress"
      :device-alias="dialogDeviceAlias"
      :initial-session="dialogInitialSession"
      :agent-id="dialogAgentId"
      :agent-name="dialogAgentName"
      @update:visible="dialogVisible = $event"
    />

    <el-footer><version-footer /></el-footer>
  </div>
</template>

<script>
import Api from '@/apis/api';
import { ownerDisplayName, hasOwner } from '@/utils/ownerDisplay';
import HeaderBar from '@/components/HeaderBar.vue';
import VersionFooter from '@/components/VersionFooter.vue';
import CustomButton from '@/components/CustomButton.vue';
import CustomTable from '@/components/CustomTable.vue';
import MacAddressMask from '@/components/MacAddressMask.vue';
import ChatHistoryDialog from '@/components/ChatHistoryDialog.vue';

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
  components: { HeaderBar, VersionFooter, CustomButton, CustomTable, MacAddressMask, ChatHistoryDialog },
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
      records: [], // 当前页的会话列表（每行一个 session_id）
      loading: false,
      total: 0,
      currentPage: 1,
      pageSize: 10,
      pageSizeOptions: [10, 20, 50, 100],
      // 弹窗入参：查询条件快照 + 当前设备 + 用户点击的那一行会话
      dialogVisible: false,
      dialogQuery: {},
      dialogMacAddress: '',
      dialogDeviceAlias: '',
      dialogInitialSession: null,
      dialogAgentId: '',
      dialogAgentName: ''
    };
  },
  computed: {
    // 只有出现其他用户的数据时才展示归属标签，默认「只看自己」时保持界面简洁
    showOwnerTags() {
      return this.records.some(row => row.isOwn === false)
        || this.deviceOptions.some(item => item.isOwn === false)
        || this.agentOptions.some(item => item.isOwn === false);
    },
    tableColumns() {
      return [
        // 列宽总量控制在略大于容器即可：过大会把操作列挤出视口，短内容列还会留出大片空白
        { prop: 'createdAtText', label: this.$t('chatRecords.colTime'), width: 150, align: 'center' },
        { prop: 'sessionTitle', label: this.$t('chatRecords.colSession'), minWidth: 120, align: 'center' },
        { prop: 'macAddress', label: this.$t('chatRecords.colDevice'), minWidth: 155, align: 'center' },
        { prop: 'agentName', label: this.$t('chatRecords.colAgent'), minWidth: 100, align: 'center' },
        { prop: 'chatCount', label: this.$t('chatRecords.colMessageCount'), width: 70, align: 'center' },
        { prop: 'summary', label: this.$t('chatRecords.colSummary'), minWidth: 160, align: 'left', showOverflowTooltip: false },
        { prop: 'extra', label: this.$t('chatRecords.colExtra'), width: 60, align: 'center' }
      ];
    }
  },
  watch: {
    '$route.query.macAddress'(value) {
      this.filters.macAddress = value || '';
      this.currentPage = 1;
      this.fetchSessions();
    },
    '$route.query.agentId'(value) {
      this.filters.agentId = value || '';
      this.currentPage = 1;
      this.fetchSessions();
    }
  },
  created() {
    // 设备管理页带 macAddress、首页智能体卡片带 agentId，进入时自动预筛选
    this.filters.macAddress = this.$route.query.macAddress || '';
    this.filters.agentId = this.$route.query.agentId || '';
    this.fetchFilterOptions();
    this.fetchSessions();
  },
  methods: {
    ownerDisplayName,
    hasOwner,
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

    fetchSessions() {
      this.loading = true;
      Api.chatHistory.getSessions(this.buildParams(), ({ data }) => {
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
      this.fetchSessions();
    },

    handleReset() {
      this.filters = { macAddress: '', agentId: '', chatType: '', keyword: '' };
      this.dateRange = [];
      this.currentPage = 1;
      this.fetchSessions();
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
      this.fetchSessions();
    },

    handlePageSizeChange(size) {
      this.pageSize = size;
      this.currentPage = 1;
      this.fetchSessions();
    },

    goToPage(page) {
      this.currentPage = page;
      this.fetchSessions();
    },

    openSession(row) {
      // 快照当前筛选条件（去掉分页，弹窗自己滚动加载），并强制锁定为当前行所属设备
      const query = this.buildParams();
      delete query.page;
      delete query.limit;
      if (row.macAddress) query.macAddress = row.macAddress;
      this.dialogQuery = query;
      this.dialogMacAddress = row.macAddress || this.filters.macAddress || '';
      this.dialogDeviceAlias = row.deviceAlias || '';
      // 把用户点击的那一行会话直接交给弹窗置顶选中，避免它因设备过滤后不在弹窗列表第一页
      this.dialogInitialSession = { ...row };
      this.dialogAgentId = row.agentId || '';
      this.dialogAgentName = row.agentName || row.agentId || '';
      this.dialogVisible = true;
    },

    deviceLabel(item) {
      const base = item.alias ? `${item.alias} · ${item.macAddress}` : item.macAddress;
      return `${base}${this.ownerSuffix(item)}`;
    },

    agentLabel(item) {
      const base = item.agentName || item.agentId;
      return `${base}${this.ownerSuffix(item)}`;
    },

    ownerSuffix(item) {
      if (!this.showOwnerTags || !this.hasOwner(item)) return '';
      const tag = item.isOwn === false ? this.$t('common.ownerOther') : this.$t('common.ownerMine');
      return `（${tag} · ${this.ownerDisplayName(item)}）`;
    },

    sessionLabel(row) {
      if (row.sessionTitle) return row.sessionTitle;
      return row.sessionId ? `${row.sessionId.slice(0, 8)}...` : this.$t('chatRecords.noTitle');
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

.agent-cell {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 4px;
}

.device-alias {
  max-width: 80px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.time-cell {
  line-height: 1.35;
  font-size: 13px;
}

.time-sub {
  font-size: 11px;
  color: #a4a7ad;
  margin-top: 2px;
}

.session-cell {
  cursor: pointer;
  color: #3d4566;
  font-weight: 500;
  &:hover { color: var(--sys-primary); }
}

.count-badge {
  display: inline-block;
  min-width: 22px;
  padding: 1px 7px;
  border-radius: 12px;
  background: #b4b4b4;
  color: #fff;
  font-size: 12px;
  line-height: 18px;
}

.summary-cell {
  cursor: pointer;
  color: #606266;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
  &:hover { color: var(--sys-primary); }
}

.extra-cell {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  color: var(--sys-primary-text);
}
</style>

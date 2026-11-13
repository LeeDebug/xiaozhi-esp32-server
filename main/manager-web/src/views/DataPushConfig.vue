<template>
  <div class="welcome">
    <HeaderBar />
    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="ai-card" shadow="never">
            <div class="page-header">
              <h2 class="page-title">{{ $t('header.dataPushConfig') }}</h2>
              <span class="page-desc">管理设备开机自动拉取的数据上报配置，设备根据生效配置决定是否上报、上报频率与上报内容。</span>
              <div class="header-actions">
                <CustomButton icon="el-icon-view" size="medium" @click="openPreview">
                  预览设备端配置
                </CustomButton>
                <CustomButton icon="el-icon-document" size="medium" @click="openGuide">
                  接入指南
                </CustomButton>
                <CustomButton icon="el-icon-plus" type="confirm" size="medium" @click="openAdd">
                  新增配置
                </CustomButton>
              </div>
            </div>
            <div class="divider"></div>

            <el-table :data="list" size="small" class="ai-table" border v-loading="loading">
              <el-table-column prop="configName" label="配置名称" min-width="160" />
              <el-table-column label="是否上报" min-width="90" align="center">
                <template slot-scope="scope">
                  <el-tag :type="scope.row.enabled === 1 ? 'success' : 'info'" size="mini">
                    {{ scope.row.enabled === 1 ? '是' : '否' }}
                  </el-tag>
                </template>
              </el-table-column>
              <el-table-column prop="reportInterval" label="上报间隔(分钟)" min-width="120" align="center" />
              <el-table-column label="上报字段数" min-width="110" align="center">
                <template slot-scope="scope">
                  {{ scope.row.reportContent ? scope.row.reportContent.length : 0 }} / {{ totalOptions }}
                </template>
              </el-table-column>
              <el-table-column label="上报设备信息" min-width="110" align="center">
                <template slot-scope="scope">
                  {{ scope.row.reportDeviceInfo === 1 ? '是' : '否' }}
                </template>
              </el-table-column>
              <el-table-column label="上报地理位置" min-width="110" align="center">
                <template slot-scope="scope">
                  {{ scope.row.reportLocation === 1 ? '是' : '否' }}
                </template>
              </el-table-column>
              <el-table-column label="生效配置" min-width="90" align="center">
                <template slot-scope="scope">
                  <el-tag v-if="scope.row.isDefault === 1" type="warning" size="mini">生效中</el-tag>
                  <span v-else>—</span>
                </template>
              </el-table-column>
              <el-table-column label="操作" min-width="220" align="center">
                <template slot-scope="scope">
                  <el-button type="text" size="small" @click="openEdit(scope.row)">编辑</el-button>
                  <el-button
                    type="text"
                    size="small"
                    :disabled="scope.row.isDefault === 1"
                    @click="handleSetDefault(scope.row)"
                  >
                    设为生效
                  </el-button>
                  <el-button type="text" size="small" class="danger-text" @click="handleDelete(scope.row)">
                    删除
                  </el-button>
                </template>
              </el-table-column>
            </el-table>

            <div class="pager">
              <el-pagination
                background
                layout="prev, pager, next"
                :total="total"
                :page-size="limit"
                :current-page.sync="page"
                @current-change="handlePageChange"
              />
            </div>
          </el-card>
        </div>
      </div>
    </div>

    <!-- 新增/编辑对话框 -->
    <el-dialog
      :title="dialogMode === 'add' ? '新增数据上报配置' : '编辑数据上报配置'"
      :visible.sync="dialogVisible"
      width="860px"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      class="report-config-dialog"
    >
      <el-form :model="form" label-width="104px" class="compact-form">
        <el-row :gutter="12">
          <el-col :span="14">
            <el-form-item label="配置名称" required>
              <el-input v-model="form.configName" placeholder="请输入配置名称" maxlength="100" />
            </el-form-item>
          </el-col>
          <el-col :span="10">
            <el-form-item label="设为生效">
              <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="8">
            <el-form-item label="是否上报">
              <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上报设备信息">
              <el-switch v-model="form.reportDeviceInfo" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="上报地理位置">
              <el-switch v-model="form.reportLocation" :active-value="1" :inactive-value="0" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-row :gutter="12">
          <el-col :span="10">
            <el-form-item label="上报间隔(分钟)">
              <el-input-number v-model="form.reportInterval" :min="1" :max="1440" controls-position="right" />
            </el-form-item>
          </el-col>
        </el-row>

        <el-form-item label="上报内容">
          <div class="rc-toolbar">
            <el-button type="text" size="small" class="rc-action" @click="selectAll">全选</el-button>
            <el-button type="text" size="small" class="rc-action" @click="invertSelection">反选</el-button>
            <el-input
              v-model="metricKeyword"
              size="mini"
              clearable
              placeholder="搜索序号 / 名称 / 英文key / 取值范围"
              class="rc-search"
            />
            <el-checkbox v-model="showRange" size="mini" class="rc-range-toggle">取值范围</el-checkbox>
            <span class="rc-count">已选 {{ selectedCount }} / {{ totalOptions }} 项</span>
          </div>
          <div class="report-content">
            <div class="rc-group" v-for="g in filteredGroups" :key="g.label">
              <div class="rc-group-title">{{ g.label }}（{{ g.options.length }} 项）</div>
              <el-checkbox-group v-model="form.reportContent">
                <el-checkbox
                  v-for="opt in g.options"
                  :key="opt.key"
                  :label="opt.key"
                  size="small"
                  border
                  :title="tipText(opt)"
                  class="rc-checkbox"
                >
                  <span class="rc-item-label">{{ metricSeqText(opt) }}. {{ opt.label }}</span>
                  <span v-if="showRange" class="rc-item-range">{{ opt.range || '—' }}</span>
                </el-checkbox>
              </el-checkbox-group>
            </div>
            <div v-if="filteredGroups.length === 0" class="rc-empty">未找到匹配的字段</div>
          </div>
          <div class="rc-hint">
            字段来源：《AI语音管家与生产监控终端通讯控制逻辑表》「管家」工作表，按序号排列，新建配置默认全选；
            存储与接口下发仅使用 <code>mb-mcp_</code> 前缀的英文 key，中文仅用于界面展示。
            序号 167 的 11 项共用寄存器 <code>0xA6</code>，一条 485 指令即可读取，详见「接入指南」第 3 节。
          </div>
        </el-form-item>

        <el-form-item label="其他配置">
          <el-input
            v-model="form.otherConfig"
            type="textarea"
            :rows="2"
            placeholder="预留，可填写扩展配置（字符串）"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <CustomButton @click="dialogVisible = false">取消</CustomButton>
        <CustomButton type="confirm" :loading="submitting" @click="submit">
          {{ dialogMode === 'add' ? '创建' : '保存' }}
        </CustomButton>
      </div>
    </el-dialog>

    <!-- 设备端接入指南 -->
    <el-dialog
      title="设备端接入指南"
      :visible.sync="guideVisible"
      width="1100px"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      class="guide-dialog"
    >
      <div class="guide-doc">
        <h4>整体流程</h4>
        <p>设备开机 → 调用「获取配置」接口拉取当前生效的上报配置 → 按返回结果中的开关 / 间隔 / 字段定时上报数据（建议每次上报后或定时重新拉取一次，以便服务端动态调整）。</p>

        <h4>1. 获取上报配置（已实现）</h4>
        <p><code>GET /xiaozhi/data-report-config/current</code> —— 公开接口，<b>无需登录</b>（设备开机通常无 token）。</p>
        <pre>{
  "enabled": 1,
  "reportContent": [
    "mb-mcp_version",
    "mb-mcp_commAddress",
    "mb-mcp_silo1CurrentWeight",
    "mb-mcp_silo1RealTimeFeedAmount"
  ],
  "reportInterval": 5,
  "reportDeviceInfo": 1,
  "reportLocation": 0,
  "otherConfig": "",
  "isDefault": 1
}</pre>
        <p>字段说明：</p>
        <ul>
          <li><b>enabled</b>：是否上报（0 否 / 1 是）；为 0 时设备无需上报。</li>
          <li><b>reportContent</b>：要上报的字段（<code>mb-mcp_</code> 前缀英文 key 数组），取值见下方「全量字段目录」，接口中不会出现中文。</li>
          <li><b>reportInterval</b>：上报间隔，单位「分钟」。</li>
          <li><b>reportDeviceInfo</b>：是否附带设备信息（0/1）。</li>
          <li><b>reportLocation</b>：是否附带地理位置（0/1）。</li>
          <li><b>otherConfig</b>：预留扩展字符串。</li>
        </ul>

        <h4>2. 上报数据（已实现）</h4>
        <p><code>POST /xiaozhi/data-analysis/upload</code> —— 公开接口，后端落库后可在「数据预览 / 数据分析 / 数据预测」中查看。请求体示例：</p>
        <pre>POST /xiaozhi/data-analysis/upload
Content-Type: application/json

{
  "deviceId": "设备唯一ID",
  "timestamp": 1699000000000,
  "data": {
    "mb-mcp_version": 100,
    "mb-mcp_commAddress": 1,
    "mb-mcp_silo1CurrentWeight": 350.5,
    "mb-mcp_silo1RealTimeFeedAmount": 12.3
  },
  "deviceInfo": { "firmware": "v1.2.3", "model": "xxx" },
  "location": { "lng": 0.0, "lat": 0.0 }
}</pre>
        <p>约定：<code>data</code> 中的 key 必须与第 1 步 <code>reportContent</code> 返回的英文 key 一一对应；<code>deviceInfo</code> / <code>location</code> 仅在对应开关为 1 时携带；数值字段按目录中的「取值范围」上送。</p>

        <h4>3. 位字段与 RS485 读取（重要）</h4>
        <p>目录中带「位」的 10 个 key（<code>mb-mcp_moduleWaterLineFlush</code> … <code>mb-mcp_moduleLightDimming</code>）
        与 <code>mb-mcp_moduleStatus</code> <b>共用同一个寄存器</b>：序号 167，DEC 地址 166，HEX <code>0xA6</code>，位域 [0:15]，只读 R。
        这 10 项是状态字的位 0~9，位 10~15 未定义，<b>不是 10 个独立寄存器地址</b>。</p>
        <p>因此采集时<b>只需一条 485 读指令</b>读取 0xA6，拿到 1 个 16 位整数后按位取 0/1 即可，
        <b>不要按这 10 个 key 分别发 10 条指令</b>。Excel 给出的典型值：<code>0000001111111000</code>（合同默认配置）、
        <code>0000001111111011</code>（无鸡蛋计数器）。</p>
        <pre>status = read_485(0xA6)                     // 唯一一次 RS485 读，返回 16 位整数
bit(n) = (status / 2^n) % 2                  // 取第 n 位，值为 0 或 1

mb-mcp_moduleStatus             = status     // 原始 16 位状态字（策略A）
mb-mcp_moduleWaterLineFlush     = bit(0)     // 位0 水线冲洗
mb-mcp_moduleWaterLineDetection = bit(1)     // 位1 水线检测
mb-mcp_moduleEggCounting        = bit(2)     // 位2 鸡蛋计数
...
mb-mcp_moduleLightDimming       = bit(9)     // 位9 灯光调光系统</pre>
        <p><b>上报策略二选一，避免冗余：</b></p>
        <ul>
          <li><b>A（推荐，省流量）</b>：<code>reportContent</code> 只保留 <code>mb-mcp_moduleStatus</code>，上送原始 16 位整数，由服务端 / 前端按位解析。</li>
          <li><b>B（便于直接看状态）</b>：按配置选中的位字段，把解出的 0/1 分别上送；此时不要再把 <code>mb-mcp_moduleStatus</code> 一起上送。</li>
          <li>注意：本页「新增配置」默认全选全部字段，会同时包含整体状态字与 10 个位（一次采集产生 11 个 key，其中 10 个冗余）。上线前建议在「上报内容」里按 A 或 B 取舍。</li>
        </ul>

        <h4>4. 关键约定</h4>
        <ul>
          <li>上报字段一律使用 <code>mb-mcp_</code> 前缀的<b>英文 key</b>（如 mb-mcp_temperature 形式），存储、传输、对接均用英文，仅前端展示使用中文名称与取值范围。</li>
          <li>只有被标记为「生效中」的配置才会被 <code>/current</code> 返回；没有生效配置时返回内置默认值（enabled=0，即默认不上报）。</li>
          <li>字段中文名、取值范围等元数据可通过 <code>GET /xiaozhi/data-analysis/metrics</code> 获取（需登录，返回 key / name / seq / bit / group / rw / defaultValue / hexAddress / valueRange）。<code>hexAddress</code> 相同即表示共用一次 485 读取。</li>
        </ul>

        <h4>5. 全量字段目录（按 Excel 序号顺序，共 {{ totalOptions }} 项）</h4>
        <div class="catalog-toolbar">
          <el-input
            v-model="guideKeyword"
            size="mini"
            clearable
            placeholder="搜索序号 / 名称 / 英文key / 分组 / 取值范围"
            class="catalog-search"
          />
          <span class="catalog-count">共 {{ filteredCatalog.length }} 项</span>
        </div>
        <el-table :data="filteredCatalog" size="mini" border height="340" class="catalog-table">
          <el-table-column label="序号/位" width="94" align="center">
            <template slot-scope="s">{{ metricSeqText(s.row) }}</template>
          </el-table-column>
          <el-table-column prop="key" label="metric_key" min-width="250" show-overflow-tooltip />
          <el-table-column prop="label" label="名称" min-width="150" show-overflow-tooltip />
          <el-table-column prop="group" label="分组" min-width="130" show-overflow-tooltip />
          <el-table-column prop="rw" label="读写" width="72" align="center" />
          <el-table-column prop="defaultValue" label="默认值" width="86" align="center" />
          <el-table-column prop="hex" label="HEX" width="72" align="center" />
          <el-table-column prop="range" label="取值范围" min-width="320" show-overflow-tooltip />
        </el-table>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import HeaderBar from '@/components/HeaderBar.vue'
import CustomButton from '@/components/CustomButton.vue'
import dataReport from '@/apis/module/dataReport.js'
import {
  REPORT_METRICS,
  REPORT_METRIC_GROUPS,
  ALL_METRIC_KEYS,
  getMetric,
  metricSeqText as formatSeqText,
} from '@/constants/reportMetrics.js'

const escapeHtml = (s) =>
  String(s === null || s === undefined ? '' : s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')

export default {
  name: 'DataPushConfig',
  components: { HeaderBar, CustomButton },
  data() {
    return {
      loading: false,
      list: [],
      total: 0,
      page: 1,
      limit: 10,
      reportGroups: REPORT_METRIC_GROUPS,
      catalog: REPORT_METRICS,
      metricKeyword: '',
      guideKeyword: '',
      showRange: false,
      dialogVisible: false,
      guideVisible: false,
      dialogMode: 'add',
      currentId: '',
      submitting: false,
      form: {
        configName: '',
        enabled: 0,
        reportContent: [],
        reportInterval: 5,
        reportDeviceInfo: 1,
        reportLocation: 0,
        otherConfig: '',
        isDefault: 0,
      },
    }
  },
  created() {
    this.loadList()
  },
  computed: {
    allReportOptions() {
      return ALL_METRIC_KEYS
    },
    totalOptions() {
      return ALL_METRIC_KEYS.length
    },
    selectedCount() {
      return this.form.reportContent ? this.form.reportContent.length : 0
    },
    filteredGroups() {
      const kw = this.metricKeyword.trim().toLowerCase()
      if (!kw) return this.reportGroups
      return this.reportGroups
        .map((g) => ({
          label: g.label,
          options: g.options.filter((o) =>
            String(o.seq) === kw ||
            o.label.toLowerCase().includes(kw) ||
            o.key.toLowerCase().includes(kw) ||
            (o.range && o.range.toLowerCase().includes(kw)) ||
            g.label.toLowerCase().includes(kw)
          ),
        }))
        .filter((g) => g.options.length > 0)
    },
    visibleOptions() {
      return this.filteredGroups.reduce((acc, g) => acc.concat(g.options.map((o) => o.key)), [])
    },
    filteredCatalog() {
      const kw = this.guideKeyword.trim().toLowerCase()
      if (!kw) return this.catalog
      return this.catalog.filter((m) =>
        String(m.seq) === kw ||
        m.label.toLowerCase().includes(kw) ||
        m.key.toLowerCase().includes(kw) ||
        (m.group && m.group.toLowerCase().includes(kw)) ||
        (m.range && m.range.toLowerCase().includes(kw))
      )
    },
  },
  methods: {
    metricSeqText(m) {
      return formatSeqText(m)
    },
    tipText(opt) {
      const lines = [
        `#${formatSeqText(opt)} ${opt.label}`,
        `metric_key: ${opt.key}`,
        `读写: ${opt.rw || '—'}　默认值: ${opt.defaultValue || '—'}　HEX: ${opt.hex || '—'}`,
        `取值范围: ${opt.range || '—'}`,
      ]
      if (opt.key === 'mb-mcp_moduleStatus') {
        lines.push('16 位状态字：一次 485 读取 0xA6，位0~9 已拆为独立字段（见接入指南第3节）')
      } else if (opt.bit !== undefined && opt.bit !== null) {
        lines.push(`共用寄存器 0xA6（序号167），与整体状态字一次 485 读取，按位${opt.bit}解析`)
      }
      return lines.join('\n')
    },
    loadList() {
      this.loading = true
      dataReport.page({ page: this.page, limit: this.limit }, (res) => {
        this.loading = false
        const body = res && res.data
        if (body && body.code === 0 && body.data) {
          this.list = body.data.list || []
          this.total = body.data.total || 0
        } else {
          this.$message.error((body && body.msg) || '加载配置列表失败')
        }
      })
    },
    handlePageChange(p) {
      this.page = p
      this.loadList()
    },
    openAdd() {
      this.dialogMode = 'add'
      this.currentId = ''
      this.form = {
        configName: '',
        enabled: 0,
        // 按 Excel 序号顺序默认纳入全部字段，用户可按需取消
        reportContent: ALL_METRIC_KEYS.slice(),
        reportInterval: 5,
        reportDeviceInfo: 1,
        reportLocation: 0,
        otherConfig: '',
        isDefault: 0,
      }
      this.dialogVisible = true
    },
    openEdit(row) {
      this.dialogMode = 'edit'
      this.currentId = row.id
      this.form = {
        configName: row.configName,
        enabled: row.enabled,
        reportContent: row.reportContent ? row.reportContent.slice() : [],
        reportInterval: row.reportInterval,
        reportDeviceInfo: row.reportDeviceInfo,
        reportLocation: row.reportLocation,
        otherConfig: row.otherConfig || '',
        isDefault: row.isDefault,
      }
      this.dialogVisible = true
    },
    selectAll() {
      if (this.metricKeyword.trim()) {
        const merged = new Set((this.form.reportContent || []).concat(this.visibleOptions))
        this.form.reportContent = Array.from(merged)
      } else {
        this.form.reportContent = ALL_METRIC_KEYS.slice()
      }
    },
    invertSelection() {
      const pool = this.metricKeyword.trim() ? this.visibleOptions : this.allReportOptions
      const selected = new Set(this.form.reportContent || [])
      const outside = (this.form.reportContent || []).filter((k) => !pool.includes(k))
      this.form.reportContent = pool.filter((k) => !selected.has(k)).concat(outside)
    },
    submit() {
      if (!this.form.configName || !this.form.configName.trim()) {
        this.$message.warning('请输入配置名称')
        return
      }
      const payload = {
        configName: this.form.configName.trim(),
        enabled: this.form.enabled,
        reportContent: this.form.reportContent,
        reportInterval: this.form.reportInterval,
        reportDeviceInfo: this.form.reportDeviceInfo,
        reportLocation: this.form.reportLocation,
        otherConfig: this.form.otherConfig,
        isDefault: this.form.isDefault,
      }
      this.submitting = true
      const finish = () => { this.submitting = false }
      if (this.dialogMode === 'add') {
        dataReport.create(payload, (res) => {
          finish()
          const body = res && res.data
          if (body && body.code === 0) {
            this.$message.success('创建成功')
            this.dialogVisible = false
            this.loadList()
          } else {
            this.$message.error((body && body.msg) || '创建失败')
          }
        })
      } else {
        dataReport.update(this.currentId, payload, (res) => {
          finish()
          const body = res && res.data
          if (body && body.code === 0) {
            this.$message.success('保存成功')
            this.dialogVisible = false
            this.loadList()
          } else {
            this.$message.error((body && body.msg) || '保存失败')
          }
        })
      }
    },
    handleSetDefault(row) {
      this.$confirm('确定将该配置设为生效配置吗？其它配置将不再生效。', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        closeOnClickModal: false,
        closeOnPressEscape: true,
      }).then(() => {
        dataReport.setDefault(row.id, (res) => {
          const body = res && res.data
          if (body && body.code === 0) {
            this.$message.success('已设为生效配置')
            this.loadList()
          } else {
            this.$message.error((body && body.msg) || '操作失败')
          }
        })
      }).catch(() => {})
    },
    handleDelete(row) {
      this.$confirm('确定删除该配置吗？', '提示', {
        confirmButtonText: '确定',
        cancelButtonText: '取消',
        type: 'warning',
        closeOnClickModal: false,
        closeOnPressEscape: true,
      }).then(() => {
        dataReport.remove(row.id, (res) => {
          const body = res && res.data
          if (body && body.code === 0) {
            this.$message.success('删除成功')
            this.loadList()
          } else {
            this.$message.error((body && body.msg) || '删除失败')
          }
        })
      }).catch(() => {})
    },
    openGuide() {
      this.guideVisible = true
    },
    openPreview() {
      dataReport.current((res) => {
        const body = res && res.data
        const data = body && body.code === 0 ? body.data : null
        if (!data) {
          this.$alert(
            '暂无生效配置（请先在列表中将某条配置设为「生效」，或接口返回异常）',
            '设备端获取到的 data-report-config 内容',
            { customClass: 'pv-message-box', closeOnClickModal: false, closeOnPressEscape: true }
          )
          return
        }
        const noteMap = {
          id: '配置记录ID（数据库主键，设备一般无需关心）',
          configName: '配置名称',
          enabled: '是否需要上报（0：否 / 1：是）',
          reportContent: '上报内容字段清单（mb-mcp_ 前缀英文 key，中文含义与取值范围见下列每一项）',
          reportInterval: '上报时间间隔（单位：分钟）',
          reportDeviceInfo: '是否上报设备信息（0：否 / 1：是）',
          reportLocation: '是否上报地理位置（0：否 / 1：是）',
          otherConfig: '其他配置（预留扩展，字符串）',
          isDefault: '是否为当前生效配置（0：否 / 1：是）',
          createDate: '配置创建时间',
          updateDate: '配置更新时间',
        }
        const order = ['id', 'configName', 'enabled', 'reportContent', 'reportInterval', 'reportDeviceInfo', 'reportLocation', 'otherConfig', 'isDefault', 'createDate', 'updateDate']
        let html = '<div class="pv-preview">'
        order.forEach((k) => {
          if (!(k in data)) return
          if (k === 'reportContent') {
            const arr = Array.isArray(data.reportContent) ? data.reportContent : []
            html += '<div class="pv-row"><span class="pv-key">reportContent</span>' +
              '<span class="pv-note">' + noteMap[k] + '</span></div>'
            html += '<div class="pv-summary">本次下发 ' + arr.length + ' 个字段（字段目录共 ' + ALL_METRIC_KEYS.length + ' 项）</div>'
            html += '<ul class="pv-list">'
            if (arr.length === 0) {
              html += '<li><span class="pv-note">（空：未选择任何上报字段）</span></li>'
            } else {
              arr.forEach((key) => {
                const meta = getMetric(key)
                if (meta) {
                  html += '<li><code>' + escapeHtml(key) + '</code> —— ' + escapeHtml(meta.label) +
                    ' <span class="pv-meta">#' + escapeHtml(formatSeqText(meta)) + ' · ' + escapeHtml(meta.group) +
                    ' · ' + escapeHtml(meta.rw || '—') + ' · ' + escapeHtml(meta.hex || '—') +
                    (meta.bit !== undefined && meta.bit !== null ? ' · 共用 0xA6，一次 485 读取' : '') +
                    (meta.key === 'mb-mcp_moduleStatus' ? ' · 位0~9 可拆分' : '') + '</span>' +
                    '<div class="pv-range">取值范围：' + escapeHtml(meta.range || '—') + '</div></li>'
                } else {
                  html += '<li><code>' + escapeHtml(key) + '</code> —— ' +
                    '<span class="pv-unknown">未在字段目录中找到（历史或自定义字段）</span></li>'
                }
              })
            }
            html += '</ul>'
          } else {
            const val = data[k] === null || data[k] === undefined ? '' : data[k]
            html += '<div class="pv-row"><span class="pv-key">' + k + '</span>' +
              '<span class="pv-val">' + escapeHtml(val) + '</span>' +
              '<span class="pv-note">' + noteMap[k] + '</span></div>'
          }
        })
        html += '</div>'
        this.$alert(
          html,
          '设备端获取到的 data-report-config 内容',
          { dangerouslyUseHTMLString: true, customClass: 'pv-message-box', closeOnClickModal: false, closeOnPressEscape: true }
        )
      })
    },
  },
}
</script>

<style lang="scss" scoped>
@import '@/styles/global.scss';

.welcome {
  min-width: 900px;
  min-height: 506px;
  height: 100vh;
  display: flex;
  position: relative;
  flex-direction: column;
  background: var(--sys-surface);
  overflow: hidden;
}

.main-wrapper {
  height: calc(100vh - 63px - 35px);
  margin: 20px 22px 0;
  border-radius: 15px;
  position: relative;
  display: flex;
  flex-direction: column;
  overflow: auto;
}

.content-panel {
  flex: 1;
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

.ai-card {
  background: white;
  flex: 1;
  display: flex;
  flex-direction: column;
  border: none;
  box-shadow: none;
  overflow: hidden;
}

.ai-card ::v-deep .el-card__body {
  padding: 20px 24px;
  display: flex;
  flex-direction: column;
  flex: 1;
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
  flex: 1;
  min-width: 240px;
}

.header-actions {
  display: flex;
  gap: 8px;
}

.divider {
  height: 1px;
  background: #f3f1f1;
  margin: 16px 0;
}

.ai-table {
  width: 100%;
}

.danger-text {
  color: #f56c6c;
}

.pager {
  display: flex;
  justify-content: flex-end;
  margin-top: 16px;
}

.report-content {
  border: 1px solid var(--sys-primary-bg-soft);
  border-radius: 8px;
  padding: 10px 12px;
  max-height: 220px;
  overflow: auto;
}

.rc-toolbar {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-bottom: 8px;
}

.rc-action {
  padding-left: 0;
  padding-right: 4px;
  margin: 0;
}

.rc-search {
  width: 230px;
  margin-left: 6px;
}

.rc-range-toggle {
  margin-left: 4px;
}

.rc-count {
  font-size: 12px;
  color: #8a93b8;
  margin-left: auto;
  white-space: nowrap;
}

.rc-group {
  margin-bottom: 8px;
}

.rc-group:last-child {
  margin-bottom: 0;
}

.rc-group-title {
  font-size: 13px;
  font-weight: 600;
  color: #2c3354;
  margin-bottom: 6px;
}

.rc-group ::v-deep .el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.rc-group ::v-deep .rc-checkbox.el-checkbox {
  height: auto;
  min-height: 30px;
  margin: 0;
  padding: 4px 8px;
  display: inline-flex;
  align-items: center;
  max-width: 100%;
}

.rc-group ::v-deep .rc-checkbox .el-checkbox__label {
  white-space: normal;
  line-height: 1.4;
  padding-left: 6px;
}

.rc-item-label {
  font-size: 12px;
}

.rc-item-range {
  display: block;
  font-size: 11px;
  color: #8a93b8;
  line-height: 1.35;
  max-width: 260px;
  margin-top: 2px;
}

.rc-empty {
  text-align: center;
  color: #8a93b8;
  font-size: 12px;
  padding: 18px 0;
}

.rc-hint {
  font-size: 12px;
  color: #8a93b8;
  line-height: 1.5;
  margin-top: 6px;
}

.rc-hint code {
  background: var(--sys-primary-bg-soft);
  padding: 0 4px;
  border-radius: 3px;
  color: var(--sys-primary-deep);
}

.compact-form .el-form-item {
  margin-bottom: 10px;
}

.compact-form .el-form-item__label {
  line-height: 32px;
}

.report-config-dialog ::v-deep .el-dialog__header {
  padding: 16px 20px 8px;
}

.report-config-dialog ::v-deep .el-dialog__body {
  padding: 8px 20px 4px;
}

.guide-doc {
  font-size: 13px;
  line-height: 1.7;
  color: #2c3354;
  text-align: left;
}

.guide-doc h4 {
  margin: 16px 0 6px;
  font-size: 14px;
  color: var(--sys-primary-deep);
}

.guide-doc h4:first-child {
  margin-top: 0;
}

.guide-doc p {
  margin: 6px 0;
}

.guide-doc code {
  background: var(--sys-primary-bg-soft);
  padding: 1px 6px;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: var(--sys-primary-deep);
}

.guide-doc pre {
  background: #1e2433;
  color: var(--sys-primary-bg-soft);
  padding: 12px 14px;
  border-radius: 8px;
  overflow: auto;
  font-size: 12px;
  line-height: 1.5;
  white-space: pre;
  margin: 6px 0 10px;
}

.guide-doc ul {
  padding-left: 20px;
  margin: 6px 0;
}

.guide-doc li {
  margin: 3px 0;
}

.catalog-toolbar {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 6px 0;
}

.catalog-search {
  width: 320px;
}

.catalog-count {
  font-size: 12px;
  color: #8a93b8;
}

.catalog-table {
  width: 100%;
  font-size: 12px;
}

.guide-dialog ::v-deep .el-dialog__body {
  padding: 8px 20px 12px;
  max-height: 76vh;
  overflow: auto;
  text-align: left;
}
</style>

<style>
/* 设备端配置预览（作用于 $alert 注入的 HTML，非 scoped） */
.pv-preview {
  font-size: 13px;
  line-height: 1.7;
  color: #2c3354;
  text-align: left;
  max-height: 66vh;
  overflow: auto;
  margin: 0;
}

.pv-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 5px 0;
  border-bottom: 1px dashed var(--sys-primary-bg-soft);
}

.pv-key {
  font-weight: 600;
  color: var(--sys-primary-deep);
  min-width: 132px;
  flex-shrink: 0;
}

.pv-val {
  color: var(--sys-primary-deep);
  font-family: Consolas, Monaco, monospace;
  word-break: break-all;
}

.pv-note {
  color: #8a93b8;
  font-size: 12px;
}

.pv-summary {
  font-size: 12px;
  color: var(--sys-primary-deep);
  padding: 4px 0 2px;
}

.pv-list {
  margin: 4px 0 8px;
  padding-left: 20px;
}

.pv-list li {
  margin: 5px 0;
}

.pv-list code {
  background: var(--sys-primary-bg-soft);
  padding: 0 5px;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  color: var(--sys-primary-deep);
}

.pv-meta {
  color: #8a93b8;
  font-size: 12px;
}

.pv-range {
  font-size: 12px;
  color: #6b7280;
  padding-left: 6px;
}

.pv-unknown {
  color: #e6a23c;
  font-size: 12px;
}

.pv-message-box.el-message-box {
  width: 760px;
  max-width: 92vw;
}

.pv-message-box .el-message-box__message {
  text-align: left;
}

/* 接入指南弹窗在小屏幕上不溢出 */
.guide-dialog.el-dialog {
  max-width: 94vw;
}

.report-config-dialog.el-dialog {
  max-width: 94vw;
}
</style>

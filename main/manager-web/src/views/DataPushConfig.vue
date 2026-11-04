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
      width="720px"
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
            <span class="rc-count">已选 {{ selectedCount }} / {{ totalOptions }} 项</span>
          </div>
          <div class="report-content">
            <div class="rc-group" v-for="g in reportGroups" :key="g.label">
              <div class="rc-group-title">{{ g.label }}</div>
              <el-checkbox-group v-model="form.reportContent">
                <el-checkbox
                  v-for="opt in g.options"
                  :key="opt.key"
                  :label="opt.key"
                  border
                  size="small"
                >{{ opt.label }}</el-checkbox>
              </el-checkbox-group>
            </div>
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
      width="900px"
      :close-on-click-modal="false"
      :close-on-press-escape="true"
      class="guide-dialog"
    >
      <div class="guide-doc">
        <h4>整体流程</h4>
        <p>设备开机 → 调用「获取配置」接口拉取当前生效的上报配置 → 按返回结果中的开关 / 间隔 / 字段定时上报数据（建议每次上报后或定时重新拉取一次，以便服务端动态调整）。</p>

        <h4>1. 获取上报配置（已实现）</h4>
        <p><code>GET /xiaozhi/data-report-config/current</code> —— 公开接口，<b>无需登录</b>（设备开机通常无 token）。</p>
        <pre>GET /xiaozhi/data-report-config/current</pre>
        <p>返回示例（Result.data 内）：</p>
        <pre>{
  "enabled": 1,
  "reportContent": ["chickenCount", "silo1Weight", "temperature"],
  "reportInterval": 5,
  "reportDeviceInfo": 1,
  "reportLocation": 0,
  "otherConfig": "",
  "isDefault": 1
}</pre>
        <p>字段说明：</p>
        <ul>
          <li><b>enabled</b>：是否上报（0 否 / 1 是）；为 0 时设备无需上报。</li>
          <li><b>reportContent</b>：要上报的字段（英文 key 数组），取值见下方清单。</li>
          <li><b>reportInterval</b>：上报间隔，单位「分钟」。</li>
          <li><b>reportDeviceInfo</b>：是否附带设备信息（0/1）。</li>
          <li><b>reportLocation</b>：是否附带地理位置（0/1）。</li>
          <li><b>otherConfig</b>：预留扩展字符串。</li>
        </ul>

        <h4>2. 上报数据（已实现）</h4>
        <p><code>POST /xiaozhi/data-analysis/upload</code> —— 设备按本配置上报数据，后端落库后可在「数据预览 / 数据分析 / 数据预测」中查看。请求体示例：</p>
        <pre>POST /xiaozhi/data-analysis/upload
Content-Type: application/json

{
  "deviceId": "设备唯一ID",
  "timestamp": 1699000000000,
  "data": {
    "chickenCount": 1200,
    "silo1Weight": 350.5,
    "temperature": 25.3
  },
  "deviceInfo": { "firmware": "v1.2.3", "model": "xxx" },
  "location": { "lng": 0.0, "lat": 0.0 }
}</pre>
        <p>约定：<code>data</code> 中的 key 必须与第 1 步 <code>reportContent</code> 返回的英文 key 一一对应；<code>deviceInfo</code> / <code>location</code> 仅在对应开关为 1 时携带。</p>

        <h4>3. 关键约定</h4>
        <ul>
          <li>上报字段一律使用<b>英文 key</b>（如 chickenCount / silo1Weight / cranePosition / temperature…），存储、传输、对接均用英文，仅前端展示用中文。</li>
          <li>只有被标记为「生效中」的配置才会被 <code>/current</code> 返回；没有生效配置时返回内置默认值（enabled=0，即默认不上报）。</li>
          <li>英文 key 清单（与前端「上报内容」一致）：
            <br>生产：perChickenData, chickenCount, envExtremes, mortalityRate, waterFeedAmount, eggLaying, eggYield, waterUsage, silo1Weight, silo2Weight
            <br>运行：envVentilation, deviceStatus, fanGroup, windowCurtain, specialFan, cranePosition
            <br>环境：tempProbe, envTemperature, envHumidity, otherEnvMetrics, light, light1Brightness, light2Brightness, temperature, humidity, ozone, airQuality
            <br>其他：powerStatistics
          </li>
        </ul>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import HeaderBar from '@/components/HeaderBar.vue'
import CustomButton from '@/components/CustomButton.vue'
import dataReport from '@/apis/module/dataReport.js'

// 上报内容可选项（分类组织，与设备端约定一致）
// 约定：value 使用英文 key 进行存储/交互/传输，label 仅用于前端展示
const REPORT_CONTENT_GROUPS = [
  {
    label: '生产数据',
    options: [
      { key: 'perChickenData', label: '只鸡数据' },
      { key: 'chickenCount', label: '鸡只数量' },
      { key: 'envExtremes', label: '环境极值' },
      { key: 'mortalityRate', label: '死亡率' },
      { key: 'waterFeedAmount', label: '水料量' },
      { key: 'eggLaying', label: '产蛋' },
      { key: 'eggYield', label: '产蛋量' },
      { key: 'waterUsage', label: '用水量' },
      { key: 'silo1Weight', label: '料塔1重量' },
      { key: 'silo2Weight', label: '料塔2重量' },
    ],
  },
  {
    label: '运行数据',
    options: [
      { key: 'envVentilation', label: '环境通风' },
      { key: 'deviceStatus', label: '设备状态' },
      { key: 'fanGroup', label: '风机组' },
      { key: 'windowCurtain', label: '小窗幕帘' },
      { key: 'specialFan', label: '特殊风机' },
      { key: 'cranePosition', label: '航车位置' },
    ],
  },
  {
    label: '环境数据',
    options: [
      { key: 'tempProbe', label: '温度探头' },
      { key: 'envTemperature', label: '环境温度' },
      { key: 'envHumidity', label: '环境湿度' },
      { key: 'otherEnvMetrics', label: '其他环境指标' },
      { key: 'light', label: '灯光' },
      { key: 'light1Brightness', label: '灯光1亮度' },
      { key: 'light2Brightness', label: '灯光2亮度' },
      { key: 'temperature', label: '温度' },
      { key: 'humidity', label: '湿度' },
      { key: 'ozone', label: '臭氧' },
      { key: 'airQuality', label: '空气质量' },
    ],
  },
  {
    label: '其他数据',
    options: [
      { key: 'powerStatistics', label: '电能统计' },
    ],
  },
]

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
      reportGroups: REPORT_CONTENT_GROUPS,
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
      return this.reportGroups.reduce((acc, g) => acc.concat(g.options.map((o) => o.key)), [])
    },
    totalOptions() {
      return this.allReportOptions.length
    },
    selectedCount() {
      return this.form.reportContent ? this.form.reportContent.length : 0
    },
  },
  methods: {
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
        reportContent: [],
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
      this.form.reportContent = this.allReportOptions.slice()
    },
    invertSelection() {
      const selected = new Set(this.form.reportContent || [])
      this.form.reportContent = this.allReportOptions.filter((o) => !selected.has(o))
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
        const escapeHtml = (s) =>
          String(s)
            .replace(/&/g, '&amp;')
            .replace(/</g, '&lt;')
            .replace(/>/g, '&gt;')
            .replace(/"/g, '&quot;')
        // 英文 key -> 中文含义 映射
        const keyLabelMap = {}
        this.reportGroups.forEach((g) => {
          g.options.forEach((o) => {
            keyLabelMap[o.key] = o.label
          })
        })
        const noteMap = {
          id: '配置记录ID（数据库主键，设备一般无需关心）',
          configName: '配置名称',
          enabled: '是否需要上报（0：否 / 1：是）',
          reportContent: '上报内容字段清单（英文 key，中文含义见下列每一项）',
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
            html += '<div class="pv-row"><span class="pv-key">reportContent</span><span class="pv-note">' + noteMap[k] + '</span></div>'
            html += '<ul class="pv-list">'
            const arr = Array.isArray(data.reportContent) ? data.reportContent : []
            if (arr.length === 0) {
              html += '<li><span class="pv-note">（空：未选择任何上报字段）</span></li>'
            } else {
              arr.forEach((key) => {
                const label = keyLabelMap[key] || key
                html += '<li><code>' + escapeHtml(key) + '</code> —— ' + escapeHtml(label) + '</li>'
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
  background: #eff4ff;
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
  border: 1px solid #ebeef5;
  border-radius: 8px;
  padding: 10px 12px;
  max-height: 220px;
  overflow: auto;
}

.rc-toolbar {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 10px;
}

.rc-action {
  padding-left: 0;
  padding-right: 6px;
  margin: 0;
}

.rc-count {
  font-size: 12px;
  color: #8a93b8;
  margin-left: auto;
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
  margin-bottom: 8px;
}

.rc-group ::v-deep .el-checkbox-group {
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #8a93b8;
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
  color: #1f2a52;
}

.guide-doc h4:first-child {
  margin-top: 0;
}

.guide-doc p {
  margin: 6px 0;
}

.guide-doc code {
  background: #f3f5fb;
  padding: 1px 6px;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  font-size: 12px;
  color: #2b6cb0;
}

.guide-doc pre {
  background: #1e2433;
  color: #e6edf3;
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

.guide-dialog ::v-deep .el-dialog__body {
  padding: 8px 20px 12px;
  max-height: 72vh;
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
  max-height: 60vh;
  overflow: auto;
  margin: 0;
}

.pv-row {
  display: flex;
  align-items: baseline;
  gap: 10px;
  padding: 5px 0;
  border-bottom: 1px dashed #eef0f5;
}

.pv-key {
  font-weight: 600;
  color: #1f2a52;
  min-width: 132px;
  flex-shrink: 0;
}

.pv-val {
  color: #2b6cb0;
  font-family: Consolas, Monaco, monospace;
  word-break: break-all;
}

.pv-note {
  color: #8a93b8;
  font-size: 12px;
}

.pv-list {
  margin: 4px 0 8px;
  padding-left: 20px;
}

.pv-list li {
  margin: 3px 0;
}

.pv-list code {
  background: #f3f5fb;
  padding: 0 5px;
  border-radius: 4px;
  font-family: Consolas, Monaco, monospace;
  color: #2b6cb0;
}

.pv-message-box.el-message-box {
  width: 680px;
  max-width: 92vw;
}

.pv-message-box .el-message-box__message {
  text-align: left;
}
</style>

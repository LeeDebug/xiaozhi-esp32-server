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
      width="760px"
    >
      <el-form :model="form" label-width="130px">
        <el-form-item label="配置名称" required>
          <el-input v-model="form.configName" placeholder="请输入配置名称" maxlength="100" />
        </el-form-item>
        <el-form-item label="是否上报">
          <el-switch v-model="form.enabled" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="上报内容">
          <div class="report-content">
            <div class="rc-group" v-for="g in reportGroups" :key="g.label">
              <div class="rc-group-title">{{ g.label }}</div>
              <el-checkbox-group v-model="form.reportContent">
                <el-checkbox
                  v-for="opt in g.options"
                  :key="opt"
                  :label="opt"
                  border
                  size="small"
                >{{ opt }}</el-checkbox>
              </el-checkbox-group>
            </div>
          </div>
        </el-form-item>
        <el-form-item label="上报间隔(分钟)">
          <el-input-number v-model="form.reportInterval" :min="1" :max="1440" />
        </el-form-item>
        <el-form-item label="上报设备信息">
          <el-switch v-model="form.reportDeviceInfo" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="上报地理位置">
          <el-switch v-model="form.reportLocation" :active-value="1" :inactive-value="0" />
        </el-form-item>
        <el-form-item label="其他配置">
          <el-input
            v-model="form.otherConfig"
            type="textarea"
            :rows="3"
            placeholder="预留，可填写扩展配置（字符串）"
          />
        </el-form-item>
        <el-form-item label="设为生效配置">
          <el-switch v-model="form.isDefault" :active-value="1" :inactive-value="0" />
          <span class="form-tip">开启后保存即生效，其它配置将不再生效</span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <CustomButton @click="dialogVisible = false">取消</CustomButton>
        <CustomButton type="confirm" :loading="submitting" @click="submit">
          {{ dialogMode === 'add' ? '创建' : '保存' }}
        </CustomButton>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import HeaderBar from '@/components/HeaderBar.vue'
import CustomButton from '@/components/CustomButton.vue'
import dataReport from '@/apis/module/dataReport.js'

// 上报内容可选项（分类组织，与设备端约定一致）
const REPORT_CONTENT_GROUPS = [
  { label: '生产数据', options: ['只鸡数据', '鸡只数量', '环境极值', '死亡率', '水料量', '产蛋', '产蛋量', '用水量', '料塔1重量', '料塔2重量'] },
  { label: '运行数据', options: ['环境通风', '设备状态', '风机组', '小窗幕帘', '特殊风机', '航车位置'] },
  { label: '环境数据', options: ['温度探头', '环境温度', '环境湿度', '其他环境指标', '灯光', '灯光1亮度', '灯光2亮度', '温度', '湿度', '臭氧', '空气质量'] },
  { label: '其他数据', options: ['电能统计'] },
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
    openPreview() {
      dataReport.current((res) => {
        const body = res && res.data
        const data = body && body.code === 0 ? body.data : null
        this.$alert(
          '<pre style="white-space:pre-wrap;word-break:break-all;">' +
            JSON.stringify(data, null, 2) +
            '</pre>',
          '设备端获取到的 data-report-config 内容',
          { dangerouslyUseHTMLString: true }
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
  padding: 12px 14px;
  max-height: 320px;
  overflow: auto;
}

.rc-group {
  margin-bottom: 12px;
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
  gap: 8px;
}

.form-tip {
  margin-left: 12px;
  font-size: 12px;
  color: #8a93b8;
}
</style>

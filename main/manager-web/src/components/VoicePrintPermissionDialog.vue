<template>
  <el-dialog :title="$t('voiceprintPermission.title')" :visible="visible" width="760px" append-to-body
    :close-on-click-modal="false" @update:visible="$emit('update:visible', $event)" @open="load">
    <div v-loading="loading">
      <el-alert v-if="!enabled" type="warning" :closable="false" show-icon
        :title="$t('voiceprintPermission.disabledTip')" class="tip" />
      <el-alert v-if="readonly" type="info" :closable="false" show-icon
        :title="$t('voiceprintPermission.readonlyTip')" class="tip" />
      <div class="unknown-line">
        {{ $t('voiceprintPermission.unknownTip') }}
        <el-tag v-for="code in unknownPermissions" :key="code" size="mini" class="code-tag">{{ itemName(code) }}</el-tag>
        <span v-if="!unknownPermissions.length">{{ $t('voiceprintPermission.none') }}</span>
      </div>
      <el-empty v-if="!loading && !rows.length" :description="$t('voiceprintPermission.empty')" />
      <el-table v-else :data="rows" border size="small">
        <el-table-column :label="$t('voiceprintPermission.speaker')" width="160">
          <template slot-scope="scope">
            <div>{{ scope.row.sourceName }}</div>
            <el-tag size="mini" :type="scope.row.configured ? 'success' : 'info'">
              {{ scope.row.configured ? $t('voiceprintPermission.custom') : $t('voiceprintPermission.useDefault') }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column :label="$t('voiceprintPermission.permissions')">
          <template slot-scope="scope">
            <el-checkbox-group v-model="scope.row.permissions" :disabled="readonly" @change="onChange(scope.row)">
              <el-checkbox v-for="item in options" :key="item.code" :label="item.code"
                :disabled="item.code !== 'all' && scope.row.permissions.includes('all')">
                <el-tooltip :disabled="!item.tools" :content="(item.tools || []).join(', ')" placement="top">
                  <span>{{ item.name }}</span>
                </el-tooltip>
              </el-checkbox>
            </el-checkbox-group>
          </template>
        </el-table-column>
        <el-table-column :label="$t('device.operation')" width="100" align="center">
          <template slot-scope="scope">
            <el-button size="mini" type="text" :disabled="readonly || !scope.row.configured"
              @click="resetDefault(scope.row)">{{ $t('voiceprintPermission.resetDefault') }}</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
    <span slot="footer">
      <el-button size="small" @click="$emit('update:visible', false)">{{ $t('button.cancel') }}</el-button>
      <el-button v-if="!readonly" size="small" type="primary" :loading="saving" @click="save">{{ $t('button.ok') }}</el-button>
    </span>
  </el-dialog>
</template>

<script>
import Api from '@/apis/api';

export default {
  name: 'VoicePrintPermissionDialog',
  props: {
    visible: { type: Boolean, default: false },
    deviceId: { type: String, default: '' },
  },
  data() {
    return {
      loading: false,
      saving: false,
      enabled: false,
      readonly: false,
      items: [],
      defaultPermissions: [],
      unknownPermissions: [],
      rows: [],
    };
  },
  computed: {
    options() {
      return [...this.items, { code: 'all', name: this.$t('voiceprintPermission.all') }];
    },
  },
  methods: {
    itemName(code) {
      const item = this.options.find(i => i.code === code);
      return item ? item.name : code;
    },
    load() {
      if (!this.deviceId) return;
      this.loading = true;
      Api.device.getVoiceprintPermission(this.deviceId, ({ data }) => {
        this.loading = false;
        if (data.code !== 0) {
          this.$message.error({ message: data.msg, showClose: true });
          return;
        }
        const d = data.data || {};
        this.enabled = !!d.enabled;
        this.readonly = !!d.readonly;
        this.items = d.items || [];
        this.defaultPermissions = d.defaultPermissions || [];
        this.unknownPermissions = d.unknownPermissions || [];
        this.rows = (d.voiceprints || []).map(r => ({ ...r, permissions: [...(r.permissions || [])] }));
      }, () => {
        this.loading = false;
      });
    },
    onChange(row) {
      row.configured = true;
    },
    resetDefault(row) {
      row.permissions = [...this.defaultPermissions];
      row.configured = false;
    },
    save() {
      this.saving = true;
      const payload = {
        voiceprints: this.rows.map(r => ({
          voiceprintId: r.voiceprintId,
          permissions: r.configured ? r.permissions : null,
        })),
      };
      Api.device.saveVoiceprintPermission(this.deviceId, payload, ({ data }) => {
        this.saving = false;
        if (data.code === 0) {
          this.$message.success({ message: this.$t('voiceprintPermission.saveSuccess'), showClose: true });
          this.$emit('update:visible', false);
        } else {
          this.$message.error({ message: data.msg, showClose: true });
        }
      }, () => {
        this.saving = false;
      });
    },
  },
};
</script>

<style scoped>
.tip {
  margin-bottom: 10px;
}

.unknown-line {
  margin-bottom: 10px;
  font-size: 13px;
  color: #606266;
}

.code-tag {
  margin-right: 4px;
}
</style>

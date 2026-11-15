<template>
  <div class="welcome">
    <HeaderBar />
    <div class="main-wrapper">
      <div class="content-panel">
        <div class="content-area">
          <el-card class="profile-card" shadow="never" v-loading="loading">
            <div class="profile-header">
              <div class="avatar-wrap">
                <img src="@/assets/home/avatar.png" alt="avatar" />
              </div>
              <div class="profile-heading">
                <h2 class="page-title">{{ $t('userProfile.title') }}</h2>
                <p class="page-subtitle">{{ $t('userProfile.subtitle') }}</p>
              </div>
              <div class="display-name">{{ profile.displayName || '-' }}</div>
            </div>

            <div class="section-title">{{ $t('userProfile.basicSection') }}</div>
            <div class="info-grid">
              <div class="info-item">
                <div class="info-label">{{ $t('userProfile.userId') }}</div>
                <div class="info-value">{{ profile.id != null ? profile.id : '-' }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">{{ $t('user.username') }}</div>
                <div class="info-value">{{ profile.username || '-' }}</div>
              </div>
              <div class="info-item info-item-wide">
                <div class="info-label">{{ $t('user.nickname') }}</div>
                <div v-if="!editing" class="info-value nickname-view">
                  <span>{{ profile.nickname || profile.username || '-' }}</span>
                  <el-button type="text" size="mini" icon="el-icon-edit" @click="startEditNickname">
                    {{ $t('user.editNickname') }}
                  </el-button>
                </div>
                <div v-else class="nickname-editor">
                  <div class="editor-row">
                    <el-input v-model="nicknameDraft" size="small" maxlength="64" show-word-limit
                      :placeholder="$t('user.nicknamePlaceholder')" @keyup.enter.native="saveNickname" />
                    <el-button size="small" type="primary" :loading="saving" @click="saveNickname">
                      {{ $t('common.confirm') }}
                    </el-button>
                    <el-button size="small" @click="cancelEditNickname">{{ $t('common.cancel') }}</el-button>
                  </div>
                  <div class="nickname-tip">{{ $t('user.nicknameTip') }}</div>
                </div>
              </div>
              <div class="info-item">
                <div class="info-label">{{ $t('userProfile.role') }}</div>
                <div class="info-value">
                  <el-tag size="mini" :type="profile.superAdmin === 1 ? 'warning' : 'info'">{{ roleText }}</el-tag>
                </div>
              </div>
              <div class="info-item">
                <div class="info-label">{{ $t('user.status') }}</div>
                <div class="info-value">
                  <el-tag size="mini" :type="profile.status === 0 ? 'danger' : 'success'">{{ statusText }}</el-tag>
                </div>
              </div>
              <div class="info-item">
                <div class="info-label">{{ $t('userProfile.createDate') }}</div>
                <div class="info-value">{{ formatDate(profile.createDate) }}</div>
              </div>
              <div class="info-item">
                <div class="info-label">{{ $t('userProfile.updateDate') }}</div>
                <div class="info-value">{{ formatDate(profile.updateDate) }}</div>
              </div>
            </div>

            <div class="section-title">{{ $t('userProfile.statsSection') }}</div>
            <div class="stat-grid">
              <div class="stat-card">
                <div class="stat-value">{{ profile.deviceCount != null ? profile.deviceCount : '-' }}</div>
                <div class="stat-label">{{ $t('userProfile.deviceCount') }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-value">{{ profile.agentCount != null ? profile.agentCount : '-' }}</div>
                <div class="stat-label">{{ $t('userProfile.agentCount') }}</div>
              </div>
              <div class="stat-card">
                <div class="stat-value stat-text">
                  {{ profile.canViewAllUserData ? $t('userProfile.scopeAll') : $t('userProfile.scopeOwn') }}
                </div>
                <div class="stat-label">{{ $t('userProfile.dataScope') }}</div>
              </div>
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

export default {
  name: 'UserProfile',
  components: { HeaderBar, VersionFooter },
  data() {
    return {
      loading: false,
      saving: false,
      editing: false,
      nicknameDraft: '',
      profile: {}
    };
  },
  created() {
    this.fetchProfile();
  },
  computed: {
    roleText() {
      return this.profile.superAdmin === 1
        ? this.$t('userProfile.superAdmin')
        : this.$t('userProfile.normalUser');
    },
    statusText() {
      return this.profile.status === 0 ? this.$t('user.disabled') : this.$t('user.normal');
    }
  },
  methods: {
    fetchProfile() {
      this.loading = true;
      Api.user.getProfile(({ data }) => {
        this.loading = false;
        if (data.code === 0) {
          this.profile = data.data || {};
        } else {
          this.$message.error(data.msg || this.$t('user.operationFailed'));
        }
      });
    },
    startEditNickname() {
      this.nicknameDraft = this.profile.nickname || '';
      this.editing = true;
    },
    cancelEditNickname() {
      this.editing = false;
      this.nicknameDraft = this.profile.nickname || '';
    },
    saveNickname() {
      const value = (this.nicknameDraft || '').trim();
      if (value.length > 64) {
        this.$message.error(this.$t('user.nicknameTooLong'));
        return;
      }
      this.saving = true;
      Api.user.updateProfile(value, ({ data }) => {
        this.saving = false;
        if (data.code === 0) {
          this.profile = data.data || this.profile;
          this.editing = false;
          this.syncHeaderUserInfo();
          this.$message.success(this.$t('user.nicknameSaveSuccess'));
        } else {
          this.$message.error(data.msg || this.$t('user.nicknameSaveFailed'));
        }
      });
    },
    // Vuex 的 userInfo 只在登录时写入，这里同步一次，右上角昵称才会立即刷新
    syncHeaderUserInfo() {
      const current = this.$store.state.userInfo || {};
      this.$store.commit('setUserInfo', {
        ...current,
        username: this.profile.username || current.username,
        nickname: this.profile.nickname || ''
      });
    },
    // 后端 Date 已按北京时间输出字面值，直接展示，避免前端再做一次时区偏移
    formatDate(value) {
      if (value == null || value === '') return '-';
      if (typeof value === 'number') {
        const d = new Date(value);
        return `${d.getFullYear()}-${String(d.getMonth() + 1).padStart(2, '0')}-${String(d.getDate()).padStart(2, '0')} ${String(d.getHours()).padStart(2, '0')}:${String(d.getMinutes()).padStart(2, '0')}:${String(d.getSeconds()).padStart(2, '0')}`;
      }
      return String(value).replace('T', ' ').slice(0, 19);
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
  display: flex;
  flex-direction: column;
  box-sizing: border-box;
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

.profile-card {
  background: white;
  flex: 1;
  border: none;
  box-shadow: none;
  overflow: auto;

  ::v-deep .el-card__body {
    padding: 22px 30px;
  }
}

.profile-header {
  display: flex;
  align-items: center;
  gap: 16px;
  padding-bottom: 22px;
  border-bottom: 1px solid #f0f2f7;
}

.avatar-wrap {
  width: 58px;
  height: 58px;
  border-radius: 50%;
  background: var(--sys-primary-bg-soft, #fff3e8);
  display: flex;
  align-items: center;
  justify-content: center;

  img {
    width: 34px;
    height: 34px;
  }
}

.profile-heading {
  flex: 1;
}

.page-title {
  font-weight: 500;
  font-size: 24px;
  margin: 0;
}

.page-subtitle {
  margin: 6px 0 0;
  color: #909399;
  font-size: 13px;
}

.display-name {
  padding: 6px 16px;
  border-radius: 18px;
  background: var(--sys-primary-bg-soft, #fff3e8);
  color: var(--sys-primary, #ff7d28);
  font-size: 14px;
  font-weight: 500;
}

.section-title {
  font-size: 16px;
  font-weight: 500;
  margin: 24px 0 14px;
  padding-left: 10px;
  border-left: 3px solid var(--sys-primary, #ff7d28);
}

.info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 2px 26px;
}

.info-item {
  padding: 12px 4px;
  border-bottom: 1px dashed #f2f4f8;
  min-width: 0;
}

.info-item-wide {
  grid-column: 1 / -1;
}

.info-label {
  color: #909399;
  font-size: 13px;
  margin-bottom: 6px;
}

.info-value {
  color: #303133;
  font-size: 15px;
  word-break: break-all;
}

.nickname-view {
  display: flex;
  align-items: center;
  gap: 14px;
}

.nickname-editor {
  max-width: 520px;
}

.editor-row {
  display: flex;
  align-items: center;
  gap: 10px;
}

.nickname-tip {
  color: #a8abb2;
  font-size: 12px;
  margin-top: 8px;
}

.stat-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 16px;
}

.stat-card {
  border-radius: 12px;
  background: var(--sys-primary-bg-soft, #fff8f2);
  padding: 20px;
  text-align: center;
}

.stat-value {
  font-size: 28px;
  font-weight: 600;
  color: var(--sys-primary, #ff7d28);
}

.stat-text {
  font-size: 16px;
  line-height: 40px;
}

.stat-label {
  margin-top: 6px;
  color: #7c8194;
  font-size: 13px;
}
</style>

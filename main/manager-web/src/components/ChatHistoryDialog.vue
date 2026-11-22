<template>
    <CustomDialog
        :title="$t('chatHistory.with') + agentName + $t('chatHistory.dialogTitle')"
        :visible.sync="dialogVisible"
        width="80%"
        size="large"
        :footer="false"
        :close-on-click-modal="false"
        custom-class="chat-history-dialog">
        <template v-slot:title>
            <span style="font-size: 18px;">
                <template v-if="recordsMode">
                    {{ $t('chatRecords.sessionDialogTitle') }}
                    <template v-if="deviceAlias"> · {{ deviceAlias }}</template>
                    <template v-if="macAddress">
                        [<MacAddressMask :macAddress="macAddress" />]
                    </template>
                </template>
                <template v-else>
                    {{ $t('chatHistory.with') + agentName + $t('chatHistory.dialogTitle') }}
                    <template v-if="currentMacAddress">
                        [<MacAddressMask :macAddress="currentMacAddress" />]
                    </template>
                </template>
            </span>
        </template>
        <div class="chat-container">
            <div class="session-list" :class="{ 'session-list--records': recordsMode }" @scroll="handleScroll">
                <div v-if="recordsMode && macAddress" class="record-filter-hint">
                    {{ $t('chatRecords.dialogOnlyDevice') }}
                </div>
                <!-- 两种模式都按「会话」维度展示：头像 + 会话标题（无标题时显示时间） + 消息数角标 -->
                <div v-for="session in sessions" :key="session.sessionId" class="session-item"
                    :class="{ active: currentSessionId === session.sessionId }" @click="selectSession(session)">
                    <img :src="getUserAvatar(session.sessionId)" class="avatar" />
                    <div class="session-info">
                        <div class="session-time">{{ sessionLabel(session) }}</div>
                        <div v-if="recordsMode" class="session-sub">{{ session.createdAtText }}</div>
                        <div class="message-count">{{ sessionCount(session) }}</div>
                    </div>
                </div>
                <div v-if="!loading && sessions.length === 0" class="no-more">
                    {{ $t('chatRecords.noRecords') }}
                </div>
                <div v-if="loading" class="loading">{{ $t('chatHistory.loading') }}</div>
                <div v-if="!hasMore && sessions.length > 0" class="no-more">
                    {{ $t('chatHistory.noMoreRecords') }}
                </div>
            </div>
            <div class="chat-content">
                <div v-if="currentSessionId" class="messages">
                    <div v-for="(message, index) in messagesWithMetricsFlag" :key="message.id || ('msg-' + index)"
                        class="message-row" :class="{ 'is-user': message.chatType === 1 }">
                        <!-- 每条消息自带时间：后端已按 GMT+8 输出 yyyy-MM-dd HH:mm:ss，直接展示 -->
                        <div v-if="message.createdAt" class="message-time">{{ formatMessageTime(message.createdAt) }}</div>
                        <div class="message-item" :class="{ 'user-message': message.chatType === 1, 'tool-message': message.chatType === 3 }">
                            <img :src="message.chatType === 1 ? getUserAvatar(currentSessionId) : require('@/assets/xiaozhi-logo.png')"
                                class="avatar" />
                            <div class="message-content">
                                <template v-if="Array.isArray(extractContentFromString(message.content))">
                                    <div class="content-wrapper">
                                        <div v-for="(item, idx) in extractContentFromString(message.content)" :key="idx">
                                            <div v-if="item.type === 'text'" class="text-content">{{ item.text }}</div>
                                            <div v-else-if="item.type === 'tool'" class="tool-call-text">{{ item.text }}</div>
                                            <div v-else-if="item.type === 'tool_result'" class="tool-call-text">
                                                <div v-if="item.text && item.text.length > 80" class="tool-result-wrapper">
                                                    <div v-if="isToolResultCollapsed(index, idx)" class="tool-result-collapsed">
                                                        {{ getFirstLineText(item.text) }}
                                                    </div>
                                                    <div v-else class="tool-result-expanded">
                                                        {{ item.text }}
                                                    </div>
                                                    <span class="tool-toggle-btn" @click="toggleToolResult(index, idx)">
                                                        <i :class="isToolResultCollapsed(index, idx) ? 'el-icon-arrow-down' : 'el-icon-arrow-up'"></i>
                                                    </span>
                                                </div>
                                                <div v-else>{{ item.text }}</div>
                                            </div>
                                        </div>
                                    </div>
                                </template>
                                <template v-else>
                                    {{ extractContentFromString(message.content) }}
                                </template>
                                <i v-if="message.audioId" :class="getAudioIconClass(message)"
                                    @click="playAudio(message)" class="audio-icon"></i>
                            </div>
                        </div>
                        <div v-if="message._showMetrics" class="metrics-panel">
                            <div class="metrics-summary" @click="toggleMetrics(index)">
                                <i :class="isMetricsExpanded(index) ? 'el-icon-arrow-up' : 'el-icon-arrow-down'"></i>
                                <span>{{ $t('chatHistory.metricsTitle') }}</span>
                                <strong>{{ formatDuration(message.metrics.e2eTotalMs) }}</strong>
                                <span v-if="Number(message.metrics.e2eFirstAudioMs) > 0" class="metrics-sub">
                                    {{ $t('chatHistory.metricsFirstAudio') }} {{ formatDuration(message.metrics.e2eFirstAudioMs) }}
                                </span>
                                <span v-if="Number(message.metrics.e2eFromUserStopMs) > 0" class="metrics-sub">
                                    {{ $t('chatHistory.metricsFromUserStop') }} {{ formatDuration(message.metrics.e2eFromUserStopMs) }}
                                </span>
                            </div>
                            <div v-if="isMetricsExpanded(index)" class="metrics-detail">
                                <template v-for="group in getMetricsGroups(message.metrics)">
                                    <div :key="group.key + '-title'" class="metrics-group-title">{{ $t(group.titleKey) }}</div>
                                    <div v-for="row in group.rows" :key="group.key + '-' + row.key" class="metrics-row">
                                        <span class="metrics-label">{{ $t(row.labelKey) }}</span>
                                        <span class="metrics-bar-wrap">
                                            <span class="metrics-bar" :style="{ width: getMetricsBarWidth(row.value, message.metrics) + '%' }"></span>
                                        </span>
                                        <span class="metrics-value">{{ formatDuration(row.value) }}</span>
                                    </div>
                                </template>
                                <div v-if="getMetricsFacts(message.metrics).length" class="metrics-facts">
                                    <div v-for="fact in getMetricsFacts(message.metrics)" :key="fact.key" class="metrics-fact">
                                        <span class="metrics-fact-label">{{ $t(fact.labelKey) }}</span>
                                        <span class="metrics-fact-value">{{ fact.text }}</span>
                                    </div>
                                </div>
                                <div v-if="Number(message.metrics.llmRounds) > 1" class="metrics-note">
                                    {{ $t('chatHistory.metricsLlmRounds') }} {{ message.metrics.llmRounds }}
                                </div>
                                <div class="metrics-note">{{ $t('chatHistory.metricsNote') }}</div>
                            </div>
                        </div>
                    </div>
                </div>
                <div v-else class="no-session-selected">
                    {{ $t('chatHistory.selectSession') }}
                </div>
            </div>
        </div>
        <div v-if="currentSessionId" class="download-buttons">
            <!-- 「前20条会话」下载接口按 agent 取全量会话，会串入其他设备，记录驱动模式下不提供 -->
            <el-button v-if="!recordsMode" type="primary" plain size="small" @click="downloadCurrentSessionWithPrevious">
                {{ $t('chatHistory.downloadCurrentWithPreviousSessions') }}
            </el-button>
            <el-button type="primary" plain size="small" @click="downloadCurrentSession">
                {{ $t('chatHistory.downloadCurrentSession') }}
            </el-button>
        </div>
    </CustomDialog>
</template>

<script>
import { debounce } from '@/utils'
import Api from '@/apis/api';
import CustomDialog from '@/components/CustomDialog.vue';
import MacAddressMask from '@/components/MacAddressMask.vue';

// 耗时面板按流程阶段分组：用户说话→识别→意图→记忆/大模型→工具→语音合成→发送→汇总。
// 单位统一为毫秒；值为 0 或缺失的指标不展示（兼容旧版本上报的历史记录）。
const METRIC_GROUPS = [
    {
        key: 'listen',
        titleKey: 'chatHistory.groupListen',
        defs: [
            { key: 'vadWaitMs', labelKey: 'chatHistory.metricVadWait' },
            { key: 'asrMs', labelKey: 'chatHistory.metricAsr' },
            { key: 'intentMs', labelKey: 'chatHistory.metricIntent' }
        ]
    },
    {
        key: 'llm',
        titleKey: 'chatHistory.groupLlm',
        defs: [
            { key: 'memoryQueryMs', labelKey: 'chatHistory.metricMemoryQuery' },
            { key: 'llmTtftMs', labelKey: 'chatHistory.metricLlmTtft' },
            { key: 'llmTotalMs', labelKey: 'chatHistory.metricLlmTotal' },
            { key: 'toolTotalMs', labelKey: 'chatHistory.metricToolTotal' }
        ]
    },
    {
        key: 'tts',
        titleKey: 'chatHistory.groupTts',
        defs: [
            { key: 'ttsTextWaitMs', labelKey: 'chatHistory.metricTtsTextWait' },
            { key: 'ttsProviderMs', labelKey: 'chatHistory.metricTtsProvider' },
            { key: 'ttsFirstMs', labelKey: 'chatHistory.metricTtsFirst' }
        ]
    },
    {
        key: 'send',
        titleKey: 'chatHistory.groupSend',
        defs: [
            { key: 'firstPacketSentMs', labelKey: 'chatHistory.metricFirstPacketSent' },
            { key: 'e2eFirstAudioMs', labelKey: 'chatHistory.metricE2eFirstAudio' },
            { key: 'e2eFromSpeechEndMs', labelKey: 'chatHistory.metricE2eFromSpeechEnd' },
            { key: 'e2eFromUserStopMs', labelKey: 'chatHistory.metricE2eFromUserStop' },
            { key: 'e2eTotalMs', labelKey: 'chatHistory.metricE2eTotal' }
        ]
    }
];
// 扁平化后的指标定义，供条形图计算共用比例尺
const METRIC_DEFS = METRIC_GROUPS.reduce((all, group) => all.concat(group.defs), []);

export default {
    name: 'ChatHistoryDialog',
    props: {
        visible: {
            type: Boolean,
            default: false
        },
        agentId: {
            type: String,
            default: ''
        },
        agentName: {
            type: String,
            default: ''
        },
        // recordsMode：由「会话记录」页面驱动，左侧列表直接使用该页的查询条件 + 当前设备
        recordsMode: {
            type: Boolean,
            default: false
        },
        // 会话记录页 buildParams() 的副本（不含分页），保证弹窗列表与页面结果同条件、同排序
        query: {
            type: Object,
            default: () => ({})
        },
        // 强制限定的设备，弹窗内只能看到这个设备
        macAddress: {
            type: String,
            default: ''
        },
        deviceAlias: {
            type: String,
            default: ''
        },
        // 会话记录页点击的那一行会话，弹窗打开时置顶并自动选中
        initialSession: {
            type: Object,
            default: null
        }
    },
    data() {
        return {
            dialogVisible: false,
            sessions: [],
            messages: [],
            currentSessionId: '',
            currentAgentId: '',
            currentMacAddress: '',
            page: 1,
            limit: 20,
            loading: false,
            hasMore: true,
            scrollTimer: null,
            isFirstLoad: true,
            playingAudioId: null,
            audioElement: null,
            expandedToolResults: {}, // 跟踪工具结果的展开状态
            expandedMetrics: {} // 跟踪耗时面板的展开状态（key 为 messagesWithMetricsFlag 的 index）
        };
    },
    components: {
        CustomDialog,
        MacAddressMask,
    },
    created() {
        // 会话记录页用 v-if 挂载本组件，挂载时 visible 恒为 true，watch 不会触发首次加载，
        // 因此这里主动初始化；旧模式（组件常驻、visible 由 false -> true）仍由 watch 负责。
        this.dialogVisible = this.visible;
        if (this.dialogVisible) {
            this.resetData();
            this.loadSessions();
        }
    },
    watch: {
        visible(val) {
            this.dialogVisible = val;
            if (val) {
                this.resetData();
                this.loadSessions();
            } else {
                this.audioElement?.pause();
                this.audioElement = null;
                this.playingAudioId = null;
            }
        },
        dialogVisible(val) {
            if (!val) {
                this.$emit('update:visible', false);
            }
        }
    },
    computed: {
        // 一轮回答可能被 TTS 拆成多条记录，后端只在最后一条上附带 metrics。
        // 若同一 roundId 意外出现多条（工具结果独立播报等），只保留最后一条显示面板；
        // 历史记录与异常轮次没有 metrics，天然不显示，无需回填。
        messagesWithMetricsFlag() {
            const source = this.messages || [];
            const lastIndexByRound = {};
            source.forEach((message, index) => {
                if (!message.metrics) return;
                const key = message.roundId || ('__no_round__' + index);
                lastIndexByRound[key] = index;
            });
            const visible = new Set(Object.values(lastIndexByRound));
            return source.map((message, index) => ({
                ...message,
                _showMetrics: visible.has(index)
            }));
        }
    },
    methods: {
        /**
         * 从 content 字段中提取聊天内容
         * 如果 content 是 JSON 格式（如 {"speaker": "未知说话人", "content": "现在几点了。"}），则提取 content 字段
         * 如果 content 是普通字符串，则直接返回
         * 
         * @param {string} content 原始内容
         * @returns {string} 提取的聊天内容
         */
        extractContentFromString(content) {
            if (!content || content.trim() === '') {
                return content;
            }

            // 尝试解析为 JSON
            try {
                const jsonObj = JSON.parse(content);

                // 如果是数组格式（包含 text 和 tool）
                if (Array.isArray(jsonObj)) {
                    return jsonObj;
                }

                // 如果是对象且有 content 字段
                if (jsonObj && typeof jsonObj === 'object' && jsonObj.content) {
                    return jsonObj.content;
                }
            } catch (e) {
                // 如果不是有效的 JSON，直接返回原内容
            }

            // 如果不是 JSON 格式或没有 content 字段，直接返回原内容
            return content;
        },
        // 切换工具结果的展开/折叠状态
        toggleToolResult(messageIndex, itemIndex) {
            const key = `${messageIndex}-${itemIndex}`;
            this.$set(this.expandedToolResults, key, !this.expandedToolResults[key]);
        },
        // 判断工具结果是否处于折叠状态
        isToolResultCollapsed(messageIndex, itemIndex) {
            const key = `${messageIndex}-${itemIndex}`;
            // 默认折叠（true表示折叠）
            return !this.expandedToolResults[key];
        },
        // 获取截断的文本（只显示第一行）
        getFirstLineText(text) {
            if (!text) return '';
            const firstLine = text.split('\n')[0];
            return firstLine.length < text.length ? firstLine + '...' : text;
        },
        resetData() {
            // 记录驱动模式把用户点击的会话置顶，后续分页追加时按 sessionId 去重，
            // 保证弹窗一打开就能看到刚点的那个会话。
            this.sessions = (this.recordsMode && this.initialSession && this.initialSession.sessionId)
                ? [{ ...this.initialSession }]
                : [];
            this.messages = [];
            this.currentSessionId = '';
            this.currentAgentId = this.agentId;
            this.currentMacAddress = this.macAddress || '';
            this.page = 1;
            this.loading = false;
            this.hasMore = true;
            this.isFirstLoad = true;
            this.expandedToolResults = {};
            this.expandedMetrics = {};
        },
        loadSessions() {
            if (this.loading || (!this.isFirstLoad && !this.hasMore)) {
                return;
            }

            this.loading = true;
            if (this.recordsMode) {
                this.loadRecords();
                return;
            }

            const params = {
                page: this.page,
                limit: this.limit
            };

            Api.agent.getAgentSessions(this.agentId, params, (res) => {
                if (res.data && res.data.data && Array.isArray(res.data.data.list)) {
                    const list = res.data.data.list;
                    this.hasMore = list.length === this.limit;

                    this.sessions = [...this.sessions, ...list];
                    this.page++;

                    if (this.sessions.length > 0 && !this.currentSessionId) {
                        this.selectSession(this.sessions[0]);
                    }
                }
                this.loading = false;
                this.isFirstLoad = false;
            });
        },
        /**
         * 记录驱动模式：复用会话记录页的会话查询接口与筛选条件（同时间口径、同排序），
         * 并强制把 macAddress 覆盖成当前点击行所属设备，保证弹窗里不混入其它设备。
         */
        loadRecords() {
            const params = { ...this.query, page: this.page, limit: this.limit };
            if (this.macAddress) {
                params.macAddress = this.macAddress;
            } else {
                delete params.macAddress;
            }

            Api.chatHistory.getSessions(params, (res) => {
                if (res && res.data && res.data.code === 0 && res.data.data && Array.isArray(res.data.data.list)) {
                    const list = res.data.data.list;
                    this.hasMore = list.length === this.limit;
                    const existing = new Set(this.sessions.map(item => item.sessionId));
                    this.sessions = [...this.sessions, ...list.filter(item => !existing.has(item.sessionId))];
                    this.page++;

                    if (!this.currentSessionId && this.sessions.length > 0) {
                        this.selectSession(this.sessions[0]);
                    }
                } else {
                    this.hasMore = false;
                }
                this.loading = false;
                this.isFirstLoad = false;
            }, () => {
                this.loading = false;
                this.isFirstLoad = false;
                this.hasMore = false;
            });
        },
        /** 选中左侧会话，右侧展示完整对话（气泡 / 音频 / 耗时面板） */
        selectSession(session) {
            if (!session || !session.sessionId) return;
            this.currentSessionId = session.sessionId;
            const agentId = this.recordsMode ? (session.agentId || this.agentId) : this.agentId;
            this.currentAgentId = agentId;
            this.currentMacAddress = this.macAddress || session.macAddress || '';
            this.expandedToolResults = {};
            this.expandedMetrics = {};

            Api.agent.getAgentChatHistory(agentId, session.sessionId, (res) => {
                if (res && res.data && Array.isArray(res.data.data)) {
                    const all = res.data.data;
                    // session_id 每次连接生成，天然只属于一台设备；按设备再兜底过滤一次
                    this.messages = this.recordsMode && this.macAddress
                        ? all.filter(item => !item.macAddress || item.macAddress === this.macAddress)
                        : all;
                    if (this.messages.length > 0 && this.messages[0].macAddress) {
                        this.currentMacAddress = this.messages[0].macAddress;
                    }
                    // 旧模式没有聚合接口，用实际返回条数回填角标
                    if (!this.recordsMode) {
                        this.sessions = this.sessions.map(item => {
                            if (item.sessionId === session.sessionId) {
                                item.chatCount = this.messages.length;
                            }
                            return item;
                        });
                    }
                } else {
                    this.messages = [];
                }
            });
        },
        /** 新旧两套会话对象的标题字段不同，统一在这里适配 */
        sessionLabel(session) {
            if (this.recordsMode) {
                return session.sessionTitle || session.createdAtText || '';
            }
            return session.title || this.formatTime(session.createdAt);
        },
        sessionCount(session) {
            const count = Number(session.chatCount || 0);
            return count > 99 ? '99' : String(count);
        },
        handleScroll(e) {
            if (this.scrollTimer) {
                clearTimeout(this.scrollTimer);
            }

            this.scrollTimer = setTimeout(() => {
                const { scrollTop, scrollHeight, clientHeight } = e.target;
                // 当滚动到底部时加载更多
                if (scrollHeight - scrollTop <= clientHeight + 50) {
                    this.loadSessions();
                }
            }, 200);
        },
        /**
         * 单条消息的时间展示。
         * 后端 WebMvcConfig 把 java.util.Date 按 GMT+8 序列化成 "yyyy-MM-dd HH:mm:ss"，
         * 这里只做字符串归一化，避免浏览器本地时区再做一次换算把中国时间改错。
         */
        formatMessageTime(value) {
            if (!value) return '';
            const text = String(value);
            const match = text.match(/^(\d{4}-\d{2}-\d{2})[T ](\d{2}:\d{2})(?::(\d{2}))?/);
            if (!match) return text;
            return `${match[1]} ${match[2]}${match[3] ? ':' + match[3] : ''}`;
        },
        formatTime(timestamp) {
            const date = new Date(timestamp);
            const now = new Date();
            const today = new Date(now.getFullYear(), now.getMonth(), now.getDate());
            const yesterday = new Date(today);
            yesterday.setDate(yesterday.getDate() - 1);

            const hours = date.getHours().toString().padStart(2, '0');
            const minutes = date.getMinutes().toString().padStart(2, '0');

            if (date >= today) {
                return `${this.$t('chatHistory.today')} ${hours}:${minutes}`;
            } else if (date >= yesterday) {
                return `${this.$t('chatHistory.yesterday')} ${hours}:${minutes}`;
            } else {
                const year = date.getFullYear();
                const month = (date.getMonth() + 1).toString().padStart(2, '0');
                const day = date.getDate().toString().padStart(2, '0');
                return `${year}-${month}-${day} ${hours}:${minutes}`;
            }
        },
        formatDuration(ms) {
            const value = Number(ms || 0);
            if (value >= 1000) return `${(value / 1000).toFixed(2)}s`;
            return `${Math.round(value)}ms`;
        },
        getMetricsRows(metrics) {
            if (!metrics) return [];
            return METRIC_DEFS
                .filter(def => Number(metrics[def.key]) > 0)
                .map(def => ({ key: def.key, labelKey: def.labelKey, value: Number(metrics[def.key]) }));
        },
        // 按流程阶段分组的数值行，空组不展示
        getMetricsGroups(metrics) {
            if (!metrics) return [];
            return METRIC_GROUPS
                .map(group => ({
                    key: group.key,
                    titleKey: group.titleKey,
                    rows: group.defs
                        .filter(def => Number(metrics[def.key]) > 0)
                        .map(def => ({ key: def.key, labelKey: def.labelKey, value: Number(metrics[def.key]) }))
                }))
                .filter(group => group.rows.length > 0);
        },
        // 非数值类详情：TTS 提供商/是否流式/重试次数、每轮 LLM 明细、工具调用明细
        getMetricsFacts(metrics) {
            if (!metrics) return [];
            const facts = [];
            if (metrics.ttsProvider) {
                const mode = metrics.ttsIsStream ? this.$t('chatHistory.ttsStream') : this.$t('chatHistory.ttsNonStream');
                facts.push({ key: 'ttsProvider', labelKey: 'chatHistory.factTtsProvider', text: `${metrics.ttsProvider} (${mode})` });
            }
            if (Number(metrics.ttsRetryCount) > 0) {
                facts.push({ key: 'ttsRetryCount', labelKey: 'chatHistory.factTtsRetry', text: String(metrics.ttsRetryCount) });
            }
            if (Array.isArray(metrics.llmRoundsDetail) && metrics.llmRoundsDetail.length > 0) {
                const text = metrics.llmRoundsDetail
                    .map((r, i) => `#${i + 1} depth=${r.depth} tools=${r.tools} ${this.$t('chatHistory.metricLlmTtft')} ${this.formatDuration(r.ttftMs)} / ${this.$t('chatHistory.metricLlmTotal')} ${this.formatDuration(r.totalMs)} / ${r.chars}${this.$t('chatHistory.charUnit')}`)
                    .join('\n');
                facts.push({ key: 'llmRoundsDetail', labelKey: 'chatHistory.factLlmRounds', text });
            }
            if (Array.isArray(metrics.toolCalls) && metrics.toolCalls.length > 0) {
                const text = metrics.toolCalls
                    .map(t => `${t.name} ${this.formatDuration(t.costMs)}${t.ok ? '' : ' (' + this.$t('chatHistory.toolFailed') + ')'}`)
                    .join('\n');
                facts.push({ key: 'toolCalls', labelKey: 'chatHistory.factToolCalls', text });
            }
            return facts;
        },
        getMetricsBarWidth(value, metrics) {
            const rows = this.getMetricsRows(metrics);
            // 条形以本轮总耗时为基准，分段指标之间允许时间重叠
            const base = Math.max(1, Number(metrics.e2eTotalMs) || 0, ...rows.map(row => row.value));
            return Math.max(2, Math.min(100, Math.round(value / base * 100)));
        },
        toggleMetrics(index) {
            this.$set(this.expandedMetrics, index, !this.expandedMetrics[index]);
        },
        isMetricsExpanded(index) {
            return !!this.expandedMetrics[index];
        },
        getAudioIconClass(message) {
            if (this.playingAudioId === message.audioId) {
                return 'el-icon-loading';
            }
            return 'el-icon-video-play';
        },
        playAudio: debounce(function(message) {
            if (this.playingAudioId === message.audioId) {
                // 如果正在播放当前音频，则停止播放
                if (this.audioElement) {
                    this.audioElement.pause();
                    this.audioElement = null;
                }
                this.playingAudioId = null;
                return;
            }

            // 停止当前正在播放的音频
            if (this.audioElement) {
                this.audioElement.pause();
                this.audioElement = null;
            }

            // 先获取音频下载ID
            this.playingAudioId = message.audioId;
            Api.agent.getAudioId(message.audioId, (res) => {
                if (res.data && res.data.data) {
                    if (!this.audioElement) {
                        this.audioElement = new Audio();
                    }
                    
                    // 使用获取到的下载ID播放音频
                    this.audioElement.src = Api.getServiceUrl() + `/agent/play/${res.data.data}`;
                    this.audioElement.onended = () => {
                        this.playingAudioId = null;
                        this.audioElement = null;
                    };

                    this.audioElement.play();
                }
            });
        }, 300),
        getUserAvatar(sessionId) {
            // 从 sessionId 中提取所有数字
            const numbers = sessionId.match(/\d+/g);
            if (!numbers) return require('@/assets/user-avatar1.png');

            // 将所有数字相加
            const sum = numbers.reduce((acc, num) => acc + parseInt(num), 0);

            // 计算模5并加1，得到1-5之间的数字
            const avatarIndex = (sum % 5) + 1;

            // 返回对应的头像图片
            return require(`@/assets/user-avatar${avatarIndex}.png`);
        },

        // 下载本会话记录
        downloadCurrentSession() {
            Api.agent.getDownloadUrl(this.currentAgentId || this.agentId, this.currentSessionId, (res) => {
                if (res && res.data && res.data.code === 0 && res.data.data) {
                    const uuid = res.data.data;
                    window.open(`${Api.getServiceUrl()}/agent/chat-history/download/${uuid}/current`, '_blank');
                } else {
                    this.$message.error(this.$t('chatHistory.downloadLinkFailed'));
                }
            });
        },

        // 下载本会话及前20条会话记录
        downloadCurrentSessionWithPrevious() {
            Api.agent.getDownloadUrl(this.agentId, this.currentSessionId, (res) => {
                if (res && res.data && res.data.code === 0 && res.data.data) {
                    const uuid = res.data.data;
                    window.open(`${Api.getServiceUrl()}/agent/chat-history/download/${uuid}/previous`, '_blank');
                } else {
                    this.$message.error(this.$t('chatHistory.downloadLinkFailed'));
                }
            });
        }
    }
};
</script>

<style scoped>
.chat-container {
    display: flex;
    flex: 1;
    min-height: 0;
}

.session-list {
    width: 250px;
    border-right: 1px solid #eee;
    overflow-y: auto;
    padding: 10px;
}

.session-list--records {
    width: 270px;
}

.record-filter-hint {
    font-size: 12px;
    color: #8a94a6;
    background: var(--sys-primary-bg-soft);
    border-radius: 6px;
    padding: 6px 8px;
    margin-bottom: 8px;
    line-height: 1.5;
}

.session-item {
    display: flex;
    align-items: center;
    padding: 10px;
    cursor: pointer;
    border-radius: 8px;
    margin-bottom: 10px;
}

.session-item:hover {
    background-color: #f5f5f5;
}

.session-item.active {
    background-color: var(--sys-primary-bg-soft);
}

.avatar {
    width: 40px;
    height: 40px;
    border-radius: 50%;
    margin-right: 10px;
    flex-shrink: 0;
}

.session-info {
    flex: 1;
    min-width: 0;
    position: relative;
    padding-right: 30px;
}

.session-time {
    font-size: 14px;
    color: #272727;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.session-sub {
    font-size: 11px;
    color: #98a4b3;
    margin-top: 4px;
    line-height: 1.4;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.message-count {
    position: absolute;
    right: 0;
    top: 50%;
    transform: translateY(-50%);
    min-width: 20px;
    height: 20px;
    padding: 0 5px;
    box-sizing: border-box;
    border-radius: 20px;
    background-color: #b4b4b4;
    color: #fff;
    font-size: 12px;
    line-height: 20px;
    text-align: center;
}

.chat-content {
    flex: 1;
    padding: 20px;
    overflow-y: auto;
}

.message-item {
    display: flex;
    margin-bottom: 20px;
}

.message-item.user-message {
    flex-direction: row-reverse;
}

.message-content {
    max-width: 60%;
    padding: 10px 15px;
    border-radius: 8px;
    background-color: #f0f0f0;
    margin: 0 10px;
    text-align: left;
    line-height: 20px;
    position: relative;
    display: flex;
    align-items: center;
}

.audio-icon {
    font-size: 20px;
    cursor: pointer;
    margin: 0 5px;
    color: var(--sys-primary-text);
}

.metrics-panel {
    margin: -12px 10px 20px 60px;
    max-width: 60%;
    border: 1px solid var(--sys-primary-bg-soft);
    border-radius: 8px;
    background-color: var(--sys-primary-bg-soft);
    font-size: 12px;
    color: #5f6b7a;
}

.metrics-summary {
    display: flex;
    align-items: center;
    gap: 6px;
    padding: 8px 10px;
    cursor: pointer;
    user-select: none;
}

.metrics-summary i {
    color: var(--sys-primary-text);
}

.metrics-summary strong {
    color: var(--sys-primary-text);
    font-size: 13px;
    font-weight: 500;
}

.metrics-sub {
    color: #98a4b3;
}

.metrics-detail {
    padding: 8px 10px 10px;
    border-top: 1px solid var(--sys-surface);
}

.metrics-row {
    display: flex;
    align-items: center;
    gap: 8px;
    margin-bottom: 7px;
}

.metrics-label {
    width: 120px;
    flex-shrink: 0;
}

.metrics-bar-wrap {
    flex: 1;
    height: 6px;
    background-color: var(--sys-surface);
    border-radius: 3px;
    overflow: hidden;
}

.metrics-bar {
    display: block;
    height: 100%;
    background-color: var(--sys-primary);
    border-radius: 3px;
}

.metrics-value {
    width: 56px;
    text-align: right;
    font-variant-numeric: tabular-nums;
}

.metrics-note {
    margin-top: 6px;
    color: #98a4b3;
    line-height: 1.5;
}

.metrics-group-title {
    margin: 8px 0 6px;
    font-size: 12px;
    font-weight: 500;
    color: var(--sys-primary-text);
}

.metrics-group-title:first-child {
    margin-top: 0;
}

.metrics-facts {
    margin-top: 8px;
    padding-top: 6px;
    border-top: 1px dashed var(--sys-surface);
}

.metrics-fact {
    display: flex;
    gap: 8px;
    margin-bottom: 4px;
    line-height: 1.5;
}

.metrics-fact-label {
    width: 120px;
    flex-shrink: 0;
}

.metrics-fact-value {
    flex: 1;
    white-space: pre-line;
    word-break: break-all;
    font-variant-numeric: tabular-nums;
}

.user-message .message-content {
    background-color: var(--sys-primary);
    color: white;
    flex-direction: row-reverse;
}

.user-message .audio-icon {
    color: white;
}

.content-wrapper {
    width: 100%;
}

.text-content {
    display: block;
    margin-bottom: 4px;
}

.tool-call-text {
    color: var(--sys-primary-text);
    font-family: 'Courier New', monospace;
    font-weight: 500;
    font-size: 12px;
    display: block;
    margin-top: 4px;
}

.user-message .tool-call-text {
    color: var(--sys-surface);
}

.tool-message .message-content {
    background-color: #f0f0f0;
}

.tool-result-wrapper {
    position: relative;
    padding-right: 20px;
}

.tool-result-collapsed {
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
}

.tool-toggle-btn {
    position: absolute;
    right: 0;
    top: 0;
    cursor: pointer;
    color: var(--sys-primary-text);
    font-size: 12px;
}

.tool-toggle-btn:hover {
    color: var(--sys-primary);
}

.loading,
.no-more {
    text-align: center;
    padding: 10px 10px 30px 10px;
    color: #999;
}

.no-session-selected {
    display: flex;
    justify-content: center;
    align-items: center;
    height: 100%;
    color: #999;
}

/* 每条消息自带时间：左右两侧各留出 60px 间距，保持镜像对称。
   AI/工具消息左对齐、用户消息右对齐，必须显式声明，否则会继承 #app 的 center。 */
.message-row .message-time {
    padding: 0 0 3px;
    padding-left: 60px;
    text-align: left;    /* 必须显式左对齐，否则会继承 #app 的 center 而漂到中间 */
    font-size: 12px;
    line-height: 1.2;
    color: #a4a7ad;
    font-variant-numeric: tabular-nums;
}

.message-row.is-user .message-time {
    padding-right: 60px;
    text-align: right;
}

.download-buttons {
    padding: 20px;
    display: flex;
    gap: 10px;
    border-top: 1px solid #eee;
    background-color: white;
}

.download-buttons .el-button {
    flex: 1;
}
</style>

<style>
.chat-history-dialog {
    min-width: 700px;
    max-width: 85vw;
    /* 兜底，防止 size="large" 那个 height 没有生效时弹框缩成内容高度 */
    height: calc(100vh - 20px) !important;
}

.chat-history-dialog .el-dialog__body {
    padding: 0;
    overflow: hidden;
    display: flex;
    flex-direction: column;
    /* 顶开 size="large" 默认的 100vh - 130px，让 body 真正撑到弹框底部，flex: 1 会自动填满 */
    max-height: calc(100vh - 74px) !important;
}
</style>
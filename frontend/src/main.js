import { createApp, onMounted, ref } from 'vue'
import './style.css'

// 前端只记住统一网关地址，不需要知道每个模型的真实 API 地址。
const API = 'http://localhost:18083/api/models'

createApp({
  setup() {
    // ref 中的数据发生变化时，Vue 会自动更新页面。
    const models = ref([])
    const selectedModel = ref('local-demo')
    const question = ref('请用一句话解释什么是 Java 接口')
    const result = ref(null)
    const calls = ref([])
    const stats = ref({ totalCalls: 0, successfulCalls: 0, successRate: 0, averageDurationMs: 0, models: [] })
    const loading = ref(false)
    const error = ref('')

    // 查询最近 20 条调用记录，用于展示模型运行历史。
    async function loadCalls() {
      const response = await fetch(`${API}/calls`)
      if (!response.ok) throw new Error('调用记录加载失败')
      calls.value = await response.json()
    }

    // 后端会读取数据库并计算评测指标，前端只负责展示结果。
    async function loadStats() {
      const response = await fetch(`${API}/stats`)
      if (!response.ok) throw new Error('评测统计加载失败')
      stats.value = await response.json()
    }

    // 页面打开时同时查询模型列表和历史记录。
    onMounted(async () => {
      try {
        const response = await fetch(API)
        if (!response.ok) throw new Error('模型列表加载失败')
        models.value = await response.json()
        await Promise.all([loadCalls(), loadStats()])
      } catch (exception) {
        error.value = '无法连接后端，请先启动模型平台后端。'
      }
    })

    async function send() {
      if (!question.value.trim() || loading.value) return
      loading.value = true
      error.value = ''
      result.value = null
      try {
        // 不管选择哪个模型，前端始终只调用这一个统一聊天接口。
        const response = await fetch(`${API}/chat`, {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({
            model: selectedModel.value,
            question: question.value.trim()
          })
        })
        const data = await response.json()
        if (!response.ok) throw new Error(data.message || '模型调用失败')
        // 后端返回回答、模型名称和调用耗时，Vue 自动展示在页面上。
        result.value = data
        // 调用成功后重新查询数据库，新记录会立即出现在页面底部。
        await Promise.all([loadCalls(), loadStats()])
      } catch (exception) {
        error.value = exception.message
        // 失败调用也会被后端记录，因此这里也刷新一次历史列表。
        try { await Promise.all([loadCalls(), loadStats()]) } catch (_) {}
      } finally {
        loading.value = false
      }
    }

    function formatTime(value) {
      return new Date(value).toLocaleString('zh-CN', { hour12: false })
    }

    return { models, selectedModel, question, result, calls, stats, loading, error, send, formatTime }
  },
  template: `
    <main class="page">
      <header>
        <div>
          <p class="eyebrow">JAVA · AI ENGINEERING</p>
          <h1>大模型统一接入平台</h1>
          <p class="subtitle">一个接口连接多个模型，记录每一次调用表现。</p>
        </div>
        <span class="phase">PHASE 02 · 调用追踪</span>
      </header>

      <section class="layout">
        <aside class="panel side">
          <p class="step">01 / 选择模型</p>
          <h2>模型提供商</h2>
          <label v-for="model in models" :key="model.id" :class="['model-card', { active: selectedModel === model.id, disabled: !model.available }]">
            <input v-model="selectedModel" type="radio" :value="model.id" :disabled="!model.available" />
            <span><strong>{{ model.name }}</strong><small>{{ model.available ? '当前可用' : '尚未配置' }}</small></span>
            <i :class="model.available ? 'online' : 'offline'"></i>
          </label>
          <div class="tip"><strong>统一接口</strong><code>POST /api/models/chat</code><p>切换模型时，前端请求地址保持不变。</p></div>
        </aside>

        <section class="panel workspace">
          <div class="workspace-head"><div><p class="step">02 / 发起调用</p><h2>模型实验室</h2></div><span class="status">Gateway Online</span></div>
          <label class="prompt-label">输入测试问题</label>
          <textarea v-model="question" maxlength="1000" placeholder="输入想交给模型的问题"></textarea>
          <div class="action-row"><span>{{ question.length }} / 1000</span><button @click="send" :disabled="loading">{{ loading ? '正在调用…' : '发送到统一网关' }}</button></div>

          <p v-if="error" class="error">{{ error }}</p>
          <article v-if="result" class="result">
            <div class="result-meta"><span>模型：<strong>{{ result.model }}</strong></span><span>耗时：<strong>{{ result.durationMs }} ms</strong></span></div>
            <div v-if="result.fallbackUsed" class="fallback-notice">⚠ {{ result.notice }}</div>
            <p>{{ result.answer }}</p>
          </article>
          <div v-else-if="!error" class="empty"><span>↗</span><p>选择模型并发送问题<br/>回答和耗时将在这里显示</p></div>
        </section>
      </section>

      <section class="panel evaluation">
        <div class="history-head">
          <div><p class="step">03 / 自动评测</p><h2>模型表现概览</h2></div>
          <span class="record-count">数据来自真实调用记录</span>
        </div>
        <div class="metric-grid">
          <article class="metric"><span>总调用次数</span><strong>{{ stats.totalCalls }}</strong><small>次</small></article>
          <article class="metric"><span>成功调用</span><strong>{{ stats.successfulCalls }}</strong><small>次</small></article>
          <article class="metric"><span>成功率</span><strong>{{ stats.successRate }}</strong><small>%</small></article>
          <article class="metric"><span>平均响应</span><strong>{{ stats.averageDurationMs }}</strong><small>ms</small></article>
        </div>
        <div v-if="stats.models.length" class="model-comparison">
          <div class="comparison-row comparison-title">
            <span>模型</span><span>调用次数</span><span>成功率</span><span>平均耗时</span>
          </div>
          <div v-for="model in stats.models" :key="model.model" class="comparison-row">
            <strong>{{ model.model }}</strong><span>{{ model.totalCalls }} 次</span>
            <span>{{ model.successRate }}%</span><span>{{ model.averageDurationMs }} ms</span>
          </div>
        </div>
      </section>

      <section class="panel history">
        <div class="history-head">
          <div><p class="step">04 / 数据库存储</p><h2>最近调用记录</h2></div>
          <span class="record-count">{{ calls.length }} 条记录</span>
        </div>
        <div v-if="calls.length" class="call-list">
          <article v-for="call in calls" :key="call.id" class="call-item">
            <div class="call-top">
              <strong>{{ call.model }}</strong>
              <span :class="['call-status', call.status === 'SUCCESS' ? 'success' : 'failed']">
                {{ call.status === 'SUCCESS' ? '成功' : '失败' }}
              </span>
              <time>{{ formatTime(call.createdAt) }}</time>
            </div>
            <p class="call-question">{{ call.question }}</p>
            <div class="call-foot">
              <span>耗时 {{ call.durationMs }} ms</span>
              <span v-if="call.errorMessage">{{ call.errorMessage }}</span>
            </div>
          </article>
        </div>
        <div v-else class="history-empty">发送第一次请求后，数据库记录会显示在这里。</div>
      </section>
    </main>`
}).mount('#app')

<template>
  <div class="page">
    <h2 class="page-title">义务录入</h2>
    <p class="page-desc">录入应付义务并按币种/交割日/状态筛选</p>

    <div class="card-panel" style="margin-bottom:16px">
      <el-form :model="form" label-width="110px" @submit.prevent>
        <el-row :gutter="16">
          <el-col :span="12">
            <el-form-item label="付款方">
              <el-select v-model="form.payerMemberId" filterable style="width:100%" :disabled="!auth.isOperator">
                <el-option v-for="m in activeMembers" :key="m.memberId" :label="m.name" :value="m.memberId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="收款方">
              <el-select v-model="form.payeeMemberId" filterable style="width:100%" :disabled="!auth.isOperator">
                <el-option v-for="m in activeMembers" :key="m.memberId" :label="m.name" :value="m.memberId" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="币种">
              <el-input v-model="form.currency" :disabled="!auth.isOperator" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="金额">
              <el-input-number v-model="form.amount" :min="0.00000001" :precision="8" :controls="false" style="width:100%" :disabled="!auth.isOperator" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="交易日">
              <el-date-picker v-model="form.tradeDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled="!auth.isOperator" />
            </el-form-item>
          </el-col>
          <el-col :span="8">
            <el-form-item label="交割日">
              <el-date-picker v-model="form.settleDate" type="date" value-format="YYYY-MM-DD" style="width:100%" :disabled="!auth.isOperator" />
            </el-form-item>
          </el-col>
        </el-row>
        <el-button type="primary" :disabled="!auth.isOperator" :loading="saving" @click="create">提交义务</el-button>
      </el-form>
    </div>

    <div class="toolbar">
      <el-select v-model="filters.currency" clearable placeholder="币种" style="width:120px">
        <el-option label="USD" value="USD" />
        <el-option label="CNY" value="CNY" />
        <el-option label="EUR" value="EUR" />
      </el-select>
      <el-date-picker v-model="filters.settleDate" type="date" value-format="YYYY-MM-DD" placeholder="交割日" />
      <el-select v-model="filters.status" clearable placeholder="状态" style="width:140px">
        <el-option v-for="s in ['OPEN','NETTED','SETTLED','CANCELLED']" :key="s" :label="s" :value="s" />
      </el-select>
      <el-button type="primary" @click="load">查询</el-button>
    </div>

    <div class="card-panel">
      <el-table :data="rows" v-loading="loading" stripe>
        <el-table-column prop="obligationId" label="义务 ID" min-width="200">
          <template #default="{ row }"><span class="mono">{{ row.obligationId }}</span></template>
        </el-table-column>
        <el-table-column label="付款方" min-width="120">
          <template #default="{ row }">{{ nameOf(row.payerMemberId) }}</template>
        </el-table-column>
        <el-table-column label="收款方" min-width="120">
          <template #default="{ row }">{{ nameOf(row.payeeMemberId) }}</template>
        </el-table-column>
        <el-table-column prop="currency" label="币种" width="80" />
        <el-table-column prop="amount" label="金额" width="140" />
        <el-table-column prop="settleDate" label="交割日" width="120" />
        <el-table-column prop="status" label="状态" width="110">
          <template #default="{ row }">
            <el-tag :type="tagType(row.status)">{{ row.status }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="取消原因" min-width="160">
          <template #default="{ row }">{{ row.cancelReason || '—' }}</template>
        </el-table-column>
        <el-table-column v-if="auth.isOperator" label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button
              v-if="row.status === 'OPEN'"
              type="danger"
              link
              size="small"
              :loading="cancellingId === row.obligationId"
              @click="cancelRow(row)"
            >取消</el-button>
          </template>
        </el-table-column>
      </el-table>
    </div>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import api from '../api/client'
import { useAuthStore } from '../stores/auth'

const auth = useAuthStore()
const members = ref([])
const rows = ref([])
const loading = ref(false)
const saving = ref(false)
const cancellingId = ref('')
const today = new Date().toISOString().slice(0, 10)

const form = reactive({
  payerMemberId: '',
  payeeMemberId: '',
  currency: 'USD',
  amount: 10000,
  tradeDate: today,
  settleDate: today
})

const filters = reactive({
  currency: 'USD',
  settleDate: today,
  status: 'OPEN'
})

const activeMembers = computed(() => members.value.filter((m) => m.status === 'ACTIVE'))
const memberMap = computed(() => Object.fromEntries(members.value.map((m) => [m.memberId, m.name])))

function nameOf(id) {
  return memberMap.value[id] || id
}

function tagType(status) {
  return { OPEN: 'warning', NETTED: 'primary', SETTLED: 'success', CANCELLED: 'info' }[status] || 'info'
}

async function loadMembers() {
  const { data } = await api.get('/members')
  members.value = data
}

async function load() {
  loading.value = true
  try {
    const params = {}
    if (filters.currency) params.currency = filters.currency
    if (filters.settleDate) params.settleDate = filters.settleDate
    if (filters.status) params.status = filters.status
    const { data } = await api.get('/obligations', { params })
    rows.value = data
  } finally {
    loading.value = false
  }
}

async function create() {
  saving.value = true
  try {
    await api.post('/obligations', { ...form })
    ElMessage.success('义务已录入')
    await load()
  } finally {
    saving.value = false
  }
}

async function cancelRow(row) {
  let reason
  try {
    const { value } = await ElMessageBox.prompt('取消后不可恢复，且不再参与轧差', '取消义务', {
      confirmButtonText: '确认取消',
      cancelButtonText: '返回',
      inputPlaceholder: '请输入取消原因（必填）',
      inputValidator: (v) => (v && v.trim().length > 0 ? true : '取消原因必填')
    })
    reason = value.trim()
  } catch {
    return // 用户放弃取消
  }
  cancellingId.value = row.obligationId
  try {
    await api.post(`/obligations/${row.obligationId}/cancel`, { reason })
    ElMessage.success('义务已取消')
  } catch {
    return // 失败信息已由拦截器提示（如非 OPEN 状态被明确拒绝）
  } finally {
    cancellingId.value = ''
  }
  // 若当前仅筛选 OPEN，切到全部状态，保证刚取消的笔立即以 CANCELLED 可见
  if (filters.status === 'OPEN') filters.status = ''
  await load()
}

onMounted(async () => {
  await loadMembers()
  await load()
})
</script>

<script setup lang="ts">
import { ref, computed, onMounted, watch } from 'vue';
import { Link2, RefreshCw, Eye, ScrollText, Filter, ArrowDownWideNarrow, ArrowUpNarrowWide } from 'lucide-vue-next';
import {
  ElButton, ElSelect, ElOption, ElForm, ElFormItem, ElInput, ElTable, ElTableColumn,
  ElDialog, ElMessage, ElMessageBox, ElTag, ElTooltip, ElAlert,
} from 'element-plus';
import { toolApi, inheritorApi, projectApi, associationApi } from '@/api';
import type {
  ToolDTO, InheritorDTO, ProjectDTO, AssociationDTO, AssociationHistoryDTO,
} from '@/types';

const tools = ref<ToolDTO[]>([]);
const inheritors = ref<InheritorDTO[]>([]);
const projects = ref<ProjectDTO[]>([]);
const associations = ref<AssociationDTO[]>([]);
const ledger = ref<AssociationHistoryDTO[]>([]);

const loadingLedger = ref(false);

const selectedToolId = ref<number | undefined>();
const selectedInheritorId = ref<number | undefined>();
const selectedProjectId = ref<number | undefined>();
const remark = ref('');
/** 后端拦下后给出的失败提示（对不上、已存在、已结项等），直接展示在绑定区 */
const bindErrorHint = ref('');

// 流水筛选
const filterToolId = ref<number | undefined>();
const filterInheritorId = ref<number | undefined>();
const filterProjectId = ref<number | undefined>();
const filterActionType = ref<string | undefined>();
// asc = 按发生先后（早 → 晚），刷新后默认保持先后顺序
const sortAsc = ref(true);

const viewDialogVisible = ref(false);
const currentAssociation = ref<AssociationDTO | null>(null);

const actionTypeOptions = [
  { value: 'BIND', label: '挂上' },
  { value: 'UNBIND', label: '解开' },
  { value: 'TRANSFER', label: '传承人移交' },
  { value: 'TRANSFER_INHERITOR', label: '更换传承人' },
  { value: 'TRANSFER_PROJECT', label: '更换项目' },
  { value: 'TRANSFER_BOTH', label: '传承人/项目均更换' },
  { value: 'UPDATE', label: '更新' },
];

const actionLabel = (type?: string) =>
  actionTypeOptions.find((item) => item.value === type)?.label ?? type ?? '-';

const actionTagType = (type?: string) => {
  if (type === 'BIND') return 'success';
  if (type === 'UNBIND') return 'info';
  return 'warning';
};

const formatTime = (row: any) =>
  row.actionTimeText || (row.actionTime ? row.actionTime.replace('T', ' ').slice(0, 19) : '-');

const describeInheritor = (row: any) => {
  if (row.actionType === 'BIND') return row.newInheritorName || '-';
  if (row.actionType === 'UNBIND') return row.oldInheritorName || '-';
  const from = row.oldInheritorName || '';
  const to = row.newInheritorName || '';
  return `${from} → ${to}`;
};

const describeProject = (row: any) => {
  if (row.actionType === 'BIND') return row.newProjectName || '-';
  if (row.actionType === 'UNBIND') return row.oldProjectName || '-';
  const from = row.oldProjectName || '';
  const to = row.newProjectName || '';
  return `${from} → ${to}`;
};

const filteredLedger = computed(() => {
  const rows = filterActionType.value
    ? ledger.value.filter((row) => row.actionType === filterActionType.value)
    : ledger.value;
  return sortAsc.value ? rows : [...rows].reverse();
});

// 已结项的项目档案锁定，不能再挂新的关联
const bindableProjects = computed(() => projects.value.filter((p) => p.stage !== 'COMPLETED'));

// 停档的人不能再挂进在研项目：下拉里标出并禁选，提交前再拦一道，后端兜底
const isInheritorSuspended = (inheritor?: InheritorDTO | null) => inheritor?.status === 'SUSPENDED';

const selectedInheritor = computed(() => inheritors.value.find((i) => i.id === selectedInheritorId.value));

const normalizeCraft = (value?: string | null) => (value || '').trim();

const selectedTool = computed(() => tools.value.find((t) => t.id === selectedToolId.value));
const selectedProject = computed(() => projects.value.find((p) => p.id === selectedProjectId.value));

/** 当前所选工具与项目工艺是否一路：任一未填工艺不参与比较 */
const selectionCraftMatched = computed(() => {
  const craft = normalizeCraft(selectedTool.value?.craftType);
  const category = normalizeCraft(selectedProject.value?.category);
  if (!craft || !category) return true;
  return craft.toLowerCase() === category.toLowerCase();
});

const selectionMismatchText = computed(() => {
  const craft = normalizeCraft(selectedTool.value?.craftType);
  const category = normalizeCraft(selectedProject.value?.category);
  if (!craft || !category || craft.toLowerCase() === category.toLowerCase()) return '';
  return `工艺对不上：工具工艺为「${craft}」，项目分类为「${category}」，不是一路，不能挂上`;
});

// 选择一变，上一轮的拦截提示立即清掉
watch([selectedToolId, selectedProjectId, selectedInheritorId], () => {
  bindErrorHint.value = '';
});

const isMismatchRow = (row: any) =>
  row.status === 'MISMATCH' || row.craftMatched === false;

const isSuspendedRow = (row: any) => row.inheritorSuspended === true;

const rowMismatchText = (row: any) => {
  const craft = normalizeCraft(row.toolCraftType);
  const category = normalizeCraft(row.projectCategory);
  if (craft && category && craft.toLowerCase() !== category.toLowerCase()) {
    return `工艺对不上：工具「${craft}」 vs 项目「${category}」`;
  }
  return '工艺对不上，请核对工具工艺与项目分类';
};

const loadTools = async () => {
  try {
    const response = await toolApi.list(0, 100);
    if (response.code === 200) {
      tools.value = response.data.content;
    }
  } catch (error) {
    console.error('Failed to load tools:', error);
  }
};

const loadInheritors = async () => {
  try {
    const response = await inheritorApi.list(0, 100);
    if (response.code === 200) {
      inheritors.value = response.data.content;
    }
  } catch (error) {
    console.error('Failed to load inheritors:', error);
  }
};

const loadProjects = async () => {
  try {
    const response = await projectApi.list(false);
    if (response.code === 200) {
      projects.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load projects:', error);
  }
};

const loadAssociations = async () => {
  try {
    // OPEN：仍挂着没解开的都要，正常在用和工艺对不上的都列在名单上
    const response = await associationApi.list('OPEN');
    if (response.code === 200) {
      associations.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load associations:', error);
  }
};

const loadLedger = async () => {
  loadingLedger.value = true;
  try {
    const response = await associationApi.ledger({
      toolId: filterToolId.value,
      inheritorId: filterInheritorId.value,
      projectId: filterProjectId.value,
    });
    if (response.code === 200) {
      ledger.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load association ledger:', error);
    ElMessage.error('关联流水加载失败');
  } finally {
    loadingLedger.value = false;
  }
};

const resetBindForm = () => {
  selectedToolId.value = undefined;
  selectedInheritorId.value = undefined;
  selectedProjectId.value = undefined;
  remark.value = '';
  bindErrorHint.value = '';
};

const handleBind = async () => {
  if (!selectedToolId.value || !selectedInheritorId.value || !selectedProjectId.value) {
    ElMessage.warning('请选择工具、传承人和非遗项目');
    return;
  }

  // 停档的人不能挂进在研项目：前端先拦一道，后端还会再兜底，并发以最后落成的状态为准
  if (isInheritorSuspended(selectedInheritor.value)) {
    const hint = `传承人「${selectedInheritor.value?.name}」已停档，不能再挂进在研项目`;
    bindErrorHint.value = hint;
    ElMessage.error(hint);
    return;
  }

  // 绑定页直接说出对不上：前端先拦一道，后端还会再兜底，两人同挂以后端为准
  if (!selectionCraftMatched.value) {
    const hint = selectionMismatchText.value;
    bindErrorHint.value = hint;
    ElMessage.error(hint);
    return;
  }

  try {
    const response = await associationApi.bind({
      toolId: selectedToolId.value,
      inheritorId: selectedInheritorId.value,
      projectId: selectedProjectId.value,
      remark: remark.value || undefined,
    });

    if (response.code === 200) {
      ElMessage.success('绑定成功');
      resetBindForm();
      await Promise.all([loadAssociations(), loadLedger()]);
    }
  } catch (error: any) {
    const message: string = error?.response?.data?.message || '绑定失败，该关联可能已存在';
    bindErrorHint.value = message;
    ElMessage.error(message);
  }
};

const handleDelete = async (id: number) => {
  try {
    await ElMessageBox.confirm('确认解开该三方关联？解开后仍可在下方流水中按时间查到本次操作。', '解绑确认', {
      confirmButtonText: '确认解绑',
      cancelButtonText: '取消',
      type: 'warning',
    });
  } catch {
    return;
  }

  try {
    const response = await associationApi.delete(id);
    if (response.code === 200) {
      ElMessage.success('解绑成功');
      await Promise.all([loadAssociations(), loadLedger()]);
    }
  } catch (error: any) {
    ElMessage.error(error?.response?.data?.message || '解绑失败');
  }
};

const handleView = (association: any) => {
  currentAssociation.value = association as AssociationDTO;
  viewDialogVisible.value = true;
};

const handleResetFilter = () => {
  filterToolId.value = undefined;
  filterInheritorId.value = undefined;
  filterProjectId.value = undefined;
  filterActionType.value = undefined;
  loadLedger();
};

const handleRefresh = () => {
  loadTools();
  loadInheritors();
  loadProjects();
  loadAssociations();
  loadLedger();
};

onMounted(() => {
  loadTools();
  loadInheritors();
  loadProjects();
  loadAssociations();
  loadLedger();
});
</script>

<template>
  <div class="space-y-6">
    <!-- 绑定登记 -->
    <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
      <div class="p-6 border-b border-heritage-secondary/20">
        <div class="flex items-center justify-between">
          <div>
            <h2 class="font-serif text-xl font-semibold text-heritage-primary">三方关联绑定</h2>
            <p class="text-gray-500 text-sm mt-1">工具-传承人-非遗项目三方同步绑定登记</p>
          </div>
          <ElButton @click="handleRefresh" class="flex items-center gap-2">
            <RefreshCw class="w-4 h-4" />
            刷新数据
          </ElButton>
        </div>
      </div>

      <div class="p-6">
        <ElForm :inline="true" class="bg-heritage-light rounded-xl p-6">
          <ElFormItem label="选择工具" class="w-64">
            <ElSelect v-model="selectedToolId" placeholder="请选择工具" class="w-full" filterable>
              <ElOption
                v-for="tool in tools"
                :key="tool.id"
                :label="`${tool.toolNumber} - ${tool.toolName}（${tool.craftType || '工艺未填'}）`"
                :value="tool.id"
              />
            </ElSelect>
          </ElFormItem>

          <ElFormItem label="选择传承人" class="w-64">
            <ElSelect v-model="selectedInheritorId" placeholder="请选择传承人" class="w-full" filterable>
              <ElOption
                v-for="inheritor in inheritors"
                :key="inheritor.id"
                :label="`${inheritor.name} - ${inheritor.title || '传承人'}${isInheritorSuspended(inheritor) ? '（已停档）' : ''}`"
                :value="inheritor.id"
                :disabled="isInheritorSuspended(inheritor)"
              />
            </ElSelect>
          </ElFormItem>

          <ElFormItem label="选择非遗项目" class="w-64">
            <ElSelect v-model="selectedProjectId" placeholder="请选择项目" class="w-full" filterable>
              <ElOption
                v-for="project in bindableProjects"
                :key="project.id"
                :label="`${project.name} - ${project.category || '分类未填'}`"
                :value="project.id"
              />
            </ElSelect>
          </ElFormItem>

          <ElFormItem label="备注" class="w-64">
            <ElInput v-model="remark" placeholder="绑定备注" />
          </ElFormItem>

          <ElFormItem>
            <ElButton
              type="primary"
              :disabled="!selectionCraftMatched"
              :title="!selectionCraftMatched ? selectionMismatchText : ''"
              @click="handleBind"
              class="flex items-center gap-2"
            >
              <Link2 class="w-4 h-4" />
              绑定关联
            </ElButton>
          </ElFormItem>

          <!-- 工艺对不上必须在绑定页当场说清楚，不允许提交 -->
          <div v-if="selectionMismatchText" class="w-full">
            <ElAlert
              :title="selectionMismatchText"
              type="error"
              show-icon
              :closable="false"
              class="mt-1"
            />
          </div>
          <div v-else-if="bindErrorHint" class="w-full">
            <ElAlert
              :title="bindErrorHint"
              type="error"
              show-icon
              :closable="false"
              class="mt-1"
            />
          </div>
        </ElForm>
      </div>
    </div>

    <!-- 当前关联名单：仍挂着没解开的都在；挂错工艺的会被标出，不算正常在用 -->
    <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
      <div class="p-6 border-b border-heritage-secondary/20">
        <div class="flex items-center gap-2">
          <Link2 class="w-5 h-5 text-heritage-accent" />
          <h3 class="font-serif text-lg font-semibold text-heritage-primary">当前关联名单</h3>
          <span class="ml-auto text-sm text-gray-400">仍挂着的一组人都在这里；工艺对不上、停档占用的会标出，解开前不能当成正常在用</span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <ElTable
          :data="associations"
          border
          class="w-full"
          :row-class-name="({ row }) => (isMismatchRow(row) ? 'row-craft-mismatch' : isSuspendedRow(row) ? 'row-inheritor-suspended' : '')"
        >
          <ElTableColumn prop="toolNumber" label="工具编号" width="120" />
          <ElTableColumn label="工具名称 / 工艺" min-width="180">
            <template #default="{ row }">
              <div>{{ row.toolName }}</div>
              <div class="text-xs text-gray-400">工艺：{{ row.toolCraftType || '-' }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="inheritorName" label="传承人" width="120" />
          <ElTableColumn label="非遗项目 / 分类" min-width="180">
            <template #default="{ row }">
              <div>{{ row.projectName }}</div>
              <div class="text-xs text-gray-400">分类：{{ row.projectCategory || '-' }}</div>
            </template>
          </ElTableColumn>
          <ElTableColumn prop="bindTime" label="本次挂上时间" width="170" />
          <ElTableColumn label="状态" width="170">
            <template #default="{ row }">
              <div class="flex flex-wrap items-center gap-1">
                <ElTag v-if="isMismatchRow(row)" type="danger" size="small">工艺对不上</ElTag>
                <ElTag v-if="isSuspendedRow(row)" type="warning" size="small">停档占用</ElTag>
                <ElTag v-if="!isMismatchRow(row) && !isSuspendedRow(row)" type="success" size="small">有效</ElTag>
              </div>
            </template>
          </ElTableColumn>
          <ElTableColumn label="标记说明" min-width="220">
            <template #default="{ row }">
              <div v-if="isMismatchRow(row)" class="text-red-600 text-sm">
                {{ rowMismatchText(row) }}
              </div>
              <div v-if="isSuspendedRow(row)" class="text-amber-600 text-sm">
                传承人已停档，停档占用，解开前不能当成正常在册
              </div>
              <span v-if="!isMismatchRow(row) && !isSuspendedRow(row)" class="text-gray-300 text-sm">-</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <ElTooltip content="查看详情">
                  <ElButton size="small" @click="handleView(row)">
                    <Eye class="w-4 h-4" />
                  </ElButton>
                </ElTooltip>
                <ElButton size="small" type="danger" @click="handleDelete(row.id)">
                  解绑
                </ElButton>
              </div>
            </template>
          </ElTableColumn>
          <template #empty>
            <div class="py-10 text-center text-gray-400">当前没有挂着的关联</div>
          </template>
        </ElTable>
      </div>
    </div>

    <!-- 挂解流水 -->
    <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
      <div class="p-6 border-b border-heritage-secondary/20">
        <div class="flex items-center gap-2">
          <ScrollText class="w-5 h-5 text-heritage-accent" />
          <h3 class="font-serif text-lg font-semibold text-heritage-primary">三方关联挂解流水</h3>
          <span class="text-sm text-gray-400">每一次挂上、解开都留痕，按发生时间排序，刷新后不丢失</span>
        </div>
      </div>

      <div class="p-6 border-b border-heritage-secondary/20 bg-heritage-light/50">
        <ElForm :inline="true" class="mb-0">
          <ElFormItem label="工具" class="w-56">
            <ElSelect v-model="filterToolId" placeholder="全部工具" clearable filterable class="w-full">
              <ElOption
                v-for="tool in tools"
                :key="tool.id"
                :label="`${tool.toolNumber} - ${tool.toolName}`"
                :value="tool.id"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="传承人" class="w-56">
            <ElSelect v-model="filterInheritorId" placeholder="全部传承人" clearable filterable class="w-full">
              <ElOption
                v-for="inheritor in inheritors"
                :key="inheritor.id"
                :label="inheritor.name"
                :value="inheritor.id"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem label="项目" class="w-56">
            <ElSelect v-model="filterProjectId" placeholder="全部项目" clearable filterable class="w-full">
              <ElOption
                v-for="project in projects"
                :key="project.id"
                :label="project.name"
                :value="project.id"
              />
            </ElSelect>
          </ElFormItem>
          <ElFormItem>
            <ElButton type="primary" plain class="flex items-center gap-2" @click="loadLedger">
              <Filter class="w-4 h-4" />
              筛选
            </ElButton>
            <ElButton @click="handleResetFilter">重置</ElButton>
          </ElFormItem>
        </ElForm>
      </div>

      <div class="px-6 pt-4 flex items-center gap-3">
        <ElSelect v-model="filterActionType" placeholder="全部操作" clearable class="w-40">
          <ElOption
            v-for="option in actionTypeOptions"
            :key="option.value"
            :label="option.label"
            :value="option.value"
          />
        </ElSelect>
        <ElButton text class="flex items-center gap-1" @click="sortAsc = !sortAsc">
          <component
            :is="sortAsc ? ArrowUpNarrowWide : ArrowDownWideNarrow"
            class="w-4 h-4"
          />
          {{ sortAsc ? '按时间正序（先发生在上）' : '按时间倒序（最新在上）' }}
        </ElButton>
        <span class="ml-auto text-sm text-gray-400">共 {{ filteredLedger.length }} 条</span>
      </div>

      <div class="overflow-x-auto p-6 pt-3">
        <ElTable :data="filteredLedger" border class="w-full" v-loading="loadingLedger" row-key="id">
          <ElTableColumn label="序号" type="index" width="64" align="center" />
          <ElTableColumn label="发生时间" width="170">
            <template #default="{ row }">{{ formatTime(row) }}</template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="110">
            <template #default="{ row }">
              <ElTag :type="actionTagType(row.actionType)" size="small">
                {{ actionLabel(row.actionType) }}
              </ElTag>
            </template>
          </ElTableColumn>
          <ElTableColumn label="工具" min-width="160">
            <template #default="{ row }">
              <span v-if="row.toolNumber">{{ row.toolNumber }} - {{ row.toolName }}</span>
              <span v-else class="text-gray-400">-</span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="传承人" min-width="180">
            <template #default="{ row }">{{ describeInheritor(row) }}</template>
          </ElTableColumn>
          <ElTableColumn label="非遗项目" min-width="180">
            <template #default="{ row }">{{ describeProject(row) }}</template>
          </ElTableColumn>
          <ElTableColumn prop="remark" label="备注" min-width="140">
            <template #default="{ row }">{{ row.remark || '-' }}</template>
          </ElTableColumn>
          <template #empty>
            <div class="py-10 text-center text-gray-400">暂无挂解流水</div>
          </template>
        </ElTable>
      </div>
    </div>

    <ElDialog v-model="viewDialogVisible" title="关联详情" width="600px">
      <div v-if="currentAssociation" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-600">工具编号</label>
            <p class="text-heritage-primary font-semibold">{{ currentAssociation.toolNumber }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">工具名称</label>
            <p class="text-heritage-primary font-semibold">{{ currentAssociation.toolName }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">传承人</label>
            <p class="text-heritage-accent font-semibold">{{ currentAssociation.inheritorName }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">非遗项目</label>
            <p class="text-heritage-accent font-semibold">{{ currentAssociation.projectName }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">本次挂上时间</label>
            <p>{{ currentAssociation.bindTime }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">状态</label>
            <div class="flex flex-wrap items-center gap-1">
              <ElTag v-if="isMismatchRow(currentAssociation)" type="danger" size="small">工艺对不上</ElTag>
              <ElTag v-if="isSuspendedRow(currentAssociation)" type="warning" size="small">停档占用</ElTag>
              <ElTag v-if="!isMismatchRow(currentAssociation) && !isSuspendedRow(currentAssociation)" type="success" size="small">有效</ElTag>
            </div>
          </div>
          <div v-if="isMismatchRow(currentAssociation)" class="col-span-2">
            <label class="block text-sm font-medium text-red-600 mb-1">对不上说明</label>
            <p class="text-red-600 text-sm">{{ rowMismatchText(currentAssociation) }}；解开前不能当成正常在用</p>
          </div>
          <div v-if="isSuspendedRow(currentAssociation)" class="col-span-2">
            <label class="block text-sm font-medium text-amber-600 mb-1">停档占用说明</label>
            <p class="text-amber-600 text-sm">传承人已停档，看板在册人数不计入；该关联解开前不能当成正常在册</p>
          </div>
        </div>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
/* 挂错工艺的整行标红，提示解开前不算正常在用 */
:deep(.el-table .row-craft-mismatch td) {
  background-color: #fef0f0 !important;
}
:deep(.el-table .row-craft-mismatch:hover td) {
  background-color: #fde2e2 !important;
}
/* 停档占用的整行标橙，提示解开前不能当成正常在册 */
:deep(.el-table .row-inheritor-suspended td) {
  background-color: #fdf6ec !important;
}
:deep(.el-table .row-inheritor-suspended:hover td) {
  background-color: #faecd8 !important;
}
</style>

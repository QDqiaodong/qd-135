<script setup lang="ts">
import { ref, computed, onMounted } from 'vue';
import { Link2, RefreshCw, Eye, ScrollText, Filter, ArrowDownWideNarrow, ArrowUpNarrowWide } from 'lucide-vue-next';
import {
  ElButton, ElSelect, ElOption, ElForm, ElFormItem, ElInput, ElTable, ElTableColumn,
  ElDialog, ElMessage, ElMessageBox, ElTag, ElTooltip,
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
    const response = await associationApi.list('ACTIVE');
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
};

const handleBind = async () => {
  if (!selectedToolId.value || !selectedInheritorId.value || !selectedProjectId.value) {
    ElMessage.warning('请选择工具、传承人和非遗项目');
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
    ElMessage.error(error?.response?.data?.message || '绑定失败，该关联可能已存在');
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
                :label="`${tool.toolNumber} - ${tool.toolName}`"
                :value="tool.id"
              />
            </ElSelect>
          </ElFormItem>

          <ElFormItem label="选择传承人" class="w-64">
            <ElSelect v-model="selectedInheritorId" placeholder="请选择传承人" class="w-full" filterable>
              <ElOption
                v-for="inheritor in inheritors"
                :key="inheritor.id"
                :label="`${inheritor.name} - ${inheritor.title || '传承人'}`"
                :value="inheritor.id"
              />
            </ElSelect>
          </ElFormItem>

          <ElFormItem label="选择非遗项目" class="w-64">
            <ElSelect v-model="selectedProjectId" placeholder="请选择项目" class="w-full" filterable>
              <ElOption
                v-for="project in projects"
                :key="project.id"
                :label="`${project.name} - ${project.category || ''}`"
                :value="project.id"
              />
            </ElSelect>
          </ElFormItem>

          <ElFormItem label="备注" class="w-64">
            <ElInput v-model="remark" placeholder="绑定备注" />
          </ElFormItem>

          <ElFormItem>
            <ElButton type="primary" @click="handleBind" class="flex items-center gap-2">
              <Link2 class="w-4 h-4" />
              绑定关联
            </ElButton>
          </ElFormItem>
        </ElForm>
      </div>
    </div>

    <!-- 当前有效关联 -->
    <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
      <div class="p-6 border-b border-heritage-secondary/20">
        <div class="flex items-center gap-2">
          <Link2 class="w-5 h-5 text-heritage-accent" />
          <h3 class="font-serif text-lg font-semibold text-heritage-primary">当前有效关联</h3>
          <span class="ml-auto text-sm text-gray-400">仅展示仍挂着的一组人；谁先挂上、谁后来解开请看下方流水</span>
        </div>
      </div>

      <div class="overflow-x-auto">
        <ElTable :data="associations" border class="w-full">
          <ElTableColumn prop="toolNumber" label="工具编号" width="120" />
          <ElTableColumn prop="toolName" label="工具名称" min-width="140" />
          <ElTableColumn prop="inheritorName" label="传承人" width="120" />
          <ElTableColumn prop="projectName" label="非遗项目" min-width="140" />
          <ElTableColumn prop="bindTime" label="本次挂上时间" width="170" />
          <ElTableColumn label="状态" width="90">
            <template #default>
              <ElTag type="success" size="small">有效</ElTag>
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
            <div class="py-10 text-center text-gray-400">当前没有有效关联</div>
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
            <ElTag type="success" size="small">有效</ElTag>
          </div>
        </div>
      </div>
    </ElDialog>
  </div>
</template>

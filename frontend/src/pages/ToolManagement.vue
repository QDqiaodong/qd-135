<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, Search, Edit, Trash2, Eye, AlertTriangle } from 'lucide-vue-next';
import { ElTable, ElTableColumn, ElPagination, ElButton, ElInput, ElDialog, ElTag, ElMessage } from 'element-plus';
import { toolApi } from '@/api';
import { isMaintenanceOverdue } from '@/lib/utils';
import type { ToolDTO, PageResponse } from '@/types';

const router = useRouter();
const tools = ref<ToolDTO[]>([]);
const loading = ref(false);
const keyword = ref('');
const currentPage = ref(0);
const pageSize = ref(10);
const total = ref(0);

const viewDialogVisible = ref(false);
const currentTool = ref<ToolDTO | null>(null);

const loadTools = async () => {
  loading.value = true;
  try {
    const response = await toolApi.list(currentPage.value, pageSize.value, keyword.value);
    if (response.code === 200) {
      const data = response.data as PageResponse<ToolDTO>;
      tools.value = data.content;
      total.value = data.totalElements;
    }
  } catch (error) {
    console.error('Failed to load tools:', error);
    ElMessage.error('加载工具列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 0;
  loadTools();
};

const handlePageChange = (page: number) => {
  currentPage.value = page - 1;
  loadTools();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 0;
  loadTools();
};

const handleCreate = () => {
  router.push('/tools/create');
};

const handleEdit = (id: number) => {
  router.push(`/tools/${id}`);
};

const handleDelete = async (id: number) => {
  try {
    const response = await toolApi.delete(id);
    if (response.code === 200) {
      ElMessage.success('删除成功');
      loadTools();
    }
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

const handleView = (tool: unknown) => {
  currentTool.value = tool as ToolDTO;
  viewDialogVisible.value = true;
};

// 整行轻底色提示已过保养期；未到期或未设置日期的行不加该类。
// 以持久化的到期日为基准在前端实时判断，避免列表缓存把过期标记冻结。
const toolRowClass = ({ row }: { row: ToolDTO }) =>
  isMaintenanceOverdue(row.maintenanceDueDate) ? 'tool-overdue-row' : '';

onMounted(() => {
  loadTools();
});
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
    <div class="p-6 border-b border-heritage-secondary/20">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="font-serif text-xl font-semibold text-heritage-primary">工具管理</h2>
          <p class="text-gray-500 text-sm mt-1">传统手工工具基础建档管理</p>
        </div>
        <ElButton type="primary" @click="handleCreate" class="flex items-center gap-2">
          <Plus class="w-4 h-4" />
          新增工具
        </ElButton>
      </div>
      
      <div class="mt-4 flex items-center gap-4">
        <div class="relative flex-1 max-w-md">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <ElInput 
            v-model="keyword" 
            placeholder="搜索工具编号、名称、工艺类型..." 
            class="pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <ElButton @click="handleSearch">搜索</ElButton>
      </div>
    </div>
    
    <div class="overflow-x-auto">
      <ElTable :data="tools" :loading="loading" border class="w-full" :row-class-name="toolRowClass">
        <ElTableColumn prop="toolNumber" label="工具编号" width="120" />
        <ElTableColumn prop="toolName" label="工具名称" width="150" />
        <ElTableColumn prop="craftType" label="适用工艺" width="120" />
        <ElTableColumn prop="material" label="材质" width="120" />
        <ElTableColumn prop="specification" label="规格" width="150" />
        <ElTableColumn prop="inheritorName" label="传承人" width="120" />
        <ElTableColumn prop="projectName" label="所属项目" width="120" />
        <ElTableColumn label="保养到期日" width="170">
          <template #default="{ row }">
            <div v-if="row.maintenanceDueDate" class="flex items-center gap-2">
              <span :class="isMaintenanceOverdue(row.maintenanceDueDate) ? 'text-red-600 font-semibold' : ''">
                {{ row.maintenanceDueDate }}
              </span>
              <ElTag v-if="isMaintenanceOverdue(row.maintenanceDueDate)" type="danger" size="small" class="flex items-center gap-1">
                <AlertTriangle class="w-3 h-3" />
                已过期
              </ElTag>
            </div>
            <span v-else class="text-gray-400">-</span>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createTime" label="创建时间" width="160" />
        <ElTableColumn label="操作" width="180" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <ElButton size="small" @click="handleView(row)">
                <Eye class="w-4 h-4" />
              </ElButton>
              <ElButton size="small" type="primary" @click="handleEdit(row.id)">
                <Edit class="w-4 h-4" />
              </ElButton>
              <ElButton size="small" type="danger" @click="handleDelete(row.id)">
                <Trash2 class="w-4 h-4" />
              </ElButton>
            </div>
          </template>
        </ElTableColumn>
      </ElTable>
    </div>
    
    <div class="p-4 border-t border-heritage-secondary/20 flex items-center justify-end">
      <ElPagination
        :current-page="currentPage + 1"
        :page-size="pageSize"
        :total="total"
        @current-change="handlePageChange"
        @size-change="handleSizeChange"
        layout="total, sizes, prev, pager, next, jumper"
      />
    </div>
    
    <ElDialog v-model="viewDialogVisible" title="工具详情" width="600px">
      <div v-if="currentTool" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-600">工具编号</label>
            <p class="text-heritage-primary font-semibold">{{ currentTool.toolNumber }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">工具名称</label>
            <p class="text-heritage-primary font-semibold">{{ currentTool.toolName }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">适用工艺</label>
            <p>{{ currentTool.craftType || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">材质</label>
            <p>{{ currentTool.material || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">规格</label>
            <p>{{ currentTool.specification || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">传承人</label>
            <p>{{ currentTool.inheritorName || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">所属项目</label>
            <p>{{ currentTool.projectName || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">保养到期日</label>
            <div v-if="currentTool.maintenanceDueDate" class="flex items-center gap-2">
              <p :class="isMaintenanceOverdue(currentTool.maintenanceDueDate) ? 'text-red-600 font-semibold' : ''">
                {{ currentTool.maintenanceDueDate }}
              </p>
              <ElTag v-if="isMaintenanceOverdue(currentTool.maintenanceDueDate)" type="danger" size="small">已过期</ElTag>
            </div>
            <p v-else class="text-gray-400">-</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">创建时间</label>
            <p>{{ currentTool.createTime }}</p>
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600">描述</label>
          <p>{{ currentTool.description || '-' }}</p>
        </div>
      </div>
    </ElDialog>
  </div>
</template>

<style scoped>
/* 已过保养到期日的行：浅红底 + 左侧强调条，便于一眼定位 */
:deep(.el-table tr.tool-overdue-row) {
  background-color: #fef2f2;
}
:deep(.el-table tr.tool-overdue-row > td.el-table__cell) {
  background-color: #fef2f2;
}
:deep(.el-table tr.tool-overdue-row > td:first-child .cell) {
  border-left: 3px solid #dc2626;
  padding-left: 8px;
}
</style>
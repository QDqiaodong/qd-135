<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Link2, Search, Save, RefreshCw, Eye } from 'lucide-vue-next';
import { ElButton, ElSelect, ElOption, ElForm, ElFormItem, ElInput, ElTable, ElTableColumn, ElDialog, ElMessage } from 'element-plus';
import { toolApi, inheritorApi, projectApi, associationApi } from '@/api';
import type { ToolDTO, InheritorDTO, ProjectDTO, AssociationDTO } from '@/types';

const tools = ref<ToolDTO[]>([]);
const inheritors = ref<InheritorDTO[]>([]);
const projects = ref<ProjectDTO[]>([]);
const associations = ref<AssociationDTO[]>([]);

const selectedToolId = ref<number | undefined>();
const selectedInheritorId = ref<number | undefined>();
const selectedProjectId = ref<number | undefined>();
const remark = ref('');

const viewDialogVisible = ref(false);
const currentAssociation = ref<AssociationDTO | null>(null);

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
    const response = await toolApi.list(0, 100);
    if (response.code === 200) {
      const toolList = response.data.content;
      const result: AssociationDTO[] = [];
      for (const tool of toolList) {
        if (tool.inheritorName || tool.projectName) {
          const traceResponse = await associationApi.traceByTool(tool.id);
          if (traceResponse.code === 200 && traceResponse.data.associations.length > 0) {
            result.push(...traceResponse.data.associations);
          }
        }
      }
      associations.value = result;
    }
  } catch (error) {
    console.error('Failed to load associations:', error);
  }
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
      selectedToolId.value = undefined;
      selectedInheritorId.value = undefined;
      selectedProjectId.value = undefined;
      remark.value = '';
      loadAssociations();
    }
  } catch (error) {
    ElMessage.error('绑定失败，该关联可能已存在');
  }
};

const handleUpdate = async (id: number) => {
  if (!selectedInheritorId.value || !selectedProjectId.value) {
    ElMessage.warning('请选择传承人和非遗项目');
    return;
  }

  try {
    const response = await associationApi.update(id, {
      inheritorId: selectedInheritorId.value,
      projectId: selectedProjectId.value,
      remark: remark.value || undefined,
    });

    if (response.code === 200) {
      ElMessage.success('更新成功');
      selectedToolId.value = undefined;
      selectedInheritorId.value = undefined;
      selectedProjectId.value = undefined;
      remark.value = '';
      loadAssociations();
    }
  } catch (error) {
    ElMessage.error('更新失败');
  }
};

const handleDelete = async (id: number) => {
  try {
    const response = await associationApi.delete(id);
    if (response.code === 200) {
      ElMessage.success('解绑成功');
      loadAssociations();
    }
  } catch (error) {
    ElMessage.error('解绑失败');
  }
};

const handleView = async (toolId: number) => {
  try {
    const response = await associationApi.traceByTool(toolId);
    if (response.code === 200 && response.data.associations.length > 0) {
      currentAssociation.value = response.data.associations[0];
      viewDialogVisible.value = true;
    }
  } catch (error) {
    console.error('Failed to view association:', error);
  }
};

const handleRefresh = () => {
  loadTools();
  loadInheritors();
  loadProjects();
  loadAssociations();
};

onMounted(() => {
  loadTools();
  loadInheritors();
  loadProjects();
  loadAssociations();
});
</script>

<template>
  <div class="space-y-6">
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
            <ElSelect v-model="selectedToolId" placeholder="请选择工具" class="w-full">
              <ElOption 
                v-for="tool in tools" 
                :key="tool.id" 
                :label="`${tool.toolNumber} - ${tool.toolName}`" 
                :value="tool.id"
              />
            </ElSelect>
          </ElFormItem>
          
          <ElFormItem label="选择传承人" class="w-64">
            <ElSelect v-model="selectedInheritorId" placeholder="请选择传承人" class="w-full">
              <ElOption 
                v-for="inheritor in inheritors" 
                :key="inheritor.id" 
                :label="`${inheritor.name} - ${inheritor.title || '传承人'}`" 
                :value="inheritor.id"
              />
            </ElSelect>
          </ElFormItem>
          
          <ElFormItem label="选择非遗项目" class="w-64">
            <ElSelect v-model="selectedProjectId" placeholder="请选择项目" class="w-full">
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
    
    <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
      <div class="p-6 border-b border-heritage-secondary/20">
        <div class="flex items-center gap-2">
          <Search class="w-5 h-5 text-heritage-accent" />
          <h3 class="font-serif text-lg font-semibold text-heritage-primary">已绑定关联列表</h3>
        </div>
      </div>
      
      <div class="overflow-x-auto">
        <ElTable :data="associations" border class="w-full">
          <ElTableColumn prop="toolNumber" label="工具编号" width="120" />
          <ElTableColumn prop="toolName" label="工具名称" width="150" />
          <ElTableColumn prop="inheritorName" label="传承人" width="120" />
          <ElTableColumn prop="projectName" label="非遗项目" width="150" />
          <ElTableColumn prop="bindTime" label="绑定时间" width="160" />
          <ElTableColumn prop="status" label="状态" width="100">
            <template #default="{ row }">
              <span :class="row.status === 'ACTIVE' ? 'text-green-600' : 'text-gray-500'">
                {{ row.status === 'ACTIVE' ? '有效' : '已解绑' }}
              </span>
            </template>
          </ElTableColumn>
          <ElTableColumn label="操作" width="150" fixed="right">
            <template #default="{ row }">
              <div class="flex items-center gap-2">
                <ElButton size="small" @click="handleView(row.toolId)">
                  <Eye class="w-4 h-4" />
                </ElButton>
                <ElButton size="small" type="danger" @click="handleDelete(row.id)">
                  <Save class="w-4 h-4" />
                  解绑
                </ElButton>
              </div>
            </template>
          </ElTableColumn>
        </ElTable>
      </div>
      
      <div v-if="associations.length === 0" class="p-12 text-center">
        <Link2 class="w-16 h-16 text-gray-300 mx-auto mb-4" />
        <p class="text-gray-500">暂无关联记录，请在上方进行三方绑定</p>
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
            <label class="block text-sm font-medium text-gray-600">绑定时间</label>
            <p>{{ currentAssociation.bindTime }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">状态</label>
            <span :class="currentAssociation.status === 'ACTIVE' ? 'text-green-600' : 'text-gray-500'">
              {{ currentAssociation.status === 'ACTIVE' ? '有效' : '已解绑' }}
            </span>
          </div>
        </div>
      </div>
    </ElDialog>
  </div>
</template>
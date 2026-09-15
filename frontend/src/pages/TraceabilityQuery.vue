<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Search, ArrowRight, Wrench, Users, FolderTree, Link2 } from 'lucide-vue-next';
import { ElButton, ElSelect, ElOption, ElTabs, ElTabPane, ElCard, ElTable, ElTableColumn, ElMessage } from 'element-plus';
import { toolApi, inheritorApi, projectApi, associationApi } from '@/api';
import type { ToolDTO, InheritorDTO, ProjectDTO, TraceabilityResult, AssociationDTO } from '@/types';

const tools = ref<ToolDTO[]>([]);
const inheritors = ref<InheritorDTO[]>([]);
const projects = ref<ProjectDTO[]>([]);

const queryType = ref<'project' | 'inheritor' | 'tool'>('project');
const selectedProjectId = ref<number | undefined>();
const selectedInheritorId = ref<number | undefined>();
const selectedToolId = ref<number | undefined>();

const traceResult = ref<TraceabilityResult | null>(null);
const loading = ref(false);

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

const handleTrace = async () => {
  loading.value = true;
  traceResult.value = null;

  try {
    let response;
    if (queryType.value === 'project' && selectedProjectId.value) {
      response = await associationApi.traceByProject(selectedProjectId.value);
    } else if (queryType.value === 'inheritor' && selectedInheritorId.value) {
      response = await associationApi.traceByInheritor(selectedInheritorId.value);
    } else if (queryType.value === 'tool' && selectedToolId.value) {
      response = await associationApi.traceByTool(selectedToolId.value);
    } else {
      ElMessage.warning('请选择查询对象');
      loading.value = false;
      return;
    }

    if (response.code === 200) {
      traceResult.value = response.data;
    }
  } catch (error) {
    ElMessage.error('溯源查询失败');
  } finally {
    loading.value = false;
  }
};

onMounted(() => {
  loadTools();
  loadInheritors();
  loadProjects();
});
</script>

<template>
  <div class="space-y-6">
    <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
      <div class="p-6 border-b border-heritage-secondary/20">
        <div class="flex items-center gap-2 mb-4">
          <Search class="w-5 h-5 text-heritage-accent" />
          <h2 class="font-serif text-xl font-semibold text-heritage-primary">三方关联溯源查询</h2>
        </div>
        <p class="text-gray-500 text-sm">支持按非遗项目、传承人、工具三个维度进行双向三方关联溯源查询</p>
      </div>
      
      <div class="p-6">
        <ElTabs v-model="queryType" type="card">
          <ElTabPane label="按非遗项目溯源" name="project">
            <div class="flex items-center gap-4 mt-4">
              <ElSelect v-model="selectedProjectId" placeholder="请选择非遗项目" class="w-80">
                <ElOption 
                  v-for="project in projects" 
                  :key="project.id" 
                  :label="`${project.name} - ${project.category || ''}`" 
                  :value="project.id"
                />
              </ElSelect>
              <ElButton type="primary" @click="handleTrace" :loading="loading" class="flex items-center gap-2">
                <ArrowRight class="w-4 h-4" />
                查询溯源
              </ElButton>
            </div>
          </ElTabPane>
          
          <ElTabPane label="按传承人溯源" name="inheritor">
            <div class="flex items-center gap-4 mt-4">
              <ElSelect v-model="selectedInheritorId" placeholder="请选择传承人" class="w-80">
                <ElOption
                  v-for="inheritor in inheritors"
                  :key="inheritor.id"
                  :label="`${inheritor.name} - ${inheritor.title || '传承人'}${inheritor.status === 'SUSPENDED' ? '（已停档）' : ''}`"
                  :value="inheritor.id"
                />
              </ElSelect>
              <ElButton type="primary" @click="handleTrace" :loading="loading" class="flex items-center gap-2">
                <ArrowRight class="w-4 h-4" />
                查询溯源
              </ElButton>
            </div>
          </ElTabPane>
          
          <ElTabPane label="按工具溯源" name="tool">
            <div class="flex items-center gap-4 mt-4">
              <ElSelect v-model="selectedToolId" placeholder="请选择工具" class="w-80">
                <ElOption 
                  v-for="tool in tools" 
                  :key="tool.id" 
                  :label="`${tool.toolNumber} - ${tool.toolName}`" 
                  :value="tool.id"
                />
              </ElSelect>
              <ElButton type="primary" @click="handleTrace" :loading="loading" class="flex items-center gap-2">
                <ArrowRight class="w-4 h-4" />
                查询溯源
              </ElButton>
            </div>
          </ElTabPane>
        </ElTabs>
      </div>
    </div>
    
    <div v-if="traceResult" class="space-y-6">
      <div class="bg-gradient-to-r from-heritage-primary to-heritage-secondary rounded-xl p-6 text-white">
        <div class="flex items-center gap-4">
          <div class="w-16 h-16 rounded-xl bg-white/20 flex items-center justify-center">
            <Link2 class="w-8 h-8" />
          </div>
          <div>
            <div class="text-sm opacity-80 mb-1">溯源主体</div>
            <div class="text-2xl font-serif font-bold">
              {{ traceResult.mainEntity.name }}
              <span class="text-sm font-normal opacity-80 ml-2">[{{ traceResult.mainEntity.type === 'PROJECT' ? '非遗项目' : traceResult.mainEntity.type === 'INHERITOR' ? '传承人' : '工具' }}]</span>
            </div>
          </div>
        </div>
      </div>
      
      <div class="grid grid-cols-1 md:grid-cols-3 gap-6">
        <ElCard class="border-2 border-heritage-secondary/30">
          <div class="flex items-center gap-2 mb-4">
            <Wrench class="w-5 h-5 text-blue-500" />
            <h3 class="font-serif font-semibold text-gray-800">关联工具</h3>
            <span class="ml-auto bg-blue-100 text-blue-600 text-xs px-2 py-1 rounded-full">
              {{ traceResult.associatedTools.length }}件
            </span>
          </div>
          <div class="space-y-2">
            <div 
              v-for="tool in traceResult.associatedTools" 
              :key="tool.id"
              class="p-3 bg-blue-50 rounded-lg"
            >
              <div class="font-medium text-gray-800">{{ tool.toolNumber }} - {{ tool.toolName }}</div>
              <div class="text-sm text-gray-500">工艺：{{ tool.craftType }} | 材质：{{ tool.material }}</div>
            </div>
            <div v-if="traceResult.associatedTools.length === 0" class="text-center text-gray-400 py-4">
              暂无关联工具
            </div>
          </div>
        </ElCard>
        
        <ElCard class="border-2 border-heritage-secondary/30">
          <div class="flex items-center gap-2 mb-4">
            <Users class="w-5 h-5 text-green-500" />
            <h3 class="font-serif font-semibold text-gray-800">关联传承人</h3>
            <span class="ml-auto bg-green-100 text-green-600 text-xs px-2 py-1 rounded-full">
              {{ traceResult.associatedInheritors.length }}人
            </span>
          </div>
          <div class="space-y-2">
            <div
              v-for="inheritor in traceResult.associatedInheritors"
              :key="inheritor.id"
              class="p-3 bg-green-50 rounded-lg"
            >
              <div class="font-medium text-gray-800">
                {{ inheritor.name }} - {{ inheritor.title || '传承人' }}
                <span v-if="inheritor.status === 'SUSPENDED'" class="ml-1 text-xs text-red-500">[停档]</span>
              </div>
              <div class="text-sm text-gray-500">专长：{{ inheritor.specialty }}</div>
            </div>
            <div v-if="traceResult.associatedInheritors.length === 0" class="text-center text-gray-400 py-4">
              暂无关联传承人
            </div>
          </div>
        </ElCard>
        
        <ElCard class="border-2 border-heritage-secondary/30">
          <div class="flex items-center gap-2 mb-4">
            <FolderTree class="w-5 h-5 text-purple-500" />
            <h3 class="font-serif font-semibold text-gray-800">关联非遗项目</h3>
            <span class="ml-auto bg-purple-100 text-purple-600 text-xs px-2 py-1 rounded-full">
              {{ traceResult.associatedProjects.length }}项
            </span>
          </div>
          <div class="space-y-2">
            <div 
              v-for="project in traceResult.associatedProjects" 
              :key="project.id"
              class="p-3 bg-purple-50 rounded-lg"
            >
              <div class="font-medium text-gray-800">{{ project.name }}</div>
              <div class="text-sm text-gray-500">分类：{{ project.category }}</div>
            </div>
            <div v-if="traceResult.associatedProjects.length === 0" class="text-center text-gray-400 py-4">
              暂无关联项目
            </div>
          </div>
        </ElCard>
      </div>
      
      <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
        <div class="p-6 border-b border-heritage-secondary/20">
          <h3 class="font-serif text-lg font-semibold text-heritage-primary">三方关联明细</h3>
        </div>
        
        <div class="overflow-x-auto">
          <ElTable :data="traceResult.associations" border class="w-full">
            <ElTableColumn prop="toolNumber" label="工具编号" width="120" />
            <ElTableColumn prop="toolName" label="工具名称" width="150" />
            <ElTableColumn prop="inheritorName" label="传承人" width="120" />
            <ElTableColumn prop="projectName" label="非遗项目" width="150" />
            <ElTableColumn prop="bindTime" label="绑定时间" width="160" />
            <ElTableColumn prop="status" label="状态" width="140">
              <template #default="{ row }">
                <div class="flex flex-wrap items-center gap-1">
                  <span :class="row.status === 'ACTIVE' ? 'text-green-600' : 'text-gray-500'">
                    {{ row.status === 'ACTIVE' ? '有效' : '已解绑' }}
                  </span>
                  <span v-if="row.inheritorSuspended" class="text-amber-600">停档占用</span>
                </div>
              </template>
            </ElTableColumn>
          </ElTable>
        </div>
      </div>
    </div>
    
    <div v-if="!traceResult" class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
      <div class="p-12 text-center">
        <Search class="w-20 h-20 text-gray-300 mx-auto mb-4" />
        <h3 class="text-lg font-medium text-gray-600 mb-2">选择查询维度开始溯源</h3>
        <p class="text-gray-400">支持按非遗项目、传承人、工具三个维度进行双向三方关联溯源查询</p>
      </div>
    </div>
  </div>
</template>
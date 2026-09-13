<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, Edit, Trash2, ChevronRight, FolderTree } from 'lucide-vue-next';
import { ElButton, ElMessage, ElCard } from 'element-plus';
import { projectApi } from '@/api';
import type { ProjectDTO } from '@/types';

const router = useRouter();
const projects = ref<ProjectDTO[]>([]);
const loading = ref(false);

const loadProjects = async () => {
  loading.value = true;
  try {
    const response = await projectApi.list(true);
    if (response.code === 200) {
      projects.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load projects:', error);
    ElMessage.error('加载项目列表失败');
  } finally {
    loading.value = false;
  }
};

const handleCreate = () => {
  router.push('/projects/create');
};

const handleEdit = (id: number) => {
  router.push(`/projects/${id}`);
};

const handleDelete = async (id: number) => {
  try {
    const response = await projectApi.delete(id);
    if (response.code === 200) {
      ElMessage.success('删除成功');
      loadProjects();
    }
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

const renderTree = (items: ProjectDTO[], level: number = 0) => {
  return items.map((item) => ({
    ...item,
    level,
    hasChildren: item.children && item.children.length > 0,
  }));
};

onMounted(() => {
  loadProjects();
});
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
    <div class="p-6 border-b border-heritage-secondary/20 flex items-center justify-between">
      <div>
        <h2 class="font-serif text-xl font-semibold text-heritage-primary">项目管理</h2>
        <p class="text-gray-500 text-sm mt-1">非遗项目分组树形管理</p>
      </div>
      <ElButton type="primary" @click="handleCreate" class="flex items-center gap-2">
        <Plus class="w-4 h-4" />
        新增项目
      </ElButton>
    </div>
    
    <div class="p-6">
      <div class="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-3 gap-4">
        <template v-for="project in projects" :key="project.id">
          <ElCard class="border-2 border-heritage-secondary/30 hover:border-heritage-secondary transition-colors">
            <div class="flex items-start justify-between mb-3">
              <div class="flex items-center gap-2">
                <FolderTree class="w-5 h-5 text-heritage-accent" />
                <span class="font-serif font-semibold text-heritage-primary">{{ project.name }}</span>
              </div>
              <div class="flex items-center gap-1">
                <ElButton size="small" @click="handleEdit(project.id)">
                  <Edit class="w-4 h-4" />
                </ElButton>
                <ElButton size="small" type="danger" @click="handleDelete(project.id)">
                  <Trash2 class="w-4 h-4" />
                </ElButton>
              </div>
            </div>
            
            <div v-if="project.category" class="text-sm text-gray-500 mb-2">
              分类：{{ project.category }}
            </div>
            
            <div v-if="project.description" class="text-sm text-gray-600 mb-3 line-clamp-2">
              {{ project.description }}
            </div>
            
            <div v-if="project.children && project.children.length > 0" class="border-t border-gray-100 pt-3">
              <div class="text-xs font-medium text-gray-500 mb-2">子项目</div>
              <div class="space-y-2">
                <div 
                  v-for="child in project.children" 
                  :key="child.id"
                  class="flex items-center justify-between p-2 bg-heritage-light rounded-lg"
                >
                  <span class="text-sm text-heritage-primary">{{ child.name }}</span>
                  <div class="flex items-center gap-1">
                    <ElButton size="small" @click="handleEdit(child.id)">
                      <Edit class="w-3 h-3" />
                    </ElButton>
                    <ElButton size="small" type="danger" @click="handleDelete(child.id)">
                      <Trash2 class="w-3 h-3" />
                    </ElButton>
                  </div>
                </div>
              </div>
            </div>
          </ElCard>
        </template>
        
        <div v-if="projects.length === 0" class="col-span-full text-center py-12">
          <FolderTree class="w-16 h-16 text-gray-300 mx-auto mb-4" />
          <p class="text-gray-500">暂无项目，请点击上方按钮新增</p>
        </div>
      </div>
    </div>
  </div>
</template>
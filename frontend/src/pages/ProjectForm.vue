<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ArrowLeft, Save } from 'lucide-vue-next';
import { ElForm, ElFormItem, ElInput, ElButton, ElSelect, ElMessage } from 'element-plus';
import { projectApi } from '@/api';
import type { ProjectCreateRequest, ProjectUpdateRequest } from '@/api/types';
import type { ProjectDTO } from '@/types';

const router = useRouter();
const route = useRoute();
const isEdit = ref(false);
const projectId = ref<number | null>(null);

const form = ref({
  name: '',
  category: '',
  description: '',
  parentId: null as number | null,
});

const formRules = {
  name: [{ required: true, message: '项目名称不能为空', trigger: 'blur' }],
};

const parentProjects = ref<ProjectDTO[]>([]);

const handleSubmit = async () => {
  if (isEdit.value && projectId.value) {
    const updateRequest: ProjectUpdateRequest = {
      name: form.value.name,
      category: form.value.category,
      description: form.value.description,
      parentId: form.value.parentId,
    };
    
    try {
      const response = await projectApi.update(projectId.value, updateRequest);
      if (response.code === 200) {
        ElMessage.success('更新成功');
        router.push('/projects');
      }
    } catch (error) {
      ElMessage.error('更新失败');
    }
  } else {
    const createRequest: ProjectCreateRequest = {
      name: form.value.name,
      category: form.value.category,
      description: form.value.description,
      parentId: form.value.parentId,
    };
    
    try {
      const response = await projectApi.create(createRequest);
      if (response.code === 200) {
        ElMessage.success('创建成功');
        router.push('/projects');
      }
    } catch (error) {
      ElMessage.error('创建失败');
    }
  }
};

const handleBack = () => {
  router.push('/projects');
};

const loadParentProjects = async () => {
  try {
    const response = await projectApi.list();
    if (response.code === 200) {
      parentProjects.value = response.data.filter(p => {
        if (isEdit.value && projectId.value) {
          return p.id !== projectId.value;
        }
        return true;
      });
    }
  } catch (error) {
    console.error('Failed to load parent projects:', error);
  }
};

onMounted(async () => {
  await loadParentProjects();
  
  const id = route.params.id;
  if (id) {
    isEdit.value = true;
    projectId.value = Number(id);
    
    try {
      const response = await projectApi.get(projectId.value);
      if (response.code === 200) {
        const data = response.data;
        form.value = {
          name: data.name,
          category: data.category,
          description: data.description,
          parentId: data.parentId || null,
        };
      }
    } catch (error) {
      console.error('Failed to load project:', error);
    }
  }
});
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
    <div class="p-6 border-b border-heritage-secondary/20 flex items-center justify-between">
      <div class="flex items-center gap-4">
        <ElButton @click="handleBack" class="text-gray-500 hover:text-heritage-primary">
          <ArrowLeft class="w-5 h-5" />
        </ElButton>
        <div>
          <h2 class="font-serif text-xl font-semibold text-heritage-primary">
            {{ isEdit ? '编辑项目' : '新增项目' }}
          </h2>
          <p class="text-gray-500 text-sm mt-1">非遗项目信息录入</p>
        </div>
      </div>
      <ElButton type="primary" @click="handleSubmit" class="flex items-center gap-2">
        <Save class="w-4 h-4" />
        {{ isEdit ? '保存修改' : '创建项目' }}
      </ElButton>
    </div>
    
    <div class="p-6">
      <ElForm :model="form" :rules="formRules" label-width="120px" class="max-w-2xl">
        <ElFormItem label="项目名称" prop="name">
          <ElInput v-model="form.name" placeholder="请输入项目名称" />
        </ElFormItem>
        
        <ElFormItem label="分类">
          <ElInput v-model="form.category" placeholder="请输入分类（如：木雕、陶瓷、打铁）" />
        </ElFormItem>
        
        <ElFormItem label="父级项目">
          <ElSelect v-model="form.parentId" placeholder="请选择父级项目（可选）">
            <ElSelectOption 
              v-for="project in parentProjects" 
              :key="project.id" 
              :label="project.name" 
              :value="project.id" 
            />
          </ElSelect>
        </ElFormItem>
        
        <ElFormItem label="描述">
          <ElInput v-model="form.description" type="textarea" :rows="4" placeholder="请输入项目描述" />
        </ElFormItem>
      </ElForm>
    </div>
  </div>
</template>
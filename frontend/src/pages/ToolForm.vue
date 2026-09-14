<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ArrowLeft, Save } from 'lucide-vue-next';
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage } from 'element-plus';
import { toolApi } from '@/api';
import { getApiErrorMessage } from '@/lib/utils';
import type { ToolCreateRequest, ToolUpdateRequest } from '@/api/types';

const router = useRouter();
const route = useRoute();
const isEdit = ref(false);
const toolId = ref<number | null>(null);
const submitting = ref(false);

const form = ref({
  toolNumber: '',
  toolName: '',
  craftType: '',
  material: '',
  specification: '',
  description: '',
});

const formRules = {
  toolNumber: [{ required: true, message: '工具编号不能为空', trigger: 'blur' }],
  toolName: [{ required: true, message: '工具名称不能为空', trigger: 'blur' }],
};

const handleSubmit = async () => {
  if (submitting.value) {
    return;
  }
  submitting.value = true;
  try {
    if (isEdit.value && toolId.value) {
      const updateRequest: ToolUpdateRequest = {
        toolName: form.value.toolName,
        craftType: form.value.craftType,
        material: form.value.material,
        specification: form.value.specification,
        description: form.value.description,
      };

      try {
        const response = await toolApi.update(toolId.value, updateRequest);
        if (response.code === 200) {
          ElMessage.success('更新成功');
          router.push('/tools');
        }
      } catch (error) {
        // 并发建档时后端会返回“工具编号已存在”等业务提示
        ElMessage.error(getApiErrorMessage(error, '更新失败'));
      }
    } else {
      const createRequest: ToolCreateRequest = {
        toolNumber: form.value.toolNumber,
        toolName: form.value.toolName,
        craftType: form.value.craftType,
        material: form.value.material,
        specification: form.value.specification,
        description: form.value.description,
      };

      try {
        const response = await toolApi.create(createRequest);
        if (response.code === 200) {
          ElMessage.success('创建成功');
          router.push('/tools');
        }
      } catch (error) {
        // 并发建档时后端会返回“工具编号已存在”等业务提示
        ElMessage.error(getApiErrorMessage(error, '创建失败'));
      }
    }
  } finally {
    submitting.value = false;
  }
};

const handleBack = () => {
  router.push('/tools');
};

onMounted(async () => {
  const id = route.params.id;
  if (id) {
    isEdit.value = true;
    toolId.value = Number(id);
    
    try {
      const response = await toolApi.get(toolId.value);
      if (response.code === 200) {
        const data = response.data;
        form.value = {
          toolNumber: data.toolNumber,
          toolName: data.toolName,
          craftType: data.craftType,
          material: data.material,
          specification: data.specification,
          description: data.description,
        };
      }
    } catch (error) {
      console.error('Failed to load tool:', error);
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
            {{ isEdit ? '编辑工具' : '新增工具' }}
          </h2>
          <p class="text-gray-500 text-sm mt-1">传统手工工具基础信息录入</p>
        </div>
      </div>
      <ElButton type="primary" :loading="submitting" @click="handleSubmit" class="flex items-center gap-2">
        <Save class="w-4 h-4" />
        {{ isEdit ? '保存修改' : '创建工具' }}
      </ElButton>
    </div>
    
    <div class="p-6">
      <ElForm :model="form" :rules="formRules" label-width="120px" class="max-w-2xl">
        <ElFormItem label="工具编号" prop="toolNumber">
          <ElInput v-model="form.toolNumber" placeholder="请输入工具编号" :disabled="isEdit" />
        </ElFormItem>
        
        <ElFormItem label="工具名称" prop="toolName">
          <ElInput v-model="form.toolName" placeholder="请输入工具名称" />
        </ElFormItem>
        
        <ElFormItem label="适用工艺">
          <ElInput v-model="form.craftType" placeholder="请输入适用工艺（如：木雕、陶瓷、打铁）" />
        </ElFormItem>
        
        <ElFormItem label="材质">
          <ElInput v-model="form.material" placeholder="请输入材质（如：红木、陶瓷、钢材）" />
        </ElFormItem>
        
        <ElFormItem label="规格">
          <ElInput v-model="form.specification" placeholder="请输入规格尺寸" />
        </ElFormItem>
        
        <ElFormItem label="描述">
          <ElInput v-model="form.description" type="textarea" :rows="4" placeholder="请输入工具描述" />
        </ElFormItem>
      </ElForm>
    </div>
  </div>
</template>
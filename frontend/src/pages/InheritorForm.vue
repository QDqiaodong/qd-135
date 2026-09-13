<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ArrowLeft, Save } from 'lucide-vue-next';
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage } from 'element-plus';
import { inheritorApi } from '@/api';
import type { InheritorCreateRequest, InheritorUpdateRequest } from '@/api/types';

const router = useRouter();
const route = useRoute();
const isEdit = ref(false);
const inheritorId = ref<number | null>(null);

const form = ref({
  name: '',
  title: '',
  specialty: '',
  contact: '',
});

const formRules = {
  name: [{ required: true, message: '传承人姓名不能为空', trigger: 'blur' }],
};

const handleSubmit = async () => {
  if (isEdit.value && inheritorId.value) {
    const updateRequest: InheritorUpdateRequest = {
      name: form.value.name,
      title: form.value.title,
      specialty: form.value.specialty,
      contact: form.value.contact,
    };
    
    try {
      const response = await inheritorApi.update(inheritorId.value, updateRequest);
      if (response.code === 200) {
        ElMessage.success('更新成功');
        router.push('/inheritors');
      }
    } catch (error) {
      ElMessage.error('更新失败');
    }
  } else {
    const createRequest: InheritorCreateRequest = {
      name: form.value.name,
      title: form.value.title,
      specialty: form.value.specialty,
      contact: form.value.contact,
    };
    
    try {
      const response = await inheritorApi.create(createRequest);
      if (response.code === 200) {
        ElMessage.success('创建成功');
        router.push('/inheritors');
      }
    } catch (error) {
      ElMessage.error('创建失败');
    }
  }
};

const handleBack = () => {
  router.push('/inheritors');
};

onMounted(async () => {
  const id = route.params.id;
  if (id) {
    isEdit.value = true;
    inheritorId.value = Number(id);
    
    try {
      const response = await inheritorApi.get(inheritorId.value);
      if (response.code === 200) {
        const data = response.data;
        form.value = {
          name: data.name,
          title: data.title,
          specialty: data.specialty,
          contact: data.contact,
        };
      }
    } catch (error) {
      console.error('Failed to load inheritor:', error);
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
            {{ isEdit ? '编辑传承人' : '新增传承人' }}
          </h2>
          <p class="text-gray-500 text-sm mt-1">非遗技艺传承人信息录入</p>
        </div>
      </div>
      <ElButton type="primary" @click="handleSubmit" class="flex items-center gap-2">
        <Save class="w-4 h-4" />
        {{ isEdit ? '保存修改' : '创建传承人' }}
      </ElButton>
    </div>
    
    <div class="p-6">
      <ElForm :model="form" :rules="formRules" label-width="120px" class="max-w-2xl">
        <ElFormItem label="姓名" prop="name">
          <ElInput v-model="form.name" placeholder="请输入传承人姓名" />
        </ElFormItem>
        
        <ElFormItem label="称号">
          <ElInput v-model="form.title" placeholder="请输入称号（如：国家级传承人）" />
        </ElFormItem>
        
        <ElFormItem label="专长">
          <ElInput v-model="form.specialty" placeholder="请输入专长技艺（如：木雕、陶瓷烧制）" />
        </ElFormItem>
        
        <ElFormItem label="联系方式">
          <ElInput v-model="form.contact" placeholder="请输入联系方式" />
        </ElFormItem>
      </ElForm>
    </div>
  </div>
</template>
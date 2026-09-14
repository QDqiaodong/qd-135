<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { ArrowLeft, Save, Upload, Download, FileText, X } from 'lucide-vue-next';
import { ElForm, ElFormItem, ElInput, ElButton, ElMessage, ElMessageBox } from 'element-plus';
import { inheritorApi } from '@/api';
import type { InheritorCreateRequest, InheritorUpdateRequest } from '@/api/types';
import type { InheritorDTO } from '@/types';
import { validateCertificate, formatFileSize, getApiErrorMessage } from '@/lib/utils';

const router = useRouter();
const route = useRoute();
const isEdit = ref(false);
const inheritorId = ref<number | null>(null);
const pageLoading = ref(false);
const uploading = ref(false);

const form = ref({
  name: '',
  title: '',
  specialty: '',
  contact: '',
});

const formRules = {
  name: [{ required: true, message: '传承人姓名不能为空', trigger: 'blur' }],
};

// 已在档的资格证明（来自服务端，刷新后仍以此为准）
const existingCertificate = ref<{
  name: string;
  contentType: string;
  size: number;
  uploadTime: string;
} | null>(null);
// 建档时暂存、待随档案一起提交的资格证明
const pendingFile = ref<File | null>(null);
const fileInput = ref<HTMLInputElement | null>(null);

const certificateUrl = () =>
  inheritorId.value ? inheritorApi.certificateUrl(inheritorId.value) : '';

const resetFileInput = () => {
  if (fileInput.value) {
    fileInput.value.value = '';
  }
};

const handleFileSelected = (event: Event) => {
  const target = event.target as HTMLInputElement;
  const file = target.files?.[0];
  if (!file) {
    return;
  }

  const result = validateCertificate(file);
  if (!result.valid) {
    // 类型或大小不合规：提示并拦住，不暂存、不上传
    ElMessage.error(result.message || '资格证明文件不符合要求');
    resetFileInput();
    return;
  }

  if (isEdit.value) {
    if (existingCertificate.value) {
      // 已有附件：只允许替换，先经建档人确认
      ElMessageBox.confirm(
        `当前档案已挂有资格证明《${existingCertificate.value.name}》，再次上传将替换原文件且不能同时保留两份，是否继续？`,
        '替换资格证明',
        { confirmButtonText: '替换', cancelButtonText: '取消', type: 'warning' }
      )
        .then(() => doUpload(file))
        .catch(() => {})
        .finally(() => resetFileInput());
    } else {
      doUpload(file).finally(() => resetFileInput());
    }
  } else {
    // 建档模式：档案尚未创建，暂存本地，保存档案时一并上传
    pendingFile.value = file;
    resetFileInput();
  }
};

const doUpload = async (file: File) => {
  if (!inheritorId.value) return;
  uploading.value = true;
  try {
    const response = await inheritorApi.uploadCertificate(inheritorId.value, file);
    if (response.code === 200 && response.data) {
      applyCertificate(response.data);
      ElMessage.success(existingCertificate.value ? '资格证明已替换' : '资格证明上传成功');
    }
  } catch (error) {
    // 后端校验拦截（类型/大小）与前端规则保持一致
    ElMessage.error(getApiErrorMessage(error, '资格证明上传失败'));
  } finally {
    uploading.value = false;
  }
};

const applyCertificate = (data: InheritorDTO) => {
  if (data.certificateName) {
    existingCertificate.value = {
      name: data.certificateName,
      contentType: data.certificateContentType || '',
      size: data.certificateSize || 0,
      uploadTime: data.certificateUploadTime || '',
    };
  } else {
    existingCertificate.value = null;
  }
};

const removePendingFile = () => {
  pendingFile.value = null;
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
      ElMessage.error(getApiErrorMessage(error, '更新失败'));
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
        const created = response.data;
        // 档案创建成功后补传暂存的资格证明
        if (pendingFile.value && created?.id) {
          try {
            await inheritorApi.uploadCertificate(created.id, pendingFile.value);
            ElMessage.success('创建成功，资格证明已上传');
            router.push('/inheritors');
          } catch (uploadError) {
            ElMessage.error(getApiErrorMessage(uploadError, '档案已创建，但资格证明上传失败，请在编辑页重新上传'));
            router.push(`/inheritors/${created.id}`);
          }
        } else {
          ElMessage.success('创建成功');
          router.push('/inheritors');
        }
      }
    } catch (error) {
      ElMessage.error(getApiErrorMessage(error, '创建失败'));
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
    pageLoading.value = true;

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
        // 以服务端档案为准回显附件状态，刷新后保持一致
        applyCertificate(data);
      }
    } catch (error) {
      console.error('Failed to load inheritor:', error);
      ElMessage.error('加载传承人信息失败');
    } finally {
      pageLoading.value = false;
    }
  }
});
</script>

<template>
  <div v-loading="pageLoading" class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
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

        <ElFormItem label="资格证明">
          <div class="w-full space-y-3">
            <!-- 已在档附件：只提供替换与下载，不会出现第二份 -->
            <div
              v-if="existingCertificate"
              class="flex items-center justify-between gap-3 w-full rounded-lg border border-heritage-secondary/40 bg-heritage-secondary/5 px-4 py-3"
            >
              <div class="flex items-center gap-3 min-w-0">
                <FileText class="w-5 h-5 text-heritage-primary shrink-0" />
                <div class="min-w-0">
                  <p class="text-sm font-medium text-heritage-primary truncate">
                    {{ existingCertificate.name }}
                  </p>
                  <p class="text-xs text-gray-500 mt-0.5">
                    <span>{{ formatFileSize(existingCertificate.size) }}</span>
                    <span v-if="existingCertificate.uploadTime" class="ml-2">
                      上传于 {{ existingCertificate.uploadTime }}
                    </span>
                  </p>
                </div>
              </div>
              <div class="flex items-center gap-2 shrink-0">
                <a :href="certificateUrl()" target="_blank" rel="noopener">
                  <ElButton size="small">
                    <Download class="w-4 h-4 mr-1" />
                    查看/下载
                  </ElButton>
                </a>
                <ElButton size="small" type="primary" :loading="uploading" @click="fileInput?.click()">
                  <Upload class="w-4 h-4 mr-1" />
                  替换
                </ElButton>
              </div>
            </div>

            <!-- 建档模式下暂存待提交的附件 -->
            <div
              v-else-if="pendingFile"
              class="flex items-center justify-between gap-3 w-full rounded-lg border border-heritage-secondary/40 bg-heritage-secondary/5 px-4 py-3"
            >
              <div class="flex items-center gap-3 min-w-0">
                <FileText class="w-5 h-5 text-heritage-primary shrink-0" />
                <div class="min-w-0">
                  <p class="text-sm font-medium text-heritage-primary truncate">{{ pendingFile.name }}</p>
                  <p class="text-xs text-gray-500 mt-0.5">
                    {{ formatFileSize(pendingFile.size) }} · 创建档案时自动上传
                  </p>
                </div>
              </div>
              <ElButton size="small" text type="danger" @click="removePendingFile">
                <X class="w-4 h-4" />
              </ElButton>
            </div>

            <div v-else>
              <ElButton :loading="uploading" @click="fileInput?.click()">
                <Upload class="w-4 h-4 mr-1" />
                上传资格证明
              </ElButton>
            </div>

            <p class="text-xs text-gray-400">
              支持 PDF、JPG、JPEG、PNG 格式，单个文件不超过 10MB；已有附件时上传将直接替换原文件。
            </p>

            <input
              ref="fileInput"
              type="file"
              accept=".pdf,.jpg,.jpeg,.png,application/pdf,image/jpeg,image/png"
              class="hidden"
              @change="handleFileSelected"
            />
          </div>
        </ElFormItem>
      </ElForm>
    </div>
  </div>
</template>

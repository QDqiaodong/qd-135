<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, Search, Edit, Trash2, Eye } from 'lucide-vue-next';
import { ElTable, ElTableColumn, ElPagination, ElButton, ElInput, ElDialog, ElMessage } from 'element-plus';
import { inheritorApi } from '@/api';
import type { InheritorDTO, PageResponse } from '@/types';

const router = useRouter();
const inheritors = ref<InheritorDTO[]>([]);
const loading = ref(false);
const keyword = ref('');
const currentPage = ref(0);
const pageSize = ref(10);
const total = ref(0);

const viewDialogVisible = ref(false);
const currentInheritor = ref<InheritorDTO | null>(null);

const loadInheritors = async () => {
  loading.value = true;
  try {
    const response = await inheritorApi.list(currentPage.value, pageSize.value, keyword.value);
    if (response.code === 200) {
      const data = response.data as PageResponse<InheritorDTO>;
      inheritors.value = data.content;
      total.value = data.totalElements;
    }
  } catch (error) {
    console.error('Failed to load inheritors:', error);
    ElMessage.error('加载传承人列表失败');
  } finally {
    loading.value = false;
  }
};

const handleSearch = () => {
  currentPage.value = 0;
  loadInheritors();
};

const handlePageChange = (page: number) => {
  currentPage.value = page - 1;
  loadInheritors();
};

const handleSizeChange = (size: number) => {
  pageSize.value = size;
  currentPage.value = 0;
  loadInheritors();
};

const handleCreate = () => {
  router.push('/inheritors/create');
};

const handleEdit = (id: number) => {
  router.push(`/inheritors/${id}`);
};

const handleDelete = async (id: number) => {
  try {
    const response = await inheritorApi.delete(id);
    if (response.code === 200) {
      ElMessage.success('删除成功');
      loadInheritors();
    }
  } catch (error) {
    ElMessage.error('删除失败');
  }
};

const handleView = (inheritor: unknown) => {
  currentInheritor.value = inheritor as InheritorDTO;
  viewDialogVisible.value = true;
};

onMounted(() => {
  loadInheritors();
});
</script>

<template>
  <div class="bg-white rounded-xl shadow-sm border border-heritage-secondary/20 overflow-hidden">
    <div class="p-6 border-b border-heritage-secondary/20">
      <div class="flex items-center justify-between">
        <div>
          <h2 class="font-serif text-xl font-semibold text-heritage-primary">传承人管理</h2>
          <p class="text-gray-500 text-sm mt-1">非遗技艺传承人信息管理</p>
        </div>
        <ElButton type="primary" @click="handleCreate" class="flex items-center gap-2">
          <Plus class="w-4 h-4" />
          新增传承人
        </ElButton>
      </div>
      
      <div class="mt-4 flex items-center gap-4">
        <div class="relative flex-1 max-w-md">
          <Search class="absolute left-3 top-1/2 -translate-y-1/2 w-4 h-4 text-gray-400" />
          <ElInput 
            v-model="keyword" 
            placeholder="搜索传承人姓名、称号、专长..." 
            class="pl-10"
            @keyup.enter="handleSearch"
          />
        </div>
        <ElButton @click="handleSearch">搜索</ElButton>
      </div>
    </div>
    
    <div class="overflow-x-auto">
      <ElTable :data="inheritors" :loading="loading" border class="w-full">
        <ElTableColumn prop="name" label="姓名" width="100" />
        <ElTableColumn prop="title" label="称号" width="120" />
        <ElTableColumn prop="specialty" label="专长" width="150" />
        <ElTableColumn prop="contact" label="联系方式" width="150" />
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
    
    <ElDialog v-model="viewDialogVisible" title="传承人详情" width="500px">
      <div v-if="currentInheritor" class="space-y-4">
        <div class="grid grid-cols-2 gap-4">
          <div>
            <label class="block text-sm font-medium text-gray-600">姓名</label>
            <p class="text-heritage-primary font-semibold">{{ currentInheritor.name }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">称号</label>
            <p>{{ currentInheritor.title || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">专长</label>
            <p>{{ currentInheritor.specialty || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">联系方式</label>
            <p>{{ currentInheritor.contact || '-' }}</p>
          </div>
          <div>
            <label class="block text-sm font-medium text-gray-600">创建时间</label>
            <p>{{ currentInheritor.createTime }}</p>
          </div>
        </div>
      </div>
    </ElDialog>
  </div>
</template>
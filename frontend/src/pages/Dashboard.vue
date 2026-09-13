<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { Wrench, Users, FolderTree, Link2, TrendingUp } from 'lucide-vue-next';
import { ElCard, ElStatistic, ElRow, ElCol } from 'element-plus';
import { dashboardApi } from '@/api';
import type { DashboardStats } from '@/types';

const stats = ref<DashboardStats>({
  toolCount: 0,
  inheritorCount: 0,
  projectCount: 0,
  associationCount: 0,
});

const loading = ref(true);

onMounted(async () => {
  try {
    const response = await dashboardApi.stats();
    if (response.code === 200) {
      stats.value = response.data;
    }
  } catch (error) {
    console.error('Failed to load stats:', error);
  } finally {
    loading.value = false;
  }
});

const statCards = [
  { 
    title: '工具总数', 
    value: () => stats.value.toolCount, 
    icon: Wrench, 
    color: 'bg-blue-500',
    suffix: '件'
  },
  { 
    title: '传承人总数', 
    value: () => stats.value.inheritorCount, 
    icon: Users, 
    color: 'bg-green-500',
    suffix: '人'
  },
  { 
    title: '非遗项目', 
    value: () => stats.value.projectCount, 
    icon: FolderTree, 
    color: 'bg-purple-500',
    suffix: '项'
  },
  { 
    title: '关联记录', 
    value: () => stats.value.associationCount, 
    icon: Link2, 
    color: 'bg-orange-500',
    suffix: '条'
  },
];
</script>

<template>
  <div class="space-y-6">
    <div class="bg-white rounded-xl p-6 shadow-sm border border-heritage-secondary/20">
      <div class="flex items-center gap-3 mb-6">
        <div class="w-12 h-12 rounded-xl bg-gradient-to-br from-heritage-primary to-heritage-secondary flex items-center justify-center">
          <TrendingUp class="w-6 h-6 text-white" />
        </div>
        <div>
          <h2 class="font-serif text-xl font-semibold text-heritage-primary">非遗手作工坊溯源系统</h2>
          <p class="text-gray-500 text-sm">传统制器工具与传承人项目绑定管理平台</p>
        </div>
      </div>
      
      <p class="text-gray-600 leading-relaxed">
        本系统实现工具-传承人-非遗项目三方关联管理与溯源查询，打破传统双绑定局限，建立完整的三方实体关联体系，便于非遗文化传承与保护。
      </p>
    </div>
    
    <ElRow :gutter="20">
      <ElCol :span="6" v-for="(card, index) in statCards" :key="index">
        <ElCard class="h-full border-0 shadow-lg rounded-xl overflow-hidden">
          <div class="flex items-center gap-4">
            <div :class="[card.color, 'w-14 h-14 rounded-xl flex items-center justify-center']">
              <component :is="card.icon" class="w-7 h-7 text-white" />
            </div>
            <div>
              <ElStatistic 
                :title="card.title" 
                :value="card.value()"
                :suffix="card.suffix"
                title-class="text-gray-500 text-sm font-medium"
                value-class="font-serif text-2xl font-bold text-heritage-primary"
              />
            </div>
          </div>
        </ElCard>
      </ElCol>
    </ElRow>
    
    <div class="grid grid-cols-1 md:grid-cols-2 gap-6">
      <div class="bg-white rounded-xl p-6 shadow-sm border border-heritage-secondary/20">
        <h3 class="font-serif text-lg font-semibold text-heritage-primary mb-4">核心功能</h3>
        <ul class="space-y-3">
          <li class="flex items-center gap-3 text-gray-600">
            <span class="w-8 h-8 rounded-full bg-heritage-secondary/20 flex items-center justify-center text-heritage-secondary font-bold">1</span>
            <span>传统手工工具基础建档：编号、适用工艺、材质规格</span>
          </li>
          <li class="flex items-center gap-3 text-gray-600">
            <span class="w-8 h-8 rounded-full bg-heritage-secondary/20 flex items-center justify-center text-heritage-secondary font-bold">2</span>
            <span>传承人、非遗项目三方同步绑定登记</span>
          </li>
          <li class="flex items-center gap-3 text-gray-600">
            <span class="w-8 h-8 rounded-full bg-heritage-secondary/20 flex items-center justify-center text-heritage-secondary font-bold">3</span>
            <span>传承人/非遗项目变更，同步更新全部关联关系</span>
          </li>
          <li class="flex items-center gap-3 text-gray-600">
            <span class="w-8 h-8 rounded-full bg-heritage-secondary/20 flex items-center justify-center text-heritage-secondary font-bold">4</span>
            <span>按非遗项目/传承人双向三方关联溯源查询</span>
          </li>
        </ul>
      </div>
      
      <div class="bg-white rounded-xl p-6 shadow-sm border border-heritage-secondary/20">
        <h3 class="font-serif text-lg font-semibold text-heritage-primary mb-4">非遗品类</h3>
        <div class="grid grid-cols-2 gap-3">
          <div class="p-4 bg-heritage-light rounded-lg border border-heritage-secondary/20">
            <div class="text-2xl font-bold text-heritage-accent mb-1">木雕</div>
            <div class="text-sm text-gray-500">传统木雕工艺工具</div>
          </div>
          <div class="p-4 bg-heritage-light rounded-lg border border-heritage-secondary/20">
            <div class="text-2xl font-bold text-heritage-accent mb-1">陶瓷</div>
            <div class="text-sm text-gray-500">传统陶瓷制器工具</div>
          </div>
          <div class="p-4 bg-heritage-light rounded-lg border border-heritage-secondary/20">
            <div class="text-2xl font-bold text-heritage-accent mb-1">打铁</div>
            <div class="text-sm text-gray-500">传统锻造工艺工具</div>
          </div>
          <div class="p-4 bg-heritage-light rounded-lg border border-heritage-secondary/20">
            <div class="text-2xl font-bold text-heritage-accent mb-1">其他</div>
            <div class="text-sm text-gray-500">其他非遗工艺工具</div>
          </div>
        </div>
      </div>
    </div>
  </div>
</template>
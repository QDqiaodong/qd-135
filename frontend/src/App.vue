<script setup lang="ts">
import { ref, computed } from 'vue';
import { useRouter, useRoute } from 'vue-router';
import { LayoutDashboard, Wrench, Users, FolderTree, Link2, GitBranch, Menu, X } from 'lucide-vue-next';

const router = useRouter();
const route = useRoute();
const collapsed = ref(false);

const menuItems = [
  { path: '/', name: '首页', icon: LayoutDashboard },
  { path: '/tools', name: '工具管理', icon: Wrench },
  { path: '/inheritors', name: '传承人管理', icon: Users },
  { path: '/projects', name: '项目管理', icon: FolderTree },
  { path: '/binding', name: '关联绑定', icon: Link2 },
  { path: '/traceability', name: '溯源查询', icon: GitBranch },
];

const activeMenu = computed(() => route.path);

const navigateTo = (path: string) => {
  router.push(path);
};
</script>

<template>
  <div class="flex h-screen bg-heritage-light">
    <aside 
      class="bg-heritage-primary text-white transition-all duration-300 flex flex-col"
      :class="collapsed ? 'w-16' : 'w-56'"
    >
      <div class="p-4 border-b border-heritage-secondary/30">
        <div class="flex items-center gap-3">
          <div class="w-10 h-10 rounded-lg bg-heritage-secondary flex items-center justify-center">
            <span class="text-heritage-dark font-bold text-xl">传</span>
          </div>
          <span v-if="!collapsed" class="font-serif text-lg font-semibold">非遗溯源系统</span>
        </div>
      </div>
      
      <nav class="flex-1 p-3 space-y-2">
        <button
          v-for="item in menuItems"
          :key="item.path"
          @click="navigateTo(item.path)"
          class="w-full flex items-center gap-3 px-3 py-2.5 rounded-lg transition-all duration-200"
          :class="activeMenu === item.path 
            ? 'bg-heritage-secondary text-heritage-dark' 
            : 'hover:bg-heritage-secondary/20 text-heritage-secondary'"
        >
          <component :is="item.icon" class="w-5 h-5 flex-shrink-0" />
          <span v-if="!collapsed" class="font-medium">{{ item.name }}</span>
        </button>
      </nav>
      
      <div class="p-3 border-t border-heritage-secondary/30">
        <button 
          @click="collapsed = !collapsed"
          class="w-full flex items-center justify-center p-2 rounded-lg hover:bg-heritage-secondary/20 transition-colors"
        >
          <component :is="collapsed ? Menu : X" class="w-5 h-5 text-heritage-secondary" />
        </button>
      </div>
    </aside>
    
    <main class="flex-1 overflow-auto">
      <header class="bg-white shadow-sm px-6 py-4">
        <h1 class="font-serif text-2xl font-semibold text-heritage-primary">
          {{ menuItems.find(item => item.path === activeMenu)?.name || '非遗溯源系统' }}
        </h1>
      </header>
      
      <div class="p-6">
        <router-view />
      </div>
    </main>
  </div>
</template>
<script setup lang="ts">
import { ref, onMounted } from 'vue';
import { useRouter } from 'vue-router';
import { Plus, Edit, Trash2, FolderTree, GitMerge, CornerRightUp, Send, CircleCheck, Lock } from 'lucide-vue-next';
import { ElTable, ElTableColumn, ElButton, ElDialog, ElSelect, ElMessage, ElMessageBox, ElTag } from 'element-plus';
import { projectApi } from '@/api';
import type { ProjectDTO } from '@/types';
import { getApiErrorMessage } from '@/lib/utils';

const router = useRouter();
const projects = ref<ProjectDTO[]>([]);
const allProjects = ref<ProjectDTO[]>([]);
const loading = ref(false);

const loadProjects = async () => {
  loading.value = true;
  try {
    const [treeResponse, flatResponse] = await Promise.all([
      projectApi.list(true),
      projectApi.list(false),
    ]);
    if (treeResponse.code === 200) {
      projects.value = treeResponse.data;
    }
    if (flatResponse.code === 200) {
      allProjects.value = flatResponse.data;
    }
  } catch (error) {
    console.error('Failed to load projects:', error);
    ElMessage.error('加载项目列表失败');
  } finally {
    loading.value = false;
  }
};

const projectNameMap = () => new Map(allProjects.value.map(p => [p.id, p.name]));

/** 收集项目自身及其所有后代的 id，挂载时这些项目均不可选作父级 */
const collectDescendantIds = (root: ProjectDTO): Set<number> => {
  const ids = new Set<number>([root.id]);
  const walk = (node: ProjectDTO) => {
    for (const child of node.children ?? []) {
      ids.add(child.id);
      walk(child);
    }
  };
  walk(root);
  return ids;
};

const findProjectInTree = (nodes: ProjectDTO[], id: number): ProjectDTO | null => {
  for (const node of nodes) {
    if (node.id === id) return node;
    const found = findProjectInTree(node.children ?? [], id);
    if (found) return found;
  }
  return null;
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
    ElMessage.error(getApiErrorMessage(error, '删除失败'));
  }
};

// ---- 挂载父级 ----
const bindDialogVisible = ref(false);
const bindSubmitting = ref(false);
const currentProject = ref<ProjectDTO | null>(null);
const selectedParentId = ref<number | null>(null);

const parentOptions = ref<ProjectDTO[]>([]);

const handleOpenBind = (row: unknown) => {
  const project = row as ProjectDTO;
  currentProject.value = project;
  selectedParentId.value = project.parentId ?? null;
  // 在最新树中定位该项目，排除自身及全部后代，避免形成自引用或环路
  const latest = findProjectInTree(projects.value, project.id);
  const excluded = latest ? collectDescendantIds(latest) : new Set<number>([project.id]);
  parentOptions.value = allProjects.value.filter(p => !excluded.has(p.id));
  bindDialogVisible.value = true;
};

const handleBindSubmit = async () => {
  if (!currentProject.value) return;
  bindSubmitting.value = true;
  try {
    const response = await projectApi.bindParent(currentProject.value.id, {
      parentId: selectedParentId.value,
    });
    if (response.code === 200) {
      ElMessage.success(selectedParentId.value ? '挂载成功' : '已解除父级挂载');
      bindDialogVisible.value = false;
      await loadProjects();
    }
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '挂载失败'));
  } finally {
    bindSubmitting.value = false;
  }
};

const parentName = (parentId?: number) => {
  if (parentId === undefined || parentId === null) return '';
  return projectNameMap().get(parentId) ?? '';
};

// ---- 阶段推进：在研 → 送审 → 结项，只能顺着走 ----
const stageLabel = (stage?: string) => {
  if (stage === 'UNDER_REVIEW') return '送审';
  if (stage === 'COMPLETED') return '结项';
  return '在研';
};

const stageTagType = (stage?: string): 'success' | 'warning' | 'info' => {
  if (stage === 'COMPLETED') return 'success';
  if (stage === 'UNDER_REVIEW') return 'warning';
  return 'info';
};

const isCompleted = (row: { stage?: string }) => row.stage === 'COMPLETED';

const handleSubmitReview = async (row: unknown) => {
  const project = row as ProjectDTO;
  try {
    const response = await projectApi.updateStage(project.id, { stage: 'UNDER_REVIEW' });
    if (response.code === 200) {
      ElMessage.success('已送审');
      await loadProjects();
    }
  } catch (error) {
    ElMessage.error(getApiErrorMessage(error, '送审失败'));
  }
};

const handleComplete = async (row: unknown) => {
  const project = row as ProjectDTO;
  try {
    await ElMessageBox.confirm(
      '结项前请确认该项目名下的关联已全部解开；结项后项目将锁定，不能再挂新的关联，也不能再改项目档案。',
      '结项确认',
      { confirmButtonText: '确认结项', cancelButtonText: '取消', type: 'warning' },
    );
  } catch {
    return;
  }
  try {
    const response = await projectApi.updateStage(project.id, { stage: 'COMPLETED' });
    if (response.code === 200) {
      ElMessage.success('已结项，项目档案已锁定');
      await loadProjects();
    }
  } catch (error) {
    // 与他人同时点结项时，后端会提示该项目已结项
    ElMessage.error(getApiErrorMessage(error, '结项失败'));
    await loadProjects();
  }
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
        <p class="text-gray-500 text-sm mt-1">项目阶段按 在研 → 送审 → 结项 逐级推进，结项后档案锁定</p>
      </div>
      <ElButton type="primary" @click="handleCreate" class="flex items-center gap-2">
        <Plus class="w-4 h-4" />
        新增项目
      </ElButton>
    </div>

    <div class="p-6">
      <ElTable
        :data="projects"
        v-loading="loading"
        row-key="id"
        border
        default-expand-all
        :tree-props="{ children: 'children' }"
      >
        <ElTableColumn label="项目名称" min-width="260">
          <template #default="{ row }">
            <div class="flex items-center gap-2">
              <FolderTree class="w-4 h-4 text-heritage-accent shrink-0" />
              <span class="font-serif font-semibold text-heritage-primary">{{ row.name }}</span>
            </div>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="category" label="分类" width="140">
          <template #default="{ row }">{{ row.category || '-' }}</template>
        </ElTableColumn>
        <ElTableColumn label="父级项目" width="180">
          <template #default="{ row }">
            <span v-if="parentName(row.parentId)" class="text-gray-600">{{ parentName(row.parentId) }}</span>
            <span v-else class="text-gray-400">顶级项目</span>
          </template>
        </ElTableColumn>
        <ElTableColumn label="阶段" width="90">
          <template #default="{ row }">
            <ElTag :type="stageTagType(row.stage)" size="small">{{ stageLabel(row.stage) }}</ElTag>
          </template>
        </ElTableColumn>
        <ElTableColumn prop="createTime" label="创建时间" width="170" />
        <ElTableColumn label="操作" width="330" fixed="right">
          <template #default="{ row }">
            <div class="flex items-center gap-1">
              <ElButton
                v-if="row.stage === 'IN_PROGRESS'"
                size="small"
                type="warning"
                plain
                @click="handleSubmitReview(row)"
              >
                <Send class="w-4 h-4 mr-1" />
                送审
              </ElButton>
              <ElButton
                v-else-if="row.stage === 'UNDER_REVIEW'"
                size="small"
                type="success"
                plain
                @click="handleComplete(row)"
              >
                <CircleCheck class="w-4 h-4 mr-1" />
                结项
              </ElButton>
              <span v-else class="text-gray-400 text-xs flex items-center gap-1 px-1">
                <Lock class="w-3.5 h-3.5" />
                已锁定
              </span>
              <ElButton size="small" type="primary" plain :disabled="isCompleted(row)" @click="handleOpenBind(row)">
                <GitMerge class="w-4 h-4 mr-1" />
                {{ row.parentId ? '调整父级' : '挂载父级' }}
              </ElButton>
              <ElButton size="small" :disabled="isCompleted(row)" @click="handleEdit(row.id)">
                <Edit class="w-4 h-4" />
              </ElButton>
              <ElButton size="small" type="danger" :disabled="isCompleted(row)" @click="handleDelete(row.id)">
                <Trash2 class="w-4 h-4" />
              </ElButton>
            </div>
          </template>
        </ElTableColumn>
        <template #empty>
          <div class="py-12 text-center">
            <FolderTree class="w-16 h-16 text-gray-300 mx-auto mb-4" />
            <p class="text-gray-500">暂无项目，请点击上方按钮新增</p>
          </div>
        </template>
      </ElTable>
    </div>

    <ElDialog v-model="bindDialogVisible" title="挂载父级项目" width="480px">
      <div v-if="currentProject" class="space-y-4">
        <div class="rounded-lg bg-heritage-secondary/5 border border-heritage-secondary/30 px-4 py-3">
          <div class="text-xs text-gray-500 mb-1">子项目</div>
          <div class="flex items-center gap-2 font-serif font-semibold text-heritage-primary">
            <CornerRightUp class="w-4 h-4 text-heritage-accent" />
            {{ currentProject.name }}
          </div>
        </div>
        <div>
          <label class="block text-sm font-medium text-gray-600 mb-2">选择父级项目</label>
          <ElSelect
            v-model="selectedParentId"
            placeholder="留空则为顶级项目（解除挂载）"
            clearable
            class="w-full"
          >
            <ElSelectOption
              v-for="project in parentOptions"
              :key="project.id"
              :label="project.name"
              :value="project.id"
            />
          </ElSelect>
          <p class="text-xs text-gray-400 mt-2">
            不可选择自身或其下级项目作为父级，否则会形成循环层级。
          </p>
        </div>
      </div>
      <template #footer>
        <ElButton @click="bindDialogVisible = false">取消</ElButton>
        <ElButton type="primary" :loading="bindSubmitting" @click="handleBindSubmit">
          确认挂载
        </ElButton>
      </template>
    </ElDialog>
  </div>
</template>

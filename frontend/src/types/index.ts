export interface ToolDTO {
  id: number;
  toolNumber: string;
  toolName: string;
  craftType: string;
  material: string;
  specification: string;
  description: string;
  /** 保养到期日 yyyy-MM-dd，可能为空；是否过期由前端按本地当天比较得出 */
  maintenanceDueDate?: string | null;
  inheritorName?: string;
  projectName?: string;
  createTime: string;
  updateTime: string;
}

export interface InheritorDTO {
  id: number;
  name: string;
  title: string;
  specialty: string;
  contact: string;
  certificateName?: string | null;
  certificateContentType?: string | null;
  certificateSize?: number | null;
  certificateUploadTime?: string | null;
  createTime: string;
  updateTime: string;
}

export interface ProjectDTO {
  id: number;
  name: string;
  category: string;
  description: string;
  parentId?: number;
  /** 项目阶段：IN_PROGRESS 在研 / UNDER_REVIEW 送审 / COMPLETED 结项 */
  stage: string;
  children?: ProjectDTO[];
  createTime: string;
  updateTime: string;
}

export interface AssociationDTO {
  id: number;
  toolId: number;
  toolNumber: string;
  toolName: string;
  /** 工具的适用工艺，用于说明对不上 */
  toolCraftType?: string | null;
  inheritorId: number;
  inheritorName: string;
  projectId: number;
  projectName: string;
  /** 项目的分类，用于说明对不上 */
  projectCategory?: string | null;
  bindTime: string;
  /** 工具工艺与项目分类是否一路：false 即挂错 */
  craftMatched?: boolean;
  /** ACTIVE 正常在用 / MISMATCH 工艺对不上 / DELETED 已解开 */
  status: string;
}

export interface AssociationHistoryDTO {
  id: number;
  associationId: number;
  toolId?: number;
  toolNumber?: string;
  toolName?: string;
  actionType: string;
  oldInheritorId?: number;
  oldInheritorName?: string;
  newInheritorId?: number;
  newInheritorName?: string;
  oldProjectId?: number;
  oldProjectName?: string;
  newProjectId?: number;
  newProjectName?: string;
  remark?: string;
  actionTime?: string;
  actionTimeText?: string;
}

export interface TraceabilityResult {
  mainEntity: { id: number; name: string; type: string };
  associatedTools: ToolDTO[];
  associatedInheritors: InheritorDTO[];
  associatedProjects: ProjectDTO[];
  associations: AssociationDTO[];
}

export interface DashboardStats {
  toolCount: number;
  inheritorCount: number;
  projectCount: number;
  associationCount: number;
}

export interface ApiResponse<T> {
  code: number;
  message: string;
  data: T;
}

export interface PageResponse<T> {
  content: T[];
  totalElements: number;
  totalPages: number;
  size: number;
  number: number;
}
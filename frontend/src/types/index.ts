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
  children?: ProjectDTO[];
  createTime: string;
  updateTime: string;
}

export interface AssociationDTO {
  id: number;
  toolId: number;
  toolNumber: string;
  toolName: string;
  inheritorId: number;
  inheritorName: string;
  projectId: number;
  projectName: string;
  bindTime: string;
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
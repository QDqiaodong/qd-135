export interface ToolCreateRequest {
  toolNumber: string;
  toolName: string;
  craftType?: string;
  material?: string;
  specification?: string;
  description?: string;
  maintenanceDueDate?: string | null;
}

export interface ToolUpdateRequest {
  toolName?: string;
  craftType?: string;
  material?: string;
  specification?: string;
  description?: string;
  maintenanceDueDate?: string | null;
}

export interface InheritorCreateRequest {
  name: string;
  title?: string;
  specialty?: string;
  contact?: string;
}

export interface InheritorUpdateRequest {
  name?: string;
  title?: string;
  specialty?: string;
  contact?: string;
}

export interface ProjectCreateRequest {
  name: string;
  category?: string;
  description?: string;
  parentId?: number | null;
}

export interface ProjectUpdateRequest {
  name?: string;
  category?: string;
  description?: string;
  parentId?: number | null;
}

export interface ProjectBindParentRequest {
  parentId?: number | null;
}

export interface ProjectStageUpdateRequest {
  stage: string;
}

export interface AssociationBindRequest {
  toolId: number;
  inheritorId: number;
  projectId: number;
  remark?: string;
}

export interface AssociationUpdateRequest {
  inheritorId?: number;
  projectId?: number;
  remark?: string;
}
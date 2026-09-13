import axios from 'axios';
import type { ApiResponse, PageResponse, ToolDTO, InheritorDTO, ProjectDTO, AssociationDTO, TraceabilityResult, DashboardStats } from '@/types';
import type { ToolCreateRequest, ToolUpdateRequest, InheritorCreateRequest, InheritorUpdateRequest, ProjectCreateRequest, ProjectUpdateRequest, AssociationBindRequest, AssociationUpdateRequest } from '@/api/types';

const request = axios.create({
  baseURL: '/api',
  timeout: 10000,
});

request.interceptors.response.use(
  (response) => response.data,
  (error) => {
    console.error('API Error:', error);
    return Promise.reject(error);
  }
);

export const toolApi = {
  list(page: number = 0, size: number = 10, keyword?: string): Promise<ApiResponse<PageResponse<ToolDTO>>> {
    return request.get('/tools', { params: { page, size, keyword } });
  },
  get(id: number): Promise<ApiResponse<ToolDTO>> {
    return request.get(`/tools/${id}`);
  },
  create(data: ToolCreateRequest): Promise<ApiResponse<ToolDTO>> {
    return request.post('/tools', data);
  },
  update(id: number, data: ToolUpdateRequest): Promise<ApiResponse<ToolDTO>> {
    return request.put(`/tools/${id}`, data);
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/tools/${id}`);
  },
};

export const inheritorApi = {
  list(page: number = 0, size: number = 10, keyword?: string): Promise<ApiResponse<PageResponse<InheritorDTO>>> {
    return request.get('/inheritors', { params: { page, size, keyword } });
  },
  get(id: number): Promise<ApiResponse<InheritorDTO>> {
    return request.get(`/inheritors/${id}`);
  },
  create(data: InheritorCreateRequest): Promise<ApiResponse<InheritorDTO>> {
    return request.post('/inheritors', data);
  },
  update(id: number, data: InheritorUpdateRequest): Promise<ApiResponse<InheritorDTO>> {
    return request.put(`/inheritors/${id}`, data);
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/inheritors/${id}`);
  },
};

export const projectApi = {
  list(tree?: boolean): Promise<ApiResponse<ProjectDTO[]>> {
    return request.get('/projects', { params: { tree } });
  },
  get(id: number): Promise<ApiResponse<ProjectDTO>> {
    return request.get(`/projects/${id}`);
  },
  create(data: ProjectCreateRequest): Promise<ApiResponse<ProjectDTO>> {
    return request.post('/projects', data);
  },
  update(id: number, data: ProjectUpdateRequest): Promise<ApiResponse<ProjectDTO>> {
    return request.put(`/projects/${id}`, data);
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/projects/${id}`);
  },
};

export const associationApi = {
  bind(data: AssociationBindRequest): Promise<ApiResponse<AssociationDTO>> {
    return request.post('/associations', data);
  },
  update(id: number, data: AssociationUpdateRequest): Promise<ApiResponse<AssociationDTO>> {
    return request.put(`/associations/${id}`, data);
  },
  delete(id: number): Promise<ApiResponse<void>> {
    return request.delete(`/associations/${id}`);
  },
  traceByProject(projectId: number): Promise<ApiResponse<TraceabilityResult>> {
    return request.get(`/associations/trace/project/${projectId}`);
  },
  traceByInheritor(inheritorId: number): Promise<ApiResponse<TraceabilityResult>> {
    return request.get(`/associations/trace/inheritor/${inheritorId}`);
  },
  traceByTool(toolId: number): Promise<ApiResponse<TraceabilityResult>> {
    return request.get(`/associations/trace/tool/${toolId}`);
  },
};

export const dashboardApi = {
  stats(): Promise<ApiResponse<DashboardStats>> {
    return request.get('/dashboard/stats');
  },
};

export default request;
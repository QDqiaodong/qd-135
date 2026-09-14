import { clsx, type ClassValue } from "clsx"
import { twMerge } from "tailwind-merge"
import axios from "axios"

export function cn(...inputs: ClassValue[]) {
  return twMerge(clsx(inputs))
}

/** 工坊允许上传的资格证明类型，与后端 InheritorService 保持一致 */
export const ALLOWED_CERTIFICATE_EXTENSIONS = ['pdf', 'jpg', 'jpeg', 'png'] as const;
export const MAX_CERTIFICATE_SIZE = 10 * 1024 * 1024; // 10MB

export interface CertificateValidation {
  valid: boolean;
  message?: string;
}

/** 校验资格证明文件：类型不在允许范围或超出大小上限时返回拦截提示 */
export function validateCertificate(file: File): CertificateValidation {
  const dotIndex = file.name.lastIndexOf('.');
  const extension = dotIndex >= 0 ? file.name.slice(dotIndex + 1).toLowerCase() : '';
  if (!ALLOWED_CERTIFICATE_EXTENSIONS.includes(extension as typeof ALLOWED_CERTIFICATE_EXTENSIONS[number])) {
    return {
      valid: false,
      message: '资格证明格式不支持，仅允许上传 PDF、JPG、JPEG、PNG 格式',
    };
  }
  if (file.size > MAX_CERTIFICATE_SIZE) {
    return {
      valid: false,
      message: '资格证明文件超出大小上限（10MB），请压缩或更换后再上传',
    };
  }
  return { valid: true };
}

export function formatFileSize(size?: number | null): string {
  if (size === null || size === undefined) return '';
  if (size < 1024) return `${size} B`;
  if (size < 1024 * 1024) return `${(size / 1024).toFixed(1)} KB`;
  return `${(size / (1024 * 1024)).toFixed(2)} MB`;
}

/** 从接口异常中提取后端返回的提示信息 */
export function getApiErrorMessage(error: unknown, fallback: string): string {
  if (axios.isAxiosError(error)) {
    const message = error.response?.data?.message;
    if (typeof message === 'string' && message) {
      return message;
    }
  }
  return fallback;
}

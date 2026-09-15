package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.request.InheritorCreateRequest;
import com.example.heritagetrace.dto.request.InheritorStatusUpdateRequest;
import com.example.heritagetrace.dto.request.InheritorUpdateRequest;
import com.example.heritagetrace.dto.response.InheritorDTO;
import com.example.heritagetrace.entity.Inheritor;
import com.example.heritagetrace.repository.InheritorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.concurrent.TimeUnit;

@Service
public class InheritorService {
    /** 工坊允许上传的资格证明类型 */
    public static final Set<String> ALLOWED_CERTIFICATE_EXTENSIONS = Set.of("pdf", "jpg", "jpeg", "png");
    public static final Set<String> ALLOWED_CERTIFICATE_CONTENT_TYPES = Set.of(
            "application/pdf", "image/jpeg", "image/png");
    /** 资格证明大小上限：10MB */
    public static final long MAX_CERTIFICATE_SIZE = 10L * 1024 * 1024;

    @Autowired
    private InheritorRepository inheritorRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    private static final String INHERITOR_CACHE_KEY_PREFIX = "inheritors:";
    private static final String INHERITOR_LIST_CACHE_KEY_PREFIX = "inheritors:list:";
    private static final long CACHE_EXPIRE_HOURS = 1;
    private static final long LIST_CACHE_EXPIRE_MINUTES = 30;

    @Transactional
    public InheritorDTO createInheritor(InheritorCreateRequest request) {
        Inheritor inheritor = Inheritor.builder()
                .name(request.getName())
                .title(request.getTitle())
                .specialty(request.getSpecialty())
                .contact(request.getContact())
                .build();

        Inheritor saved = inheritorRepository.save(inheritor);
        clearListCache();
        return InheritorDTO.fromEntity(saved);
    }

    public InheritorDTO getInheritorById(Long id) {
        String cacheKey = INHERITOR_CACHE_KEY_PREFIX + id;
        try {
            InheritorDTO cached = (InheritorDTO) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ignored) {
            // 旧版本缓存结构不兼容时回退到数据库
        }

        Inheritor inheritor = inheritorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));

        InheritorDTO dto = InheritorDTO.fromEntity(inheritor);
        redisTemplate.opsForValue().set(cacheKey, dto, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return dto;
    }

    public Page<InheritorDTO> getInheritorList(int page, int size, String keyword) {
        String cacheKey = INHERITOR_LIST_CACHE_KEY_PREFIX + page + ":" + size + ":" + (keyword != null ? keyword : "");
        try {
            @SuppressWarnings("unchecked")
            Page<InheritorDTO> cached = (Page<InheritorDTO>) redisTemplate.opsForValue().get(cacheKey);
            if (cached != null) {
                return cached;
            }
        } catch (Exception ignored) {
            // 旧版本缓存结构不兼容时回退到数据库
        }

        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<InheritorDTO> dtoPage = inheritorRepository.searchDtoByKeyword(keyword, pageable);

        redisTemplate.opsForValue().set(cacheKey, dtoPage, LIST_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return dtoPage;
    }

    /**
     * 停档 / 重新启用。传承人行加悲观写锁：
     * 两人同一时刻一个停档、一个启用时，后到的事务阻塞到先到的提交，
     * 再读到最新状态继续，看板人数、名单标记和能不能再挂
     * 都以最后落成的那一份为准。
     */
    @Transactional
    public InheritorDTO updateStatus(Long id, InheritorStatusUpdateRequest request) {
        String target = request.getStatus() == null ? "" : request.getStatus().trim();
        if (!Inheritor.STATUS_ACTIVE.equals(target) && !Inheritor.STATUS_SUSPENDED.equals(target)) {
            throw new IllegalArgumentException("未知的传承人状态：" + target);
        }

        Inheritor inheritor = inheritorRepository.findByIdForUpdate(id)
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));

        String current = inheritor.getStatus() == null ? Inheritor.STATUS_ACTIVE : inheritor.getStatus();
        if (current.equals(target)) {
            throw new IllegalArgumentException("该传承人已处于「" + statusLabel(target) + "」状态，请勿重复操作");
        }

        inheritor.setStatus(target);
        Inheritor saved = inheritorRepository.save(inheritor);
        clearCache(id);
        // 停档占用标记会出现在溯源结果里，按旧状态算好的缓存一并作废
        clearTraceCaches();
        return InheritorDTO.fromEntity(saved);
    }

    private String statusLabel(String status) {
        return Inheritor.STATUS_SUSPENDED.equals(status) ? "停档" : "在册";
    }

    @Transactional
    public InheritorDTO updateInheritor(Long id, InheritorUpdateRequest request) {
        Inheritor inheritor = inheritorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));

        if (request.getName() != null) {
            inheritor.setName(request.getName());
        }
        if (request.getTitle() != null) {
            inheritor.setTitle(request.getTitle());
        }
        if (request.getSpecialty() != null) {
            inheritor.setSpecialty(request.getSpecialty());
        }
        if (request.getContact() != null) {
            inheritor.setContact(request.getContact());
        }

        Inheritor updated = inheritorRepository.save(inheritor);
        clearCache(id);
        return InheritorDTO.fromEntity(updated);
    }

    /**
     * 上传资格证明附件。已有附件时本次上传即覆盖替换，档案中始终只保留一份。
     */
    @Transactional
    public InheritorDTO uploadCertificate(Long id, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new IllegalArgumentException("资格证明文件不能为空");
        }
        if (file.getSize() > MAX_CERTIFICATE_SIZE) {
            throw new IllegalArgumentException("资格证明文件超出大小上限（10MB），请压缩或更换后再上传");
        }

        String originalFilename = file.getOriginalFilename();
        String extension = getExtension(originalFilename);
        if (!ALLOWED_CERTIFICATE_EXTENSIONS.contains(extension)) {
            throw new IllegalArgumentException("资格证明格式不支持，仅允许上传 PDF、JPG、JPEG、PNG 格式");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CERTIFICATE_CONTENT_TYPES.contains(contentType.toLowerCase(Locale.ROOT))) {
            throw new IllegalArgumentException("资格证明格式不支持，仅允许上传 PDF、JPG、JPEG、PNG 格式");
        }

        Inheritor inheritor = inheritorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));

        try {
            inheritor.setCertificateData(file.getBytes());
        } catch (Exception e) {
            throw new IllegalArgumentException("资格证明读取失败，请重新选择文件");
        }
        inheritor.setCertificateName(originalFilename);
        inheritor.setCertificateContentType(contentType);
        inheritor.setCertificateSize(file.getSize());
        inheritor.setCertificateUploadTime(LocalDateTime.now());

        Inheritor updated = inheritorRepository.save(inheritor);
        clearCache(id);
        return InheritorDTO.fromEntity(updated);
    }

    /**
     * 下载资格证明附件。
     */
    @Transactional
    public CertificateFile downloadCertificate(Long id) {
        Inheritor inheritor = inheritorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));
        if (inheritor.getCertificateData() == null) {
            throw new IllegalArgumentException("该传承人尚未上传资格证明");
        }
        return new CertificateFile(
                inheritor.getCertificateName(),
                inheritor.getCertificateContentType(),
                inheritor.getCertificateData());
    }

    @Transactional
    public void deleteInheritor(Long id) {
        if (!inheritorRepository.existsById(id)) {
            throw new IllegalArgumentException("传承人不存在");
        }
        inheritorRepository.deleteById(id);
        clearCache(id);
    }

    public List<InheritorDTO> getInheritorsByIds(List<Long> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        return inheritorRepository.findDtoByIdIn(ids);
    }

    private String getExtension(String filename) {
        if (filename == null) {
            return "";
        }
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase(Locale.ROOT);
    }

    private void clearCache(Long id) {
        redisTemplate.delete(INHERITOR_CACHE_KEY_PREFIX + id);
        clearListCache();
    }

    private void clearListCache() {
        redisTemplate.keys(INHERITOR_LIST_CACHE_KEY_PREFIX + "*").forEach(redisTemplate::delete);
    }

    private void clearTraceCaches() {
        Set<String> keys = redisTemplate.keys("associations:*");
        if (keys != null && !keys.isEmpty()) {
            redisTemplate.delete(keys);
        }
    }

    /** 资格证明文件内容载体 */
    public record CertificateFile(String filename, String contentType, byte[] data) {
    }
}

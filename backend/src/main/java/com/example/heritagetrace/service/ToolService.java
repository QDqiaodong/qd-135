package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.request.ToolCreateRequest;
import com.example.heritagetrace.dto.request.ToolUpdateRequest;
import com.example.heritagetrace.dto.response.ToolDTO;
import com.example.heritagetrace.entity.Tool;
import com.example.heritagetrace.repository.AssociationRepository;
import com.example.heritagetrace.repository.InheritorRepository;
import com.example.heritagetrace.repository.ProjectRepository;
import com.example.heritagetrace.repository.ToolRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Lazy;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.Collections;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class ToolService {
    @Autowired
    private ToolRepository toolRepository;

    @Autowired
    private AssociationRepository associationRepository;

    @Autowired
    private InheritorRepository inheritorRepository;

    @Autowired
    private ProjectRepository projectRepository;

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    @Lazy
    private ToolService self;

    @Autowired
    @Lazy
    private AssociationService associationService;

    private static final String TOOL_CACHE_KEY_PREFIX = "tools:";
    private static final String TOOL_LIST_CACHE_KEY_PREFIX = "tools:list:";
    /** 工具编号建档互斥锁前缀，同一编号同一时刻只允许一个建档请求进入 */
    private static final String TOOL_NUMBER_LOCK_PREFIX = "lock:tool:number:";
    private static final long CACHE_EXPIRE_HOURS = 1;
    private static final long LIST_CACHE_EXPIRE_MINUTES = 30;
    /** 持锁超时兜底，防止持锁方异常宕机造成死锁 */
    private static final long LOCK_EXPIRE_SECONDS = 10;
    /** 抢锁最长等待时间，超过则提示编号正被占用 */
    private static final long LOCK_WAIT_MILLIS = 3000;
    private static final long LOCK_RETRY_INTERVAL_MILLIS = 50;

    /** 仅释放自己持有的锁，避免误删其他请求的锁 */
    private static final DefaultRedisScript<Long> UNLOCK_SCRIPT = new DefaultRedisScript<>(
            "if redis.call('get', KEYS[1]) == ARGV[1] then "
                    + "return redis.call('del', KEYS[1]) else return 0 end",
            Long.class);

    public ToolDTO createTool(ToolCreateRequest request) {
        String lockKey = TOOL_NUMBER_LOCK_PREFIX + request.getToolNumber();
        String lockToken = UUID.randomUUID().toString();

        if (!acquireNumberLock(lockKey, lockToken)) {
            throw new IllegalArgumentException("该工具编号正在被其他用户建档，请稍后重试或更换编号");
        }
        try {
            // 走代理调用以保证 @Transactional 生效：锁在事务之外，先提交再放锁
            return self.createToolInTransaction(request);
        } finally {
            releaseNumberLock(lockKey, lockToken);
        }
    }

    @Transactional
    public ToolDTO createToolInTransaction(ToolCreateRequest request) {
        try {
            if (toolRepository.existsByToolNumber(request.getToolNumber())) {
                throw new IllegalArgumentException("工具编号已存在");
            }

            Tool tool = Tool.builder()
                    .toolNumber(request.getToolNumber())
                    .toolName(request.getToolName())
                    .craftType(request.getCraftType())
                    .material(request.getMaterial())
                    .specification(request.getSpecification())
                    .description(request.getDescription())
                    .maintenanceDueDate(parseDueDate(request.getMaintenanceDueDate()))
                    .build();

            // 并发下应用层检查可能同时放行，数据库唯一约束是编号唯一的最后防线
            Tool saved = toolRepository.save(tool);
            clearListCache();
            return ToolDTO.fromEntity(saved);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("工具编号已存在");
        }
    }

    /**
     * 基于 SET NX EX 抢编号锁，短时间自旋等待前一个建档完成（提交并放锁），
     * 使后提交方能够读到已落库的编号并得到“编号已存在”的明确结果。
     * Redis 不可用时退化为直接放行，由数据库唯一约束兜底。
     */
    private boolean acquireNumberLock(String lockKey, String lockToken) {
        long deadline = System.currentTimeMillis() + LOCK_WAIT_MILLIS;
        do {
            try {
                Boolean acquired = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockToken, LOCK_EXPIRE_SECONDS, TimeUnit.SECONDS);
                if (Boolean.TRUE.equals(acquired)) {
                    return true;
                }
                Thread.sleep(LOCK_RETRY_INTERVAL_MILLIS);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                return false;
            } catch (Exception e) {
                // Redis 异常不阻断业务，唯一约束仍保证编号不重复
                return true;
            }
        } while (System.currentTimeMillis() < deadline);
        return false;
    }

    private void releaseNumberLock(String lockKey, String lockToken) {
        try {
            redisTemplate.execute(UNLOCK_SCRIPT, Collections.singletonList(lockKey), lockToken);
        } catch (Exception ignored) {
            // 释放失败依赖锁超时自动过期
        }
    }

    public ToolDTO getToolById(Long id) {
        String cacheKey = TOOL_CACHE_KEY_PREFIX + id;
        ToolDTO cached = (ToolDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在"));

        ToolDTO dto = ToolDTO.fromEntity(tool);
        fillAssociationInfo(dto);
        
        redisTemplate.opsForValue().set(cacheKey, dto, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return dto;
    }
    
    public Page<ToolDTO> getToolList(int page, int size, String keyword) {
        String cacheKey = TOOL_LIST_CACHE_KEY_PREFIX + page + ":" + size + ":" + (keyword != null ? keyword : "");
        @SuppressWarnings("unchecked")
        Page<ToolDTO> cached = (Page<ToolDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Tool> toolPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            toolPage = toolRepository.searchByKeyword(keyword, pageable);
        } else {
            toolPage = toolRepository.findAll(pageable);
        }
        
        Page<ToolDTO> dtoPage = toolPage.map(tool -> {
            ToolDTO dto = ToolDTO.fromEntity(tool);
            fillAssociationInfo(dto);
            return dto;
        });
        
        redisTemplate.opsForValue().set(cacheKey, dtoPage, LIST_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return dtoPage;
    }
    
    @Transactional
    public ToolDTO updateTool(Long id, ToolUpdateRequest request) {
        Tool tool = toolRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("工具不存在"));
        
        if (request.getToolName() != null) {
            tool.setToolName(request.getToolName());
        }
        if (request.getCraftType() != null) {
            tool.setCraftType(request.getCraftType());
        }
        if (request.getMaterial() != null) {
            tool.setMaterial(request.getMaterial());
        }
        if (request.getSpecification() != null) {
            tool.setSpecification(request.getSpecification());
        }
        if (request.getDescription() != null) {
            tool.setDescription(request.getDescription());
        }
        // null 表示本次不修改；空串表示清除保养到期日
        if (request.getMaintenanceDueDate() != null) {
            tool.setMaintenanceDueDate(parseDueDate(request.getMaintenanceDueDate()));
        }
        
        Tool updated = toolRepository.save(tool);
        clearCache(id);

        // 工艺可能被改对或改错，重新对账名下关联的 ACTIVE/MISMATCH 标记
        if (request.getCraftType() != null) {
            associationService.reconcileByTool(id);
        }

        ToolDTO dto = ToolDTO.fromEntity(updated);
        fillAssociationInfo(dto);
        return dto;
    }
    
    @Transactional
    public void deleteTool(Long id) {
        if (!toolRepository.existsById(id)) {
            throw new IllegalArgumentException("工具不存在");
        }
        
        associationRepository.findByToolId(id).forEach(association -> {
            association.setStatus(com.example.heritagetrace.entity.Association.STATUS_DELETED);
            associationRepository.save(association);
        });
        
        toolRepository.deleteById(id);
        clearCache(id);
    }
    
    public List<ToolDTO> getToolsByIds(List<Long> ids) {
        List<Tool> tools = toolRepository.findByIdIn(ids);
        return tools.stream()
                .map(tool -> {
                    ToolDTO dto = ToolDTO.fromEntity(tool);
                    fillAssociationInfo(dto);
                    return dto;
                })
                .collect(Collectors.toList());
    }
    
    private void fillAssociationInfo(ToolDTO dto) {
        List<com.example.heritagetrace.entity.Association> associations = 
                associationRepository.findActiveByToolId(dto.getId());
        
        if (!associations.isEmpty()) {
            com.example.heritagetrace.entity.Association association = associations.get(0);
            
            inheritorRepository.findById(association.getInheritorId())
                    .ifPresent(inheritor -> dto.setInheritorName(inheritor.getName()));
            
            projectRepository.findById(association.getProjectId())
                    .ifPresent(project -> dto.setProjectName(project.getName()));
        }
    }
    
    /**
     * 解析前端传入的保养到期日（yyyy-MM-dd）。
     * 空白/空串视为未设置日期；格式非法时给出明确的 400 提示，避免把脏数据落库。
     */
    private LocalDate parseDueDate(String value) {
        if (value == null || value.trim().isEmpty()) {
            return null;
        }
        try {
            return LocalDate.parse(value.trim(), DateTimeFormatter.ISO_LOCAL_DATE);
        } catch (DateTimeParseException e) {
            throw new IllegalArgumentException("保养到期日格式不正确，应为 yyyy-MM-dd");
        }
    }

    private void clearCache(Long id) {
        redisTemplate.delete(TOOL_CACHE_KEY_PREFIX + id);
        clearListCache();
    }
    
    private void clearListCache() {
        redisTemplate.keys(TOOL_LIST_CACHE_KEY_PREFIX + "*").forEach(redisTemplate::delete);
    }
}
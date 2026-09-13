package com.example.heritagetrace.service;

import com.example.heritagetrace.dto.request.InheritorCreateRequest;
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

import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class InheritorService {
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
        InheritorDTO cached = (InheritorDTO) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Inheritor inheritor = inheritorRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("传承人不存在"));
        
        InheritorDTO dto = InheritorDTO.fromEntity(inheritor);
        redisTemplate.opsForValue().set(cacheKey, dto, CACHE_EXPIRE_HOURS, TimeUnit.HOURS);
        return dto;
    }
    
    public Page<InheritorDTO> getInheritorList(int page, int size, String keyword) {
        String cacheKey = INHERITOR_LIST_CACHE_KEY_PREFIX + page + ":" + size + ":" + (keyword != null ? keyword : "");
        @SuppressWarnings("unchecked")
        Page<InheritorDTO> cached = (Page<InheritorDTO>) redisTemplate.opsForValue().get(cacheKey);
        if (cached != null) {
            return cached;
        }
        
        Pageable pageable = PageRequest.of(page, size, Sort.by(Sort.Direction.DESC, "createTime"));
        Page<Inheritor> inheritorPage;
        
        if (keyword != null && !keyword.isEmpty()) {
            inheritorPage = inheritorRepository.searchByKeyword(keyword, pageable);
        } else {
            inheritorPage = inheritorRepository.findAll(pageable);
        }
        
        Page<InheritorDTO> dtoPage = inheritorPage.map(InheritorDTO::fromEntity);
        
        redisTemplate.opsForValue().set(cacheKey, dtoPage, LIST_CACHE_EXPIRE_MINUTES, TimeUnit.MINUTES);
        return dtoPage;
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
    
    @Transactional
    public void deleteInheritor(Long id) {
        if (!inheritorRepository.existsById(id)) {
            throw new IllegalArgumentException("传承人不存在");
        }
        inheritorRepository.deleteById(id);
        clearCache(id);
    }
    
    public List<InheritorDTO> getInheritorsByIds(List<Long> ids) {
        List<Inheritor> inheritors = inheritorRepository.findByIdIn(ids);
        return inheritors.stream()
                .map(InheritorDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    private void clearCache(Long id) {
        redisTemplate.delete(INHERITOR_CACHE_KEY_PREFIX + id);
        clearListCache();
    }
    
    private void clearListCache() {
        redisTemplate.keys(INHERITOR_LIST_CACHE_KEY_PREFIX + "*").forEach(redisTemplate::delete);
    }
}
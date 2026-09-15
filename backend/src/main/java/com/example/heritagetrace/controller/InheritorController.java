package com.example.heritagetrace.controller;

import com.example.heritagetrace.dto.request.InheritorCreateRequest;
import com.example.heritagetrace.dto.request.InheritorStatusUpdateRequest;
import com.example.heritagetrace.dto.request.InheritorUpdateRequest;
import com.example.heritagetrace.dto.response.ApiResponse;
import com.example.heritagetrace.dto.response.InheritorDTO;
import com.example.heritagetrace.service.InheritorService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@RestController
@RequestMapping("/api/inheritors")
public class InheritorController {
    @Autowired
    private InheritorService inheritorService;

    @GetMapping
    public ApiResponse<Page<InheritorDTO>> getInheritors(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String keyword) {
        Page<InheritorDTO> inheritors = inheritorService.getInheritorList(page, size, keyword);
        return ApiResponse.success(inheritors);
    }

    @GetMapping("/{id}")
    public ApiResponse<InheritorDTO> getInheritor(@PathVariable Long id) {
        InheritorDTO inheritor = inheritorService.getInheritorById(id);
        return ApiResponse.success(inheritor);
    }

    @PostMapping
    public ApiResponse<InheritorDTO> createInheritor(@Valid @RequestBody InheritorCreateRequest request) {
        InheritorDTO inheritor = inheritorService.createInheritor(request);
        return ApiResponse.success("创建成功", inheritor);
    }

    @PutMapping("/{id}")
    public ApiResponse<InheritorDTO> updateInheritor(
            @PathVariable Long id,
            @RequestBody InheritorUpdateRequest request) {
        InheritorDTO inheritor = inheritorService.updateInheritor(id, request);
        return ApiResponse.success("更新成功", inheritor);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<InheritorDTO> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody InheritorStatusUpdateRequest request) {
        InheritorDTO inheritor = inheritorService.updateStatus(id, request);
        return ApiResponse.success("状态已更新", inheritor);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> deleteInheritor(@PathVariable Long id) {
        inheritorService.deleteInheritor(id);
        return ApiResponse.success("删除成功", null);
    }

    @PostMapping(value = "/{id}/certificate", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ApiResponse<InheritorDTO> uploadCertificate(
            @PathVariable Long id,
            @RequestPart("file") MultipartFile file) {
        InheritorDTO inheritor = inheritorService.uploadCertificate(id, file);
        return ApiResponse.success("资格证明上传成功", inheritor);
    }

    @GetMapping("/{id}/certificate")
    public ResponseEntity<byte[]> downloadCertificate(@PathVariable Long id) {
        InheritorService.CertificateFile file = inheritorService.downloadCertificate(id);
        String encodedFilename = URLEncoder.encode(file.filename(), StandardCharsets.UTF_8)
                .replace("+", "%20");
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + encodedFilename + "\"; filename*=UTF-8''" + encodedFilename)
                .contentType(MediaType.parseMediaType(file.contentType()))
                .body(file.data());
    }
}

package org.example.nursingtrainingbackend.modules.courseware.ppt.controller;

import com.baomidou.mybatisplus.core.metadata.IPage;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.courseware.ppt.dto.CreatePptRequest;
import org.example.nursingtrainingbackend.modules.courseware.ppt.dto.UpdatePptRequest;
import org.example.nursingtrainingbackend.modules.courseware.ppt.dto.UpdateStatusRequest;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;
import org.example.nursingtrainingbackend.modules.courseware.ppt.service.PptService;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptDetailVO;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptListItem;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptOverviewVO;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptPreviewFile;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;

@RestController
@RequestMapping("/api/admin/ppts")
@RequiredArgsConstructor
public class PptController {

    private final PptService pptService;

    @PostMapping
    public Result<Ppt> createPpt(@Valid @RequestBody CreatePptRequest request) {
        Authentication auth = SecurityContextHolder.getContext().getAuthentication();
        AuthenticatedUser user = (AuthenticatedUser) auth.getPrincipal();
        // ✅ record 使用 id() 和 nickname() 方法
        return Result.success(pptService.createPpt(request, user.id(), user.nickname()));
    }

    @GetMapping
    public Result<IPage<PptListItem>> pagePpt(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String uploadedFrom,
            @RequestParam(required = false) String uploadedTo,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        return Result.success(pptService.pagePpt(keyword, status, uploadedFrom,
                uploadedTo, sortOrder, page, size));
    }

    /**
     * 查询PPT概览统计（必须在 /{id} 之前，避免 "overview" 被当作 id 解析）
     */
    @GetMapping("/overview")
    public Result<PptOverviewVO> getOverview() {
        return Result.success(pptService.getOverview());
    }

    @GetMapping("/{id}")
    public Result<PptDetailVO> getPptDetail(@PathVariable Long id) {
        return Result.success(pptService.getPptDetail(id));
    }

    @GetMapping(value = "/{id}/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    public void previewPpt(@PathVariable Long id, HttpServletResponse response) throws IOException {
        try (PptPreviewFile preview = pptService.getPreviewFile(id)) {
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setContentLengthLong(preview.contentLength());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.inline().filename("ppt-preview-" + id + ".pdf").build().toString());
            preview.inputStream().transferTo(response.getOutputStream());
            response.flushBuffer();
        }
    }

    @PutMapping("/{id}")
    public Result<Ppt> updatePpt(@PathVariable Long id,
                                 @Valid @RequestBody UpdatePptRequest request) {
        return Result.success(pptService.updatePpt(id, request));
    }

    @PatchMapping("/{id}/status")
    public Result<Ppt> updateStatus(@PathVariable Long id,
                                    @Valid @RequestBody UpdateStatusRequest request) {
        return Result.success(pptService.updateStatus(id, request.getStatus()));
    }

    @GetMapping("/{id}/download-url")
    public Result<DownloadUrlResponse> getDownloadUrl(
            @PathVariable Long id,
            @RequestParam(defaultValue = "600") Integer expiresIn) {
        String url = pptService.getDownloadUrl(id, expiresIn);
        return Result.success(new DownloadUrlResponse(url));
    }

    @PostMapping("/{id}/convert")
    public Result<Void> convertPpt(@PathVariable Long id) {
        pptService.requestConversion(id);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deletePpt(@PathVariable Long id) {
        pptService.deletePpt(id);
        return Result.success();
    }

    record DownloadUrlResponse(String downloadUrl) {}
}

package org.example.nursingtrainingbackend.modules.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.learning.dto.VideoProgressRequest;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerStudy;
import org.example.nursingtrainingbackend.modules.learning.vo.CourseStudyVO;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptPreviewFile;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;

import java.io.IOException;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端课程学习控制器
 */
@RestController
@RequestMapping("/api/learner/courses")
@RequiredArgsConstructor
public class LearnerStudyController {

    private final LearnerStudy learnerStudy;

    /**
     * 获取课程学习页数据（当前课程点、三类课件、导航）
     * GET /api/learner/courses/{courseId}/points/{pointId}/study
     */
    @GetMapping("/{courseId}/points/{pointId}/study")
    public Result<CourseStudyVO> getCourseStudy(
            @PathVariable Long courseId,
            @PathVariable Long pointId,
            @RequestParam(required = false) String activeType) {
        return Result.success(learnerStudy.getCourseStudy(courseId, pointId, activeType));
    }

    /**
     * 上报视频播放进度
     * POST /api/learner/courses/{courseId}/points/{coursePointId}/videos/{videoId}/progress
     */
    @PostMapping("/{courseId}/points/{coursePointId}/videos/{videoId}/progress")
    public Result<Void> reportVideoProgress(
            @PathVariable Long courseId,
            @PathVariable Long coursePointId,
            @PathVariable Long videoId,
            @Valid @RequestBody VideoProgressRequest request) {
        learnerStudy.reportVideoProgress(courseId, coursePointId, videoId, request);
        return Result.success(null);
    }

    /**
     * 完成文章/PPT资源学习
     * POST /api/learner/courses/{courseId}/points/{coursePointId}/resources/complete
     */
    @PostMapping("/{courseId}/points/{coursePointId}/resources/complete")
    public Result<Void> completeResource(
            @PathVariable Long courseId,
            @PathVariable Long coursePointId,
            @RequestParam Integer resourceType,
            @RequestParam Long resourceId) {
        learnerStudy.completeResource(courseId, coursePointId, resourceType, resourceId);
        return Result.success(null);
    }

    /** 返回可内嵌显示的学员端 PPT PDF 流。 */
    @GetMapping(value = "/points/{coursePointId}/ppts/{pptId}/preview", produces = MediaType.APPLICATION_PDF_VALUE)
    public void previewPpt(@PathVariable Long coursePointId,
                           @PathVariable Long pptId,
                           HttpServletResponse response) throws IOException {
        try (PptPreviewFile preview = learnerStudy.getPptPreview(coursePointId, pptId)) {
            response.setContentType(MediaType.APPLICATION_PDF_VALUE);
            response.setContentLengthLong(preview.contentLength());
            response.setHeader(HttpHeaders.CONTENT_DISPOSITION,
                    ContentDisposition.inline().filename("ppt-preview-" + pptId + ".pdf").build().toString());
            preview.inputStream().transferTo(response.getOutputStream());
            response.flushBuffer();
        }
    }
}

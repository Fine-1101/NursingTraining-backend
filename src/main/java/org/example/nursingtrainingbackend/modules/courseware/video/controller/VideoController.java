package org.example.nursingtrainingbackend.modules.courseware.video.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.annotation.RateLimit;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoBatchRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoStatusUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoUploadRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.service.VideoService;
import org.example.nursingtrainingbackend.modules.courseware.video.vo.*;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/videos")
@RequiredArgsConstructor
public class VideoController {

    private final VideoService videoService;

    @GetMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<PageResult<VideoListItemVO>> listVideos(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String uploadedFrom,
            @RequestParam(required = false) String uploadedTo,
            @RequestParam(defaultValue = "desc") String sortOrder,
            @RequestParam(defaultValue = "1") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {

        PageResult<VideoListItemVO> result = videoService.listVideos(
                keyword, status, uploadedFrom, uploadedTo, sortOrder, page, size);
        return Result.success(result);
    }

    @GetMapping("/overview")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VideoOverviewVO> getVideoOverview() {
        VideoOverviewVO overview = videoService.getVideoOverview();
        return Result.success(overview);
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VideoDetailVO> getVideoDetail(@PathVariable Long id) {
        VideoDetailVO detail = videoService.getVideoDetail(id);
        return Result.success(detail);
    }

    @PostMapping
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VideoUploadResponseVO> uploadVideo(
            @Valid @RequestBody VideoUploadRequest request,
            @AuthenticationPrincipal AuthenticatedUser user) {

        VideoUploadResponseVO response = videoService.uploadVideo(request, user);
        return Result.success(response);
    }

    @PostMapping("/batch-publish")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VideoBatchPublishResponseVO> batchPublishVideos(
            @Valid @RequestBody VideoBatchRequest request) {

        VideoBatchPublishResponseVO response = videoService.batchPublishVideos(request);
        return Result.success(response);
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VideoUpdateResponseVO> updateVideo(
            @PathVariable Long id,
            @Valid @RequestBody VideoUpdateRequest request) {

        VideoUpdateResponseVO response = videoService.updateVideo(id, request);
        return Result.success(response);
    }

    @PatchMapping("/{id}/status")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VideoStatusUpdateResponseVO> updateVideoStatus(
            @PathVariable Long id,
            @Valid @RequestBody VideoStatusUpdateRequest request) {

        VideoStatusUpdateResponseVO response = videoService.updateVideoStatus(id, request);
        return Result.success(response);
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<Void> deleteVideo(@PathVariable Long id) {
        videoService.deleteVideo(id);
        return Result.success();
    }

    @DeleteMapping("/batch")
    @PreAuthorize("hasRole('ADMIN')")
    public Result<VideoBatchDeleteResponseVO> batchDeleteVideos(
            @Valid @RequestBody VideoBatchRequest request) {

        VideoBatchDeleteResponseVO response = videoService.batchDeleteVideos(request);
        return Result.success(response);
    }

    @GetMapping("/{id}/play-url")
    @PreAuthorize("hasRole('ADMIN')")
    @RateLimit(key = "video-play", time = 300, count = 20, limitType = RateLimit.LimitType.USER)
    public Result<VideoPlayUrlVO> getVideoPlayUrl(
            @PathVariable Long id,
            @RequestParam(required = false, defaultValue = "600") Integer expiresIn) {

        VideoPlayUrlVO response = videoService.getVideoPlayUrl(id, expiresIn);
        return Result.success(response);
    }
}

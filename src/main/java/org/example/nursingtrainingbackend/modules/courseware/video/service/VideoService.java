package org.example.nursingtrainingbackend.modules.courseware.video.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoBatchRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoStatusUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoUpdateRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.dto.VideoUploadRequest;
import org.example.nursingtrainingbackend.modules.courseware.video.vo.*;
import org.example.nursingtrainingbackend.security.AuthenticatedUser;

public interface VideoService {

    PageResult<VideoListItemVO> listVideos(String keyword, String status,
                                           String uploadedFrom, String uploadedTo,
                                           String sortOrder, Integer page, Integer size);

    VideoOverviewVO getVideoOverview();

    VideoDetailVO getVideoDetail(Long id);

    VideoUploadResponseVO uploadVideo(VideoUploadRequest request, AuthenticatedUser user);

    VideoUpdateResponseVO updateVideo(Long id, VideoUpdateRequest request);

    VideoStatusUpdateResponseVO updateVideoStatus(Long id, VideoStatusUpdateRequest request);

    void deleteVideo(Long id);

    VideoBatchPublishResponseVO batchPublishVideos(VideoBatchRequest request);

    VideoBatchDeleteResponseVO batchDeleteVideos(VideoBatchRequest request);

    VideoPlayUrlVO getVideoPlayUrl(Long id, Integer expiresIn);
}

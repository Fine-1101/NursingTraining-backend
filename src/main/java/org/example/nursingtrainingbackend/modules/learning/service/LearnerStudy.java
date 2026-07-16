package org.example.nursingtrainingbackend.modules.learning.service;

import org.example.nursingtrainingbackend.modules.learning.dto.VideoProgressRequest;
import org.example.nursingtrainingbackend.modules.learning.vo.CourseStudyVO;
import org.example.nursingtrainingbackend.modules.courseware.ppt.vo.PptPreviewFile;

public interface LearnerStudy {
    CourseStudyVO getCourseStudy(Long courseId, Long pointId, String activeType);
    void reportVideoProgress(Long courseId, Long coursePointId, Long videoId, VideoProgressRequest request);
    void completeResource(Long courseId, Long coursePointId, Integer resourceType, Long resourceId);
    PptPreviewFile getPptPreview(Long coursePointId, Long pptId);

}

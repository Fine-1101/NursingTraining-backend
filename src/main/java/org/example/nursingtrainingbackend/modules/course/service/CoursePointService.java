package org.example.nursingtrainingbackend.modules.course.service;

import org.example.nursingtrainingbackend.modules.course.vo.CoursePointDetailVO;

public interface CoursePointService {

    CoursePointDetailVO getPointDetail(Long courseId, Long chapterId, Long pointId);
}

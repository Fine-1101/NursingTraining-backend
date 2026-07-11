package org.example.nursingtrainingbackend.modules.learning.service;

import org.example.nursingtrainingbackend.modules.learning.vo.CourseStudyVO;

public interface LearnerStudy {
    CourseStudyVO getCourseStudy(Long courseId, Long pointId, String activeType);
}

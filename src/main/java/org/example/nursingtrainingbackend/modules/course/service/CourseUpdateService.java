package org.example.nursingtrainingbackend.modules.course.service;

import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.vo.CourseUpdateBasicVO;

public interface CourseUpdateService {

    CourseUpdateBasicVO updateCourseBasic(Long courseId, CreateCourseInitial dto);

    Void NewCourse(Long courseId);
}
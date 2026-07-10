package org.example.nursingtrainingbackend.modules.course.service;

import org.example.nursingtrainingbackend.modules.course.dto.CompletionRuleDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateCourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.vo.CompletionRuleVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseUpdateBasicVO;

public interface CourseUpdateService {

    CourseUpdateBasicVO updateCourseBasic(Long courseId, CreateCourseInitial dto);

    CompletionRuleVO saveCompletionRule(Long courseId, CompletionRuleDTO dto);

    void updateCourseStatus(Long courseId, UpdateCourseStatusDTO dto);

    void deleteCourseDraft(Long courseId);
}
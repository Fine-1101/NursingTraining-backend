package org.example.nursingtrainingbackend.modules.course.service;

import org.example.nursingtrainingbackend.modules.course.dto.CompletionRuleDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.CreatePoint;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateChapter;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateChapterOrder;
import org.example.nursingtrainingbackend.modules.course.dto.UpdatePointOrder;
import org.example.nursingtrainingbackend.modules.course.vo.*;

import java.util.List;

public interface CourseCreateService {
    List<InstructorOptionVO> getInstructorOptions(String a,Integer b);

    List<DepartmentOptionVO> getDepartmentOptions();

    CreateCourseInitialVO createCourseInitial(CreateCourseInitial createCourseInitial);

    CreateChapterVO createChapter(Long courseId, String title);

    UpdateChapterVO updateChapter(Long courseId, Long chapterId, UpdateChapter updateChapter);

    CreatePointVO createPoint(Long courseId, Long chapterId, CreatePoint createPoint);

    UpdatePointVO updatePoint(Long courseId, Long chapterId, Long pointId, CreatePoint createPoint);

    void deletePoint(Long courseId, Long chapterId, Long pointId);

    void deleteChapter(Long courseId, Long chapterId);

    CourseDetailVO getCourseDetail(Long courseId);

    UpdateChapterOrderVO updateChapterOrder(Long courseId, UpdateChapterOrder updateChapterOrder);

    UpdatePointOrderVO updatePointOrder(Long courseId, Long chapterId, UpdatePointOrder updatePointOrder);

    CompletionRuleVO updateCompletionRule(Long courseId, CompletionRuleDTO completionRuleDTO);
}

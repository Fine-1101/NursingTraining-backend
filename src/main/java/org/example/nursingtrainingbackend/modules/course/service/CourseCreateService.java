package org.example.nursingtrainingbackend.modules.course.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.CreatePointDTO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseDetailVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseListItemVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseOverviewVO;
import org.example.nursingtrainingbackend.modules.course.vo.CreateCourseInitialVO;
import org.example.nursingtrainingbackend.modules.course.vo.DepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.course.vo.InstructorOptionVO;

import java.util.List;

public interface CourseCreateService {

    PageResult<CourseListItemVO> getCourses(String keyword, String categoryId, String status, Integer page, Integer size);

    CourseOverviewVO getCourseOverview();

    List<InstructorOptionVO> getInstructorOptions(String a, Integer b);

    List<DepartmentOptionVO> getDepartmentOptions();

    CreateCourseInitialVO createCourseInitial(CreateCourseInitial createCourseInitial);

    CourseDetailVO getCourseDetail(Long courseId);

    CourseDetailVO.ChapterItem createChapter(Long courseId, String title);

    CourseDetailVO.ChapterItem updateChapter(Long courseId, Long chapterId, String title);

    void deleteChapter(Long courseId, Long chapterId);

    void orderChapters(Long courseId, List<Long> chapterIds);

    CourseDetailVO.PointItem createPoint(Long courseId, Long chapterId, CreatePointDTO dto);

    CourseDetailVO.PointItem getPoint(Long courseId, Long chapterId, Long pointId);

    CourseDetailVO.PointItem updatePoint(Long courseId, Long chapterId, Long pointId, CreatePointDTO dto);

    void deletePoint(Long courseId, Long chapterId, Long pointId);

    void orderPoints(Long courseId, Long chapterId, List<Long> pointIds);
}

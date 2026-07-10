package org.example.nursingtrainingbackend.modules.course.controller;

import jakarta.validation.Valid;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.course.dto.CompletionRuleDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.CreatePointDTO;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateCourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.service.CourseUpdateService;
import org.example.nursingtrainingbackend.modules.course.vo.CompletionRuleVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseDetailVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseListItemVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseOverviewVO;
import org.example.nursingtrainingbackend.modules.course.vo.CourseUpdateBasicVO;
import org.example.nursingtrainingbackend.modules.course.vo.CreateCourseInitialVO;
import org.example.nursingtrainingbackend.modules.course.vo.DepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.course.vo.InstructorOptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
public class CourseCreateController {
    @Autowired
    private CourseCreateService courseCreateService;
    @Autowired
    private CourseUpdateService courseUpdateService;

    @GetMapping("/instructors/options")
    public Result<List<InstructorOptionVO>> getinstructorOptions(@RequestParam String keyword, @RequestParam(defaultValue = "10") Integer limit) {
        return Result.success(courseCreateService.getInstructorOptions(keyword, limit));
    }

    @GetMapping("/department/options")
    public Result<List<DepartmentOptionVO>> getDepartmentOptions(@RequestParam(required = false) String keyword) {
        return Result.success(courseCreateService.getDepartmentOptions());
    }

    @GetMapping("/courses")
    public Result<PageResult<CourseListItemVO>> getCourses(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String categoryId,
            @RequestParam(required = false) String status,
            @RequestParam(required = false, defaultValue = "1") Integer page,
            @RequestParam(required = false, defaultValue = "10") Integer size) {
        return Result.success(courseCreateService.getCourses(keyword, categoryId, status, page, size));
    }

    @GetMapping("/courses/overview")
    public Result<CourseOverviewVO> getCourseOverview() {
        return Result.success(courseCreateService.getCourseOverview());
    }

    @PostMapping("/courses")
    public Result<CreateCourseInitialVO> createCourseInitial(@Valid @RequestBody CreateCourseInitial createCourseInitial) {
        return Result.success(courseCreateService.createCourseInitial(createCourseInitial));
    }

    @GetMapping("/courses/{courseId}")
    public Result<CourseDetailVO> getCourseDetail(@PathVariable Long courseId) {
        return Result.success(courseCreateService.getCourseDetail(courseId));
    }

    @PutMapping("/courses/{courseId}/basic")
    public Result<CourseUpdateBasicVO> updateCourseBasic(@PathVariable Long courseId,
                                                         @Valid @RequestBody CreateCourseInitial createCourseInitial) {
        return Result.success(courseUpdateService.updateCourseBasic(courseId, createCourseInitial));
    }

    @PostMapping("/courses/{id}/publish")
    public Result<Void> publishCourse(@PathVariable Long id) {
        return Result.success(null);
    }

    @PutMapping("/courses/{courseId}/completion-rule")
    public Result<CompletionRuleVO> saveCompletionRule(@PathVariable Long courseId,
                                                       @RequestBody CompletionRuleDTO dto) {
        return Result.success(courseUpdateService.saveCompletionRule(courseId, dto));
    }

    @PatchMapping("/courses/{courseId}/status")
    public Result<Void> updateCourseStatus(@PathVariable Long courseId,
                                           @RequestBody UpdateCourseStatusDTO dto) {
        courseUpdateService.updateCourseStatus(courseId, dto);
        return Result.success(null);
    }

    @DeleteMapping("/courses/{courseId}")
    public Result<Void> deleteCourseDraft(@PathVariable Long courseId) {
        courseUpdateService.deleteCourseDraft(courseId);
        return Result.success(null);
    }

    @PostMapping("/courses/{courseId}/chapters")
    public Result<CourseDetailVO.ChapterItem> createChapter(@PathVariable Long courseId,
                                                            @RequestBody String title) {
        return Result.success(courseCreateService.createChapter(courseId, title));
    }

    @PutMapping("/courses/{courseId}/chapters/{chapterId}")
    public Result<CourseDetailVO.ChapterItem> updateChapter(@PathVariable Long courseId,
                                                            @PathVariable Long chapterId,
                                                            @RequestBody Map<String, String> body) {
        return Result.success(courseCreateService.updateChapter(courseId, chapterId, body.get("title")));
    }

    @DeleteMapping("/courses/{courseId}/chapters/{chapterId}")
    public Result<Void> deleteChapter(@PathVariable Long courseId, @PathVariable Long chapterId) {
        courseCreateService.deleteChapter(courseId, chapterId);
        return Result.success(null);
    }

    @PutMapping("/courses/{courseId}/chapters/order")
    public Result<Void> orderChapters(@PathVariable Long courseId,
                                      @RequestBody Map<String, List<Long>> body) {
        courseCreateService.orderChapters(courseId, body.get("chapterIds"));
        return Result.success(null);
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/points")
    public Result<CourseDetailVO.PointItem> createPoint(@PathVariable Long courseId,
                                                        @PathVariable Long chapterId,
                                                        @RequestBody CreatePointDTO dto) {
        return Result.success(courseCreateService.createPoint(courseId, chapterId, dto));
    }

    @GetMapping("/courses/{courseId}/chapters/{chapterId}/points/{pointId}")
    public Result<CourseDetailVO.PointItem> getPoint(@PathVariable Long courseId,
                                                     @PathVariable Long chapterId,
                                                     @PathVariable Long pointId) {
        return Result.success(courseCreateService.getPoint(courseId, chapterId, pointId));
    }

    @PutMapping("/courses/{courseId}/chapters/{chapterId}/points/{pointId}")
    public Result<CourseDetailVO.PointItem> updatePoint(@PathVariable Long courseId,
                                                        @PathVariable Long chapterId,
                                                        @PathVariable Long pointId,
                                                        @RequestBody CreatePointDTO dto) {
        return Result.success(courseCreateService.updatePoint(courseId, chapterId, pointId, dto));
    }

    @DeleteMapping("/courses/{courseId}/chapters/{chapterId}/points/{pointId}")
    public Result<Void> deletePoint(@PathVariable Long courseId,
                                    @PathVariable Long chapterId,
                                    @PathVariable Long pointId) {
        courseCreateService.deletePoint(courseId, chapterId, pointId);
        return Result.success(null);
    }

    @PutMapping("/courses/{courseId}/chapters/{chapterId}/points/order")
    public Result<Void> orderPoints(@PathVariable Long courseId,
                                    @PathVariable Long chapterId,
                                    @RequestBody Map<String, List<Long>> body) {
        courseCreateService.orderPoints(courseId, chapterId, body.get("pointIds"));
        return Result.success(null);
    }
}

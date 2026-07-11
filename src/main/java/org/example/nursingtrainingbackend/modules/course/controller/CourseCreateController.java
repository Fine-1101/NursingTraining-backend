package org.example.nursingtrainingbackend.modules.course.controller;

import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.course.dto.CompletionRuleDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CourseStatusDTO;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.CreatePoint;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateChapter;
import org.example.nursingtrainingbackend.modules.course.dto.UpdateChapterOrder;
import org.example.nursingtrainingbackend.modules.course.dto.UpdatePointOrder;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.service.CoursePointService;
import org.example.nursingtrainingbackend.modules.course.service.CourseUpdateService;
import org.example.nursingtrainingbackend.modules.course.vo.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class CourseCreateController {
    @Autowired
    private CourseCreateService courseCreateService;
    @Autowired
    private CourseUpdateService courseUpdateService;
    @Autowired
    private CoursePointService coursePointService;

  
    @GetMapping("/instructors/options")
    public Result<List<InstructorOptionVO>> getinstructorOptions1(@RequestParam (required=false)String keyword,@RequestParam(defaultValue = "10") Integer limit){
        return Result.success(courseCreateService.getInstructorOptions(keyword,limit));
    }


    @GetMapping("/department/options")
    public Result<List<DepartmentOptionVO>> getDepartmentOptions(@RequestParam(required = false) String keyword){
    return Result.success(courseCreateService.getDepartmentOptions());
    }

    @PostMapping("/courses")
    public Result<CreateCourseInitialVO> createCourseInitial(@Validated @RequestBody CreateCourseInitial createCourseInitial){
        return Result.success(courseCreateService.createCourseInitial(createCourseInitial));
    }

    @PutMapping("/courses/{courseId}/basic")
    public Result<CourseUpdateBasicVO> updateCourseBasic(@PathVariable Long courseId,
                                                         @RequestBody CreateCourseInitial createCourseInitial) {
        return Result.success(courseUpdateService.updateCourseBasic(courseId, createCourseInitial));
    }

    @PostMapping("/courses/{id}/publish")
    public Result<Void> publishCourse(@RequestBody Long courseId) {
        return Result.success(courseUpdateService.NewCourse(courseId));
    }
    @PostMapping("/courses/{courseId}/chapters")
    public Result<CreateChapterVO> createChapter(@PathVariable Long courseId,
                                                 @RequestBody String title) {
        return Result.success(courseCreateService.createChapter(courseId, title));
    }

    @PutMapping("/courses/{courseId}/chapters/{chapterId}")
    public Result<UpdateChapterVO> updateChapter(@PathVariable Long courseId,
                                                 @PathVariable Long chapterId,
                                                 @RequestBody UpdateChapter updateChapter) {
        return Result.success(courseCreateService.updateChapter(courseId, chapterId, updateChapter));
    }

    @GetMapping("/courses/{courseId}/chapters/{chapterId}/points/{pointId}")
    public Result<CoursePointDetailVO> getPointDetail(@PathVariable Long courseId,
                                                       @PathVariable Long chapterId,
                                                       @PathVariable Long pointId) {
        return Result.success(coursePointService.getPointDetail(courseId, chapterId, pointId));
    }

    @PostMapping("/courses/{courseId}/chapters/{chapterId}/points")
    public Result<CreatePointVO> createPoint(@PathVariable Long courseId,
                                             @PathVariable Long chapterId,
                                             @RequestBody CreatePoint createPoint) {
        return Result.success(courseCreateService.createPoint(courseId, chapterId, createPoint));
    }

    @PutMapping("/courses/{courseId}/chapters/{chapterId}/points/{pointId}")
    public Result<UpdatePointVO> updatePoint(@PathVariable Long courseId,
                                             @PathVariable Long chapterId,
                                             @PathVariable Long pointId,
                                             @RequestBody CreatePoint createPoint) {
        return Result.success(courseCreateService.updatePoint(courseId, chapterId, pointId, createPoint));
    }

    @DeleteMapping("/courses/{courseId}/chapters/{chapterId}/points/{pointId}")
    public Result<Void> deletePoint(@PathVariable Long courseId,
                                    @PathVariable Long chapterId,
                                    @PathVariable Long pointId) {
        courseCreateService.deletePoint(courseId, chapterId, pointId);
        return Result.success(null);
    }

    @GetMapping("/courses/{courseId}")
    public Result<CourseDetailVO> getCourseDetail(@PathVariable Long courseId) {
        return Result.success(courseCreateService.getCourseDetail(courseId));
    }

    @PutMapping("/courses/{courseId}/chapters/order")
    public Result<UpdateChapterOrderVO> updateChapterOrder(@PathVariable Long courseId,
                                                           @Validated @RequestBody UpdateChapterOrder updateChapterOrder) {
        return Result.success(courseCreateService.updateChapterOrder(courseId, updateChapterOrder));
    }

    @PutMapping("/courses/{courseId}/chapters/{chapterId}/points/order")
    public Result<UpdatePointOrderVO> updatePointOrder(@PathVariable Long courseId,
                                                       @PathVariable Long chapterId,
                                                       @Validated @RequestBody UpdatePointOrder updatePointOrder) {
        return Result.success(courseCreateService.updatePointOrder(courseId, chapterId, updatePointOrder));
    }

    @PutMapping("/courses/{courseId}/completion-rule")
    public Result<CompletionRuleVO> updateCompletionRule(@PathVariable Long courseId,
                                                         @Validated @RequestBody CompletionRuleDTO completionRuleDTO) {
        return Result.success(courseCreateService.updateCompletionRule(courseId, completionRuleDTO));
    }

    @PatchMapping("/courses/{courseId}/status")
    public Result<CourseStatusVO> updateCourseStatus(@PathVariable Long courseId,
                                                      @Validated @RequestBody CourseStatusDTO courseStatusDTO) {
        return Result.success(courseUpdateService.updateCourseStatus(courseId, courseStatusDTO));
    }


}

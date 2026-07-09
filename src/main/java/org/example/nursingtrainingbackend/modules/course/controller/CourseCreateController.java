package org.example.nursingtrainingbackend.modules.course.controller;

import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.service.CourseUpdateService;
import org.example.nursingtrainingbackend.modules.course.vo.CourseUpdateBasicVO;
import org.example.nursingtrainingbackend.modules.course.vo.CreateCourseInitialVO;
import org.example.nursingtrainingbackend.modules.course.vo.DepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.course.vo.InstructorOptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Controller
@RequestMapping("/api/admin")
public class CourseCreateController {
    @Autowired
    private CourseCreateService courseCreateService;
    @Autowired
    private CourseUpdateService courseUpdateService;

    @GetMapping("/instructors/options")
    public Result<List<InstructorOptionVO>> getinstructorOptions(@RequestParam String keyword,@RequestParam(defaultValue = "10") Integer limit){
    return Result.success(courseCreateService.getInstructorOptions(keyword,limit));
    }

    @GetMapping("/department/options")
    public Result<List<DepartmentOptionVO>> getDepartmentOptions(@RequestParam String keyword){
    return Result.success(courseCreateService.getDepartmentOptions());
    }

    @PostMapping("/courses")
    public Result<CreateCourseInitialVO> createCourseInitial(@RequestBody CreateCourseInitial createCourseInitial){
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

}

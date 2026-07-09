package org.example.nursingtrainingbackend.modules.course.controller;

import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.vo.InstructorOptionVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

@Controller
@RequestMapping("/api/admin")
public class CourseCreateController {
    @Autowired
    private CourseCreateService courseCreateService;
    @GetMapping("/instructors/options")
    public Result<List<InstructorOptionVO>> getinstructorOptions(@RequestParam String keyword,@RequestParam(defaultValue = "10") Integer limit){
    return Result.success(courseCreateService.getInstructorOptions(keyword,limit));
    }

}

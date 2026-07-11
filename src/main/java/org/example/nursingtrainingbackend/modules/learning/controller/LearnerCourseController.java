package org.example.nursingtrainingbackend.modules.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.learning.dto.LearnerCourseQuery;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerCourseService;
import org.example.nursingtrainingbackend.modules.learning.vo.CourseStatsVO;
import org.example.nursingtrainingbackend.modules.learning.vo.LearnerCourseDetailVO;
import org.example.nursingtrainingbackend.modules.learning.vo.LearnerCourseVO;
import org.example.nursingtrainingbackend.modules.learning.vo.StartLearningVO;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端课程列表控制器
 */
@RestController
@RequestMapping("/api/learner/courses")
@RequiredArgsConstructor
public class LearnerCourseController {

    private final LearnerCourseService learnerCourseService;

    /**
     * 分页查询学员课程列表
     * GET /api/learner/courses?learningStatus=ALL&courseType=ALL&keyword=xxx&page=1&size=10
     */
    @GetMapping
    public Result<PageResult<LearnerCourseVO>> getLearnerCourses(@Valid LearnerCourseQuery query) {
        return Result.success(learnerCourseService.getLearnerCourses(query));
    }

    /**
     * 获取学员课程统计
     * GET /api/learner/courses/stats
     */
    @GetMapping("/stats")
    public Result<CourseStatsVO> getLearnerCourseStats() {
        return Result.success(learnerCourseService.getLearnerCourseStats());
    }

    /**
     * 开始学习课程
     * POST /api/learner/courses/{courseId}/start
     */
    @PostMapping("/{courseId}/start")
    public Result<StartLearningVO> startLearning(@PathVariable Long courseId) {
        return Result.success(learnerCourseService.startLearning(courseId));
    }

    /**
     * 获取课程学习详情（含章节、课程点、课件和进度）
     * GET /api/learner/courses/{courseId}
     */
    @GetMapping("/{courseId}")
    public Result<LearnerCourseDetailVO> getCourseDetail(@PathVariable Long courseId) {
        return Result.success(learnerCourseService.getCourseDetail(courseId));
    }
}

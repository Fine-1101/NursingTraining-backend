package org.example.nursingtrainingbackend.modules.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.learning.dto.LearnerPageQuery;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerHomeService;
import org.example.nursingtrainingbackend.modules.learning.vo.ContinueCourseVO;
import org.example.nursingtrainingbackend.modules.learning.vo.HomePageVO;
import org.example.nursingtrainingbackend.modules.learning.vo.LearningRecordVO;
import org.example.nursingtrainingbackend.modules.learning.vo.RecommendedCourseVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 学员端首页控制器
 */
@RestController
@RequestMapping("/api/learner/home")
@RequiredArgsConstructor
public class LearnerHomeController {

    private final LearnerHomeService learnerHomeService;

    /**
     * 查询学员首页聚合数据
     * GET /api/learner/home
     */
    @GetMapping
    public Result<HomePageVO> getHomePage() {
        return Result.success(learnerHomeService.getHomePage());
    }

    /**
     * 分页查询推荐课程
     * GET /api/learner/home/recommended-courses?page=1&size=10
     */
    @GetMapping("/recommended-courses")
    public Result<PageResult<RecommendedCourseVO>> getRecommendedCourses(@Valid LearnerPageQuery query) {
        return Result.success(learnerHomeService.getRecommendedCourses(query));
    }

    /**
     * 分页查询继续学习课程
     * GET /api/learner/home/continue-courses?page=1&size=10
     */
    @GetMapping("/continue-courses")
    public Result<PageResult<ContinueCourseVO>> getContinueCourses(@Valid LearnerPageQuery query) {
        return Result.success(learnerHomeService.getContinueCourses(query));
    }

    /**
     * 分页查询学习记录
     * GET /api/learner/home/recent-records?page=1&size=10
     */
    @GetMapping("/recent-records")
    public Result<PageResult<LearningRecordVO>> getRecentRecords(@Valid LearnerPageQuery query) {
        return Result.success(learnerHomeService.getRecentRecords(query));
    }
}

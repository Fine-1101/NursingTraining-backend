package org.example.nursingtrainingbackend.modules.learning.controller;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerStudy;
import org.example.nursingtrainingbackend.modules.learning.vo.CourseStudyVO;
import org.springframework.web.bind.annotation.*;

/**
 * 学员端课程学习控制器
 */
@RestController
@RequestMapping("/api/learner/courses")
@RequiredArgsConstructor
public class LearnerStudyController {

    private final LearnerStudy learnerStudy;

    /**
     * 获取课程学习页数据（当前课程点、三类课件、导航）
     * GET /api/learner/courses/{courseId}/points/{pointId}/study
     */
    @GetMapping("/{courseId}/points/{pointId}/study")
    public Result<CourseStudyVO> getCourseStudy(
            @PathVariable Long courseId,
            @PathVariable Long pointId,
            @RequestParam(required = false) String activeType) {
        return Result.success(learnerStudy.getCourseStudy(courseId, pointId, activeType));
    }
}

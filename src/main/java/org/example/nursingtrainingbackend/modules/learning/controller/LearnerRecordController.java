package org.example.nursingtrainingbackend.modules.learning.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.learning.dto.RecordQuery;
import org.example.nursingtrainingbackend.modules.learning.service.LearnerRecordService;
import org.example.nursingtrainingbackend.modules.learning.vo.*;
import org.example.nursingtrainingbackend.modules.learning.dto.TopCoursesQuery;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/learner/learning-records")
@RequiredArgsConstructor
public class LearnerRecordController {

    private final LearnerRecordService learnerRecordService;

    @GetMapping
    public Result<PageResult<LearningRecordVO>> getRecords(@Valid RecordQuery query) {
        return Result.success(learnerRecordService.getRecords(query));
    }

    @GetMapping("/top-courses")
    public Result<PageResult<TopCourseVO>> getTopCourses(@Valid TopCoursesQuery query) {
        return Result.success(learnerRecordService.getTopCourses(query));
    }

    @GetMapping("/overview")
    public Result<RecordOverviewVO> getOverview(
            @RequestParam(required = false, defaultValue = "TODAY") String range) {
        return Result.success(learnerRecordService.getOverview(range));
    }

    @GetMapping("/calendar")
    public Result<CalendarVO> getCalendar(
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Integer month) {
        return Result.success(learnerRecordService.getCalendar(year, month));
    }

    @GetMapping("/{id}")
    public Result<List<LearningRecordVO>> getRecordDetail(@PathVariable String id) {
        return Result.success(learnerRecordService.getRecordDetail(id));
    }

    @PatchMapping("/{id}/complete")
    public Result<Void> markComplete(@PathVariable String id) {
        learnerRecordService.markComplete(id);
        return Result.success();
    }

    @PatchMapping("/{id}/reset")
    public Result<Void> resetProgress(@PathVariable String id) {
        learnerRecordService.resetProgress(id);
        return Result.success();
    }

    @GetMapping("/stats")
    public Result<RecordStatsVO> getStats(
            @RequestParam(required = false) String range) {
        return Result.success(learnerRecordService.getStats(range));
    }

    @GetMapping("/resource-distribution")
    public Result<List<ResourceDistributionVO>> getResourceDistribution(
            @RequestParam(required = false) String range) {
        return Result.success(learnerRecordService.getResourceDistribution(range));
    }

    @GetMapping("/frequency-trend")
    public Result<FrequencyTrendVO> getFrequencyTrend(
            @RequestParam(required = false) String range) {
        return Result.success(learnerRecordService.getFrequencyTrend(range));
    }
}

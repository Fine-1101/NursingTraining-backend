package org.example.nursingtrainingbackend.modules.dashboard.controller;

import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.dashboard.service.DashboardService;
import org.example.nursingtrainingbackend.modules.dashboard.vo.CourseLearningTrendVO;
import org.example.nursingtrainingbackend.modules.dashboard.vo.DashboardVO;
import org.example.nursingtrainingbackend.modules.dashboard.vo.LearningTrendDrillVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class DashBoardController {

    private final DashboardService dashboardService;
    private final CourseMapper courseMapper;

    @GetMapping("/dashboard")
    public Result<DashboardVO> getDashboard(
            @RequestParam(defaultValue = "LAST_1_WEEKS") String range,
            @RequestParam(defaultValue = "10") int departmentLimit
    ) {
        // 校验 range 参数
        if (!range.matches("LAST_1_WEEKS|LAST_1_MONTHS|LAST_6_MONTHS")) {
            range = "LAST_1_WEEKS";
        }
        // 校验 departmentLimit 范围
        if (departmentLimit < 1) departmentLimit = 1;
        if (departmentLimit > 20) departmentLimit = 20;

        DashboardVO vo = dashboardService.getDashboard(range, departmentLimit);
        return Result.success(vo);
    }

    @GetMapping("/dashboard/course-options")
    public Result<Map<Long, String>> getCourses() {
        Map<Long, String> courseMap = courseMapper.selectMap().stream()
                .collect(Collectors.toMap(
                        m -> ((Number) m.get("id")).longValue(),
                        m -> (String) m.get("title")
                ));
        return Result.success(courseMap);
    }

    @GetMapping("/dashboard/course-learning-trend")
    public Result<CourseLearningTrendVO> getCourseLearningTrend(
            @RequestParam Long courseId,
            @RequestParam String range,
            @RequestParam String granularity
    ) {
        CourseLearningTrendVO vo = dashboardService.getCourseLearningTrend(courseId, range, granularity);
        return Result.success(vo);
    }

    @GetMapping("/dashboard/learning-trend-drill")
    public Result<LearningTrendDrillVO> getLearningTrendDrill(
            @RequestParam int year,
            @RequestParam int month,
            @RequestParam(required = false) Integer weekIndex
    ) {
        LearningTrendDrillVO vo = dashboardService.getLearningTrendDrill(year, month, weekIndex);
        return Result.success(vo);
    }
}

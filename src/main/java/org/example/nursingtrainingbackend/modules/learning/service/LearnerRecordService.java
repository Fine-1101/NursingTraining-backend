package org.example.nursingtrainingbackend.modules.learning.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.learning.dto.RecordQuery;
import org.example.nursingtrainingbackend.modules.learning.vo.*;
import org.example.nursingtrainingbackend.modules.learning.dto.TopCoursesQuery;


import java.util.List;

public interface LearnerRecordService {

    PageResult<LearningRecordVO> getRecords(RecordQuery query);

    PageResult<TopCourseVO> getTopCourses(TopCoursesQuery query);

    RecordOverviewVO getOverview(String range);

    CalendarVO getCalendar(Integer year, Integer month);

    List<LearningRecordVO> getRecordDetail(String id);

    void markComplete(String id);

    void resetProgress(String id);

    RecordStatsVO getStats(String range);

    List<ResourceDistributionVO> getResourceDistribution(String range);

    FrequencyTrendVO getFrequencyTrend(String range);

//    List<TopCourseVO> getTopCourses(String range, Integer limit);
}

package org.example.nursingtrainingbackend.modules.learning.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.learning.dto.LearnerPageQuery;
import org.example.nursingtrainingbackend.modules.learning.vo.ContinueCourseVO;
import org.example.nursingtrainingbackend.modules.learning.vo.HomePageVO;
import org.example.nursingtrainingbackend.modules.learning.vo.LearningRecordVO;
import org.example.nursingtrainingbackend.modules.learning.vo.RecommendedCourseVO;

/**
 * 学员端首页服务接口
 */
public interface LearnerHomeService {

    /**
     * 获取学员首页聚合数据
     * @return 首页完整数据
     */
    HomePageVO getHomePage();

    /**
     * 分页查询推荐课程
     * @param query 分页参数
     * @return 推荐课程分页结果
     */
    PageResult<RecommendedCourseVO> getRecommendedCourses(LearnerPageQuery query);

    /**
     * 分页查询继续学习课程
     * @param query 分页参数
     * @return 继续学习课程分页结果
     */
    PageResult<ContinueCourseVO> getContinueCourses(LearnerPageQuery query);

    /**
     * 分页查询学习记录
     * @param query 分页参数
     * @return 学习记录分页结果
     */
    PageResult<LearningRecordVO> getRecentRecords(LearnerPageQuery query);
}

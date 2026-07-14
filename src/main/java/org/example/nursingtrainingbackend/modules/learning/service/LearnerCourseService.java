package org.example.nursingtrainingbackend.modules.learning.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.learning.dto.LearnerCourseQuery;
import org.example.nursingtrainingbackend.modules.learning.vo.CourseStatsVO;
import org.example.nursingtrainingbackend.modules.learning.vo.LearnerCourseDetailVO;
import org.example.nursingtrainingbackend.modules.learning.vo.LearnerCourseVO;
import org.example.nursingtrainingbackend.modules.learning.vo.StartLearningVO;

/**
 * 学员端课程列表服务接口
 */
public interface LearnerCourseService {

    /**
     * 分页查询学员课程列表
     * @param query 查询参数
     * @return 课程分页结果
     */
    PageResult<LearnerCourseVO> getLearnerCourses(LearnerCourseQuery query);

    /**
     * 获取学员课程统计
     * @return 课程统计数据
     */
    CourseStatsVO getLearnerCourseStats();

    /**
     * 开始学习课程
     * 如果尚无进度记录，创建学习中记录；如果已有记录，直接返回当前状态
     * @param courseId 课程ID
     * @return 开始学习响应
     */
    StartLearningVO startLearning(Long courseId);

    /**
     * 获取课程学习详情（含章节、课程点、课件和进度）
     * @param courseId 课程ID
     * @return 课程学习详情
     */
    LearnerCourseDetailVO getCourseDetail(Long courseId);
}

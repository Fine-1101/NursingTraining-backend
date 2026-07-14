package org.example.nursingtrainingbackend.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.assessment.entity.Assessment;
import org.example.nursingtrainingbackend.modules.assessment.vo.AssessmentListItemVO;

import java.time.LocalDateTime;

@Mapper
public interface AssessmentMapper extends BaseMapper<Assessment> {

    IPage<AssessmentListItemVO> selectAssessmentPage(Page<AssessmentListItemVO> page,
                                                      @Param("keyword") String keyword,
                                                      @Param("courseId") Long courseId,
                                                      @Param("categoryId") Long categoryId,
                                                      @Param("status") Integer status,
                                                      @Param("startFrom") LocalDateTime startFrom,
                                                      @Param("startTo") LocalDateTime startTo);
}

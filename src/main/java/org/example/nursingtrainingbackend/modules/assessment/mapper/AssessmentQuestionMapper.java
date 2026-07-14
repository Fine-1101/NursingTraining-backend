package org.example.nursingtrainingbackend.modules.assessment.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.nursingtrainingbackend.modules.assessment.entity.AssessmentQuestion;

@Mapper
public interface AssessmentQuestionMapper extends BaseMapper<AssessmentQuestion> {
}

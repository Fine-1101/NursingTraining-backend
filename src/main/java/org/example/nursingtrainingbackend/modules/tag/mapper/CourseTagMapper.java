package org.example.nursingtrainingbackend.modules.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.nursingtrainingbackend.modules.tag.entity.CourseTag;

/**
 * 课程-标签关联数据访问层
 */
@Mapper
public interface CourseTagMapper extends BaseMapper<CourseTag> {
}

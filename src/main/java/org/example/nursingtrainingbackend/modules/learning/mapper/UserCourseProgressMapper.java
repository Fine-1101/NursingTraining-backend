package org.example.nursingtrainingbackend.modules.learning.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.nursingtrainingbackend.modules.learning.entity.UserCourseProgress;

@Mapper
public interface UserCourseProgressMapper extends BaseMapper<UserCourseProgress> {
}

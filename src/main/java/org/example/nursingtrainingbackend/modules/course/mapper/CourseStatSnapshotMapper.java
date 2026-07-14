package org.example.nursingtrainingbackend.modules.course.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.nursingtrainingbackend.modules.course.entity.CourseStatSnapshot;

@Mapper
public interface CourseStatSnapshotMapper extends BaseMapper<CourseStatSnapshot> {
}

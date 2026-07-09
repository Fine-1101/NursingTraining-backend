package org.example.nursingtrainingbackend.modules.user.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.nursingtrainingbackend.modules.user.entity.Department;

@Mapper
public interface DepartmentMapper extends BaseMapper<Department> {
}

// 文件路径: src/main/java/org/example/nursingtrainingbackend/modules/category/mapper/CategoryMapper.java
package org.example.nursingtrainingbackend.modules.category.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.example.nursingtrainingbackend.modules.category.entity.Category;

import java.util.List;

@Mapper
public interface CategoryMapper extends BaseMapper<Category> {
    List<Category> selectList();
}

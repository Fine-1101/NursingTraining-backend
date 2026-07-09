package org.example.nursingtrainingbackend.modules.tag.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.tag.entity.Tag;
import org.example.nursingtrainingbackend.modules.tag.vo.TopTagVO;

@Mapper
public interface TagMapper extends BaseMapper<Tag> {

    IPage<TagWithCount> selectPageWithCount(IPage<TagWithCount> page,
                                            @Param("keyword") String keyword,
                                            @Param("status") Integer status,
                                            @Param("sortBy") String sortBy,
                                            @Param("sortOrder") String sortOrder);

    TagWithCount selectDetailWithCount(@Param("id") Long id);

    List<TopTagVO> selectTopTags(@Param("limit") int limit);

    List<TagStatisticsRow> selectTagStatistics();
}

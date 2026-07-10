package org.example.nursingtrainingbackend.modules.courseware.ppt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Select;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.Ppt;

@Mapper
public interface PptMapper extends BaseMapper<Ppt> {

    /**
     * 统计未删除PPT总数
     */
    @Select("SELECT COUNT(*) FROM ppt WHERE deleted_at IS NULL")
    long countTotalPpts();

    /**
     * 统计已发布PPT数
     */
    @Select("SELECT COUNT(*) FROM ppt WHERE status = 1 AND deleted_at IS NULL")
    long countPublishedPpts();

    /**
     * 统计草稿PPT数
     */
    @Select("SELECT COUNT(*) FROM ppt WHERE status = 0 AND deleted_at IS NULL")
    long countDraftPpts();

    /**
     * 统计本月新增PPT数（当月1日至今）
     */
    @Select("SELECT COUNT(*) FROM ppt WHERE deleted_at IS NULL AND created_at >= DATE_FORMAT(NOW(), '%Y-%m-01')")
    long countMonthlyAdded();
}

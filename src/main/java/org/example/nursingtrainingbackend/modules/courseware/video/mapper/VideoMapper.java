package org.example.nursingtrainingbackend.modules.courseware.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.Video;

public interface VideoMapper extends BaseMapper<Video> {
    @Select("SELECT COUNT(*) FROM video WHERE deleted_at IS NULL")
    long countTotalVideos();

    @Select("SELECT COUNT(*) FROM video WHERE status = 1 AND deleted_at IS NULL")
    long countPublishedVideos();

    @Select("SELECT COUNT(*) FROM video WHERE status = 0 AND deleted_at IS NULL")
    long countDraftVideos();

    @Select("SELECT COALESCE(SUM(file_size), 0) FROM video WHERE deleted_at IS NULL")
    long sumFileSizeTotal();
}

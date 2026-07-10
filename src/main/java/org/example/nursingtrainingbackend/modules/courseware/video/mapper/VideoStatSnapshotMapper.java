package org.example.nursingtrainingbackend.modules.courseware.video.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.nursingtrainingbackend.modules.courseware.video.entity.VideoStatSnapshot;

import java.time.LocalDate;

@Mapper
public interface VideoStatSnapshotMapper extends BaseMapper<VideoStatSnapshot> {

    @Update("INSERT INTO video_stat_snapshot (stat_date, total_videos, storage_bytes, published_videos, draft_videos) " +
            "VALUES (#{statDate}, #{totalVideos}, #{storageBytes}, #{publishedVideos}, #{draftVideos}) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_videos = VALUES(total_videos), " +
            "storage_bytes = VALUES(storage_bytes), " +
            "published_videos = VALUES(published_videos), " +
            "draft_videos = VALUES(draft_videos)")
    void upsertSnapshot(@Param("statDate") LocalDate statDate,
                        @Param("totalVideos") long totalVideos,
                        @Param("storageBytes") long storageBytes,
                        @Param("publishedVideos") long publishedVideos,
                        @Param("draftVideos") long draftVideos);

    @Select("SELECT * FROM video_stat_snapshot WHERE stat_date = #{statDate}")
    VideoStatSnapshot selectByDate(@Param("statDate") LocalDate statDate);
}

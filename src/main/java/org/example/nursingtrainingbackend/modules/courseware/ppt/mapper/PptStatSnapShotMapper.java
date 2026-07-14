package org.example.nursingtrainingbackend.modules.courseware.ppt.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.nursingtrainingbackend.modules.courseware.ppt.entity.PptStatSnapshot;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;

public interface PptStatSnapShotMapper extends BaseMapper<PptStatSnapshot> {
    @Update("INSERT INTO ppt_stat_snapshot " +
            "(stat_date, total_ppts, published_ppts, draft_ppts, monthly_added) " +
            "VALUES (#{statDate}, #{totalPpts}, #{publishedPpts}, #{draftPpts}, #{monthlyAdded}) " +
            "ON DUPLICATE KEY UPDATE " +
            "total_ppts = VALUES(total_ppts), " +
            "published_ppts = VALUES(published_ppts), " +
            "draft_ppts = VALUES(draft_ppts), " +
            "monthly_added = VALUES(monthly_added)")
    void upsertSnapshot(@Param("statDate") LocalDate statDate,
                        @Param("totalPpts") long totalPpts,
                        @Param("publishedPpts") long publishedPpts,
                        @Param("draftPpts") long draftPpts,
                        @Param("monthlyAdded") long monthlyAdded);

    @Select("SELECT * FROM ppt_stat_snapshot WHERE stat_date = #{statDate}")
    PptStatSnapshot selectByDate(@Param("statDate") LocalDate statDate);

}

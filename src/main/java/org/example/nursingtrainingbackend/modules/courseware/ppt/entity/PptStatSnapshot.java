package org.example.nursingtrainingbackend.modules.courseware.ppt.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDate;

@Data
@TableName("ppt_stat_snapshot")
public class PptStatSnapshot {

    private LocalDate statDate;

    private Long totalPpts;

    private Long publishedPpts;

    private Long draftPpts;

    private Long monthlyAdded;
}
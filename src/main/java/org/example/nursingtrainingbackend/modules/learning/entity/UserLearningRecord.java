package org.example.nursingtrainingbackend.modules.learning.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("user_learning_record")
public class UserLearningRecord {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    private Long courseId;

    private Long coursePointId;

    private Integer resourceType;

    private Long resourceId;

    private Integer actionType;

    private String title;

    private String description;

    private LocalDateTime createdAt;
}

package org.example.nursingtrainingbackend.modules.user.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 科室实体类
 */
@Data
@TableName("department")
public class Department {

    /**
     * 科室ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 科室名称
     */
    private String name;

    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;
}

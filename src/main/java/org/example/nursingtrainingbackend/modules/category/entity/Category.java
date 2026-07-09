package org.example.nursingtrainingbackend.modules.category.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 培训分类实体类
 */
@Data
@TableName("category")
public class Category {

    /**
     * 分类ID
     */
    @TableId(type = IdType.AUTO)
    private Long id;

    /**
     * 分类名称
     */
    private String name;

    /**
     * 父分类ID，0表示顶级分类
     */
    private Long parentId;

    /**
     * 层级：1-顶级 2-二级 3-三级
     */
    private Integer level;

    /**
     * 同级排序号，越小越靠前
     */
    private Integer sort;

    /**
     * 图标
     */
    private String icon;

    /**
     * 状态：0-停用 1-启用
     */
    private Integer status;

    /**
     * 创建时间
     */
    private LocalDateTime createdAt;

    /**
     * 更新时间
     */
    private LocalDateTime updatedAt;

    /**
     * 软删除时间
     */
    @TableLogic
    private LocalDateTime deletedAt;
}

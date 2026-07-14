package org.example.nursingtrainingbackend.modules.courseware.video.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class VideoUploadRequest {

    @NotBlank(message = "视频标题不能为空")
    @Size(max = 1200, message = "视频标题长度不能超过1200")
    private String title;

    @Size(max = 2000, message = "视频描述长度不能超过2000")
    private String description;

    @NotBlank(message = "视频地址不能为空")
    @Size(max = 500, message = "视频地址长度不能超过500")
    private String videoUrl;

    @NotBlank(message = "原始文件名不能为空")
    @Size(max = 255, message = "原始文件名长度不能超过255")
    private String originalName;

    @Size(max = 500, message = "封面地址长度不能超过500")
    private String coverUrl;

    @NotNull(message = "视频时长不能为空")
    @Min(value = 1, message = "视频时长必须大于0")
    private Integer duration;

    @NotNull(message = "文件大小不能为空")
    @Min(value = 1, message = "文件大小必须大于0")
    private Long fileSize;

    private Boolean allowDrag = false;

    private Boolean allowSpeed = true;

    private Boolean allowCache = true;

    private String status = "DRAFT";
}

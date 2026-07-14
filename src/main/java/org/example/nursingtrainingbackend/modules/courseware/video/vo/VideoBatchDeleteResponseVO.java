package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

@Data
public class VideoBatchDeleteResponseVO {

    private Integer requestedCount;

    private Integer deletedCount;
}

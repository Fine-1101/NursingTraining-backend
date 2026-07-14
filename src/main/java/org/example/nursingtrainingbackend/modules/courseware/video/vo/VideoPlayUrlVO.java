package org.example.nursingtrainingbackend.modules.courseware.video.vo;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class VideoPlayUrlVO {

    private String playUrl;

    private String contentType;

    private Integer duration;

    private LocalDateTime expireAt;
}

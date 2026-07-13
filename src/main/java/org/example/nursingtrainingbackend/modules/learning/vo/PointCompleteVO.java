package org.example.nursingtrainingbackend.modules.learning.vo;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PointCompleteVO {

    private Long pointId;

    private String learningStatus;

    private Boolean completed;

    private Boolean courseCompleted;

    private List<UnfinishedResourceVO> unfinishedResources;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class UnfinishedResourceVO {
        private String resourceType;
        private Long resourceId;
        private String title;
    }
}

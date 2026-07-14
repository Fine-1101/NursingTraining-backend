package org.example.nursingtrainingbackend.modules.file.job;

import com.aliyun.oss.OSS;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.nursingtrainingbackend.common.annotation.DistributedLock;
import org.example.nursingtrainingbackend.config.OssConfig;
import org.example.nursingtrainingbackend.modules.file.entity.FileUploadRecord;
import org.example.nursingtrainingbackend.modules.file.mapper.FileUploadRecordMapper;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * 孤立文件清理定时任务
 * 每天凌晨2点执行，清理上传成功但未被业务引用的OSS文件
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class OrphanFileCleanupJob {

    private final FileUploadRecordMapper fileUploadRecordMapper;
    private final OSS ossClient;
    private final OssConfig ossConfig;

    /**
     * 每天凌晨2点执行
     */
    @Scheduled(cron = "0 0 2 * * ?")
    @DistributedLock(key = "job:orphan_file_cleanup", leaseTime = 600, waitTime = 120)
    @Transactional(rollbackFor = Exception.class)
    public void cleanup() {
        log.info("开始执行孤立文件清理任务");

        // 计算24小时前的时间点
        LocalDateTime beforeTime = LocalDateTime.now().minusHours(24);
        List<FileUploadRecord> unusedRecords = fileUploadRecordMapper.selectUnusedRecordsBefore(beforeTime);

        if (unusedRecords.isEmpty()) {
            log.info("没有需要清理的孤立文件");
            return;
        }

        int successCount = 0;
        int failCount = 0;

        for (FileUploadRecord record : unusedRecords) {
            try {
                // 删除OSS文件
                ossClient.deleteObject(ossConfig.getBucketName(), record.getObjectKey());

                // 软删除记录
                record.setDeletedAt(LocalDateTime.now());
                fileUploadRecordMapper.updateById(record);

                successCount++;
                log.debug("已清理孤立文件: objectKey={}", record.getObjectKey());
            } catch (Exception e) {
                failCount++;
                log.error("清理孤立文件失败: objectKey={}", record.getObjectKey(), e);
            }
        }

        log.info("孤立文件清理任务完成: 总数={}, 成功={}, 失败={}", unusedRecords.size(), successCount, failCount);
    }
}

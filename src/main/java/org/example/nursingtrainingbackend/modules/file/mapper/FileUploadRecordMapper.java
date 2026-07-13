package org.example.nursingtrainingbackend.modules.file.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.file.entity.FileUploadRecord;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface FileUploadRecordMapper extends BaseMapper<FileUploadRecord> {

    /**
     * 查询未绑定且超过指定时间的记录
     */
    List<FileUploadRecord> selectUnusedRecordsBefore(@Param("beforeTime") LocalDateTime beforeTime);
}

package org.example.nursingtrainingbackend.modules.message.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;
import org.example.nursingtrainingbackend.modules.message.entity.CourseStudentMessage;

@Mapper
public interface CourseStudentMessageMapper extends BaseMapper<CourseStudentMessage> {

    @Update("UPDATE course_student_message SET read_at = COALESCE(read_at, NOW()) " +
            "WHERE id = #{messageId} AND receiver_id = #{receiverId} AND deleted_at IS NULL")
    int markAsRead(@Param("messageId") Long messageId, @Param("receiverId") Long receiverId);

    @Update("UPDATE course_student_message SET read_at = NOW() " +
            "WHERE receiver_id = #{receiverId} AND read_at IS NULL AND deleted_at IS NULL")
    int markAllAsRead(@Param("receiverId") Long receiverId);
}

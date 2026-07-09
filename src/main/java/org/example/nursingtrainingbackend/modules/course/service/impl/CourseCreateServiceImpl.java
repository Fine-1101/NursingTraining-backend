package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.vo.InstructorOptionVO;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
public class CourseCreateServiceImpl implements CourseCreateService {
    @Autowired
    private UserMapper userMapper;
    @Override
    public List<InstructorOptionVO> getInstructorOptions(String keyword,Integer limit) {
        //选讲师应该要真实姓名
       User user=userMapper.selectOne(Wrappers.<User>lambdaQuery().like(StringUtils.hasText(keyword), User::getRealName, keyword));
        return List.of();
    }
}

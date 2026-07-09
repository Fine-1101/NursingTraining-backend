package org.example.nursingtrainingbackend.modules.course.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.dto.DepartmentDTO;
import org.example.nursingtrainingbackend.modules.course.entity.Course;
import org.example.nursingtrainingbackend.modules.course.entity.CourseDepartment;
import org.example.nursingtrainingbackend.modules.course.entity.CourseTag;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseDepartmentMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseMapper;
import org.example.nursingtrainingbackend.modules.course.mapper.CourseTagMapper;
import org.example.nursingtrainingbackend.modules.course.service.CourseCreateService;
import org.example.nursingtrainingbackend.modules.course.vo.CreateCourseInitialVO;
import org.example.nursingtrainingbackend.modules.course.vo.DepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.course.vo.InstructorOptionVO;
import org.example.nursingtrainingbackend.modules.tag.entity.Tag;
import org.example.nursingtrainingbackend.modules.user.entity.Department;
import org.example.nursingtrainingbackend.modules.user.entity.User;
import org.example.nursingtrainingbackend.modules.user.mapper.DepartmentMapper;
import org.example.nursingtrainingbackend.modules.user.mapper.UserMapper;
import org.example.nursingtrainingbackend.security.SecurityUtils;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
public class CourseCreateServiceImpl implements CourseCreateService {
    @Autowired
    private UserMapper userMapper;
    @Autowired
    private DepartmentMapper departmentMapper;
    @Autowired
    private CourseMapper courseMapper;
    @Autowired
    private CourseDepartmentMapper courseDepartmentMapper;
    @Autowired
    private CourseTagMapper courseTagMapper;

    @Override
    public List<InstructorOptionVO> getInstructorOptions(String keyword, Integer limit) {
        //选讲师应该要真实姓名
        List<User> users = userMapper.selectList(
                Wrappers.<User>lambdaQuery()
                        .like(StringUtils.hasText(keyword), User::getRealName, keyword)
        );

        return users.stream().map(user -> {
            InstructorOptionVO vo = new InstructorOptionVO();
            vo.setId(user.getId());
            vo.setRealname(user.getRealName());
            vo.setUsername(user.getUsername());
            vo.setDepartmentName(user.getDeptId().toString());
            return vo;
        }).toList();

    }

    @Override
    public List<DepartmentOptionVO> getDepartmentOptions() {
        List<Department> departments = departmentMapper.selectList(null);
        return departments.stream().map(department -> {
            DepartmentOptionVO vo = new DepartmentOptionVO();
            vo.setId(department.getId());
            vo.setName(department.getName());
            return vo;
        }).toList();
    }

    @Override
    @Transactional
    public CreateCourseInitialVO createCourseInitial(CreateCourseInitial createCourseInitial) {
        Course course = new Course();
        BeanUtils.copyProperties(createCourseInitial, course);
        course.setCreatedAt(LocalDateTime.now());
        course.setCreatedBy(SecurityUtils.currentUserId());
        if (createCourseInitial.getDepartments() != null) {
            int i=0;
            for (DepartmentDTO departmentDTO : createCourseInitial.getDepartments()) {
                CourseDepartment courseDepartment = new CourseDepartment();
                courseDepartment.setCourseId(course.getId());
                courseDepartment.setDepartmentId(createCourseInitial.getDepartments().get(i).getId());
                courseDepartment.setRequired(createCourseInitial.getDepartments().get(i).getRequired() ? 1 : 0);
                courseDepartment.setCreatedAt(LocalDateTime.now());
                courseDepartmentMapper.insert(courseDepartment);
                i++;
            }
        }
        if(createCourseInitial.getTagIds()!=null){
        for(Long tagId:createCourseInitial.getTagIds()){
            CourseTag courseTag = new CourseTag();
            courseTag.setCourseId(course.getId());
            courseTag.setTagId(tagId);
            courseTag.setCreatedAt(LocalDateTime.now());
            courseTagMapper.insert(courseTag);
        }
        }
        courseMapper.insert(course);
        CreateCourseInitialVO vo = new CreateCourseInitialVO();
        vo.setCourseId(course.getId());
        vo.setCreatedAt(course.getCreatedAt());
        return vo;
    }
}
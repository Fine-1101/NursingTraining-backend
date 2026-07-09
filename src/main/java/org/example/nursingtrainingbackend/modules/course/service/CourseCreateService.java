package org.example.nursingtrainingbackend.modules.course.service;

import org.example.nursingtrainingbackend.modules.course.dto.CreateCourseInitial;
import org.example.nursingtrainingbackend.modules.course.vo.CreateCourseInitialVO;
import org.example.nursingtrainingbackend.modules.course.vo.DepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.course.vo.InstructorOptionVO;

import java.util.List;

public interface CourseCreateService {
    List<InstructorOptionVO> getInstructorOptions(String a,Integer b);

    List<DepartmentOptionVO> getDepartmentOptions();

    CreateCourseInitialVO createCourseInitial(CreateCourseInitial createCourseInitial);
}

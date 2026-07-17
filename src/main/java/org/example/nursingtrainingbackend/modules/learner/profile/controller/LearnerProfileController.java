package org.example.nursingtrainingbackend.modules.learner.profile.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.learner.profile.dto.LearnerPasswordUpdateRequest;
import org.example.nursingtrainingbackend.modules.learner.profile.dto.LearnerProfileUpdateRequest;
import org.example.nursingtrainingbackend.modules.learner.profile.service.LearnerProfileService;
import org.example.nursingtrainingbackend.modules.learner.profile.vo.LearnerDepartmentOptionVO;
import org.example.nursingtrainingbackend.modules.learner.profile.vo.LearnerProfileVO;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/learner/profile")
@RequiredArgsConstructor
public class LearnerProfileController {

    private final LearnerProfileService learnerProfileService;

    @GetMapping
    public Result<LearnerProfileVO> getProfile() {
        return Result.success(learnerProfileService.getProfile());
    }

    @PutMapping
    public Result<LearnerProfileVO> updateProfile(@Valid @RequestBody LearnerProfileUpdateRequest request) {
        return Result.success(learnerProfileService.updateProfile(request));
    }

    @PutMapping("/password")
    public Result<Void> updatePassword(@Valid @RequestBody LearnerPasswordUpdateRequest request) {
        learnerProfileService.updatePassword(request);
        return Result.success();
    }

    @GetMapping("/departments")
    public Result<List<LearnerDepartmentOptionVO>> getDepartments() {
        return Result.success(learnerProfileService.getDepartments());
    }
}

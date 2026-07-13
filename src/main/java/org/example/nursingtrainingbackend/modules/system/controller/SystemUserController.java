package org.example.nursingtrainingbackend.modules.system.controller;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.common.result.Result;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserCreateDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserQueryDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserStatusDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserUpdateDTO;
import org.example.nursingtrainingbackend.modules.system.service.SystemUserService;
import org.example.nursingtrainingbackend.modules.system.vo.SystemUserItemVO;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/users")
@RequiredArgsConstructor
public class SystemUserController {

    private final SystemUserService systemUserService;

    @GetMapping
    public Result<PageResult<SystemUserItemVO>> queryPage(@Valid SystemUserQueryDTO query) {
        return Result.success(systemUserService.queryPage(query));
    }

    @GetMapping("/{id}")
    public Result<SystemUserItemVO> getDetail(@PathVariable Long id) {
        return Result.success(systemUserService.getDetail(id));
    }

    @PostMapping
    public Result<SystemUserItemVO> createUser(@RequestBody @Valid SystemUserCreateDTO dto) {
        return Result.success(systemUserService.createUser(dto));
    }

    @PutMapping("/{id}")
    public Result<SystemUserItemVO> updateUser(@PathVariable Long id,
                                               @RequestBody @Valid SystemUserUpdateDTO dto) {
        return Result.success(systemUserService.updateUser(id, dto));
    }

    @PatchMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id,
                                     @RequestBody @Valid SystemUserStatusDTO dto) {
        systemUserService.updateStatus(id, dto);
        return Result.success();
    }

    @DeleteMapping("/{id}")
    public Result<Void> deleteUser(@PathVariable Long id) {
        systemUserService.deleteUser(id);
        return Result.success();
    }

    @PatchMapping("/{id}/reset-password")
    public Result<Void> resetPassword(@PathVariable Long id,
                                      @RequestParam String newPassword) {
        systemUserService.resetPassword(id, newPassword);
        return Result.success();
    }
}

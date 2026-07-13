package org.example.nursingtrainingbackend.modules.system.service;

import org.example.nursingtrainingbackend.common.page.PageResult;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserCreateDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserQueryDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserStatusDTO;
import org.example.nursingtrainingbackend.modules.system.dto.SystemUserUpdateDTO;
import org.example.nursingtrainingbackend.modules.system.vo.SystemUserItemVO;

public interface SystemUserService {

    PageResult<SystemUserItemVO> queryPage(SystemUserQueryDTO query);

    SystemUserItemVO getDetail(Long id);

    SystemUserItemVO createUser(SystemUserCreateDTO dto);

    SystemUserItemVO updateUser(Long id, SystemUserUpdateDTO dto);

    void updateStatus(Long id, SystemUserStatusDTO dto);

    void deleteUser(Long id);

    void resetPassword(Long id, String newPassword);
}

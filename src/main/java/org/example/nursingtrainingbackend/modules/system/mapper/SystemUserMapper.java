package org.example.nursingtrainingbackend.modules.system.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.example.nursingtrainingbackend.modules.system.vo.SystemUserItemVO;
import org.example.nursingtrainingbackend.modules.user.entity.User;

@Mapper
public interface SystemUserMapper extends BaseMapper<User> {

    IPage<SystemUserItemVO> selectPageUsers(IPage<SystemUserItemVO> page,
                                            @Param("keyword") String keyword,
                                            @Param("status") Integer status,
                                            @Param("roleType") Integer roleType);
}

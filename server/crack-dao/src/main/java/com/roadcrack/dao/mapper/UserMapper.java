package com.roadcrack.dao.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.roadcrack.dao.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {

    @Select("SELECT r.code FROM role r " +
            "INNER JOIN user_role ur ON r.id = ur.role_id " +
            "WHERE ur.user_id = #{userId} AND r.status = 1 AND r.deleted = 0")
    List<String> selectRoleCodesByUserId(@Param("userId") Long userId);

    @Select("SELECT ur.role_id FROM user_role ur WHERE ur.user_id = #{userId}")
    List<Long> selectRoleIdsByUserId(@Param("userId") Long userId);
}

package com.roadcrack.service.service;

import com.roadcrack.api.request.user.UserPageQuery;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.dao.entity.UserEntity;

import java.util.List;
import com.roadcrack.dao.entity.UserEntity;

public interface UserService {

    PageResponse<UserEntity> pageQuery(UserPageQuery query);

    UserEntity getById(Long id);

    void createUser(UserEntity user, List<Long> roleIds);

    void updateUser(UserEntity user, List<Long> roleIds);

    void deleteUser(Long id);

    void toggleStatus(Long id, Integer status);

    void resetUserPassword(Long id);

    List<Long> getUserRoleIds(Long userId);

    List<String> getUserRoleCodes(Long userId);

    UserEntity findByUsername(String username);
}

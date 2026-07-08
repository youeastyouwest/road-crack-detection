package com.roadcrack.service.service;

import com.roadcrack.dao.entity.RoleEntity;

import java.util.List;

public interface RoleService {

    List<RoleEntity> listEnabledRoles();

    RoleEntity getById(Long id);

    void createRole(RoleEntity role);

    void updateRole(RoleEntity role);

    void deleteRole(Long id);
}

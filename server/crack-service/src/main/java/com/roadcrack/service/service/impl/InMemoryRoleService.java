package com.roadcrack.service.service.impl;

import com.roadcrack.dao.entity.RoleEntity;
import com.roadcrack.service.service.RoleService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryRoleService implements RoleService {
    @Override public List<RoleEntity> listEnabledRoles() { return new ArrayList<>(); }
    @Override public RoleEntity getById(Long id) { return null; }
    @Override public void createRole(RoleEntity entity) {}
    @Override public void updateRole(RoleEntity entity) {}
    @Override public void deleteRole(Long id) {}
}
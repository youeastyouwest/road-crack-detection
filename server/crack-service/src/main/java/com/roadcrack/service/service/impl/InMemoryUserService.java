package com.roadcrack.service.service.impl;

import com.roadcrack.api.request.user.UserPageQuery;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.service.service.UserService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.Collections;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryUserService implements UserService {
    @Override public PageResponse<UserEntity> pageQuery(UserPageQuery query) {
        return new PageResponse<>(Collections.emptyList(), 0L, query.getSize(), query.getPage(), 0L);
    }
    @Override public UserEntity getById(Long id) { return null; }
    @Override public void createUser(UserEntity user, List<Long> roleIds) {}
    @Override public void updateUser(UserEntity user, List<Long> roleIds) {}
    @Override public void deleteUser(Long id) {}
    @Override public void toggleStatus(Long id, Integer status) {}
    @Override public void resetUserPassword(Long id) {}
    @Override public List<Long> getUserRoleIds(Long userId) { return Collections.emptyList(); }
    @Override public List<String> getUserRoleCodes(Long userId) { return Collections.emptyList(); }
}
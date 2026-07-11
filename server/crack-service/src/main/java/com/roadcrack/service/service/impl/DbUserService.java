

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.roadcrack.api.request.user.UserPageQuery;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.dao.entity.UserRoleEntity;
import com.roadcrack.dao.mapper.UserMapper;
import com.roadcrack.dao.mapper.UserRoleMapper;
import com.roadcrack.service.service.UserService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")public class DbUserService implements UserService {

    private static final String DEFAULT_PASSWORD = "123456";

    private final UserMapper userMapper;
    private final UserRoleMapper userRoleMapper;
    private final PasswordEncoder passwordEncoder = new BCryptPasswordEncoder();

    public DbUserService(UserMapper userMapper, UserRoleMapper userRoleMapper) {
        this.userMapper = userMapper;
        this.userRoleMapper = userRoleMapper;
    }

    @Override
    public PageResponse<UserEntity> pageQuery(UserPageQuery query) {
        int page = query.getPage() == null || query.getPage() < 1 ? 1 : query.getPage();
        int size = query.getSize() == null || query.getSize() < 1 ? 10 : query.getSize();

        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>()
                .orderByDesc(UserEntity::getCreateTime);
        if (hasText(query.getUsername())) {
            wrapper.like(UserEntity::getUsername, query.getUsername());
        }
        if (hasText(query.getRealName())) {
            wrapper.like(UserEntity::getRealName, query.getRealName());
        }
        if (hasText(query.getPhone())) {
            wrapper.eq(UserEntity::getPhone, query.getPhone());
        }
        if (query.getStatus() != null) {
            wrapper.eq(UserEntity::getStatus, query.getStatus());
        }
        if (query.getDeptId() != null) {
            wrapper.eq(UserEntity::getDeptId, query.getDeptId());
        }

        Page<UserEntity> result = userMapper.selectPage(new Page<>(page, size), wrapper);
        return new PageResponse<>(result.getRecords(), result.getTotal(), result.getSize(), result.getCurrent(), result.getPages());
    }

    @Override
    public UserEntity getById(Long id) {
        return userMapper.selectById(id);
    }

    @Override
    @Transactional
    public void createUser(UserEntity user, List<Long> roleIds) {
        if (countByUsername(user.getUsername(), null) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "username already exists");
        }

        if (hasText(user.getEmail()) && countByEmail(user.getEmail(), null) > 0) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS, "email already exists");
        }

        String rawPassword = hasText(user.getPassword()) ? user.getPassword() : DEFAULT_PASSWORD;
        user.setPassword(passwordEncoder.encode(rawPassword));
        if (user.getStatus() == null) {
            user.setStatus(1);
        }
        if (user.getLoginFailCount() == null) {
            user.setLoginFailCount(0);
        }
        userMapper.insert(user);
        replaceRoles(user.getId(), roleIds);
    }

    @Override
    @Transactional
    public void updateUser(UserEntity user, List<Long> roleIds) {
        UserEntity existing = requireUser(user.getId());

        if (hasText(user.getUsername()) && countByUsername(user.getUsername(), user.getId()) > 0) {
            throw new BusinessException(ResultCode.USER_EXISTS, "username already exists");
        }

        if (hasText(user.getEmail()) && countByEmail(user.getEmail(), user.getId()) > 0) {
            throw new BusinessException(ResultCode.EMAIL_EXISTS, "email already exists");
        }

        user.setPassword(existing.getPassword());
        user.setLoginFailCount(existing.getLoginFailCount());
        user.setLockUntil(existing.getLockUntil());
        user.setLastLoginTime(existing.getLastLoginTime());
        user.setLastLoginIp(existing.getLastLoginIp());
        userMapper.updateById(user);

        if (roleIds != null) {
            replaceRoles(user.getId(), roleIds);
        }
    }

    @Override
    @Transactional
    public void deleteUser(Long id) {
        UserEntity user = requireUser(id);
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, id));
        userMapper.deleteById(id);
    }

    @Override
    public void toggleStatus(Long id, Integer status) {
        UserEntity user = requireUser(id);
        user.setStatus(status);
        if (Integer.valueOf(1).equals(status)) {
            user.setLoginFailCount(0);
            user.setLockUntil(null);
        }
        userMapper.updateById(user);
    }

    @Override
    public void resetUserPassword(Long id) {
        UserEntity user = requireUser(id);
        user.setPassword(passwordEncoder.encode(DEFAULT_PASSWORD));
        user.setStatus(1);
        user.setLoginFailCount(0);
        user.setLockUntil(null);
        userMapper.updateById(user);
    }

    @Override
    public List<Long> getUserRoleIds(Long userId) {
        requireUser(userId);
        return userMapper.selectRoleIdsByUserId(userId);
    }

    @Override
    public List<String> getUserRoleCodes(Long userId) {
        requireUser(userId);
        return userMapper.selectRoleCodesByUserId(userId);
    }

    private UserEntity requireUser(Long id) {
        UserEntity user = userMapper.selectById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not found: " + id);
        }
        return user;
    }

    private void replaceRoles(Long userId, List<Long> roleIds) {
        userRoleMapper.delete(new LambdaQueryWrapper<UserRoleEntity>().eq(UserRoleEntity::getUserId, userId));
        if (roleIds == null || roleIds.isEmpty()) {
            return;
        }
        for (Long roleId : roleIds) {
            UserRoleEntity relation = new UserRoleEntity();
            relation.setUserId(userId);
            relation.setRoleId(roleId);
            relation.setCreateTime(LocalDateTime.now());
            userRoleMapper.insert(relation);
        }
    }

    private long countByUsername(String username, Long excludeId) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getUsername, username);
        if (excludeId != null) {
            wrapper.ne(UserEntity::getId, excludeId);
        }
        return userMapper.selectCount(wrapper);
    }

    private long countByEmail(String email, Long excludeId) {
        LambdaQueryWrapper<UserEntity> wrapper = new LambdaQueryWrapper<UserEntity>().eq(UserEntity::getEmail, email);
        if (excludeId != null) {
            wrapper.ne(UserEntity::getId, excludeId);
        }
        return userMapper.selectCount(wrapper);
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

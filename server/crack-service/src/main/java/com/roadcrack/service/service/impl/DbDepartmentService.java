package com.roadcrack.service.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.DepartmentEntity;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.dao.mapper.DepartmentMapper;
import com.roadcrack.dao.mapper.UserMapper;
import com.roadcrack.service.service.DepartmentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DbDepartmentService implements DepartmentService {

    private final DepartmentMapper departmentMapper;
    private final UserMapper userMapper;

    public DbDepartmentService(DepartmentMapper departmentMapper, UserMapper userMapper) {
        this.departmentMapper = departmentMapper;
        this.userMapper = userMapper;
    }

    @Override
    public List<DepartmentEntity> listEnabledDepartments() {
        return departmentMapper.selectList(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getStatus, 1)
                .orderByAsc(DepartmentEntity::getSortOrder)
                .orderByAsc(DepartmentEntity::getId));
    }

    @Override
    public List<DepartmentEntity> getDepartmentTree() {
        return listEnabledDepartments();
    }

    @Override
    public DepartmentEntity getById(Long id) {
        return departmentMapper.selectById(id);
    }

    @Override
    public void createDepartment(DepartmentEntity department) {
        if (countByCode(department.getCode(), null) > 0) {
            throw new BusinessException(ResultCode.DEPT_CODE_EXISTS, "department code already exists");
        }
        if (department.getStatus() == null) {
            department.setStatus(1);
        }
        departmentMapper.insert(department);
    }

    @Override
    public void updateDepartment(DepartmentEntity department) {
        requireDepartment(department.getId());
        if (countByCode(department.getCode(), department.getId()) > 0) {
            throw new BusinessException(ResultCode.DEPT_CODE_EXISTS, "department code already exists");
        }
        departmentMapper.updateById(department);
    }

    @Override
    public void deleteDepartment(Long id) {
        DepartmentEntity department = requireDepartment(id);

        long childrenCount = departmentMapper.selectCount(new LambdaQueryWrapper<DepartmentEntity>()
                .eq(DepartmentEntity::getParentId, id));
        if (childrenCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_CHILDREN, "department still has child departments");
        }

        long userCount = userMapper.selectCount(new LambdaQueryWrapper<UserEntity>()
                .eq(UserEntity::getDeptId, id));
        if (userCount > 0) {
            throw new BusinessException(ResultCode.DEPT_HAS_USERS, "department still has users");
        }

        departmentMapper.deleteById(department.getId());
    }

    private DepartmentEntity requireDepartment(Long id) {
        DepartmentEntity department = departmentMapper.selectById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND, "department not found: " + id);
        }
        return department;
    }

    private long countByCode(String code, Long excludeId) {
        LambdaQueryWrapper<DepartmentEntity> wrapper = new LambdaQueryWrapper<DepartmentEntity>().eq(DepartmentEntity::getCode, code);
        if (excludeId != null) {
            wrapper.ne(DepartmentEntity::getId, excludeId);
        }
        return departmentMapper.selectCount(wrapper);
    }
}

package com.roadcrack.service.service.impl;

import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.DepartmentEntity;
import com.roadcrack.service.service.DepartmentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicLong;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryDepartmentService implements DepartmentService {

    private final List<DepartmentEntity> departments = new ArrayList<>();
    private final AtomicLong idGen = new AtomicLong(0);

    public InMemoryDepartmentService() {
        seedDepartments();
    }

    private void seedDepartments() {
        addDept(1L, "道路巡检中心", "ROAD_ADMIN", 0L, "负责全市道路巡检和病害检测管理", 1, 1);
        addDept(2L, "道路巡检一科", "ROAD_INSPECT_1", 1L, "负责城东片区道路巡检", 1, 1);
        addDept(3L, "道路巡检二科", "ROAD_INSPECT_2", 1L, "负责城西片区道路巡检", 1, 2);
        addDept(4L, "环卫管理科", "SANITATION", 0L, "负责道路保洁和环卫设施管理", 1, 2);
        addDept(5L, "维修作业科", "MAINTENANCE", 0L, "负责道路病害维修和养护作业", 1, 3);
    }

    private void addDept(Long id, String name, String code, Long parentId, String desc, int status, int sortOrder) {
        DepartmentEntity d = new DepartmentEntity();
        d.setId(id);
        d.setName(name);
        d.setCode(code);
        d.setParentId(parentId);
        d.setDescription(desc);
        d.setStatus(status);
        d.setSortOrder(sortOrder);
        d.setCreateTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        d.setUpdateTime(LocalDateTime.of(2025, 1, 1, 9, 0));
        departments.add(d);
        if (idGen.get() < id) idGen.set(id);
    }

    @Override
    public List<DepartmentEntity> listEnabledDepartments() {
        return new ArrayList<>(departments);
    }

    @Override
    public List<DepartmentEntity> getDepartmentTree() {
        return new ArrayList<>(departments);
    }

    @Override
    public DepartmentEntity getById(Long id) {
        return departments.stream().filter(d -> d.getId().equals(id)).findFirst().orElse(null);
    }

    @Override
    public void createDepartment(DepartmentEntity dept) {
        if (departments.stream().anyMatch(d -> d.getCode().equalsIgnoreCase(dept.getCode()))) {
            throw new BusinessException(ResultCode.DEPT_CODE_EXISTS, "部门编码已存在: " + dept.getCode());
        }
        dept.setId(idGen.incrementAndGet());
        if (dept.getStatus() == null) dept.setStatus(1);
        if (dept.getSortOrder() == null) dept.setSortOrder(99);
        dept.setCreateTime(LocalDateTime.now());
        dept.setUpdateTime(LocalDateTime.now());
        departments.add(dept);
    }

    @Override
    public void updateDepartment(DepartmentEntity dept) {
        DepartmentEntity existing = requireDept(dept.getId());
        if (departments.stream().anyMatch(d -> !d.getId().equals(dept.getId()) && d.getCode().equalsIgnoreCase(dept.getCode()))) {
            throw new BusinessException(ResultCode.DEPT_CODE_EXISTS, "部门编码已存在: " + dept.getCode());
        }
        if (dept.getName() != null) existing.setName(dept.getName());
        if (dept.getCode() != null) existing.setCode(dept.getCode());
        if (dept.getParentId() != null) existing.setParentId(dept.getParentId());
        if (dept.getDescription() != null) existing.setDescription(dept.getDescription());
        if (dept.getStatus() != null) existing.setStatus(dept.getStatus());
        if (dept.getSortOrder() != null) existing.setSortOrder(dept.getSortOrder());
        existing.setUpdateTime(LocalDateTime.now());
    }

    @Override
    public void deleteDepartment(Long id) {
        DepartmentEntity dept = requireDept(id);
        // 检查子部门
        if (departments.stream().anyMatch(d -> id.equals(d.getParentId()))) {
            throw new BusinessException(ResultCode.DEPT_HAS_CHILDREN, "部门下存在子部门，无法删除");
        }
        // 内置部门不可删除
        if (dept.getId() <= 5L) {
            throw new BusinessException(ResultCode.DEPT_HAS_USERS, "内置部门不可删除");
        }
        departments.removeIf(d -> d.getId().equals(id));
    }

    private DepartmentEntity requireDept(Long id) {
        Optional<DepartmentEntity> opt = departments.stream().filter(d -> d.getId().equals(id)).findFirst();
        if (opt.isEmpty()) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND, "部门不存在: " + id);
        }
        return opt.get();
    }
}

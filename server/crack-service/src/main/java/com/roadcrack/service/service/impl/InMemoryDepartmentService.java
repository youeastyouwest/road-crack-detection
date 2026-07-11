package com.roadcrack.service.service.impl;

import com.roadcrack.dao.entity.DepartmentEntity;
import com.roadcrack.service.service.DepartmentService;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;

@Service
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "memory", matchIfMissing = true)
public class InMemoryDepartmentService implements DepartmentService {
    @Override public List<DepartmentEntity> listEnabledDepartments() { return new ArrayList<>(); }
    @Override public List<DepartmentEntity> getDepartmentTree() { return new ArrayList<>(); }
    @Override public DepartmentEntity getById(Long id) { return null; }
    @Override public void createDepartment(DepartmentEntity entity) {}
    @Override public void updateDepartment(DepartmentEntity entity) {}
    @Override public void deleteDepartment(Long id) {}
}
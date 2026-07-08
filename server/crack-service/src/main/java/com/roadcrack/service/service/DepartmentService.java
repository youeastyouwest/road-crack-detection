package com.roadcrack.service.service;

import com.roadcrack.dao.entity.DepartmentEntity;

import java.util.List;

public interface DepartmentService {

    List<DepartmentEntity> listEnabledDepartments();

    List<DepartmentEntity> getDepartmentTree();

    DepartmentEntity getById(Long id);

    void createDepartment(DepartmentEntity department);

    void updateDepartment(DepartmentEntity department);

    void deleteDepartment(Long id);
}

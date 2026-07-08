package com.roadcrack.bootstrap.controller;

import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.DepartmentEntity;
import com.roadcrack.service.service.DepartmentService;
import jakarta.validation.Valid;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/department")
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class DepartmentController {

    private final DepartmentService departmentService;

    public DepartmentController(DepartmentService departmentService) {
        this.departmentService = departmentService;
    }

    @GetMapping("/tree")
    public ApiResponse<List<DepartmentEntity>> tree() {
        return ApiResponse.success(departmentService.getDepartmentTree());
    }

    @GetMapping("/list")
    public ApiResponse<List<DepartmentEntity>> list() {
        return ApiResponse.success(departmentService.listEnabledDepartments());
    }

    @GetMapping("/{id}")
    public ApiResponse<DepartmentEntity> detail(@PathVariable Long id) {
        DepartmentEntity department = departmentService.getById(id);
        if (department == null) {
            throw new BusinessException(ResultCode.DEPT_NOT_FOUND, "department not found: " + id);
        }
        return ApiResponse.success(department);
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody DepartmentEntity department) {
        departmentService.createDepartment(department);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody DepartmentEntity department) {
        department.setId(id);
        departmentService.updateDepartment(department);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        departmentService.deleteDepartment(id);
        return ApiResponse.success(null);
    }
}

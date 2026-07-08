package com.roadcrack.bootstrap.controller;

import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.RoleEntity;
import com.roadcrack.service.service.RoleService;
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
@RequestMapping("/api/role")
@ConditionalOnProperty(name = "crack.persistence.mode", havingValue = "db")
public class RoleController {

    private final RoleService roleService;

    public RoleController(RoleService roleService) {
        this.roleService = roleService;
    }

    @GetMapping("/list")
    public ApiResponse<List<RoleEntity>> list() {
        return ApiResponse.success(roleService.listEnabledRoles());
    }

    @GetMapping("/{id}")
    public ApiResponse<RoleEntity> detail(@PathVariable Long id) {
        RoleEntity role = roleService.getById(id);
        if (role == null) {
            throw new BusinessException(ResultCode.ROLE_NOT_FOUND, "role not found: " + id);
        }
        return ApiResponse.success(role);
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody RoleEntity role) {
        roleService.createRole(role);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable Long id, @Valid @RequestBody RoleEntity role) {
        role.setId(id);
        roleService.updateRole(role);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        roleService.deleteRole(id);
        return ApiResponse.success(null);
    }
}

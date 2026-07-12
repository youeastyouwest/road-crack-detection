package com.roadcrack.bootstrap.controller;

import com.roadcrack.api.request.user.UserPageQuery;
import com.roadcrack.common.model.ApiResponse;
import com.roadcrack.common.model.BusinessException;
import com.roadcrack.common.model.PageResponse;
import com.roadcrack.common.model.ResultCode;
import com.roadcrack.dao.entity.UserEntity;
import com.roadcrack.service.service.UserService;
import jakarta.validation.Valid;

import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestAttribute;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/page")
    public ApiResponse<PageResponse<UserEntity>> page(UserPageQuery query) {
        return ApiResponse.success(userService.pageQuery(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> detail(@PathVariable("id") Long id) {
        UserEntity user = userService.getById(id);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not found: " + id);
        }
        return ApiResponse.success(buildUserDetail(user));
    }

    @PostMapping
    public ApiResponse<Void> create(@Valid @RequestBody UserEntity user,
                                    @RequestParam(value = "roleIds", required = false) List<Long> roleIds) {
        userService.createUser(user, roleIds);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(@PathVariable("id") Long id,
                                    @Valid @RequestBody UserEntity user,
                                    @RequestParam(value = "roleIds", required = false) List<Long> roleIds) {
        user.setId(id);
        userService.updateUser(user, roleIds);
        return ApiResponse.success(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable("id") Long id) {
        userService.deleteUser(id);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/status")
    public ApiResponse<Void> toggleStatus(@PathVariable("id") Long id, @RequestParam(value = "status") Integer status) {
        userService.toggleStatus(id, status);
        return ApiResponse.success(null);
    }

    @PutMapping("/{id}/reset-password")
    public ApiResponse<Void> resetPassword(@PathVariable("id") Long id) {
        userService.resetUserPassword(id);
        return ApiResponse.success(null);
    }

    @GetMapping("/current")
    public ApiResponse<Map<String, Object>> current(@RequestAttribute("userId") Long userId) {
        UserEntity user = userService.getById(userId);
        if (user == null) {
            throw new BusinessException(ResultCode.USER_NOT_FOUND, "user not found: " + userId);
        }
        return ApiResponse.success(buildUserDetail(user));
    }

    private Map<String, Object> buildUserDetail(UserEntity user) {
        Map<String, Object> result = new HashMap<>();
        result.put("user", user);
        result.put("roles", userService.getUserRoleCodes(user.getId()));
        result.put("roleIds", userService.getUserRoleIds(user.getId()));
        return result;
    }
}

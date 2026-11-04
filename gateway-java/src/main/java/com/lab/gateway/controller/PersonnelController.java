package com.lab.gateway.controller;

import com.lab.gateway.dto.PersonnelRequest;
import com.lab.gateway.entity.ApiResponse;
import com.lab.gateway.entity.Personnel;
import com.lab.gateway.service.PersonnelService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/**
 * 人员管理 Controller —— 暴露 /personnels REST 接口。
 * <p>
 * 这是 Java 网关的"本地业务"部分:数据由 Java 服务直接写入 MySQL 的 personnel 表,
 * 不经过任何跨语言调用。
 */
@RestController
@RequestMapping("/personnels")
public class PersonnelController {

    private final PersonnelService personnelService;

    public PersonnelController(PersonnelService personnelService) {
        this.personnelService = personnelService;
    }

    /**
     * GET /personnels
     * 查询人员列表
     *
     * @param keyword    姓名/职位/部门 模糊关键字
     * @param position   职位精确过滤
     * @param status     在职状态过滤
     * @param department 部门过滤
     * @param page       页码
     * @param pageSize   每页条数
     */
    @GetMapping
    public ApiResponse<Map<String, Object>> list(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String position,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String department,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "10") int pageSize) {

        if (page < 1 || pageSize < 1 || pageSize > 200) {
            return ApiResponse.error(400, "参数校验失败: page >= 1, 1 <= pageSize <= 200");
        }
        Map<String, Object> data = personnelService.list(keyword, position, status, department, page, pageSize);
        return ApiResponse.ok(data, "查询到 " + data.get("total") + " 条人员记录");
    }

    /**
     * GET /personnels/stats
     * 人员统计(职位分布 / 状态分布)
     */
    @GetMapping("/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(personnelService.statistics());
    }

    /**
     * GET /personnels/{id}
     */
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<Personnel>> getById(@PathVariable Long id) {
        return personnelService.getById(id)
                .map(p -> ResponseEntity.ok(ApiResponse.ok(p)))
                .orElseGet(() -> ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error(404, "人员 ID=" + id + " 不存在")));
    }

    /**
     * POST /personnels
     * 新增人员
     */
    @PostMapping
    public ResponseEntity<ApiResponse<Personnel>> create(@Valid @RequestBody PersonnelRequest request) {
        Personnel created = personnelService.create(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(created, "人员创建成功"));
    }

    /**
     * PUT /personnels/{id}
     * 更新人员
     */
    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<Personnel>> update(
            @PathVariable Long id, @Valid @RequestBody PersonnelRequest request) {

        if (!personnelService.update(id, request)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "人员 ID=" + id + " 不存在,更新失败"));
        }
        Personnel updated = personnelService.getById(id).orElseThrow();
        return ResponseEntity.ok(ApiResponse.ok(updated, "人员更新成功"));
    }

    /**
     * DELETE /personnels/{id}
     * 删除人员
     */
    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> delete(@PathVariable Long id) {
        if (!personnelService.delete(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(ApiResponse.error(404, "人员 ID=" + id + " 不存在,删除失败"));
        }
        return ResponseEntity.ok(ApiResponse.ok(Map.of("id", id), "人员删除成功"));
    }
}

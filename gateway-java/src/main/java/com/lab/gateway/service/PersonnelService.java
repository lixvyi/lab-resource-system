package com.lab.gateway.service;

import com.lab.gateway.dto.PersonnelRequest;
import com.lab.gateway.entity.Personnel;
import com.lab.gateway.repository.PersonnelDao;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 人员管理业务服务(本地业务 + DAO)。
 */
@Service
public class PersonnelService {

    private final PersonnelDao personnelDao;

    public PersonnelService(PersonnelDao personnelDao) {
        this.personnelDao = personnelDao;
    }

    /**
     * 分页查询人员
     */
    public Map<String, Object> list(String keyword, String position, String status,
                                    String department, int page, int pageSize) {
        return personnelDao.findAll(keyword, position, status, department, page, pageSize);
    }

    /**
     * 按ID查询人员
     */
    public Optional<Personnel> getById(Long id) {
        return personnelDao.findById(id);
    }

    /**
     * 新增人员
     */
    @Transactional
    public Personnel create(PersonnelRequest request) {
        Personnel p = new Personnel();
        applyRequest(p, request);
        Long id = personnelDao.insert(p);
        return personnelDao.findById(id).orElseThrow(
                () -> new IllegalStateException("新增人员后无法回查该记录, id=" + id));
    }

    /**
     * 更新人员
     *
     * @return true 表示更新成功,false 表示目标不存在
     */
    @Transactional
    public boolean update(Long id, PersonnelRequest request) {
        Personnel existing = personnelDao.findById(id).orElse(null);
        if (existing == null) {
            return false;
        }
        applyRequest(existing, request);
        return personnelDao.update(id, existing) > 0;
    }

    /**
     * 删除人员
     */
    @Transactional
    public boolean delete(Long id) {
        return personnelDao.delete(id) > 0;
    }

    /**
     * 统计:按职位分布 + 按状态分布
     */
    public Map<String, Object> statistics() {
        List<Map<String, Object>> byPosition = personnelDao.countByPosition();
        List<Map<String, Object>> byStatus = personnelDao.countByStatus();
        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("byPosition", byPosition);
        result.put("byStatus", byStatus);
        return result;
    }

    /**
     * 把请求 DTO 的字段拷贝到实体对象
     */
    private void applyRequest(Personnel p, PersonnelRequest request) {
        p.setName(request.getName());
        p.setGender(request.getGender());
        p.setAge(request.getAge());
        p.setPhone(request.getPhone());
        p.setEmail(request.getEmail());
        p.setPosition(request.getPosition() == null || request.getPosition().isBlank()
                ? "实验室成员" : request.getPosition());
        p.setDepartment(request.getDepartment());
        p.setLabRoom(request.getLabRoom());
        p.setEntryDate(request.getEntryDate());
        p.setStatus(request.getStatus() == null || request.getStatus().isBlank()
                ? "ACTIVE" : request.getStatus());
    }
}

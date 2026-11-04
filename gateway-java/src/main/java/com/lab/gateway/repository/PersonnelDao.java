package com.lab.gateway.repository;

import com.lab.gateway.entity.Personnel;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.support.GeneratedKeyHolder;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.stereotype.Repository;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;

/**
 * 人员数据访问对象(DAO)。
 * <p>
 * 使用 Spring JDBC 的 {@link JdbcTemplate} 手写 SQL,
 * 体现"本地业务 + DAO"的经典分层,便于课程实验讲解。
 */
@Repository
public class PersonnelDao {

    private final JdbcTemplate jdbcTemplate;

    public PersonnelDao(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    /** 行映射器:ResultSet -> Personnel */
    private static final RowMapper<Personnel> ROW_MAPPER = new RowMapper<Personnel>() {
        @Override
        public Personnel mapRow(ResultSet rs, int rowNum) throws SQLException {
            Personnel p = new Personnel();
            p.setId(rs.getLong("id"));
            p.setName(rs.getString("name"));
            p.setGender(rs.getString("gender"));
            p.setAge(rs.getInt("age"));
            p.setPhone(rs.getString("phone"));
            p.setEmail(rs.getString("email"));
            p.setPosition(rs.getString("position"));
            p.setDepartment(rs.getString("department"));
            p.setLabRoom(rs.getString("lab_room"));
            java.sql.Date entry = rs.getDate("entry_date");
            p.setEntryDate(entry == null ? null : entry.toLocalDate());
            p.setStatus(rs.getString("status"));
            p.setCreatedAt(rs.getTimestamp("created_at") == null ? null
                    : rs.getTimestamp("created_at").toLocalDateTime());
            p.setUpdatedAt(rs.getTimestamp("updated_at") == null ? null
                    : rs.getTimestamp("updated_at").toLocalDateTime());
            return p;
        }
    };

    /**
     * 按条件分页查询人员
     *
     * @param keyword    姓名/职位/部门 模糊匹配
     * @param position   职位精确匹配
     * @param status     在职状态精确匹配
     * @param department 部门精确匹配
     * @param page       页码(从 1 开始)
     * @param pageSize   每页条数
     */
    public Map<String, Object> findAll(String keyword, String position, String status,
                                       String department, int page, int pageSize) {
        StringBuilder where = new StringBuilder(" WHERE 1 = 1");
        List<Object> params = new ArrayList<>();

        if (keyword != null && !keyword.isBlank()) {
            where.append(" AND (name LIKE ? OR position LIKE ? OR department LIKE ?)");
            String kw = "%" + keyword.trim() + "%";
            params.add(kw);
            params.add(kw);
            params.add(kw);
        }
        if (position != null && !position.isBlank()) {
            where.append(" AND position = ?");
            params.add(position.trim());
        }
        if (status != null && !status.isBlank()) {
            where.append(" AND status = ?");
            params.add(status.trim());
        }
        if (department != null && !department.isBlank()) {
            where.append(" AND department = ?");
            params.add(department.trim());
        }

        Long total = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM personnel" + where, Long.class, params.toArray());

        int offset = (page - 1) * pageSize;
        List<Object> queryParams = new ArrayList<>(params);
        queryParams.add(pageSize);
        queryParams.add(offset);
        List<Personnel> items = jdbcTemplate.query(
                "SELECT * FROM personnel" + where + " ORDER BY id DESC LIMIT ? OFFSET ?",
                ROW_MAPPER, queryParams.toArray());

        Map<String, Object> result = new java.util.LinkedHashMap<>();
        result.put("items", items);
        result.put("total", total == null ? 0 : total);
        result.put("page", page);
        result.put("pageSize", pageSize);
        result.put("totalPages", total == null ? 0 : (int) Math.ceil(total * 1.0 / pageSize));
        return result;
    }

    /**
     * 按ID查询人员
     */
    public Optional<Personnel> findById(Long id) {
        List<Personnel> list = jdbcTemplate.query(
                "SELECT * FROM personnel WHERE id = ?", ROW_MAPPER, id);
        return list.isEmpty() ? Optional.empty() : Optional.of(list.get(0));
    }

    /**
     * 新增人员,返回自增主键
     */
    public Long insert(Personnel p) {
        String sql = "INSERT INTO personnel "
                + "(name, gender, age, phone, email, position, department, lab_room, entry_date, status) "
                + "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        KeyHolder keyHolder = new GeneratedKeyHolder();
        jdbcTemplate.update(con -> {
            PreparedStatement ps = con.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS);
            ps.setString(1, p.getName());
            ps.setString(2, p.getGender());
            ps.setInt(3, p.getAge());
            ps.setString(4, emptyToNull(p.getPhone()));
            ps.setString(5, emptyToNull(p.getEmail()));
            ps.setString(6, p.getPosition() == null ? "实验室成员" : p.getPosition());
            ps.setString(7, emptyToNull(p.getDepartment()));
            ps.setString(8, emptyToNull(p.getLabRoom()));
            if (p.getEntryDate() == null) {
                ps.setDate(9, null);
            } else {
                ps.setDate(9, java.sql.Date.valueOf(p.getEntryDate()));
            }
            ps.setString(10, p.getStatus() == null ? "ACTIVE" : p.getStatus());
            return ps;
        }, keyHolder);
        Number key = keyHolder.getKey();
        if (key == null) {
            throw new IllegalStateException("新增人员后未能获取自增主键");
        }
        return key.longValue();
    }

    /**
     * 更新人员,返回受影响的行数
     */
    public int update(Long id, Personnel p) {
        String sql = "UPDATE personnel SET name = ?, gender = ?, age = ?, phone = ?, email = ?, "
                + "position = ?, department = ?, lab_room = ?, entry_date = ?, status = ? WHERE id = ?";
        return jdbcTemplate.update(sql,
                p.getName(),
                p.getGender(),
                p.getAge(),
                emptyToNull(p.getPhone()),
                emptyToNull(p.getEmail()),
                p.getPosition() == null ? "实验室成员" : p.getPosition(),
                emptyToNull(p.getDepartment()),
                emptyToNull(p.getLabRoom()),
                p.getEntryDate(),
                p.getStatus() == null ? "ACTIVE" : p.getStatus(),
                id);
    }

    /**
     * 删除人员
     */
    public int delete(Long id) {
        return jdbcTemplate.update("DELETE FROM personnel WHERE id = ?", id);
    }

    /**
     * 统计:按职位分组人数
     */
    public List<Map<String, Object>> countByPosition() {
        return jdbcTemplate.queryForList(
                "SELECT position, COUNT(*) AS cnt FROM personnel GROUP BY position ORDER BY cnt DESC");
    }

    /**
     * 统计:在职状态分布
     */
    public List<Map<String, Object>> countByStatus() {
        return jdbcTemplate.queryForList(
                "SELECT status, COUNT(*) AS cnt FROM personnel GROUP BY status");
    }

    private static String emptyToNull(String s) {
        return (s == null || s.isBlank()) ? null : s;
    }
}

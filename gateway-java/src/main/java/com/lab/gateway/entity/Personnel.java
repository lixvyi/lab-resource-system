package com.lab.gateway.entity;

import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * 实验室人员实体(对应 MySQL 表 personnel)。
 * <p>
 * 使用普通 JavaBean + Jackson 注解,避免 Lombok 在不同 JDK 下的注解处理器兼容性问题。
 */
public class Personnel {

    private Long id;

    /** 姓名 */
    private String name;

    /** 性别 */
    private String gender;

    /** 年龄 */
    private Integer age;

    /** 联系电话 */
    private String phone;

    /** 电子邮箱 */
    private String email;

    /** 职位 */
    private String position;

    /** 所属部门 */
    private String department;

    /** 所属实验室房间号 */
    private String labRoom;

    /** 入职/入组日期 */
    private LocalDate entryDate;

    /** 在职状态: ACTIVE / LEFT / SUSPENDED */
    private String status;

    @JsonProperty("createdAt")
    private LocalDateTime createdAt;

    @JsonProperty("updatedAt")
    private LocalDateTime updatedAt;

    public Personnel() {
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public String getGender() {
        return gender;
    }

    public void setGender(String gender) {
        this.gender = gender;
    }

    public Integer getAge() {
        return age;
    }

    public void setAge(Integer age) {
        this.age = age;
    }

    public String getPhone() {
        return phone;
    }

    public void setPhone(String phone) {
        this.phone = phone;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPosition() {
        return position;
    }

    public void setPosition(String position) {
        this.position = position;
    }

    public String getDepartment() {
        return department;
    }

    public void setDepartment(String department) {
        this.department = department;
    }

    public String getLabRoom() {
        return labRoom;
    }

    public void setLabRoom(String labRoom) {
        this.labRoom = labRoom;
    }

    public LocalDate getEntryDate() {
        return entryDate;
    }

    public void setEntryDate(LocalDate entryDate) {
        this.entryDate = entryDate;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "Personnel{id=" + id + ", name='" + name + "', position='" + position + "', status='" + status + "'}";
    }
}

package com.lab.gateway.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;

/**
 * 人员创建/更新请求 DTO。
 * <p>
 * 使用 Jakarta Bean Validation 注解做声明式参数校验,
 * 通过 {@code @Valid} 在 Controller 层自动生效,校验失败返回 400。
 */
public class PersonnelRequest {

    @NotBlank(message = "姓名(name)不能为空")
    @Size(max = 50, message = "姓名长度不能超过 50")
    private String name;

    @NotBlank(message = "性别(gender)不能为空")
    @Pattern(regexp = "^(男|女|Male|Female|M|F)$", message = "性别(gender)只能是 男/女")
    private String gender;

    @NotNull(message = "年龄(age)不能为空")
    @Min(value = 1, message = "年龄(age)必须大于 0")
    @Max(value = 120, message = "年龄(age)不能超过 120")
    private Integer age;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号(phone)格式不正确")
    private String phone;

    @Email(message = "邮箱(email)格式不正确")
    @Size(max = 100, message = "邮箱长度不能超过 100")
    private String email;

    @Size(max = 50, message = "职位(position)长度不能超过 50")
    private String position;

    @Size(max = 50, message = "部门(department)长度不能超过 50")
    private String department;

    @Size(max = 50, message = "房间号(labRoom)长度不能超过 50")
    private String labRoom;

    private LocalDate entryDate;

    @Pattern(regexp = "^$|^ACTIVE$|^LEFT$|^SUSPENDED$",
            message = "状态(status)只能是 ACTIVE/LEFT/SUSPENDED")
    private String status;

    public PersonnelRequest() {
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
}

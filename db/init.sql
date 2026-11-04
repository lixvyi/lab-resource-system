-- ============================================================
-- 跨语言调用的实验室资源管理系统 —— 数据库初始化脚本
-- 数据库: lab_resource_system
-- 说明: 三个微服务(Node.js / Python / Java)共用同一个 MySQL 库,
--       但各自只操作自己负责的表,体现"服务自治 + 共享存储"的SOA架构。
-- ============================================================

-- Windows 下 mysql 客户端默认可能是 gbk/latin1，必须先切到 utf8mb4，
-- 否则中文(如性别「男」)会按错误编码写入，在 STRICT 模式下触发
-- "Data too long for column 'gender'"（3 字节 UTF-8 塞进 latin1/CHAR(1)）。
SET NAMES utf8mb4;
SET character_set_client = utf8mb4;
SET character_set_connection = utf8mb4;
SET character_set_results = utf8mb4;
SET FOREIGN_KEY_CHECKS = 0;

-- 重建库，避免沿用此前 latin1 / 过窄 gender 列的旧表结构
DROP DATABASE IF EXISTS lab_resource_system;
CREATE DATABASE lab_resource_system
  DEFAULT CHARACTER SET utf8mb4
  DEFAULT COLLATE utf8mb4_unicode_ci;

USE lab_resource_system;

-- ------------------------------------------------------------
-- 1. 人员表 (由 Java/Spring Boot 网关服务负责读写)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS personnel;
CREATE TABLE personnel (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '人员主键ID',
    name          VARCHAR(50)  CHARACTER SET utf8mb4 NOT NULL COMMENT '姓名',
    gender        VARCHAR(16)  CHARACTER SET utf8mb4 NOT NULL COMMENT '性别: 男/女',
    age           INT          NOT NULL COMMENT '年龄',
    phone         VARCHAR(20)           DEFAULT NULL COMMENT '联系电话',
    email         VARCHAR(100)          DEFAULT NULL COMMENT '电子邮箱',
    position      VARCHAR(50)  CHARACTER SET utf8mb4 NOT NULL COMMENT '职位: 实验室主任/实验员/研究生/本科生/设备管理员',
    department    VARCHAR(50)  CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '所属部门/课题组',
    lab_room      VARCHAR(50)  CHARACTER SET utf8mb4 DEFAULT NULL COMMENT '所属实验室房间号',
    entry_date    DATE                  DEFAULT NULL COMMENT '入职/入组日期',
    status        VARCHAR(20)  NOT NULL COMMENT '在职状态: ACTIVE-在职, LEFT-离职, SUSPENDED-停职',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    UNIQUE KEY uk_personnel_phone (phone),
    KEY idx_personnel_position (position),
    KEY idx_personnel_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='实验室人员表';

-- ------------------------------------------------------------
-- 2. 物资表 (由 Node.js/Express 物资服务负责读写)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS material;
CREATE TABLE material (
    id            BIGINT        NOT NULL AUTO_INCREMENT COMMENT '物资主键ID',
    name          VARCHAR(100)  NOT NULL COMMENT '物资名称',
    category      VARCHAR(50)   NOT NULL COMMENT '物资分类: 试剂/仪器/耗材/防护用品/文档资料',
    specification VARCHAR(200)           DEFAULT NULL COMMENT '规格型号',
    quantity      INT           NOT NULL COMMENT '库存数量',
    unit          VARCHAR(20)   NOT NULL COMMENT '计量单位',
    price         DECIMAL(10, 2) NOT NULL COMMENT '单价(元)',
    storage_room  VARCHAR(50)            DEFAULT NULL COMMENT '存放房间号',
    supplier      VARCHAR(100)           DEFAULT NULL COMMENT '供应商',
    purchase_date DATE                   DEFAULT NULL COMMENT '采购日期',
    expiry_date   DATE                   DEFAULT NULL COMMENT '有效期/到期日',
    status        VARCHAR(20)    NOT NULL COMMENT '状态: IN_STOCK-在库, LOW_STOCK-低库存, SCRAPPED-报废',
    created_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_material_category (category),
    KEY idx_material_status (status),
    KEY idx_material_name (name)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='实验室物资表';

-- ------------------------------------------------------------
-- 3. 借用记录表 (由 Python/Flask 借用服务负责读写)
--    通过 personnel_id / material_id 与其它表关联(逻辑外键,跨服务不建物理约束)
-- ------------------------------------------------------------
DROP TABLE IF EXISTS borrow;
CREATE TABLE borrow (
    id            BIGINT       NOT NULL AUTO_INCREMENT COMMENT '借用记录ID',
    personnel_id  BIGINT       NOT NULL COMMENT '借用人ID(对应personnel.id)',
    material_id   BIGINT       NOT NULL COMMENT '借用物资ID(对应material.id)',
    quantity      INT          NOT NULL COMMENT '借用数量',
    borrow_date   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '借出时间',
    expect_return DATETIME              DEFAULT NULL COMMENT '预计归还时间',
    actual_return DATETIME              DEFAULT NULL COMMENT '实际归还时间',
    status        VARCHAR(20)  NOT NULL COMMENT '状态: BORROWED-借用中, RETURNED-已归还, OVERDUE-逾期, LOST-遗失',
    purpose       VARCHAR(200)          DEFAULT NULL COMMENT '用途说明',
    remark        VARCHAR(200)          DEFAULT NULL COMMENT '备注',
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '创建时间',
    updated_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
        ON UPDATE CURRENT_TIMESTAMP COMMENT '更新时间',
    PRIMARY KEY (id),
    KEY idx_borrow_personnel (personnel_id),
    KEY idx_borrow_material (material_id),
    KEY idx_borrow_status (status)
) ENGINE = InnoDB DEFAULT CHARSET = utf8mb4 COLLATE = utf8mb4_unicode_ci COMMENT ='实验室物资借用记录表';

-- ============================================================
-- 初始化测试数据
-- ============================================================

-- 人员数据 (8条)
INSERT INTO personnel (name, gender, age, phone, email, position, department, lab_room, entry_date, status)
VALUES ('张伟', '男', 42, '13800138001', 'zhangwei@lab.edu.cn', '实验室主任', '生物科学学院', '生物楼A301', '2015-08-15', 'ACTIVE'),
       ('李秀英', '女', 35, '13800138002', 'lixiuying@lab.edu.cn', '实验员', '生物科学学院', '生物楼A305', '2017-03-20', 'ACTIVE'),
       ('王强', '男', 29, '13800138003', 'wangqiang@lab.edu.cn', '研究生', '生物科学学院', '生物楼B201', '2019-09-01', 'ACTIVE'),
       ('刘洋', '女', 27, '13800138004', 'liuyang@lab.edu.cn', '研究生', '生物科学学院', '生物楼B201', '2020-07-10', 'ACTIVE'),
       ('陈晓明', '男', 24, '13800138005', 'chenxiaoming@lab.edu.cn', '本科生', '生物科学学院', '生物楼C102', '2022-09-05', 'ACTIVE'),
       ('赵敏', '女', 38, '13800138006', 'zhaomin@lab.edu.cn', '设备管理员', '生物科学学院', '生物楼A101', '2016-05-18', 'ACTIVE'),
       ('孙磊', '男', 31, '13800138007', 'sunlei@lab.edu.cn', '实验员', '生物科学学院', '生物楼A305', '2018-11-12', 'ACTIVE'),
       ('周静', '女', 26, '13800138008', 'zhoujing@lab.edu.cn', '本科生', '生物科学学院', '生物楼C102', '2023-09-01', 'ACTIVE');

-- 物资数据 (10条)
INSERT INTO material (name, category, specification, quantity, unit, price, storage_room, supplier, purchase_date, expiry_date, status)
VALUES ('PBS缓冲液', '试剂', '1x, 500mL/瓶', 48, '瓶', 25.00, '生物楼A101', '华康生物试剂', '2025-03-10', '2027-03-09', 'IN_STOCK'),
       ('胰蛋白酶', '试剂', '0.25%, 100mL', 12, '瓶', 120.00, '生物楼A101', 'Sigma-Aldrich', '2025-01-22', '2026-01-21', 'IN_STOCK'),
       ('Taq DNA聚合酶', '试剂', '50U/µL, 500U', 6, '支', 680.00, '生物楼A101', 'Takara', '2024-12-05', '2026-12-04', 'LOW_STOCK'),
       ('移液枪', '仪器', 'Eppendorf 0.1-1000µL', 15, '把', 1580.00, '生物楼A105', 'Eppendorf', '2024-05-16', NULL, 'IN_STOCK'),
       ('恒温培养箱', '仪器', 'MCO-170, 150L', 3, '台', 9800.00, '生物楼A108', 'Thermo Fisher', '2023-11-08', NULL, 'IN_STOCK'),
       ('超低温冰箱', '仪器', '-80℃, 500L', 2, '台', 45800.00, '生物楼A110', '海尔生物医疗', '2022-06-30', NULL, 'IN_STOCK'),
       ('离心管', '耗材', '1.5mL 无菌, 500支/箱', 22, '箱', 320.00, '生物楼A103', '上海远华', '2025-06-01', NULL, 'IN_STOCK'),
       ('移液器吸头', '耗材', '200µL, 1000支/盒', 35, '盒', 95.00, '生物楼A103', '上海远华', '2025-06-01', NULL, 'IN_STOCK'),
       ('一次性手套', '防护用品', '丁腈, 100只/盒', 60, '盒', 28.50, '生物楼A102', '蓝帆医疗', '2025-07-20', '2028-07-19', 'IN_STOCK'),
       ('护目镜', '防护用品', '防雾防溅, 10副/箱', 8, '箱', 150.00, '生物楼A102', '3M 中国', '2025-02-14', NULL, 'LOW_STOCK'),
       ('移液器吸头', '耗材', '1000µL, 500支/盒', 4, '盒', 140.00, '生物楼A103', '上海远华', '2025-06-01', NULL, 'LOW_STOCK'),
       ('实验记录本', '文档资料', 'A4  lined, 100页', 80, '本', 12.00, '生物楼A106', '晨光文具', '2025-08-01', NULL, 'IN_STOCK');

-- 借用记录数据 (10条)
INSERT INTO borrow (personnel_id, material_id, quantity, borrow_date, expect_return, actual_return, status, purpose, remark)
VALUES (1, 9, 2, '2026-10-20 09:15:00', '2026-11-20 09:15:00', NULL, 'BORROWED', '细胞培养防护操作', '常规耗材领用'),
       (2, 4, 1, '2026-10-22 10:30:00', '2026-12-22 10:30:00', NULL, 'BORROWED', '蛋白表达实验梯度加样', '需提前预约仪器'),
       (3, 7, 3, '2026-10-25 14:02:00', '2026-11-25 14:02:00', NULL, 'BORROWED', 'DNA提取实验', NULL),
       (4, 1, 5, '2026-10-15 08:45:00', '2026-11-15 08:45:00', '2026-11-12 16:20:00', 'RETURNED', '细胞洗涤缓冲', '按时归还'),
       (5, 8, 2, '2026-10-18 11:20:00', '2026-11-18 11:20:00', NULL, 'OVERDUE', 'PCR体系配制', '已超期未归还,待催缴'),
       (6, 10, 1, '2026-10-28 15:50:00', '2026-11-28 15:50:00', NULL, 'BORROWED', '离心机安全防护', NULL),
       (7, 2, 2, '2026-10-12 09:00:00', '2026-11-12 09:00:00', '2026-11-10 17:05:00', 'RETURNED', '组织消化实验', NULL),
       (8, 5, 1, '2026-10-30 16:33:00', '2026-11-30 16:33:00', NULL, 'BORROWED', '细菌培养实验', '使用后需清洁'),
       (3, 3, 1, '2026-09-15 10:10:00', '2026-10-15 10:10:00', '2026-10-14 18:40:00', 'RETURNED', 'PCR扩增', NULL),
       (2, 12, 3, '2026-11-01 13:25:00', '2026-12-01 13:25:00', NULL, 'BORROWED', '批量移液操作', NULL);

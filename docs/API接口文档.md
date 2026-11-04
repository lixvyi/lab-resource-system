# 实验室资源管理系统 —— API 接口文档

> 跨语言调用的实验室资源管理系统 · 接口版本 v1.0
> 统一入口:Java 网关 `http://localhost:8080`

---

## 一、接口设计约定

### 1.1 统一响应信封

三个微服务(Java / Node.js / Python)使用**完全一致**的响应结构,客户端只需实现一套解析逻辑:

```json
{
  "code": 0,
  "message": "success",
  "data": { },
  "details": null
}
```

| 字段 | 类型 | 说明 |
|---|---|---|
| `code` | int | `0` 表示成功;非 0 为错误码(与 HTTP 状态码一致) |
| `message` | string | 提示信息,成功时为业务描述,失败时为错误原因 |
| `data` | object/array | 业务数据,失败时为 `null` |
| `details` | object/string | 错误详情(如字段级校验错误),成功时为 `null` |

### 1.2 HTTP 状态码约定

| 状态码 | 含义 | 典型场景 |
|---|---|---|
| 200 | 请求成功 | 查询、更新成功 |
| 201 | 创建成功 | POST 新增人员 |
| 400 | 参数错误 | 缺少必填字段、格式校验失败 |
| 404 | 资源不存在 | 查询/更新/删除不存在的 ID |
| 409 | 数据冲突 | 手机号等唯一字段重复 |
| 500 | 服务内部错误 | 未捕获异常 |
| 502 | 下游服务不可用 | Java 网关调用 Node.js / Python 失败 |

### 1.3 字符编码与内容类型

- 编码:UTF-8
- 请求与响应:`application/json; charset=UTF-8`

---

## 二、服务与端口总览

| 功能模块 | 服务实现 | 端口 | 对外接口前缀 | 数据存储 |
|---|---|---|---|---|
| 人员管理 | Java / Spring Boot | 8080 | `/personnels` | MySQL `personnel` 表 |
| 物资管理 | Node.js / Express | 8082 | `/materials` | MySQL `material` 表 |
| 借用记录 | Python / Flask | 8081 | `/borrows` | MySQL `borrow` 表 |
| 聚合看板 | Java / Spring Boot | 8080 | `/dashboard` | 聚合三方 |

> **重要**:客户端应统一访问 `http://localhost:8080`,由 Java 网关转发到 8082 / 8081。
> 直连下游端口仅用于调试与跨语言调用链路的验证。

---

## 三、人员管理 API(Java / Spring Boot)

> 负责人:Java 网关服务 · 数据经 `PersonnelDao` 直接读写 MySQL `personnel` 表
> 跨语言调用:**无**(本地业务)

### 3.1 查询人员列表

```
GET /personnels
```

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `keyword` | string | 否 | - | 姓名/职位/部门 模糊匹配 |
| `position` | string | 否 | - | 职位精确过滤 |
| `status` | string | 否 | - | 在职状态:`ACTIVE`/`LEFT`/`SUSPENDED` |
| `department` | string | 否 | - | 部门精确过滤 |
| `page` | int | 否 | 1 | 页码(≥1) |
| `pageSize` | int | 否 | 10 | 每页条数(1~200) |

**请求示例**
```bash
curl "http://localhost:8080/personnels?position=研究生&page=1&pageSize=5"
```

**响应示例**
```json
{
  "code": 0,
  "message": "查询到 2 条人员记录",
  "data": {
    "items": [
      {
        "id": 3,
        "name": "王强",
        "gender": "男",
        "age": 29,
        "phone": "13800138003",
        "email": "wangqiang@lab.edu.cn",
        "position": "研究生",
        "department": "生物科学学院",
        "labRoom": "生物楼B201",
        "entryDate": "2019-09-01",
        "status": "ACTIVE",
        "createdAt": "2026-11-04T14:30:00",
        "updatedAt": "2026-11-04T14:30:00"
      }
    ],
    "total": 2,
    "page": 1,
    "pageSize": 5,
    "totalPages": 1
  }
}
```

---

### 3.2 查询人员统计

```
GET /personnels/stats
```

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "byPosition": [
      { "position": "研究生", "cnt": 2 },
      { "position": "本科生", "cnt": 2 }
    ],
    "byStatus": [
      { "status": "ACTIVE", "cnt": 8 }
    ]
  }
}
```

---

### 3.3 查询单个人员

```
GET /personnels/{id}
```

| 参数 | 位置 | 类型 | 必填 | 说明 |
|---|---|---|---|---|
| `id` | Path | long | 是 | 人员主键 ID |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "id": 1,
    "name": "张伟",
    "gender": "男",
    "age": 42,
    "position": "实验室主任",
    "department": "生物科学学院",
    "labRoom": "生物楼A301",
    "status": "ACTIVE"
  }
}
```

**错误响应(404)**
```json
{
  "code": 404,
  "message": "人员 ID=9999 不存在",
  "data": null
}
```

---

### 3.4 新增人员

```
POST /personnels
Content-Type: application/json
```

**请求体字段**

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `name` | string | ✔ | 非空,≤50 字符 | 姓名 |
| `gender` | string | ✔ | `男`/`女`/`Male`/`Female`/`M`/`F` | 性别 |
| `age` | int | ✔ | 1~120 | 年龄 |
| `phone` | string | ✘ | 11 位手机号或留空 | 联系电话(**唯一**) |
| `email` | string | ✘ | 合法邮箱,≤100 | 电子邮箱 |
| `position` | string | ✘ | ≤50,默认"实验室成员" | 职位 |
| `department` | string | ✘ | ≤50 | 所属部门 |
| `labRoom` | string | ✘ | ≤50 | 实验室房间号 |
| `entryDate` | string | ✘ | `yyyy-MM-dd` | 入职/入组日期 |
| `status` | string | ✘ | `ACTIVE`/`LEFT`/`SUSPENDED`,默认 `ACTIVE` | 在职状态 |

**请求示例**
```bash
curl -X POST http://localhost:8080/personnels \
  -H "Content-Type: application/json" \
  -d '{
        "name": "郑凯",
        "gender": "男",
        "age": 28,
        "phone": "13800138009",
        "email": "zhengkai@lab.edu.cn",
        "position": "实验员",
        "department": "生物科学学院",
        "labRoom": "生物楼A305",
        "entryDate": "2026-11-01"
      }'
```

**成功响应(201)**
```json
{
  "code": 0,
  "message": "人员创建成功",
  "data": { "id": 9, "name": "郑凯", "...": "..." }
}
```

**参数校验失败(400)**
```json
{
  "code": 400,
  "message": "参数校验失败",
  "data": null,
  "details": {
    "name": "姓名(name)不能为空",
    "age": "年龄(age)必须大于 0"
  }
}
```

**唯一约束冲突(409)**
```json
{
  "code": 409,
  "message": "数据重复:手机号或唯一字段已存在",
  "data": null,
  "details": "Duplicate entry '13800138001' for key 'uk_personnel_phone'"
}
```

---

### 3.5 更新人员

```
PUT /personnels/{id}
Content-Type: application/json
```

请求体字段同 3.4(全部按新值覆盖)。成功返回 200 与更新后的完整对象。

---

### 3.6 删除人员

```
DELETE /personnels/{id}
```

**成功响应(200)**
```json
{
  "code": 0,
  "message": "人员删除成功",
  "data": { "id": 9 }
}
```

---

## 四、物资管理 API(Node.js / Express)

> 负责人:Node.js 物资服务(:8082)· 数据经 `MaterialRepository` 读写 MySQL `material` 表
> 跨语言调用:**Java 网关 → Node.js**(WebClient)

### 4.1 查询物资列表

```
GET /materials
```

**Query 参数**

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `category` | string | 否 | - | 分类:`试剂`/`仪器`/`耗材`/`防护用品`/`文档资料` |
| `status` | string | 否 | - | 状态:`IN_STOCK`/`LOW_STOCK`/`SCRAPPED` |
| `keyword` | string | 否 | - | 名称/规格/供应商 模糊匹配 |
| `storageRoom` | string | 否 | - | 存放房间号精确过滤 |
| `minQuantity` | int | 否 | - | 库存数量下限 |
| `maxQuantity` | int | 否 | - | 库存数量上限 |
| `sortBy` | string | 否 | `id` | 排序字段(白名单,防注入) |
| `order` | string | 否 | `desc` | `asc`/`desc` |
| `page` | int | 否 | 1 | 页码 |
| `pageSize` | int | 否 | 10 | 每页条数(1~200) |

**请求示例**
```bash
curl "http://localhost:8082/materials?category=试剂&minQuantity=10"
```

**响应示例**
```json
{
  "code": 0,
  "message": "查询到 3 条物资记录",
  "data": {
    "items": [
      {
        "id": 1,
        "name": "PBS缓冲液",
        "category": "试剂",
        "specification": "1x, 500mL/瓶",
        "quantity": 48,
        "unit": "瓶",
        "price": 25.00,
        "storage_room": "生物楼A101",
        "supplier": "华康生物试剂",
        "purchase_date": "2025-03-10",
        "expiry_date": "2027-03-09",
        "status": "IN_STOCK",
        "created_at": "2026-11-04T14:30:00",
        "updated_at": "2026-11-04T14:30:00"
      }
    ],
    "total": 3,
    "page": 1,
    "pageSize": 10,
    "totalPages": 1
  }
}
```

---

### 4.2 库存统计与预警

```
GET /materials/stats?threshold=10
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `threshold` | int | 否 | 10 | 低库存预警阈值 |

**响应示例**
```json
{
  "code": 0,
  "message": "success",
  "data": {
    "byCategory": [
      { "category": "试剂", "kinds": 3, "totalQuantity": 66, "totalValue": 1930.00 }
    ],
    "warnings": [
      { "id": 3, "name": "Taq DNA聚合酶", "quantity": 6, "status": "LOW_STOCK" }
    ],
    "threshold": 10
  }
}
```

---

### 4.3 查询单条物资

```
GET /materials/{id}
```

响应 `data` 为单个物资对象;不存在时返回 404。

---

### 4.4 新增物资

```
POST /materials
Content-Type: application/json
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `name` | string | ✔ | 非空 | 物资名称 |
| `category` | string | ✔ | 非空 | 物资分类 |
| `specification` | string | ✘ | - | 规格型号 |
| `quantity` | int | ✘ | ≥0,默认 0 | 库存数量 |
| `unit` | string | ✘ | 默认 `个` | 计量单位 |
| `price` | number | ✘ | ≥0,默认 0 | 单价(元) |
| `storageRoom` | string | ✘ | - | 存放房间号 |
| `supplier` | string | ✘ | - | 供应商 |
| `purchaseDate` | string | ✘ | `yyyy-MM-dd` | 采购日期 |
| `expiryDate` | string | ✘ | `yyyy-MM-dd` | 有效期 |
| `status` | string | ✘ | 默认 `IN_STOCK` | 状态 |

**请求示例**
```bash
curl -X POST http://localhost:8082/materials \
  -H "Content-Type: application/json" \
  -d '{"name":"无血清培养基","category":"试剂","specification":"DMEM 500mL","quantity":20,"unit":"瓶","price":380.00,"storageRoom":"生物楼A101","supplier":"Gibco","expiryDate":"2027-05-01"}'
```

---

### 4.5 更新物资 / 4.6 删除物资

```
PUT /materials/{id}
DELETE /materials/{id}
```

---

## 五、借用记录 API(Python / Flask)

> 负责人:Python 借用服务(:8081)· 数据经 `BorrowRepository` 读写 MySQL `borrow` 表
> 跨语言调用:**Java 网关 → Python**(WebClient)
> **业务亮点**:借出时扣减 `material` 库存、归还时回补,均在数据库事务中完成

### 5.1 查询借用记录

```
GET /borrows
```

| 参数 | 类型 | 必填 | 默认值 | 说明 |
|---|---|---|---|---|
| `personnelId` | int | 否 | - | 按借用人过滤 |
| `materialId` | int | 否 | - | 按物资过滤 |
| `status` | string | 否 | - | `BORROWED`/`RETURNED`/`OVERDUE`/`LOST` |
| `keyword` | string | 否 | - | 用途/备注 模糊匹配 |
| `page` | int | 否 | 1 | 页码 |
| `pageSize` | int | 否 | 10 | 每页条数 |

**响应示例**
```json
{
  "code": 0,
  "message": "查询到 2 条借用记录",
  "data": {
    "items": [
      {
        "id": 1,
        "personnel_id": 1,
        "material_id": 9,
        "quantity": 2,
        "borrow_date": "2026-10-20 09:15:00",
        "expect_return": "2026-11-20 09:15:00",
        "actual_return": null,
        "status": "BORROWED",
        "purpose": "细胞培养防护操作",
        "remark": "常规耗材领用",
        "personnel_name": "张伟",
        "material_name": "一次性手套",
        "material_unit": "盒"
      }
    ],
    "total": 2,
    "page": 1,
    "pageSize": 10,
    "totalPages": 1
  }
}
```

> `personnel_name` / `material_name` 由服务端通过 SQL JOIN 联表带出,体现跨服务的只读关联。

---

### 5.2 借用统计

```
GET /borrows/stats
```

```json
{
  "code": 0,
  "message": "success",
  "data": {
    "byStatus": [
      { "status": "BORROWED", "cnt": 6, "total_qty": 9 },
      { "status": "RETURNED", "cnt": 3, "total_qty": 6 }
    ],
    "topPersonnels": [ { "personnel_name": "张伟", "cnt": 1 } ],
    "topMaterials": [ { "material_name": "一次性手套", "cnt": 1 } ]
  }
}
```

---

### 5.3 查询单条借用记录

```
GET /borrows/{id}
```

---

### 5.4 新增借用记录(借出)

```
POST /borrows
Content-Type: application/json
```

| 字段 | 类型 | 必填 | 校验规则 | 说明 |
|---|---|---|---|---|
| `personnelId` | int | ✔ | 正整数 | 借用人 ID(对应 `personnel.id`) |
| `materialId` | int | ✔ | 正整数 | 物资 ID(对应 `material.id`) |
| `quantity` | int | ✘ | 正整数,默认 1 | 借用数量 |
| `borrowDate` | string | ✘ | `yyyy-MM-dd HH:mm:ss` | 借出时间,默认当前时间 |
| `expectReturn` | string | ✘ | `yyyy-MM-dd HH:mm:ss` | 预计归还时间 |
| `status` | string | ✘ | 默认 `BORROWED` | 状态 |
| `purpose` | string | ✘ | - | 用途说明 |
| `remark` | string | ✘ | - | 备注 |

**请求示例**
```bash
curl -X POST http://localhost:8081/borrows \
  -H "Content-Type: application/json" \
  -d '{"personnelId":3,"materialId":1,"quantity":2,"expectReturn":"2026-12-04 09:00:00","purpose":"细胞洗涤实验"}'
```

**成功响应(201)**
```json
{
  "code": 0,
  "message": "借用记录创建成功",
  "data": { "id": 11, "personnel_name": "王强", "material_name": "PBS缓冲液", "...": "..." }
}
```

**库存不足(400)**
```json
{
  "code": 400,
  "message": "物资 ID=3 库存不足: 现有 6,请求 20",
  "data": null
}
```

> **事务语义**:借出成功的同时 `material.quantity` 相应减少;库存不足时整体回滚,
> 不会产生"记录已建但库存未扣"的脏数据。

---

### 5.5 更新借用记录(归还)

```
PUT /borrows/{id}
Content-Type: application/json
```

归还时把 `status` 改为 `RETURNED`;若未提供 `actualReturn`,服务端自动填当前时间,
同时**自动回补** `material.quantity`。

```bash
curl -X PUT http://localhost:8081/borrows/1 \
  -H "Content-Type: application/json" \
  -d '{"status":"RETURNED"}'
```

---

### 5.6 删除借用记录

```
DELETE /borrows/{id}
```

若记录仍为 `BORROWED` 状态,删除时会回补库存。

---

## 六、聚合看板 API(Java 网关)

> 这是最能体现"跨语言协作"价值的接口:一次请求触达三种语言的实现。

### 6.1 实验室资源总览

```
GET /dashboard/overview
```

**实现说明**:`AggregationService.overview()` 并行调用
`PersonnelService`(Java 本地)+ `DownstreamClient.getMaterials()`(Node.js)+ `DownstreamClient.getBorrows()`(Python)。

**响应示例**
```json
{
  "code": 0,
  "message": "实验室资源总览(聚合 Java + Node.js + Python 三方数据)",
  "data": {
    "personnel": { "items": [ "..." ], "total": 8, "page": 1 },
    "materials": { "items": [ "..." ], "total": 12, "page": 1 },
    "borrows":   { "items": [ "..." ], "total": 10, "page": 1 }
  }
}
```

**下游服务不可用时的降级**
```json
{
  "code": 0,
  "message": "实验室资源总览(聚合 Java + Node.js + Python 三方数据)",
  "data": {
    "personnel": { "total": 8 },
    "materials": { "available": false, "error": "无法连接 Node.js 物资服务(...)" },
    "borrows":   { "items": [ "..." ], "total": 10 }
  }
}
```

> 降级策略:单个下游失败不影响其它模块返回,失败模块以 `available:false` 占位。

---

### 6.2 三方统计聚合

```
GET /dashboard/statistics
```

聚合人员职位/状态分布(Node.js 物资分类汇总)、Python 借用状态分布。

---

### 6.3 某人员的借用记录(跨语言关联查询)

```
GET /personnels/{id}/borrows
```

一次请求返回 **Java 侧人员信息** 与 **Python 侧借用记录**,是"跨服务数据关联"的直观体现。

---

## 七、健康检查

三个服务都提供 `/health`:

| 服务 | 地址 | 检查内容 |
|---|---|---|
| Java 网关 | `http://localhost:8080/health` | 自身数据库 + 两个下游服务的可达性 |
| Node.js 物资 | `http://localhost:8082/health` | 自身数据库连接 |
| Python 借用 | `http://localhost:8081/health` | 自身数据库连接 |

**响应示例**
```json
{
  "code": 0,
  "message": "gateway-java 存活",
  "data": {
    "service": "gateway-java",
    "version": "1.0.0",
    "port": 8080,
    "database": "UP",
    "downstream": {
      "material-service(Node.js/8082)": "UP",
      "borrow-service(Python/8081)": "UP"
    },
    "timestamp": "2026-11-04T14:30:00"
  }
}
```

---

## 八、跨语言调用链路说明

### 8.1 请求流转

```
客户端 (浏览器 / curl)
        │  HTTP REST
        ▼
┌──────────────────────────────────────────┐
│  Java Gateway (Spring Boot) :8080        │
│  PersonnelController / PersonnelService  │  ← 本地业务,直连 MySQL
│  PersonnelDao → personnel 表             │
│                                          │
│  MaterialProxyController  ──┐            │
│  BorrowProxyController    ──┼─ WebClient │
│  DownstreamClient         ──┘            │
└──────────────────────────────────────────┘
        │ HTTP              │ HTTP
        ▼                   ▼
┌────────────────┐  ┌────────────────────┐
│ Node.js/Express│  │ Python/Flask       │
│ :8082          │  │ :8081              │
│ /materials     │  │ /borrows           │
└────────────────┘  └────────────────────┘
        │                   │
        └─────────┬─────────┘
                  ▼
        ┌──────────────────┐
        │ MySQL            │
        │ lab_resource_... │
        └──────────────────┘
```

### 8.2 调用实现要点

| 关注点 | 实现方式 |
|---|---|
| HTTP 客户端 | Spring `WebClient`(响应式,Reactor Netty) |
| 超时控制 | 连接超时 3s / 读超时 5s(`WebClientConfig`) |
| 响应解析 | Jackson 反序列化为 `Map<String,Object>`,统一提取 `data` 字段 |
| 错误映射 | `WebClientResponseException` / 连接失败 → `DownstreamServiceException` → HTTP 502 |
| 服务发现 | 直接使用 `application.yml` 中的绝对地址(单机多服务场景) |
| 请求标识 | 自定义头 `X-Caller-Language: Java/SpringBoot`、`X-Caller-Service: gateway-java` |

---

## 九、测试用例建议

| 编号 | 接口 | 操作 | 预期结果 |
|---|---|---|---|
| T1 | `GET /personnels` | 无参数查询 | 200,返回 8 条初始人员数据 |
| T2 | `GET /personnels?position=研究生` | 按职位过滤 | 200,仅返回研究生 |
| T3 | `POST /personnels` | 缺少 `name` | 400,返回字段级校验错误 |
| T4 | `POST /personnels` | 重复手机号 | 409,唯一约束冲突 |
| T5 | `GET /materials` | 经网关查物资 | 200,数据来自 Node.js(日志出现"跨语言调用") |
| T6 | `GET /materials?category=试剂` | 按分类过滤 | 200,仅返回试剂类 |
| T7 | `GET /borrows` | 经网关查借用 | 200,数据来自 Python |
| T8 | `POST /borrows` | 借出超库存数量 | 400,"库存不足" |
| T9 | `POST /borrows` | 正常借出 2 个 | 201,`material.quantity` 减少 2 |
| T10 | `PUT /borrows/{id}` | 状态改 `RETURNED` | 200,库存回补 |
| T11 | `DELETE /materials/{id}` | 删除不存在的 ID | 404 |
| T12 | `GET /dashboard/overview` | 聚合看板 | 200,含 personnel/materials/borrows 三段 |
| T13 | 停掉 Node 服务后 `GET /materials` | 下游不可用 | 502,提示"下游服务调用失败" |
| T14 | `GET /dashboard/overview` | 下游不可用 | 200,失败模块 `available:false`,其余正常 |

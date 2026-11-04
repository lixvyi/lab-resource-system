"""
跨语言调用的实验室资源管理系统 —— 借用记录微服务
==================================================
技术栈 : Python 3 + Flask
端口   : 8081
接口   : /borrows  REST API
数据库 : MySQL (PyMySQL 连接池)
"""

import sys
import os
from pathlib import Path

# 将当前目录添加到 sys.path，确保 'app' 模块能被正确识别
current_dir = Path(__file__).resolve().parent
if str(current_dir) not in sys.path:
    sys.path.insert(0, str(current_dir))

from flask import Flask, jsonify, request
import pymysql
from pymysql.cursors import DictCursor
from flask_cors import CORS

from config.settings import Settings
from app.repository import BorrowRepository
from app.service import BorrowService

settings = Settings()

app = Flask(__name__)
CORS(app)

repo = BorrowRepository(settings)
service = BorrowService(repo)


# ------------------------------------------------------------------
# 统一响应工具
# ------------------------------------------------------------------
def ok(data, message="success", http_status=200):
    return jsonify({"code": 0, "message": message, "data": data}), http_status


def fail(message, http_status=400, details=None):
    return jsonify({"code": http_status, "message": message, "data": None, "details": details}), http_status


# ------------------------------------------------------------------
# 健康检查
# ------------------------------------------------------------------
@app.route("/health", methods=["GET"])
def health():
    db_state = "UP"
    try:
        repo.ping()
    except Exception as exc:  # noqa: BLE001
        db_state = "DOWN: %s" % exc
    return ok({
        "service": settings.SERVICE_NAME,
        "version": settings.SERVICE_VERSION,
        "port": settings.PORT,
        "database": db_state,
        "timestamp": __import__("datetime").datetime.now().isoformat(),
    }, "borrow-service 存活")


# ------------------------------------------------------------------
# 借用记录 CRUD
# ------------------------------------------------------------------

@app.route("/borrows", methods=["GET"])
def list_borrows():
    """GET /borrows  查询借用记录(分页 + 过滤)"""
    try:
        filters = {
            "personnelId": request.args.get("personnelId", type=int),
            "materialId": request.args.get("materialId", type=int),
            "status": request.args.get("status"),
            "keyword": request.args.get("keyword"),
            "page": request.args.get("page", default=1, type=int),
            "pageSize": request.args.get("pageSize", default=10, type=int),
        }
        result = service.list_borrows(filters)
        return ok(result, "查询到 %d 条借用记录" % result["total"])
    except Exception as exc:  # noqa: BLE001
        app.logger.exception("GET /borrows 失败")
        return fail("查询借用记录失败", 500, str(exc))


@app.route("/borrows/<int:borrow_id>", methods=["GET"])
def get_borrow(borrow_id):
    """GET /borrows/{id}  查询单条借用记录(含关联人员/物资名称)"""
    try:
        record = service.get_borrow(borrow_id)
        if record is None:
            return fail("借用记录 ID=%d 不存在" % borrow_id, 404)
        return ok(record)
    except Exception as exc:  # noqa: BLE001
        app.logger.exception("GET /borrows/<id> 失败")
        return fail("查询借用记录详情失败", 500, str(exc))


@app.route("/borrows", methods=["POST"])
def create_borrow():
    """POST /borrows  新增借用记录(校验库存并扣减)"""
    try:
        payload = request.get_json(silent=True) or {}
        err = service.validate_payload(payload, for_create=True)
        if err:
            return fail("参数校验失败: " + err, 400)
        record = service.create_borrow(payload)
        return ok(record, "借用记录创建成功", 201)
    except ValueError as exc:
        return fail(str(exc), 400)
    except Exception as exc:  # noqa: BLE001
        app.logger.exception("POST /borrows 失败")
        return fail("创建借用记录失败", 500, str(exc))


@app.route("/borrows/<int:borrow_id>", methods=["PUT"])
def update_borrow(borrow_id):
    """PUT /borrows/{id}  更新借用记录(归还时回补库存)"""
    try:
        payload = request.get_json(silent=True) or {}
        exists = repo.find_by_id(borrow_id)
        if not exists:
            return fail("借用记录 ID=%d 不存在" % borrow_id, 404)
        err = service.validate_payload(payload, for_create=False)
        if err:
            return fail("参数校验失败: " + err, 400)
        record = service.update_borrow(borrow_id, payload)
        return ok(record, "借用记录更新成功")
    except ValueError as exc:
        return fail(str(exc), 400)
    except Exception as exc:  # noqa: BLE001
        app.logger.exception("PUT /borrows/<id> 失败")
        return fail("更新借用记录失败", 500, str(exc))


@app.route("/borrows/<int:borrow_id>", methods=["DELETE"])
def delete_borrow(borrow_id):
    """DELETE /borrows/{id}  删除借用记录(若仍在借用中则回补库存)"""
    try:
        removed = service.delete_borrow(borrow_id)
        if not removed:
            return fail("借用记录 ID=%d 不存在,删除失败" % borrow_id, 404)
        return ok({"id": borrow_id}, "借用记录删除成功")
    except Exception as exc:  # noqa: BLE001
        app.logger.exception("DELETE /borrows/<id> 失败")
        return fail("删除借用记录失败", 500, str(exc))


# ------------------------------------------------------------------
# 兜底
# ------------------------------------------------------------------
@app.errorhandler(404)
def not_found(_e):
    return fail("未找到路径: %s %s" % (request.method, request.path), 404)


@app.errorhandler(500)
def server_error(e):
    return fail("服务器内部错误: %s" % e, 500)


if __name__ == "__main__":
    app.run(host="0.0.0.0", port=settings.PORT, debug=settings.DEBUG, threaded=True)

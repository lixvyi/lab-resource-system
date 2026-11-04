/**
 * 物资管理 Controller (Express Router)
 * ---------------------------------------------------------------
 * 暴露 /materials 与 /materials/stats 等 REST 接口。
 * 分层约定:Route(本文件) -> Service/Repository(数据) -> DB
 */
'use strict';

const express = require('express');
const materialRepository = require('./materialRepository');

const router = express.Router();

/**
 * 统一响应包装
 */
function ok(res, data, message = 'success') {
  res.json({ code: 0, message, data });
}

function fail(res, httpStatus, message, details = null) {
  res.status(httpStatus).json({ code: httpStatus, message, data: null, details });
}

/**
 * GET /materials
 * 查询物资列表(分页 + 多条件过滤)
 * Query: page, pageSize, category, status, keyword, storageRoom,
 *        minQuantity, maxQuantity, sortBy, order
 */
router.get('/', async (req, res) => {
  try {
    const page = await materialRepository.findAll(req.query);
    ok(res, page, `查询到 ${page.total} 条物资记录`);
  } catch (err) {
    console.error('[material-service] GET /materials 失败:', err.message);
    fail(res, 500, '查询物资列表失败', err.message);
  }
});

/**
 * GET /materials/stats
 * 库存统计与预警(网关聚合报表会调用)
 */
router.get('/stats', async (req, res) => {
  try {
    const threshold = parseInt(req.query.threshold || '10', 10);
    const [byCategory, warnings] = await Promise.all([
      materialRepository.statisticsByCategory(),
      materialRepository.lowStockWarnings(threshold)
    ]);
    ok(res, { byCategory, warnings, threshold });
  } catch (err) {
    console.error('[material-service] GET /materials/stats 失败:', err.message);
    fail(res, 500, '查询物资统计失败', err.message);
  }
});

/**
 * GET /materials/:id
 */
router.get('/:id', async (req, res) => {
  try {
    const material = await materialRepository.findById(req.params.id);
    if (!material) {
      return fail(res, 404, `物资 ID=${req.params.id} 不存在`);
    }
    ok(res, material);
  } catch (err) {
    console.error('[material-service] GET /materials/:id 失败:', err.message);
    fail(res, 500, '查询物资详情失败', err.message);
  }
});

/**
 * POST /materials
 * 新增物资
 */
router.post('/', async (req, res) => {
  try {
    const body = req.body || {};
    if (!body.name || !body.category) {
      return fail(res, 400, '参数校验失败: name(名称) 与 category(分类) 为必填项');
    }
    if (body.quantity !== undefined && (isNaN(body.quantity) || body.quantity < 0)) {
      return fail(res, 400, '参数校验失败: quantity 必须是非负整数');
    }
    const created = await materialRepository.create(body);
    res.status(201).json({ code: 0, message: '物资创建成功', data: created });
  } catch (err) {
    console.error('[material-service] POST /materials 失败:', err.message);
    fail(res, 500, '创建物资失败', err.message);
  }
});

/**
 * PUT /materials/:id
 * 更新物资
 */
router.put('/:id', async (req, res) => {
  try {
    const exists = await materialRepository.findById(req.params.id);
    if (!exists) {
      return fail(res, 404, `物资 ID=${req.params.id} 不存在`);
    }
    const updated = await materialRepository.update(req.params.id, req.body || {});
    ok(res, updated, '物资更新成功');
  } catch (err) {
    console.error('[material-service] PUT /materials/:id 失败:', err.message);
    fail(res, 500, '更新物资失败', err.message);
  }
});

/**
 * DELETE /materials/:id
 * 删除物资
 */
router.delete('/:id', async (req, res) => {
  try {
    const removed = await materialRepository.remove(req.params.id);
    if (!removed) {
      return fail(res, 404, `物资 ID=${req.params.id} 不存在,删除失败`);
    }
    ok(res, { id: Number(req.params.id) }, '物资删除成功');
  } catch (err) {
    console.error('[material-service] DELETE /materials/:id 失败:', err.message);
    fail(res, 500, '删除物资失败', err.message);
  }
});

module.exports = router;

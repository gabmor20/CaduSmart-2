const express = require('express');
const pool = require('../db');
const { requireAuth, requireRole } = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth);

const TZ = 'America/Bogota';
const TODAY = `(now() AT TIME ZONE '${TZ}')::date`;

const SELECT = `
  SELECT p.id, p.name, p.category, p.quantity, p.lot, p.barcode,
         to_char(p.expiry_date, 'YYYY-MM-DD') AS expiry_date,
         (p.expiry_date - ${TODAY}) AS days_left,
         (p.consumed_at IS NOT NULL) AS consumed,
         to_char(p.created_at AT TIME ZONE '${TZ}', 'YYYY-MM-DD') AS registered_at,
         to_char(p.deleted_at AT TIME ZONE '${TZ}', 'YYYY-MM-DD') AS deleted_at
  FROM products p`;

/** Personal: sus productos. Comercio (admin y empleado): los del comercio. */
const scope = (user) =>
  user.commerceId
    ? { sql: 'p.commerce_id = $1', params: [user.commerceId] }
    : { sql: 'p.owner_id = $1 AND p.commerce_id IS NULL', params: [user.id] };

async function alertDaysOf(userId) {
  const r = await pool.query('SELECT alert_days FROM users WHERE id = $1', [userId]);
  return r.rows[0] ? r.rows[0].alert_days : 2;
}

function toDto(row, alertDays) {
  let status;
  if (row.consumed) status = 'CONSUMIDO';
  else if (row.days_left <= 0) status = 'VENCIDO';
  else if (row.days_left <= alertDays) status = 'POR_VENCER';
  else status = 'FRESCO';

  return {
    id: row.id,
    code: 'P-' + String(row.id).padStart(4, '0'),
    name: row.name,
    category: row.category,
    quantity: row.quantity,
    lot: row.lot,
    barcode: row.barcode,
    expiryDate: row.expiry_date,
    daysLeft: row.days_left,
    status,
    registeredAt: row.registered_at,
    deletedAt: row.deleted_at,
  };
}

const RANK = { VENCIDO: 0, POR_VENCER: 1, FRESCO: 2, CONSUMIDO: 3 };

async function findOne(user, id, { deleted = false } = {}) {
  const s = scope(user);
  const r = await pool.query(
    `${SELECT} WHERE ${s.sql} AND p.id = $2 AND p.deleted_at IS ${deleted ? 'NOT NULL' : 'NULL'}`,
    [...s.params, id]
  );
  return r.rows[0] || null;
}

function validDate(s) {
  if (!/^\d{4}-\d{2}-\d{2}$/.test(s)) return false;
  try {
    return new Date(`${s}T00:00:00Z`).toISOString().slice(0, 10) === s;
  } catch {
    return false;
  }
}

function parseBody(b = {}) {
  const name = String(b.name ?? '').trim();
  const category = String(b.category ?? '').trim();
  const quantity = Number(b.quantity ?? 1);
  const lot = String(b.lot ?? '').trim() || null;
  const barcode = String(b.barcode ?? '').trim() || null;
  const expiryDate = String(b.expiryDate ?? '').trim();

  if (!name) return { error: 'El nombre es obligatorio' };
  if (!category) return { error: 'La categoría es obligatoria' };
  if (!Number.isInteger(quantity) || quantity < 1) {
    return { error: 'La cantidad debe ser un número entero mayor a 0' };
  }
  if (!validDate(expiryDate)) return { error: 'La fecha de vencimiento no es válida' };
  return { value: { name, category, quantity, lot, barcode, expiryDate } };
}

const idParam = (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id)) {
    res.status(400).json({ error: 'Id inválido' });
    return null;
  }
  return id;
};

/** GET /products -> todos los no eliminados (incluye consumidos), ordenados por urgencia */
router.get('/', async (req, res, next) => {
  try {
    const s = scope(req.user);
    const [r, alertDays] = await Promise.all([
      pool.query(`${SELECT} WHERE ${s.sql} AND p.deleted_at IS NULL`, s.params),
      alertDaysOf(req.user.id),
    ]);
    const items = r.rows
      .map((row) => toDto(row, alertDays))
      .sort((a, b) => RANK[a.status] - RANK[b.status] || a.expiryDate.localeCompare(b.expiryDate));
    res.json({ items });
  } catch (err) {
    next(err);
  }
});

/** GET /products/trash -> productos eliminados (solo Admin) */
router.get('/trash', requireRole('COMERCIO_ADMIN'), async (req, res, next) => {
  try {
    const s = scope(req.user);
    const [r, alertDays] = await Promise.all([
      pool.query(`${SELECT} WHERE ${s.sql} AND p.deleted_at IS NOT NULL ORDER BY p.deleted_at DESC`, s.params),
      alertDaysOf(req.user.id),
    ]);
    res.json({ items: r.rows.map((row) => toDto(row, alertDays)) });
  } catch (err) {
    next(err);
  }
});

/** GET /products/:id */
router.get('/:id', async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    const row = await findOne(req.user, id);
    if (!row) return res.status(404).json({ error: 'Producto no encontrado' });
    res.json({ item: toDto(row, await alertDaysOf(req.user.id)) });
  } catch (err) {
    next(err);
  }
});

/** POST /products */
router.post('/', async (req, res, next) => {
  try {
    const { error, value } = parseBody(req.body);
    if (error) return res.status(400).json({ error });

    const r = await pool.query(
      `INSERT INTO products (owner_id, commerce_id, name, category, quantity, lot, barcode, expiry_date)
       VALUES ($1, $2, $3, $4, $5, $6, $7, $8) RETURNING id`,
      [req.user.id, req.user.commerceId || null, value.name, value.category,
       value.quantity, value.lot, value.barcode, value.expiryDate]
    );
    const row = await findOne(req.user, r.rows[0].id);
    res.status(201).json({ item: toDto(row, await alertDaysOf(req.user.id)) });
  } catch (err) {
    next(err);
  }
});

/** PUT /products/:id  (el estado se recalcula solo a partir de la fecha) */
router.put('/:id', async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    if (!(await findOne(req.user, id))) return res.status(404).json({ error: 'Producto no encontrado' });

    const { error, value } = parseBody(req.body);
    if (error) return res.status(400).json({ error });

    await pool.query(
      `UPDATE products SET name = $1, category = $2, quantity = $3, lot = $4, barcode = $5, expiry_date = $6
       WHERE id = $7`,
      [value.name, value.category, value.quantity, value.lot, value.barcode, value.expiryDate, id]
    );
    const row = await findOne(req.user, id);
    res.json({ item: toDto(row, await alertDaysOf(req.user.id)) });
  } catch (err) {
    next(err);
  }
});

/** POST /products/:id/consume  (cualquier rol) */
router.post('/:id/consume', async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    if (!(await findOne(req.user, id))) return res.status(404).json({ error: 'Producto no encontrado' });

    await pool.query('UPDATE products SET consumed_at = COALESCE(consumed_at, now()) WHERE id = $1', [id]);
    const row = await findOne(req.user, id);
    res.json({ item: toDto(row, await alertDaysOf(req.user.id)) });
  } catch (err) {
    next(err);
  }
});

/** DELETE /products/:id -> borrado lógico (Personal y Admin; el Empleado no) */
router.delete('/:id', requireRole('PERSONAL', 'COMERCIO_ADMIN'), async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    if (!(await findOne(req.user, id))) return res.status(404).json({ error: 'Producto no encontrado' });

    await pool.query('UPDATE products SET deleted_at = now(), deleted_by = $1 WHERE id = $2', [req.user.id, id]);
    res.json({ ok: true });
  } catch (err) {
    next(err);
  }
});

/** DELETE /products/:id/purge -> eliminación definitiva (solo Admin, solo si ya está en la papelera) */
router.delete('/:id/purge', requireRole('COMERCIO_ADMIN'), async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    if (!(await findOne(req.user, id, { deleted: true }))) {
      return res.status(404).json({ error: 'Producto no encontrado en la papelera' });
    }
    await pool.query('DELETE FROM products WHERE id = $1', [id]);
    res.json({ ok: true });
  } catch (err) {
    next(err);
  }
});

module.exports = router;

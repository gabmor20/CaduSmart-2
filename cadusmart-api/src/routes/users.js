const express = require('express');
const bcrypt = require('bcryptjs');
const pool = require('../db');
const { requireAuth, requireRole } = require('../middleware/auth');

const router = express.Router();
router.use(requireAuth, requireRole('COMERCIO_ADMIN'));

const ROLES = ['COMERCIO_ADMIN', 'COMERCIO_EMPLEADO'];
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const SELECT = `
  SELECT u.id, u.nombre, u.email, u.role, u.active, u.must_change_password,
         (SELECT count(*)::int FROM products p WHERE p.owner_id = u.id) AS product_count
  FROM users u`;

const toDto = (r) => ({
  id: r.id,
  nombre: r.nombre,
  email: r.email,
  role: r.role,
  active: r.active,
  mustChangePassword: r.must_change_password,
  productCount: r.product_count,
});

/** Solo usuarios del mismo comercio que el admin */
async function findInCommerce(admin, id) {
  const r = await pool.query(`${SELECT} WHERE u.id = $1 AND u.commerce_id = $2`, [id, admin.commerceId]);
  return r.rows[0] || null;
}

const idParam = (req, res) => {
  const id = Number(req.params.id);
  if (!Number.isInteger(id)) {
    res.status(400).json({ error: 'Id inválido' });
    return null;
  }
  return id;
};

function validateData({ nombre, email, role }) {
  if (!nombre || !nombre.trim()) return 'El nombre es obligatorio';
  if (!EMAIL_RE.test(email || '')) return 'Correo inválido';
  if (!ROLES.includes(role)) return 'Rol inválido';
  return null;
}

/** GET /users */
router.get('/', async (req, res, next) => {
  try {
    const r = await pool.query(`${SELECT} WHERE u.commerce_id = $1 ORDER BY u.nombre`, [req.user.commerceId]);
    res.json({ items: r.rows.map(toDto) });
  } catch (err) {
    next(err);
  }
});

/** POST /users  body: { nombre, email, password, role, active } (la contraseña es temporal) */
router.post('/', async (req, res, next) => {
  try {
    const { nombre, email, password, role, active } = req.body || {};
    const error = validateData({ nombre, email, role });
    if (error) return res.status(400).json({ error });
    if (!password || password.length < 8) {
      return res.status(400).json({ error: 'La contraseña temporal debe tener mínimo 8 caracteres' });
    }

    const hash = await bcrypt.hash(password, 10);
    const r = await pool.query(
      `INSERT INTO users (commerce_id, nombre, email, password_hash, role, active, must_change_password)
       VALUES ($1, $2, $3, $4, $5, $6, TRUE) RETURNING id`,
      [req.user.commerceId, nombre.trim(), email.trim().toLowerCase(), hash, role, active !== false]
    );
    res.status(201).json({ item: toDto(await findInCommerce(req.user, r.rows[0].id)) });
  } catch (err) {
    if (err.code === '23505') return res.status(409).json({ error: 'Ese correo ya está registrado' });
    next(err);
  }
});

/** PUT /users/:id  body: { nombre, email, role, active } */
router.put('/:id', async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    if (id === req.user.id) {
      return res.status(400).json({ error: 'Edita tus propios datos desde Mi perfil' });
    }
    if (!(await findInCommerce(req.user, id))) return res.status(404).json({ error: 'Usuario no encontrado' });

    const { nombre, email, role, active } = req.body || {};
    const error = validateData({ nombre, email, role });
    if (error) return res.status(400).json({ error });
    if (typeof active !== 'boolean') return res.status(400).json({ error: 'active debe ser true o false' });

    await pool.query(
      'UPDATE users SET nombre = $1, email = $2, role = $3, active = $4 WHERE id = $5',
      [nombre.trim(), email.trim().toLowerCase(), role, active, id]
    );
    res.json({ item: toDto(await findInCommerce(req.user, id)) });
  } catch (err) {
    if (err.code === '23505') return res.status(409).json({ error: 'Ese correo ya está registrado' });
    next(err);
  }
});

/** POST /users/:id/reset-password  body: { password } -> contraseña temporal */
router.post('/:id/reset-password', async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    if (id === req.user.id) {
      return res.status(400).json({ error: 'Cambia tu propia contraseña desde Mi perfil' });
    }
    if (!(await findInCommerce(req.user, id))) return res.status(404).json({ error: 'Usuario no encontrado' });

    const { password } = req.body || {};
    if (!password || password.length < 8) {
      return res.status(400).json({ error: 'La contraseña debe tener mínimo 8 caracteres' });
    }
    const hash = await bcrypt.hash(password, 10);
    await pool.query('UPDATE users SET password_hash = $1, must_change_password = TRUE WHERE id = $2', [hash, id]);
    res.json({ ok: true });
  } catch (err) {
    next(err);
  }
});

/** DELETE /users/:id -> borra la cuenta y sus productos (cascada) */
router.delete('/:id', async (req, res, next) => {
  try {
    const id = idParam(req, res);
    if (id === null) return;
    if (id === req.user.id) return res.status(400).json({ error: 'No puedes eliminar tu propia cuenta' });

    const target = await findInCommerce(req.user, id);
    if (!target) return res.status(404).json({ error: 'Usuario no encontrado' });

    await pool.query('DELETE FROM users WHERE id = $1', [id]);
    res.json({ ok: true, deletedProducts: target.product_count });
  } catch (err) {
    next(err);
  }
});

module.exports = router;

const express = require('express');
const bcrypt = require('bcryptjs');
const jwt = require('jsonwebtoken');
const pool = require('../db');
const { requireAuth } = require('../middleware/auth');

const router = express.Router();
const EMAIL_RE = /^[^\s@]+@[^\s@]+\.[^\s@]+$/;

const publicUser = (u) => ({
  id: u.id,
  nombre: u.nombre,
  email: u.email,
  role: u.role,
  commerceId: u.commerce_id,
  alertDays: u.alert_days,
  notificationsEnabled: u.notifications_enabled,
  mustChangePassword: u.must_change_password,
});

const signToken = (u) =>
  jwt.sign({ id: u.id, role: u.role, commerceId: u.commerce_id }, process.env.JWT_SECRET, {
    expiresIn: '7d',
  });

const getUser = async (id) => (await pool.query('SELECT * FROM users WHERE id = $1', [id])).rows[0];

/**
 * POST /auth/register
 * body: { tipo: "PERSONAL" | "COMERCIO", nombre, email, password, razonSocial? }
 */
router.post('/register', async (req, res, next) => {
  const { tipo, nombre, email, password, razonSocial } = req.body || {};

  if (!['PERSONAL', 'COMERCIO'].includes(tipo)) {
    return res.status(400).json({ error: 'tipo debe ser PERSONAL o COMERCIO' });
  }
  if (!nombre || !nombre.trim()) return res.status(400).json({ error: 'El nombre es obligatorio' });
  if (!EMAIL_RE.test(email || '')) return res.status(400).json({ error: 'Correo inválido' });
  if (!password || password.length < 8) {
    return res.status(400).json({ error: 'La contraseña debe tener mínimo 8 caracteres' });
  }
  if (tipo === 'COMERCIO' && !(razonSocial || '').trim()) {
    return res.status(400).json({ error: 'La razón social es obligatoria' });
  }

  const client = await pool.connect();
  try {
    await client.query('BEGIN');

    let commerceId = null;
    if (tipo === 'COMERCIO') {
      const c = await client.query(
        'INSERT INTO commerces (razon_social) VALUES ($1) RETURNING id',
        [razonSocial.trim()]
      );
      commerceId = c.rows[0].id;
    }

    const role = tipo === 'COMERCIO' ? 'COMERCIO_ADMIN' : 'PERSONAL';
    const hash = await bcrypt.hash(password, 10);
    const u = await client.query(
      `INSERT INTO users (commerce_id, nombre, email, password_hash, role)
       VALUES ($1, $2, $3, $4, $5) RETURNING *`,
      [commerceId, nombre.trim(), email.trim().toLowerCase(), hash, role]
    );

    await client.query('COMMIT');
    const user = u.rows[0];
    res.status(201).json({ token: signToken(user), user: publicUser(user) });
  } catch (err) {
    await client.query('ROLLBACK');
    if (err.code === '23505') return res.status(409).json({ error: 'Ese correo ya está registrado' });
    next(err);
  } finally {
    client.release();
  }
});

/** POST /auth/login  body: { email, password } */
router.post('/login', async (req, res, next) => {
  try {
    const { email, password } = req.body || {};
    if (!email || !password) {
      return res.status(400).json({ error: 'Correo y contraseña son obligatorios' });
    }

    const r = await pool.query('SELECT * FROM users WHERE email = $1', [email.trim().toLowerCase()]);
    const user = r.rows[0];
    const ok = user && (await bcrypt.compare(password, user.password_hash));
    if (!ok) return res.status(401).json({ error: 'Correo o contraseña incorrectos' });
    if (!user.active) return res.status(403).json({ error: 'Tu cuenta está desactivada' });

    res.json({ token: signToken(user), user: publicUser(user) });
  } catch (err) {
    next(err);
  }
});

/** GET /auth/me */
router.get('/me', requireAuth, async (req, res, next) => {
  try {
    res.json({ user: publicUser(await getUser(req.user.id)) });
  } catch (err) {
    next(err);
  }
});

/** PUT /auth/me  body: { nombre, email }  -> "Editar mis datos" */
router.put('/me', requireAuth, async (req, res, next) => {
  try {
    const { nombre, email } = req.body || {};
    if (!nombre || !nombre.trim()) return res.status(400).json({ error: 'El nombre es obligatorio' });
    if (!EMAIL_RE.test(email || '')) return res.status(400).json({ error: 'Correo inválido' });

    const r = await pool.query(
      'UPDATE users SET nombre = $1, email = $2 WHERE id = $3 RETURNING *',
      [nombre.trim(), email.trim().toLowerCase(), req.user.id]
    );
    res.json({ user: publicUser(r.rows[0]) });
  } catch (err) {
    if (err.code === '23505') return res.status(409).json({ error: 'Ese correo ya está registrado' });
    next(err);
  }
});

/** PUT /auth/me/settings  body: { alertDays (1-30), notificationsEnabled } */
router.put('/me/settings', requireAuth, async (req, res, next) => {
  try {
    const { alertDays, notificationsEnabled } = req.body || {};
    if (!Number.isInteger(alertDays) || alertDays < 1 || alertDays > 30) {
      return res.status(400).json({ error: 'Los días de aviso deben estar entre 1 y 30' });
    }
    if (typeof notificationsEnabled !== 'boolean') {
      return res.status(400).json({ error: 'notificationsEnabled debe ser true o false' });
    }
    const r = await pool.query(
      'UPDATE users SET alert_days = $1, notifications_enabled = $2 WHERE id = $3 RETURNING *',
      [alertDays, notificationsEnabled, req.user.id]
    );
    res.json({ user: publicUser(r.rows[0]) });
  } catch (err) {
    next(err);
  }
});

/**
 * POST /auth/change-password  body: { currentPassword, newPassword }
 * Sirve para el empleado, el personal y el admin (también para la contraseña temporal).
 * Si la actual no coincide responde 400 (no 401, para no cerrar la sesión en la app).
 */
router.post('/change-password', requireAuth, async (req, res, next) => {
  try {
    const { currentPassword, newPassword } = req.body || {};
    if (!currentPassword || !newPassword) {
      return res.status(400).json({ error: 'Escribe la contraseña actual y la nueva' });
    }
    if (newPassword.length < 8) {
      return res.status(400).json({ error: 'La contraseña nueva debe tener mínimo 8 caracteres' });
    }
    if (newPassword === currentPassword) {
      return res.status(400).json({ error: 'La contraseña nueva debe ser distinta de la actual' });
    }

    const user = await getUser(req.user.id);
    if (!(await bcrypt.compare(currentPassword, user.password_hash))) {
      return res.status(400).json({ error: 'La contraseña actual no es correcta' });
    }

    const hash = await bcrypt.hash(newPassword, 10);
    const r = await pool.query(
      'UPDATE users SET password_hash = $1, must_change_password = FALSE WHERE id = $2 RETURNING *',
      [hash, req.user.id]
    );
    res.json({ user: publicUser(r.rows[0]) });
  } catch (err) {
    next(err);
  }
});

module.exports = router;

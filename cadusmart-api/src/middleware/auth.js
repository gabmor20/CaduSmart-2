const jwt = require('jsonwebtoken');
const pool = require('../db');

/**
 * Verifica el JWT y recarga al usuario desde la BD:
 *  - una cuenta desactivada o eliminada deja de funcionar al instante
 *  - un cambio de rol se aplica al instante
 * Deja { id, role, commerceId } en req.user
 */
async function requireAuth(req, res, next) {
  const header = req.headers.authorization || '';
  const token = header.startsWith('Bearer ') ? header.slice(7) : null;
  if (!token) return res.status(401).json({ error: 'Token requerido' });

  let payload;
  try {
    payload = jwt.verify(token, process.env.JWT_SECRET);
  } catch {
    return res.status(401).json({ error: 'Token inválido o vencido' });
  }

  try {
    const r = await pool.query(
      'SELECT id, role, commerce_id, active FROM users WHERE id = $1',
      [payload.id]
    );
    const u = r.rows[0];
    if (!u || !u.active) return res.status(401).json({ error: 'Sesión no válida' });
    req.user = { id: u.id, role: u.role, commerceId: u.commerce_id };
    next();
  } catch (err) {
    next(err);
  }
}

/** Uso: router.get('/x', requireAuth, requireRole('COMERCIO_ADMIN'), handler) */
function requireRole(...roles) {
  return (req, res, next) => {
    if (!roles.includes(req.user.role)) {
      return res.status(403).json({ error: 'No tienes permiso para esta acción' });
    }
    next();
  };
}

module.exports = { requireAuth, requireRole };

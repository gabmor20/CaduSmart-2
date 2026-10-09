require('dotenv').config();
const pool = require('../src/db');

(async () => {
  try {
    const v = await pool.query('SELECT version()');
    console.log('Conexión OK ->', v.rows[0].version);

    const t = await pool.query(
      "SELECT table_name FROM information_schema.tables WHERE table_schema = 'public' ORDER BY 1"
    );
    const names = t.rows.map((r) => r.table_name);
    console.log('Tablas:', names.length ? names.join(', ') : '(ninguna: ejecuta npm run db:init)');
  } catch (err) {
    console.error('No se pudo conectar:', err.message);
    process.exitCode = 1;
  } finally {
    await pool.end();
  }
})();

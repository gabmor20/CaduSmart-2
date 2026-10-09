require('dotenv').config();
const fs = require('fs');
const path = require('path');
const pool = require('../src/db');

(async () => {
  try {
    const exists = await pool.query("SELECT to_regclass('public.users') AS t");
    if (exists.rows[0].t) {
      console.log('Las tablas ya existen: no se hizo nada.');
      return;
    }
    const sql = fs.readFileSync(path.join(__dirname, '..', 'schema.sql'), 'utf8');
    await pool.query(sql);
    console.log('Esquema creado correctamente.');
  } catch (err) {
    console.error('Error creando el esquema:', err.message);
    process.exitCode = 1;
  } finally {
    await pool.end();
  }
})();

require('dotenv').config();

const express = require('express');
const cors = require('cors');
const pool = require('./src/db');
const authRoutes = require('./src/routes/auth');
const productRoutes = require('./src/routes/products');
const userRoutes = require('./src/routes/users');

for (const key of ['DATABASE_URL', 'JWT_SECRET']) {
  if (!process.env[key]) {
    console.error(`Falta la variable ${key} en el archivo .env`);
    process.exit(1);
  }
}

const app = express();
app.use(cors());
app.use(express.json());

// Prueba rápida de servidor + base de datos
app.get('/health', async (_req, res) => {
  try {
    const r = await pool.query('SELECT now() AS ahora');
    res.json({ ok: true, db: 'conectada', ahora: r.rows[0].ahora });
  } catch (err) {
    res.status(500).json({ ok: false, db: 'error', detalle: err.message });
  }
});

app.use('/auth', authRoutes);
app.use('/products', productRoutes);
app.use('/users', userRoutes);

app.use((_req, res) => res.status(404).json({ error: 'Ruta no encontrada' }));

app.use((err, _req, res, _next) => {
  console.error(err);
  res.status(500).json({ error: 'Error interno del servidor' });
});

const port = process.env.PORT || 3000;
// 0.0.0.0 permite que el celular (misma red Wi-Fi) llegue al servidor
app.listen(port, '0.0.0.0', () => console.log(`API CaduSmart en http://localhost:${port}`));

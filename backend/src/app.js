import express from 'express';
import cors from 'cors';
import { config } from './config.js';
import { db } from './db.js';
import categorias from './routes/categorias.js';
import produtos from './routes/produtos.js';
import movimentacoes from './routes/movimentacoes.js';
import dashboard from './routes/dashboard.js';
import { errorHandler } from './errors.js';

export const app = express();

app.disable('x-powered-by');
app.use(cors({ origin: config.corsOrigin }));
app.use(express.json({ limit: '100kb' }));

app.get('/api/health', (_req, res) => {
  db.prepare('SELECT 1').get();
  res.json({ status: 'ok' });
});
app.use('/api/categorias', categorias);
app.use('/api/produtos', produtos);
app.use('/api/movimentacoes', movimentacoes);
app.use('/api/dashboard', dashboard);

app.use('/api', (_req, res) => res.status(404).json({ erro: 'Rota não encontrada.' }));
app.use(errorHandler);

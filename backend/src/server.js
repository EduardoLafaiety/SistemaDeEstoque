import express from 'express';
import cors from 'cors';
import './db.js';
import categorias from './routes/categorias.js';
import produtos from './routes/produtos.js';
import movimentacoes from './routes/movimentacoes.js';
import dashboard from './routes/dashboard.js';
import { errorHandler } from './errors.js';

const app = express();
const PORT = Number(process.env.PORT ?? 3000);

app.use(cors({ origin: process.env.CORS_ORIGIN ?? 'http://localhost:4200' }));
app.use(express.json());

app.get('/api/health', (_req, res) => res.json({ status: 'ok' }));
app.use('/api/categorias', categorias);
app.use('/api/produtos', produtos);
app.use('/api/movimentacoes', movimentacoes);
app.use('/api/dashboard', dashboard);

app.use('/api', (_req, res) => res.status(404).json({ erro: 'Rota não encontrada.' }));
app.use(errorHandler);

app.listen(PORT, () => {
  console.log(`API do Sistema de Estoque rodando em http://localhost:${PORT}/api`);
});

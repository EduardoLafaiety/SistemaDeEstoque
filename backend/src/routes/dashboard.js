import { Router } from 'express';
import { db } from '../db.js';
import { SELECT_PRODUTO } from './produtos.js';
import { SELECT_MOVIMENTACAO } from './movimentacoes.js';

const router = Router();

router.get('/', (_req, res) => {
  const resumo = db
    .prepare(
      `SELECT COUNT(*)                                        AS totalProdutos,
              COALESCE(SUM(quantidade), 0)                    AS totalItens,
              ROUND(COALESCE(SUM(quantidade * preco), 0), 2)  AS valorTotal,
              COALESCE(SUM(quantidade <= estoque_minimo), 0)  AS produtosBaixoEstoque
       FROM produtos WHERE ativo = 1`,
    )
    .get();
  const { totalCategorias } = db.prepare('SELECT COUNT(*) AS totalCategorias FROM categorias').get();

  const baixoEstoque = db
    .prepare(
      `${SELECT_PRODUTO} WHERE p.ativo = 1 AND p.quantidade <= p.estoque_minimo
       ORDER BY p.quantidade - p.estoque_minimo LIMIT 10`,
    )
    .all()
    .map((p) => ({ ...p, ativo: true }));

  const ultimasMovimentacoes = db
    .prepare(`${SELECT_MOVIMENTACAO} ORDER BY m.criado_em DESC, m.id DESC LIMIT 8`)
    .all();

  res.json({ ...resumo, totalCategorias, baixoEstoque, ultimasMovimentacoes });
});

export default router;

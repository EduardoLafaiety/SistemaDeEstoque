import { Router } from 'express';
import { db } from '../db.js';
import { SELECT_PRODUTO } from './produtos.js';

const router = Router();

router.get('/', (_req, res) => {
  const resumo = db
    .prepare(
      `SELECT COUNT(*)                                        AS totalProdutos,
              COALESCE(SUM(quantidade), 0)                    AS totalItens,
              COALESCE(SUM(quantidade * preco), 0)            AS valorTotal,
              COALESCE(SUM(quantidade <= estoque_minimo), 0)  AS produtosBaixoEstoque
       FROM produtos`,
    )
    .get();
  const { totalCategorias } = db.prepare('SELECT COUNT(*) AS totalCategorias FROM categorias').get();

  const baixoEstoque = db
    .prepare(`${SELECT_PRODUTO} WHERE p.quantidade <= p.estoque_minimo ORDER BY p.quantidade - p.estoque_minimo LIMIT 10`)
    .all();

  const ultimasMovimentacoes = db
    .prepare(
      `SELECT m.id, m.tipo, m.quantidade, m.observacao, m.criado_em AS criadoEm,
              m.produto_id AS produtoId, p.nome AS produtoNome, p.sku AS produtoSku
       FROM movimentacoes m JOIN produtos p ON p.id = m.produto_id
       ORDER BY m.criado_em DESC, m.id DESC LIMIT 8`,
    )
    .all();

  res.json({ ...resumo, totalCategorias, baixoEstoque, ultimasMovimentacoes });
});

export default router;

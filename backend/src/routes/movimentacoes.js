import { Router } from 'express';
import { db, transaction } from '../db.js';
import { badRequest, notFound, num, str } from '../errors.js';

const router = Router();

const SELECT = `
  SELECT m.id, m.tipo, m.quantidade, m.observacao, m.criado_em AS criadoEm,
         m.produto_id AS produtoId, p.nome AS produtoNome, p.sku AS produtoSku
  FROM movimentacoes m
  JOIN produtos p ON p.id = m.produto_id
`;

router.get('/', (req, res) => {
  const { produtoId, tipo } = req.query;
  const limite = Math.min(Number(req.query.limite) || 100, 500);
  const where = [];
  const params = [];
  if (produtoId) {
    where.push('m.produto_id = ?');
    params.push(produtoId);
  }
  if (tipo) {
    where.push('m.tipo = ?');
    params.push(String(tipo).toUpperCase());
  }
  const sql = `${SELECT} ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
               ORDER BY m.criado_em DESC, m.id DESC LIMIT ?`;
  res.json(db.prepare(sql).all(...params, limite));
});

router.post('/', (req, res) => {
  const body = req.body ?? {};
  const produtoId = num(body.produtoId, 'produtoId', { int: true, min: 1 });
  const quantidade = num(body.quantidade, 'quantidade', { int: true, min: 1 });
  const tipo = String(body.tipo ?? '').toUpperCase();
  if (tipo !== 'ENTRADA' && tipo !== 'SAIDA') {
    throw badRequest('O campo "tipo" deve ser ENTRADA ou SAIDA.');
  }
  const observacao = str(body.observacao, 'observacao', { required: false });

  const mov = transaction(() => {
    const produto = db.prepare('SELECT quantidade FROM produtos WHERE id = ?').get(produtoId);
    if (!produto) throw notFound('Produto');
    if (tipo === 'SAIDA' && produto.quantidade < quantidade) {
      throw badRequest(`Estoque insuficiente. Disponível: ${produto.quantidade}.`);
    }
    const delta = tipo === 'ENTRADA' ? quantidade : -quantidade;
    db.prepare(
      `UPDATE produtos SET quantidade = quantidade + ?, atualizado_em = datetime('now') WHERE id = ?`,
    ).run(delta, produtoId);
    const { lastInsertRowid } = db
      .prepare('INSERT INTO movimentacoes (produto_id, tipo, quantidade, observacao) VALUES (?, ?, ?, ?)')
      .run(produtoId, tipo, quantidade, observacao);
    return db.prepare(`${SELECT} WHERE m.id = ?`).get(lastInsertRowid);
  });

  res.status(201).json(mov);
});

export default router;

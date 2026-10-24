import { Router } from 'express';
import { db, transaction } from '../db.js';
import { badRequest, conflict, data, id, notFound, num, str } from '../errors.js';

const router = Router();

export const SELECT_MOVIMENTACAO = `
  SELECT m.id, m.tipo, m.quantidade, m.observacao, m.criado_em AS criadoEm,
         m.quantidade_anterior  AS quantidadeAnterior,
         m.quantidade_posterior AS quantidadePosterior,
         m.produto_id AS produtoId, p.nome AS produtoNome, p.sku AS produtoSku
  FROM movimentacoes m
  JOIN produtos p ON p.id = m.produto_id
`;

const TIPOS = ['ENTRADA', 'SAIDA'];

function tipoValido(valor) {
  const tipo = String(valor ?? '').toUpperCase();
  if (!TIPOS.includes(tipo)) throw badRequest('O campo "tipo" deve ser ENTRADA ou SAIDA.');
  return tipo;
}

router.get('/', (req, res) => {
  const { produtoId, tipo } = req.query;
  const inicio = data(req.query.inicio, 'inicio');
  const fim = data(req.query.fim, 'fim');
  const limite = Math.min(Number(req.query.limite) || 100, 500);
  const where = [];
  const params = [];
  if (produtoId) {
    where.push('m.produto_id = ?');
    params.push(id(produtoId));
  }
  if (tipo) {
    where.push('m.tipo = ?');
    params.push(tipoValido(tipo));
  }
  if (inicio) {
    where.push('date(m.criado_em) >= ?');
    params.push(inicio);
  }
  if (fim) {
    where.push('date(m.criado_em) <= ?');
    params.push(fim);
  }
  const sql = `${SELECT_MOVIMENTACAO} ${where.length ? 'WHERE ' + where.join(' AND ') : ''}
               ORDER BY m.criado_em DESC, m.id DESC LIMIT ?`;
  res.json(db.prepare(sql).all(...params, limite));
});

router.get('/:id', (req, res) => {
  const mov = db.prepare(`${SELECT_MOVIMENTACAO} WHERE m.id = ?`).get(id(req.params.id));
  if (!mov) throw notFound('Movimentação');
  res.json(mov);
});

// Movimentações são imutáveis: para corrigir um lançamento, registre a movimentação inversa.
router.post('/', (req, res) => {
  const body = req.body ?? {};
  const produtoId = num(body.produtoId, 'produtoId', { int: true, min: 1 });
  const quantidade = num(body.quantidade, 'quantidade', { int: true, min: 1 });
  const tipo = tipoValido(body.tipo);
  const observacao = str(body.observacao, 'observacao', { required: false });

  const mov = transaction(() => {
    const produto = db.prepare('SELECT quantidade, ativo FROM produtos WHERE id = ?').get(produtoId);
    if (!produto) throw notFound('Produto');
    if (!produto.ativo) throw badRequest('Produto inativo não pode ser movimentado.');
    if (tipo === 'SAIDA' && produto.quantidade < quantidade) {
      throw conflict(`Estoque insuficiente. Disponível: ${produto.quantidade}.`);
    }
    const anterior = produto.quantidade;
    const posterior = tipo === 'ENTRADA' ? anterior + quantidade : anterior - quantidade;
    db.prepare("UPDATE produtos SET quantidade = ?, atualizado_em = datetime('now') WHERE id = ?")
      .run(posterior, produtoId);
    const { lastInsertRowid } = db
      .prepare(
        `INSERT INTO movimentacoes
           (produto_id, tipo, quantidade, quantidade_anterior, quantidade_posterior, observacao)
         VALUES (?, ?, ?, ?, ?, ?)`,
      )
      .run(produtoId, tipo, quantidade, anterior, posterior, observacao);
    return db.prepare(`${SELECT_MOVIMENTACAO} WHERE m.id = ?`).get(lastInsertRowid);
  });

  res.status(201).json(mov);
});

export default router;

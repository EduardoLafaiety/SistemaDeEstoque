import { Router } from 'express';
import { db } from '../db.js';
import { badRequest, notFound, num, str } from '../errors.js';

const router = Router();

export const SELECT_PRODUTO = `
  SELECT p.id, p.sku, p.nome, p.descricao, p.preco, p.quantidade,
         p.estoque_minimo AS estoqueMinimo,
         p.categoria_id   AS categoriaId,
         c.nome           AS categoriaNome,
         p.criado_em      AS criadoEm,
         p.atualizado_em  AS atualizadoEm
  FROM produtos p
  LEFT JOIN categorias c ON c.id = p.categoria_id
`;

export const buscarProduto = (id) => db.prepare(`${SELECT_PRODUTO} WHERE p.id = ?`).get(id);

function validar(body = {}) {
  const categoriaId = num(body.categoriaId, 'categoriaId', { int: true, min: 1, required: false }) ?? null;
  if (categoriaId && !db.prepare('SELECT 1 FROM categorias WHERE id = ?').get(categoriaId)) {
    throw badRequest('Categoria informada não existe.');
  }
  return {
    sku: str(body.sku, 'sku').toUpperCase(),
    nome: str(body.nome, 'nome'),
    descricao: str(body.descricao, 'descricao', { required: false }),
    categoriaId,
    preco: num(body.preco, 'preco'),
    estoqueMinimo: num(body.estoqueMinimo, 'estoqueMinimo', { int: true, required: false }) ?? 0,
  };
}

router.get('/', (req, res) => {
  const { busca, categoriaId, baixoEstoque } = req.query;
  const where = [];
  const params = [];
  if (busca) {
    where.push('(p.nome LIKE ? OR p.sku LIKE ?)');
    params.push(`%${busca}%`, `%${busca}%`);
  }
  if (categoriaId) {
    where.push('p.categoria_id = ?');
    params.push(categoriaId);
  }
  if (baixoEstoque === 'true') {
    where.push('p.quantidade <= p.estoque_minimo');
  }
  const sql = `${SELECT_PRODUTO} ${where.length ? 'WHERE ' + where.join(' AND ') : ''} ORDER BY p.nome`;
  res.json(db.prepare(sql).all(...params));
});

router.get('/:id', (req, res) => {
  const produto = buscarProduto(req.params.id);
  if (!produto) throw notFound('Produto');
  res.json(produto);
});

// A quantidade inicial só é definida na criação; depois, muda apenas via movimentações.
router.post('/', (req, res) => {
  const p = validar(req.body);
  const quantidade = num(req.body?.quantidade, 'quantidade', { int: true, required: false }) ?? 0;
  const { lastInsertRowid } = db
    .prepare(
      `INSERT INTO produtos (sku, nome, descricao, categoria_id, preco, quantidade, estoque_minimo)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    )
    .run(p.sku, p.nome, p.descricao, p.categoriaId, p.preco, quantidade, p.estoqueMinimo);
  res.status(201).json(buscarProduto(lastInsertRowid));
});

router.put('/:id', (req, res) => {
  const p = validar(req.body);
  const { changes } = db
    .prepare(
      `UPDATE produtos
       SET sku = ?, nome = ?, descricao = ?, categoria_id = ?, preco = ?, estoque_minimo = ?,
           atualizado_em = datetime('now')
       WHERE id = ?`,
    )
    .run(p.sku, p.nome, p.descricao, p.categoriaId, p.preco, p.estoqueMinimo, req.params.id);
  if (!changes) throw notFound('Produto');
  res.json(buscarProduto(req.params.id));
});

router.delete('/:id', (req, res) => {
  const { changes } = db.prepare('DELETE FROM produtos WHERE id = ?').run(req.params.id);
  if (!changes) throw notFound('Produto');
  res.status(204).end();
});

export default router;

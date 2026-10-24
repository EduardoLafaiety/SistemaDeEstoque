import { Router } from 'express';
import { db, inserirProduto, transaction } from '../db.js';
import { badRequest, escaparLike, id, notFound, num, str } from '../errors.js';

const router = Router();

export const SELECT_PRODUTO = `
  SELECT p.id, p.sku, p.nome, p.descricao, p.preco, p.quantidade,
         p.estoque_minimo AS estoqueMinimo,
         p.categoria_id   AS categoriaId,
         c.nome           AS categoriaNome,
         p.ativo = 1      AS ativo,
         p.criado_em      AS criadoEm,
         p.atualizado_em  AS atualizadoEm
  FROM produtos p
  LEFT JOIN categorias c ON c.id = p.categoria_id
`;

export const buscarProduto = (produtoId) => {
  const p = db.prepare(`${SELECT_PRODUTO} WHERE p.id = ?`).get(produtoId);
  return p && { ...p, ativo: Boolean(p.ativo) };
};

const comAtivo = (lista) => lista.map((p) => ({ ...p, ativo: Boolean(p.ativo) }));

function validar(body = {}) {
  const categoriaId = num(body.categoriaId, 'categoriaId', { int: true, min: 1, required: false }) ?? null;
  if (categoriaId && !db.prepare('SELECT 1 FROM categorias WHERE id = ?').get(categoriaId)) {
    throw badRequest('Categoria informada não existe.');
  }
  return {
    sku: str(body.sku, 'sku', { max: 60 }).toUpperCase(),
    nome: str(body.nome, 'nome', { max: 200 }),
    descricao: str(body.descricao, 'descricao', { required: false, max: 1000 }),
    categoriaId,
    preco: Math.round(num(body.preco, 'preco') * 100) / 100,
    estoqueMinimo: num(body.estoqueMinimo, 'estoqueMinimo', { int: true, required: false }) ?? 0,
  };
}

router.get('/', (req, res) => {
  const { busca, categoriaId, baixoEstoque, inativos } = req.query;
  const where = inativos === 'true' ? [] : ['p.ativo = 1'];
  const params = [];
  if (busca) {
    const termo = `%${escaparLike(busca)}%`;
    where.push("(p.nome LIKE ? ESCAPE '\\' OR p.sku LIKE ? ESCAPE '\\')");
    params.push(termo, termo);
  }
  if (categoriaId) {
    where.push('p.categoria_id = ?');
    params.push(id(categoriaId));
  }
  if (baixoEstoque === 'true') {
    where.push('p.quantidade <= p.estoque_minimo');
  }
  const sql = `${SELECT_PRODUTO} ${where.length ? 'WHERE ' + where.join(' AND ') : ''} ORDER BY p.nome`;
  res.json(comAtivo(db.prepare(sql).all(...params)));
});

router.get('/:id', (req, res) => {
  const produto = buscarProduto(id(req.params.id));
  if (!produto) throw notFound('Produto');
  res.json(produto);
});

// A quantidade inicial só é definida na criação (gera uma ENTRADA "Estoque inicial");
// depois, o saldo muda apenas via movimentações.
router.post('/', (req, res) => {
  const p = validar(req.body);
  const quantidade = num(req.body?.quantidade, 'quantidade', { int: true, required: false }) ?? 0;
  const novoId = transaction(() => inserirProduto(p, quantidade));
  res.status(201).json(buscarProduto(novoId));
});

router.put('/:id', (req, res) => {
  const produtoId = id(req.params.id);
  const p = validar(req.body);
  const { changes } = db
    .prepare(
      `UPDATE produtos
       SET sku = ?, nome = ?, descricao = ?, categoria_id = ?, preco = ?, estoque_minimo = ?,
           atualizado_em = datetime('now')
       WHERE id = ?`,
    )
    .run(p.sku, p.nome, p.descricao, p.categoriaId, p.preco, p.estoqueMinimo, produtoId);
  if (!changes) throw notFound('Produto');
  res.json(buscarProduto(produtoId));
});

// Produtos com movimentações são inativados (preserva o histórico); sem histórico, são excluídos.
router.delete('/:id', (req, res) => {
  const produtoId = id(req.params.id);
  transaction(() => {
    if (!db.prepare('SELECT 1 FROM produtos WHERE id = ?').get(produtoId)) throw notFound('Produto');
    const temHistorico = db.prepare('SELECT 1 FROM movimentacoes WHERE produto_id = ? LIMIT 1').get(produtoId);
    if (temHistorico) {
      db.prepare("UPDATE produtos SET ativo = 0, atualizado_em = datetime('now') WHERE id = ?").run(produtoId);
    } else {
      db.prepare('DELETE FROM produtos WHERE id = ?').run(produtoId);
    }
  });
  res.status(204).end();
});

// Reativa um produto inativado.
router.post('/:id/reativar', (req, res) => {
  const produtoId = id(req.params.id);
  const { changes } = db
    .prepare("UPDATE produtos SET ativo = 1, atualizado_em = datetime('now') WHERE id = ?")
    .run(produtoId);
  if (!changes) throw notFound('Produto');
  res.json(buscarProduto(produtoId));
});

export default router;

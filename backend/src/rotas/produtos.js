import { Router } from 'express';
import { z } from 'zod';
import { getDb, transacao } from '../db.js';
import { aplicarMovimentacao } from '../estoque.js';
import { exigirPapel, paramId, validar } from '../middlewares.js';
import {
  atualizarRegistro, booleanoQuery, buscarOu404, esquemaPaginacao, paginar, textoOpcional,
} from '../util.js';

export const rotasProdutos = Router();

const CAMPOS = `
  p.*,
  c.nome AS categoria_nome,
  f.nome AS fornecedor_nome,
  (p.quantidade <= p.estoque_minimo) AS estoque_baixo,
  ROUND(p.quantidade * p.preco_custo, 2) AS valor_em_estoque`;
const JOINS = `
  FROM produtos p
  LEFT JOIN categorias c ON c.id = p.categoria_id
  LEFT JOIN fornecedores f ON f.id = p.fornecedor_id`;
const SELECT_UM = `SELECT ${CAMPOS} ${JOINS} WHERE p.id = ?`;

const ORDENACOES = {
  nome: 'p.nome',
  sku: 'p.sku',
  quantidade: 'p.quantidade',
  preco_venda: 'p.preco_venda',
  criado_em: 'p.criado_em',
};

const idOpcional = z.number().int().positive().nullish();
const dinheiro = z.number().nonnegative().multipleOf(0.01, 'Use no máximo 2 casas decimais');

const esquemaBase = z.object({
  sku: z.string().trim().min(1).max(60),
  nome: z.string().trim().min(1).max(200),
  descricao: textoOpcional,
  categoria_id: idOpcional,
  fornecedor_id: idOpcional,
  unidade: z.string().trim().min(1).max(10).default('UN'),
  preco_custo: dinheiro.default(0),
  preco_venda: dinheiro.default(0),
  estoque_minimo: z.number().int().nonnegative().default(0),
  ativo: z.boolean().default(true),
});

const esquemaCriacao = esquemaBase.extend({
  quantidade_inicial: z.number().int().nonnegative().default(0),
});

// O saldo (quantidade) só muda via movimentações, por isso não está no esquema de edição.
const esquemaEdicao = esquemaBase
  .extend({ unidade: z.string().trim().min(1).max(10) })
  .partial()
  .strict();

rotasProdutos.get(
  '/',
  validar({
    query: z.object({
      ...esquemaPaginacao,
      busca: z.string().trim().optional(),
      categoria_id: z.coerce.number().int().positive().optional(),
      fornecedor_id: z.coerce.number().int().positive().optional(),
      estoque_baixo: booleanoQuery,
      ativo: booleanoQuery.default('true'),
      ordenar: z.enum(Object.keys(ORDENACOES)).default('nome'),
      direcao: z.enum(['asc', 'desc']).default('asc'),
    }),
  }),
  (req, res) => {
    const f = req.filtros;
    const where = ['p.ativo = ?'];
    const params = [f.ativo ? 1 : 0];

    if (f.busca) {
      where.push('(p.nome LIKE ? OR p.sku LIKE ?)');
      params.push(`%${f.busca}%`, `%${f.busca}%`);
    }
    if (f.categoria_id) {
      where.push('p.categoria_id = ?');
      params.push(f.categoria_id);
    }
    if (f.fornecedor_id) {
      where.push('p.fornecedor_id = ?');
      params.push(f.fornecedor_id);
    }
    if (f.estoque_baixo !== undefined) {
      where.push(f.estoque_baixo ? 'p.quantidade <= p.estoque_minimo' : 'p.quantidade > p.estoque_minimo');
    }

    res.json(
      paginar({
        campos: CAMPOS,
        base: `${JOINS} WHERE ${where.join(' AND ')}`,
        params,
        ordem: `${ORDENACOES[f.ordenar]} ${f.direcao.toUpperCase()}, p.id`,
        pagina: f.pagina,
        limite: f.limite,
      }),
    );
  },
);

rotasProdutos.get('/:id', validar({ params: paramId }), (req, res) => {
  res.json(buscarOu404(SELECT_UM, req.ids.id, 'Produto não encontrado'));
});

rotasProdutos.post('/', validar({ body: esquemaCriacao }), (req, res) => {
  const b = req.body;
  const id = transacao((db) => {
    const { lastInsertRowid } = db
      .prepare(
        `INSERT INTO produtos
           (sku, nome, descricao, categoria_id, fornecedor_id, unidade, preco_custo, preco_venda, estoque_minimo, ativo)
         VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)`,
      )
      .run(
        b.sku, b.nome, b.descricao ?? null, b.categoria_id ?? null, b.fornecedor_id ?? null,
        b.unidade, b.preco_custo, b.preco_venda, b.estoque_minimo, b.ativo ? 1 : 0,
      );

    if (b.quantidade_inicial > 0) {
      aplicarMovimentacao({
        produtoId: Number(lastInsertRowid),
        tipo: 'entrada',
        quantidade: b.quantidade_inicial,
        motivo: 'Estoque inicial',
        usuarioId: req.usuario.id,
      });
    }
    return lastInsertRowid;
  });

  res.status(201).json(buscarOu404(SELECT_UM, id));
});

rotasProdutos.put('/:id', validar({ params: paramId, body: esquemaEdicao }), (req, res) => {
  const { ativo, ...resto } = req.body;
  atualizarRegistro(
    'produtos',
    req.ids.id,
    { ...resto, ativo: ativo === undefined ? undefined : ativo ? 1 : 0 },
    { comTimestamp: true },
  );
  res.json(buscarOu404(SELECT_UM, req.ids.id));
});

// Produtos com histórico não podem ser apagados; são inativados. Sem histórico, são excluídos.
rotasProdutos.delete('/:id', exigirPapel('admin'), validar({ params: paramId }), (req, res) => {
  const db = getDb();
  const { id } = req.ids;
  buscarOu404('SELECT id FROM produtos WHERE id = ?', id, 'Produto não encontrado');

  const { total } = db.prepare('SELECT COUNT(*) AS total FROM movimentacoes WHERE produto_id = ?').get(id);
  if (total > 0) {
    atualizarRegistro('produtos', id, { ativo: 0 }, { comTimestamp: true });
  } else {
    db.prepare('DELETE FROM produtos WHERE id = ?').run(id);
  }
  res.status(204).end();
});

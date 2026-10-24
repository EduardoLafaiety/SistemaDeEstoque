import { Router } from 'express';
import { z } from 'zod';
import { registrarMovimentacao, SELECT_MOVIMENTACAO } from '../estoque.js';
import { paramId, validar } from '../middlewares.js';
import { buscarOu404, esquemaPaginacao, paginar, textoOpcional } from '../util.js';

export const rotasMovimentacoes = Router();

const data = z.string().regex(/^\d{4}-\d{2}-\d{2}$/, 'Use o formato AAAA-MM-DD');

const esquema = z
  .object({
    produto_id: z.number().int().positive(),
    tipo: z.enum(['entrada', 'saida', 'ajuste']),
    quantidade: z.number().int().nonnegative(),
    motivo: textoOpcional,
  })
  .refine((m) => m.tipo === 'ajuste' || m.quantidade > 0, {
    message: 'Quantidade deve ser maior que zero para entradas e saídas',
    path: ['quantidade'],
  });

rotasMovimentacoes.get(
  '/',
  validar({
    query: z.object({
      ...esquemaPaginacao,
      produto_id: z.coerce.number().int().positive().optional(),
      usuario_id: z.coerce.number().int().positive().optional(),
      tipo: z.enum(['entrada', 'saida', 'ajuste']).optional(),
      inicio: data.optional(),
      fim: data.optional(),
    }),
  }),
  (req, res) => {
    const f = req.filtros;
    const where = [];
    const params = [];

    if (f.produto_id) { where.push('m.produto_id = ?'); params.push(f.produto_id); }
    if (f.usuario_id) { where.push('m.usuario_id = ?'); params.push(f.usuario_id); }
    if (f.tipo) { where.push('m.tipo = ?'); params.push(f.tipo); }
    if (f.inicio) { where.push('date(m.criado_em) >= ?'); params.push(f.inicio); }
    if (f.fim) { where.push('date(m.criado_em) <= ?'); params.push(f.fim); }

    const base = SELECT_MOVIMENTACAO.replace(/^\s*SELECT[\s\S]*?(?=FROM)/, '')
      + (where.length ? ` WHERE ${where.join(' AND ')}` : '');

    res.json(
      paginar({
        campos: 'm.*, p.sku AS produto_sku, p.nome AS produto_nome, u.nome AS usuario_nome',
        base,
        params,
        ordem: 'm.criado_em DESC, m.id DESC',
        pagina: f.pagina,
        limite: f.limite,
      }),
    );
  },
);

rotasMovimentacoes.get('/:id', validar({ params: paramId }), (req, res) => {
  res.json(buscarOu404(`${SELECT_MOVIMENTACAO} WHERE m.id = ?`, req.ids.id, 'Movimentação não encontrada'));
});

// Movimentações são imutáveis (trilha de auditoria): para corrigir, registre um ajuste.
rotasMovimentacoes.post('/', validar({ body: esquema }), (req, res) => {
  const { produto_id, tipo, quantidade, motivo } = req.body;
  const mov = registrarMovimentacao({
    produtoId: produto_id, tipo, quantidade, motivo, usuarioId: req.usuario.id,
  });
  res.status(201).json(mov);
});

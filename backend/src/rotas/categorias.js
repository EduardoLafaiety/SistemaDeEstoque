import { Router } from 'express';
import { z } from 'zod';
import { getDb } from '../db.js';
import { HttpError } from '../erros.js';
import { exigirPapel, paramId, validar } from '../middlewares.js';
import { atualizarRegistro, buscarOu404, esquemaPaginacao, paginar, textoOpcional } from '../util.js';

export const rotasCategorias = Router();

const SELECT_UM = `
  SELECT c.*, (SELECT COUNT(*) FROM produtos p WHERE p.categoria_id = c.id AND p.ativo = 1) AS total_produtos
  FROM categorias c WHERE c.id = ?`;

const esquema = z.object({
  nome: z.string().trim().min(1).max(120),
  descricao: textoOpcional,
});

rotasCategorias.get(
  '/',
  validar({ query: z.object({ ...esquemaPaginacao, busca: z.string().trim().optional() }) }),
  (req, res) => {
    const { busca, pagina, limite } = req.filtros;
    const params = [];
    let base = 'FROM categorias c';
    if (busca) {
      base += ' WHERE c.nome LIKE ?';
      params.push(`%${busca}%`);
    }
    const campos = 'c.*, (SELECT COUNT(*) FROM produtos p WHERE p.categoria_id = c.id AND p.ativo = 1) AS total_produtos';
    res.json(paginar({ campos, base, params, ordem: 'c.nome', pagina, limite }));
  },
);

rotasCategorias.get('/:id', validar({ params: paramId }), (req, res) => {
  res.json(buscarOu404(SELECT_UM, req.ids.id, 'Categoria não encontrada'));
});

rotasCategorias.post('/', validar({ body: esquema }), (req, res) => {
  const { nome, descricao } = req.body;
  const { lastInsertRowid } = getDb()
    .prepare('INSERT INTO categorias (nome, descricao) VALUES (?, ?)')
    .run(nome, descricao ?? null);
  res.status(201).json(buscarOu404(SELECT_UM, lastInsertRowid));
});

rotasCategorias.put('/:id', validar({ params: paramId, body: esquema.partial().strict() }), (req, res) => {
  atualizarRegistro('categorias', req.ids.id, req.body);
  res.json(buscarOu404(SELECT_UM, req.ids.id));
});

rotasCategorias.delete('/:id', exigirPapel('admin'), validar({ params: paramId }), (req, res) => {
  const db = getDb();
  const { total } = db.prepare('SELECT COUNT(*) AS total FROM produtos WHERE categoria_id = ?').get(req.ids.id);
  if (total > 0) throw new HttpError(409, `Categoria possui ${total} produto(s) vinculado(s)`);

  const { changes } = db.prepare('DELETE FROM categorias WHERE id = ?').run(req.ids.id);
  if (changes === 0) throw new HttpError(404, 'Categoria não encontrada');
  res.status(204).end();
});

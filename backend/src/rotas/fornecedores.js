import { Router } from 'express';
import { z } from 'zod';
import { getDb } from '../db.js';
import { HttpError } from '../erros.js';
import { exigirPapel, paramId, validar } from '../middlewares.js';
import { atualizarRegistro, buscarOu404, esquemaPaginacao, paginar } from '../util.js';

export const rotasFornecedores = Router();

const SELECT_UM = 'SELECT * FROM fornecedores WHERE id = ?';

const esquema = z.object({
  nome: z.string().trim().min(1).max(160),
  cnpj: z
    .string()
    .transform((v) => v.replace(/\D/g, ''))
    .refine((v) => v.length === 14, 'CNPJ deve ter 14 dígitos')
    .nullish(),
  email: z.string().trim().email().nullish(),
  telefone: z.string().trim().max(30).nullish(),
});

rotasFornecedores.get(
  '/',
  validar({ query: z.object({ ...esquemaPaginacao, busca: z.string().trim().optional() }) }),
  (req, res) => {
    const { busca, pagina, limite } = req.filtros;
    const params = [];
    let base = 'FROM fornecedores';
    if (busca) {
      base += ' WHERE nome LIKE ? OR cnpj LIKE ?';
      params.push(`%${busca}%`, `%${busca.replace(/\D/g, '') || busca}%`);
    }
    res.json(paginar({ campos: '*', base, params, ordem: 'nome', pagina, limite }));
  },
);

rotasFornecedores.get('/:id', validar({ params: paramId }), (req, res) => {
  res.json(buscarOu404(SELECT_UM, req.ids.id, 'Fornecedor não encontrado'));
});

rotasFornecedores.post('/', validar({ body: esquema }), (req, res) => {
  const { nome, cnpj, email, telefone } = req.body;
  const { lastInsertRowid } = getDb()
    .prepare('INSERT INTO fornecedores (nome, cnpj, email, telefone) VALUES (?, ?, ?, ?)')
    .run(nome, cnpj ?? null, email ?? null, telefone ?? null);
  res.status(201).json(buscarOu404(SELECT_UM, lastInsertRowid));
});

rotasFornecedores.put('/:id', validar({ params: paramId, body: esquema.partial().strict() }), (req, res) => {
  atualizarRegistro('fornecedores', req.ids.id, req.body);
  res.json(buscarOu404(SELECT_UM, req.ids.id));
});

rotasFornecedores.delete('/:id', exigirPapel('admin'), validar({ params: paramId }), (req, res) => {
  const db = getDb();
  const { total } = db.prepare('SELECT COUNT(*) AS total FROM produtos WHERE fornecedor_id = ?').get(req.ids.id);
  if (total > 0) throw new HttpError(409, `Fornecedor possui ${total} produto(s) vinculado(s)`);

  const { changes } = db.prepare('DELETE FROM fornecedores WHERE id = ?').run(req.ids.id);
  if (changes === 0) throw new HttpError(404, 'Fornecedor não encontrado');
  res.status(204).end();
});

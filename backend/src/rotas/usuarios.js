import { Router } from 'express';
import { z } from 'zod';
import bcrypt from 'bcryptjs';
import { getDb } from '../db.js';
import { HttpError } from '../erros.js';
import { exigirPapel, paramId, validar } from '../middlewares.js';
import { atualizarRegistro, buscarOu404, esquemaPaginacao, paginar } from '../util.js';

export const rotasUsuarios = Router();
rotasUsuarios.use(exigirPapel('admin'));

const CAMPOS = 'id, nome, email, papel, ativo, criado_em';

const esquemaUsuario = z.object({
  nome: z.string().trim().min(1).max(120),
  email: z.string().trim().email(),
  senha: z.string().min(8, 'A senha deve ter ao menos 8 caracteres'),
  papel: z.enum(['admin', 'operador']).default('operador'),
  ativo: z.boolean().default(true),
});

rotasUsuarios.get(
  '/',
  validar({ query: z.object({ ...esquemaPaginacao, busca: z.string().trim().optional() }) }),
  (req, res) => {
    const { busca, pagina, limite } = req.filtros;
    const params = [];
    let base = 'FROM usuarios';
    if (busca) {
      base += ' WHERE nome LIKE ? OR email LIKE ?';
      params.push(`%${busca}%`, `%${busca}%`);
    }
    res.json(paginar({ campos: CAMPOS, base, params, ordem: 'nome', pagina, limite }));
  },
);

rotasUsuarios.get('/:id', validar({ params: paramId }), (req, res) => {
  res.json(buscarOu404(`SELECT ${CAMPOS} FROM usuarios WHERE id = ?`, req.ids.id, 'Usuário não encontrado'));
});

rotasUsuarios.post('/', validar({ body: esquemaUsuario }), (req, res) => {
  const { nome, email, senha, papel, ativo } = req.body;
  const { lastInsertRowid } = getDb()
    .prepare('INSERT INTO usuarios (nome, email, senha_hash, papel, ativo) VALUES (?, ?, ?, ?, ?)')
    .run(nome, email, bcrypt.hashSync(senha, 10), papel, ativo ? 1 : 0);
  res.status(201).json(buscarOu404(`SELECT ${CAMPOS} FROM usuarios WHERE id = ?`, lastInsertRowid));
});

rotasUsuarios.put(
  '/:id',
  validar({ params: paramId, body: esquemaUsuario.partial().strict() }),
  (req, res) => {
    const { id } = req.ids;
    const { senha, ativo, papel, ...resto } = req.body;

    if (id === req.usuario.id && (ativo === false || (papel && papel !== 'admin'))) {
      throw new HttpError(400, 'Você não pode desativar ou rebaixar o próprio usuário');
    }

    atualizarRegistro('usuarios', id, {
      ...resto,
      papel,
      ativo: ativo === undefined ? undefined : ativo ? 1 : 0,
      senha_hash: senha ? bcrypt.hashSync(senha, 10) : undefined,
    });
    res.json(buscarOu404(`SELECT ${CAMPOS} FROM usuarios WHERE id = ?`, id));
  },
);

// Usuários têm histórico de movimentações, então são desativados em vez de excluídos.
rotasUsuarios.delete('/:id', validar({ params: paramId }), (req, res) => {
  const { id } = req.ids;
  if (id === req.usuario.id) throw new HttpError(400, 'Você não pode desativar o próprio usuário');
  atualizarRegistro('usuarios', id, { ativo: 0 });
  res.status(204).end();
});

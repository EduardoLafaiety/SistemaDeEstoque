import jwt from 'jsonwebtoken';
import { z } from 'zod';
import { config } from './config.js';
import { getDb } from './db.js';
import { HttpError } from './erros.js';

export function autenticar(req, res, next) {
  const [tipo, token] = (req.headers.authorization ?? '').split(' ');
  if (tipo !== 'Bearer' || !token) throw new HttpError(401, 'Token de acesso não informado');

  let payload;
  try {
    payload = jwt.verify(token, config.jwtSecret);
  } catch {
    throw new HttpError(401, 'Token inválido ou expirado');
  }

  const usuario = getDb()
    .prepare('SELECT id, nome, email, papel, ativo FROM usuarios WHERE id = ?')
    .get(Number(payload.sub));
  if (!usuario || !usuario.ativo) throw new HttpError(401, 'Usuário inexistente ou inativo');

  req.usuario = usuario;
  next();
}

export const exigirPapel = (...papeis) => (req, res, next) => {
  if (!papeis.includes(req.usuario?.papel)) {
    throw new HttpError(403, 'Você não tem permissão para esta operação');
  }
  next();
};

/** Valida body/query/params com Zod. Resultado em req.body, req.filtros e req.ids. */
export const validar = ({ body, query, params }) => (req, res, next) => {
  if (body) req.body = body.parse(req.body ?? {});
  if (query) req.filtros = query.parse(req.query);
  if (params) req.ids = params.parse(req.params);
  next();
};

export const paramId = z.object({ id: z.coerce.number().int().positive() });

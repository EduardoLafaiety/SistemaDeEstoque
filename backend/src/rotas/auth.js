import { Router } from 'express';
import { z } from 'zod';
import bcrypt from 'bcryptjs';
import jwt from 'jsonwebtoken';
import { config } from '../config.js';
import { getDb } from '../db.js';
import { HttpError } from '../erros.js';
import { autenticar, validar } from '../middlewares.js';

export const rotasAuth = Router();

rotasAuth.post(
  '/login',
  validar({
    body: z.object({
      email: z.string().trim().email(),
      senha: z.string().min(1),
    }),
  }),
  (req, res) => {
    const { email, senha } = req.body;
    const usuario = getDb().prepare('SELECT * FROM usuarios WHERE email = ?').get(email);

    if (!usuario || !usuario.ativo || !bcrypt.compareSync(senha, usuario.senha_hash)) {
      throw new HttpError(401, 'E-mail ou senha inválidos');
    }

    const token = jwt.sign({ sub: String(usuario.id), papel: usuario.papel }, config.jwtSecret, {
      expiresIn: config.jwtExpiresIn,
    });

    res.json({
      token,
      usuario: { id: usuario.id, nome: usuario.nome, email: usuario.email, papel: usuario.papel },
    });
  },
);

rotasAuth.get('/me', autenticar, (req, res) => {
  const { ativo, ...usuario } = req.usuario;
  res.json(usuario);
});

rotasAuth.put(
  '/senha',
  autenticar,
  validar({
    body: z.object({
      senha_atual: z.string().min(1),
      nova_senha: z.string().min(8, 'A nova senha deve ter ao menos 8 caracteres'),
    }),
  }),
  (req, res) => {
    const db = getDb();
    const { senha_hash } = db.prepare('SELECT senha_hash FROM usuarios WHERE id = ?').get(req.usuario.id);
    if (!bcrypt.compareSync(req.body.senha_atual, senha_hash)) {
      throw new HttpError(400, 'Senha atual incorreta');
    }
    db.prepare('UPDATE usuarios SET senha_hash = ? WHERE id = ?')
      .run(bcrypt.hashSync(req.body.nova_senha, 10), req.usuario.id);
    res.status(204).end();
  },
);

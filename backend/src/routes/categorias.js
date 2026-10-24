import { Router } from 'express';
import { db } from '../db.js';
import { notFound, str } from '../errors.js';

const router = Router();

const SELECT = `
  SELECT c.id, c.nome, COUNT(p.id) AS totalProdutos
  FROM categorias c
  LEFT JOIN produtos p ON p.categoria_id = c.id
`;

const buscar = (id) => db.prepare(`${SELECT} WHERE c.id = ? GROUP BY c.id`).get(id);

router.get('/', (_req, res) => {
  res.json(db.prepare(`${SELECT} GROUP BY c.id ORDER BY c.nome`).all());
});

router.get('/:id', (req, res) => {
  const cat = buscar(req.params.id);
  if (!cat) throw notFound('Categoria');
  res.json(cat);
});

router.post('/', (req, res) => {
  const nome = str(req.body?.nome, 'nome');
  const { lastInsertRowid } = db.prepare('INSERT INTO categorias (nome) VALUES (?)').run(nome);
  res.status(201).json(buscar(lastInsertRowid));
});

router.put('/:id', (req, res) => {
  const nome = str(req.body?.nome, 'nome');
  const { changes } = db.prepare('UPDATE categorias SET nome = ? WHERE id = ?').run(nome, req.params.id);
  if (!changes) throw notFound('Categoria');
  res.json(buscar(req.params.id));
});

router.delete('/:id', (req, res) => {
  const { changes } = db.prepare('DELETE FROM categorias WHERE id = ?').run(req.params.id);
  if (!changes) throw notFound('Categoria');
  res.status(204).end();
});

export default router;

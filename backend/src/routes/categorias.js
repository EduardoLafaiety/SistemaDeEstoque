import { Router } from 'express';
import { db } from '../db.js';
import { id, notFound, str } from '../errors.js';

const router = Router();

const SELECT = `
  SELECT c.id, c.nome, COUNT(p.id) AS totalProdutos
  FROM categorias c
  LEFT JOIN produtos p ON p.categoria_id = c.id AND p.ativo = 1
`;

const buscar = (categoriaId) => db.prepare(`${SELECT} WHERE c.id = ? GROUP BY c.id`).get(categoriaId);

router.get('/', (_req, res) => {
  res.json(db.prepare(`${SELECT} GROUP BY c.id ORDER BY c.nome`).all());
});

router.get('/:id', (req, res) => {
  const cat = buscar(id(req.params.id));
  if (!cat) throw notFound('Categoria');
  res.json(cat);
});

router.post('/', (req, res) => {
  const nome = str(req.body?.nome, 'nome', { max: 120 });
  const { lastInsertRowid } = db.prepare('INSERT INTO categorias (nome) VALUES (?)').run(nome);
  res.status(201).json(buscar(lastInsertRowid));
});

router.put('/:id', (req, res) => {
  const categoriaId = id(req.params.id);
  const nome = str(req.body?.nome, 'nome', { max: 120 });
  const { changes } = db.prepare('UPDATE categorias SET nome = ? WHERE id = ?').run(nome, categoriaId);
  if (!changes) throw notFound('Categoria');
  res.json(buscar(categoriaId));
});

// Os produtos da categoria excluída ficam sem categoria (ON DELETE SET NULL).
router.delete('/:id', (req, res) => {
  const { changes } = db.prepare('DELETE FROM categorias WHERE id = ?').run(id(req.params.id));
  if (!changes) throw notFound('Categoria');
  res.status(204).end();
});

export default router;

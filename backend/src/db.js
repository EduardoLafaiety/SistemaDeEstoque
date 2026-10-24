import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname, resolve } from 'node:path';

const dbPath = resolve(process.env.DB_PATH ?? 'data/estoque.db');
mkdirSync(dirname(dbPath), { recursive: true });

export const db = new DatabaseSync(dbPath);

db.exec(`
  PRAGMA foreign_keys = ON;
  PRAGMA journal_mode = WAL;

  CREATE TABLE IF NOT EXISTS categorias (
    id    INTEGER PRIMARY KEY AUTOINCREMENT,
    nome  TEXT NOT NULL UNIQUE COLLATE NOCASE
  );

  CREATE TABLE IF NOT EXISTS produtos (
    id              INTEGER PRIMARY KEY AUTOINCREMENT,
    sku             TEXT NOT NULL UNIQUE COLLATE NOCASE,
    nome            TEXT NOT NULL,
    descricao       TEXT,
    categoria_id    INTEGER REFERENCES categorias(id) ON DELETE SET NULL,
    preco           REAL NOT NULL DEFAULT 0 CHECK (preco >= 0),
    quantidade      INTEGER NOT NULL DEFAULT 0 CHECK (quantidade >= 0),
    estoque_minimo  INTEGER NOT NULL DEFAULT 0 CHECK (estoque_minimo >= 0),
    criado_em       TEXT NOT NULL DEFAULT (datetime('now')),
    atualizado_em   TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS movimentacoes (
    id          INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id  INTEGER NOT NULL REFERENCES produtos(id) ON DELETE CASCADE,
    tipo        TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    quantidade  INTEGER NOT NULL CHECK (quantidade > 0),
    observacao  TEXT,
    criado_em   TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE INDEX IF NOT EXISTS idx_mov_produto ON movimentacoes(produto_id);
`);

/** Executa fn dentro de uma transação, com rollback em caso de erro. */
export function transaction(fn) {
  db.exec('BEGIN');
  try {
    const result = fn();
    db.exec('COMMIT');
    return result;
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }
}

function seed() {
  const { total } = db.prepare('SELECT COUNT(*) AS total FROM categorias').get();
  if (total > 0) return;

  transaction(() => {
    const insCat = db.prepare('INSERT INTO categorias (nome) VALUES (?)');
    const cats = ['Eletrônicos', 'Escritório', 'Limpeza'].map(
      (nome) => insCat.run(nome).lastInsertRowid,
    );
    const insProd = db.prepare(
      `INSERT INTO produtos (sku, nome, descricao, categoria_id, preco, quantidade, estoque_minimo)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    );
    insProd.run('ELE-001', 'Mouse sem fio', 'Mouse óptico 2.4GHz', cats[0], 79.9, 25, 10);
    insProd.run('ELE-002', 'Teclado USB', 'Teclado ABNT2', cats[0], 119.0, 4, 8);
    insProd.run('ESC-001', 'Resma papel A4', '500 folhas 75g', cats[1], 32.5, 60, 20);
    insProd.run('ESC-002', 'Caneta azul (cx 50)', null, cats[1], 45.0, 2, 5);
    insProd.run('LIM-001', 'Detergente 500ml', null, cats[2], 3.99, 120, 30);
  });
}

seed();

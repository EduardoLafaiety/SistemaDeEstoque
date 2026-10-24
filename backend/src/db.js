import { DatabaseSync } from 'node:sqlite';
import { mkdirSync } from 'node:fs';
import { dirname } from 'node:path';
import { config } from './config.js';

if (config.dbPath !== ':memory:') mkdirSync(dirname(config.dbPath), { recursive: true });

export const db = new DatabaseSync(config.dbPath);

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
    ativo           INTEGER NOT NULL DEFAULT 1,
    criado_em       TEXT NOT NULL DEFAULT (datetime('now')),
    atualizado_em   TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE TABLE IF NOT EXISTS movimentacoes (
    id                    INTEGER PRIMARY KEY AUTOINCREMENT,
    produto_id            INTEGER NOT NULL REFERENCES produtos(id) ON DELETE RESTRICT,
    tipo                  TEXT NOT NULL CHECK (tipo IN ('ENTRADA', 'SAIDA')),
    quantidade            INTEGER NOT NULL CHECK (quantidade > 0),
    quantidade_anterior   INTEGER,
    quantidade_posterior  INTEGER,
    observacao            TEXT,
    criado_em             TEXT NOT NULL DEFAULT (datetime('now'))
  );

  CREATE INDEX IF NOT EXISTS idx_mov_produto   ON movimentacoes(produto_id);
  CREATE INDEX IF NOT EXISTS idx_mov_criado_em ON movimentacoes(criado_em);
  CREATE INDEX IF NOT EXISTS idx_prod_categoria ON produtos(categoria_id);
`);

/** Migrações aditivas para bancos criados por versões anteriores do esquema. */
function adicionarColuna(tabela, coluna, definicao) {
  const existe = db.prepare(`SELECT 1 FROM pragma_table_info('${tabela}') WHERE name = ?`).get(coluna);
  if (!existe) db.exec(`ALTER TABLE ${tabela} ADD COLUMN ${coluna} ${definicao}`);
}
adicionarColuna('produtos', 'ativo', 'INTEGER NOT NULL DEFAULT 1');
adicionarColuna('movimentacoes', 'quantidade_anterior', 'INTEGER');
adicionarColuna('movimentacoes', 'quantidade_posterior', 'INTEGER');

/** Executa fn dentro de uma transação, com rollback em caso de erro. */
export function transaction(fn) {
  db.exec('BEGIN IMMEDIATE');
  try {
    const result = fn();
    db.exec('COMMIT');
    return result;
  } catch (err) {
    db.exec('ROLLBACK');
    throw err;
  }
}

/** Registra a movimentação inicial de estoque de um produto recém-criado. */
function registrarEstoqueInicial(produtoId, quantidade) {
  db.prepare(
    `INSERT INTO movimentacoes (produto_id, tipo, quantidade, quantidade_anterior, quantidade_posterior, observacao)
     VALUES (?, 'ENTRADA', ?, 0, ?, 'Estoque inicial')`,
  ).run(produtoId, quantidade, quantidade);
}

export function inserirProduto(p, quantidade = 0) {
  const { lastInsertRowid } = db
    .prepare(
      `INSERT INTO produtos (sku, nome, descricao, categoria_id, preco, quantidade, estoque_minimo)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    )
    .run(p.sku, p.nome, p.descricao, p.categoriaId, p.preco, quantidade, p.estoqueMinimo);
  if (quantidade > 0) registrarEstoqueInicial(lastInsertRowid, quantidade);
  return lastInsertRowid;
}

function seed() {
  const { total } = db.prepare('SELECT COUNT(*) AS total FROM categorias').get();
  if (total > 0) return;

  transaction(() => {
    const insCat = db.prepare('INSERT INTO categorias (nome) VALUES (?)');
    const [ele, esc, lim] = ['Eletrônicos', 'Escritório', 'Limpeza'].map(
      (nome) => insCat.run(nome).lastInsertRowid,
    );
    const produtos = [
      ['ELE-001', 'Mouse sem fio', 'Mouse óptico 2.4GHz', ele, 79.9, 25, 10],
      ['ELE-002', 'Teclado USB', 'Teclado ABNT2', ele, 119.0, 4, 8],
      ['ESC-001', 'Resma papel A4', '500 folhas 75g', esc, 32.5, 60, 20],
      ['ESC-002', 'Caneta azul (cx 50)', null, esc, 45.0, 2, 5],
      ['LIM-001', 'Detergente 500ml', null, lim, 3.99, 120, 30],
    ];
    for (const [sku, nome, descricao, categoriaId, preco, qtd, estoqueMinimo] of produtos) {
      inserirProduto({ sku, nome, descricao, categoriaId, preco, estoqueMinimo }, qtd);
    }
  });
}

if (config.seed) seed();

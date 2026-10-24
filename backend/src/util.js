import { z } from 'zod';
import { getDb } from './db.js';
import { HttpError } from './erros.js';

export const esquemaPaginacao = {
  pagina: z.coerce.number().int().min(1).default(1),
  limite: z.coerce.number().int().min(1).max(100).default(20),
};

export const textoOpcional = z.string().trim().max(1000).nullish();

/** Query string "true"/"false"/"1"/"0" → boolean. */
export const booleanoQuery = z
  .enum(['true', 'false', '1', '0'])
  .transform((v) => v === 'true' || v === '1')
  .optional();

/**
 * Executa uma listagem paginada.
 * @param {string} base   SQL a partir do FROM (com JOINs e WHERE), sem ORDER/LIMIT
 * @param {string} campos colunas do SELECT
 */
export function paginar({ campos, base, params = [], ordem, pagina, limite }) {
  const db = getDb();
  const { total } = db.prepare(`SELECT COUNT(*) AS total ${base}`).get(...params);
  const dados = db
    .prepare(`SELECT ${campos} ${base} ORDER BY ${ordem} LIMIT ? OFFSET ?`)
    .all(...params, limite, (pagina - 1) * limite);

  return {
    dados,
    paginacao: { pagina, limite, total, total_paginas: Math.ceil(total / limite) },
  };
}

/** UPDATE dinâmico apenas com os campos presentes em `dados`. */
export function atualizarRegistro(tabela, id, dados, { comTimestamp = false } = {}) {
  const colunas = Object.keys(dados).filter((c) => dados[c] !== undefined);
  if (colunas.length === 0) throw new HttpError(400, 'Nenhum campo para atualizar');

  const sets = colunas.map((c) => `${c} = ?`);
  if (comTimestamp) sets.push("atualizado_em = datetime('now')");

  const { changes } = getDb()
    .prepare(`UPDATE ${tabela} SET ${sets.join(', ')} WHERE id = ?`)
    .run(...colunas.map((c) => dados[c]), id);
  if (changes === 0) throw new HttpError(404, 'Registro não encontrado');
}

export function buscarOu404(sql, id, mensagem = 'Registro não encontrado') {
  const registro = getDb().prepare(sql).get(id);
  if (!registro) throw new HttpError(404, mensagem);
  return registro;
}

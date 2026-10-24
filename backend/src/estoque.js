import { getDb, transacao } from './db.js';
import { HttpError } from './erros.js';

export const SELECT_MOVIMENTACAO = `
  SELECT m.*, p.sku AS produto_sku, p.nome AS produto_nome, u.nome AS usuario_nome
  FROM movimentacoes m
  JOIN produtos p ON p.id = m.produto_id
  LEFT JOIN usuarios u ON u.id = m.usuario_id`;

/**
 * Aplica uma movimentação ao saldo do produto. Deve rodar dentro de uma transação.
 * - entrada: soma `quantidade` ao saldo
 * - saida:   subtrai `quantidade` (falha se o saldo for insuficiente)
 * - ajuste:  define o saldo como `quantidade` (inventário/contagem)
 */
export function aplicarMovimentacao({ produtoId, tipo, quantidade, motivo, usuarioId }) {
  const db = getDb();
  const produto = db.prepare('SELECT id, quantidade, ativo FROM produtos WHERE id = ?').get(produtoId);
  if (!produto) throw new HttpError(404, 'Produto não encontrado');
  if (!produto.ativo) throw new HttpError(422, 'Produto inativo não pode ser movimentado');

  const anterior = produto.quantidade;
  let posterior;
  switch (tipo) {
    case 'entrada':
      posterior = anterior + quantidade;
      break;
    case 'saida':
      if (quantidade > anterior) {
        throw new HttpError(409, 'Estoque insuficiente', { disponivel: anterior, solicitado: quantidade });
      }
      posterior = anterior - quantidade;
      break;
    case 'ajuste':
      posterior = quantidade;
      break;
    default:
      throw new HttpError(400, `Tipo de movimentação inválido: ${tipo}`);
  }

  db.prepare("UPDATE produtos SET quantidade = ?, atualizado_em = datetime('now') WHERE id = ?")
    .run(posterior, produtoId);

  const { lastInsertRowid } = db
    .prepare(
      `INSERT INTO movimentacoes
         (produto_id, tipo, quantidade, quantidade_anterior, quantidade_posterior, motivo, usuario_id)
       VALUES (?, ?, ?, ?, ?, ?, ?)`,
    )
    .run(produtoId, tipo, quantidade, anterior, posterior, motivo ?? null, usuarioId ?? null);

  return db.prepare(`${SELECT_MOVIMENTACAO} WHERE m.id = ?`).get(lastInsertRowid);
}

export function registrarMovimentacao(dados) {
  return transacao(() => aplicarMovimentacao(dados));
}

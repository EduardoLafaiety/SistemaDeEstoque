export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export const notFound = (what) => new HttpError(404, `${what} não encontrado(a).`);
export const badRequest = (msg) => new HttpError(400, msg);

/** Converte erros de constraint do SQLite em respostas HTTP legíveis. */
export function errorHandler(err, _req, res, _next) {
  if (err instanceof HttpError) {
    return res.status(err.status).json({ erro: err.message });
  }
  const msg = String(err?.message ?? '');
  if (msg.includes('UNIQUE constraint failed')) {
    const campo = msg.split('.').pop();
    return res.status(409).json({ erro: `Já existe um registro com este ${campo}.` });
  }
  if (msg.includes('FOREIGN KEY constraint failed')) {
    return res.status(400).json({ erro: 'Referência inválida.' });
  }
  if (msg.includes('CHECK constraint failed')) {
    return res.status(400).json({ erro: 'Valor inválido.' });
  }
  if (err?.type === 'entity.parse.failed') {
    return res.status(400).json({ erro: 'JSON inválido.' });
  }
  console.error(err);
  res.status(500).json({ erro: 'Erro interno do servidor.' });
}

/** Valida e normaliza números inteiros/decimais do corpo da requisição. */
export function num(value, campo, { int = false, min = 0, required = true } = {}) {
  if (value === undefined || value === null || value === '') {
    if (required) throw badRequest(`O campo "${campo}" é obrigatório.`);
    return undefined;
  }
  const n = Number(value);
  if (!Number.isFinite(n) || (int && !Number.isInteger(n)) || n < min) {
    throw badRequest(`O campo "${campo}" deve ser um número ${int ? 'inteiro ' : ''}>= ${min}.`);
  }
  return n;
}

export function str(value, campo, { required = true } = {}) {
  const s = typeof value === 'string' ? value.trim() : '';
  if (!s) {
    if (required) throw badRequest(`O campo "${campo}" é obrigatório.`);
    return null;
  }
  return s;
}

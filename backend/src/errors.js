export class HttpError extends Error {
  constructor(status, message) {
    super(message);
    this.status = status;
  }
}

export const notFound = (what) => new HttpError(404, `${what} não encontrado(a).`);
export const badRequest = (msg) => new HttpError(400, msg);
export const conflict = (msg) => new HttpError(409, msg);

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
  const n = typeof value === 'number' || typeof value === 'string' ? Number(value) : NaN;
  if (!Number.isFinite(n) || (int && !Number.isInteger(n)) || n < min) {
    throw badRequest(`O campo "${campo}" deve ser um número ${int ? 'inteiro ' : ''}>= ${min}.`);
  }
  return n;
}

export function str(value, campo, { required = true, max = 500 } = {}) {
  const s = typeof value === 'string' ? value.trim() : '';
  if (!s) {
    if (required) throw badRequest(`O campo "${campo}" é obrigatório.`);
    return null;
  }
  if (s.length > max) throw badRequest(`O campo "${campo}" deve ter no máximo ${max} caracteres.`);
  return s;
}

/** Escapa % _ e \ para que a busca com LIKE (usando ESCAPE '\') seja literal. */
export function escaparLike(texto) {
  return String(texto).replace(/[\\%_]/g, (c) => `\\${c}`);
}

/** Valida o parâmetro :id da rota. */
export function id(value) {
  const n = Number(value);
  if (!Number.isInteger(n) || n < 1) throw badRequest('Identificador inválido.');
  return n;
}

/** Valida uma data AAAA-MM-DD vinda da query string. */
export function data(value, campo) {
  if (value === undefined || value === '') return undefined;
  if (typeof value !== 'string' || !/^\d{4}-\d{2}-\d{2}$/.test(value) || Number.isNaN(Date.parse(value))) {
    throw badRequest(`O parâmetro "${campo}" deve estar no formato AAAA-MM-DD.`);
  }
  return value;
}

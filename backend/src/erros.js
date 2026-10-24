import { ZodError } from 'zod';

export class HttpError extends Error {
  constructor(status, mensagem, detalhes) {
    super(mensagem);
    this.status = status;
    this.detalhes = detalhes;
  }
}

export function naoEncontrado(req, res) {
  res.status(404).json({ erro: `Rota não encontrada: ${req.method} ${req.originalUrl}` });
}

// eslint-disable-next-line no-unused-vars
export function tratarErros(err, req, res, next) {
  if (err instanceof HttpError) {
    return res.status(err.status).json({ erro: err.message, detalhes: err.detalhes });
  }

  if (err instanceof ZodError) {
    return res.status(400).json({
      erro: 'Dados inválidos',
      detalhes: err.issues.map((i) => ({ campo: i.path.join('.'), mensagem: i.message })),
    });
  }

  if (err?.type === 'entity.parse.failed') {
    return res.status(400).json({ erro: 'JSON inválido no corpo da requisição' });
  }

  const msg = String(err?.message ?? '');
  if (msg.includes('UNIQUE constraint failed')) {
    const campo = msg.split('.').pop();
    return res.status(409).json({ erro: `Já existe um registro com este valor de '${campo}'` });
  }
  if (msg.includes('FOREIGN KEY constraint failed')) {
    return res.status(409).json({ erro: 'Operação viola um relacionamento: registro relacionado inexistente ou em uso' });
  }

  console.error(err);
  res.status(500).json({ erro: 'Erro interno do servidor' });
}

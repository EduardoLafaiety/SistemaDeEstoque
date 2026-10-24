import { existsSync } from 'node:fs';
import { dirname, resolve } from 'node:path';
import { fileURLToPath } from 'node:url';

/** Pasta raiz do backend: caminhos relativos do .env são resolvidos a partir dela, não do cwd. */
const raiz = resolve(dirname(fileURLToPath(import.meta.url)), '..');
const arquivoEnv = resolve(raiz, '.env');

// Variáveis já definidas no ambiente têm prioridade sobre o .env.
if (process.env.NODE_ENV !== 'test' && existsSync(arquivoEnv)) {
  process.loadEnvFile(arquivoEnv);
}

const env = process.env;

export const config = {
  porta: Number(env.PORT ?? 3000),
  corsOrigin: env.CORS_ORIGIN ?? 'http://localhost:4200',
  dbPath: env.DB_PATH === ':memory:' ? ':memory:' : resolve(raiz, env.DB_PATH ?? 'data/estoque.db'),
  /** Popula categorias e produtos de exemplo quando o banco está vazio. */
  seed: (env.SEED ?? 'true') !== 'false',
};

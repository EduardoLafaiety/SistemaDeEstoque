import { existsSync } from 'node:fs';
import { fileURLToPath } from 'node:url';
import path from 'node:path';

const raiz = path.resolve(path.dirname(fileURLToPath(import.meta.url)), '..');
const arquivoEnv = path.join(raiz, '.env');

if (process.env.NODE_ENV !== 'test' && existsSync(arquivoEnv)) {
  process.loadEnvFile(arquivoEnv);
}

const env = process.env;

if (!env.JWT_SECRET) {
  throw new Error('JWT_SECRET não definido. Copie .env.example para .env e configure-o.');
}

export const config = {
  porta: Number(env.PORT ?? 3333),
  dbPath: env.DB_PATH === ':memory:' ? ':memory:' : path.resolve(raiz, env.DB_PATH ?? './data/estoque.db'),
  jwtSecret: env.JWT_SECRET,
  jwtExpiresIn: env.JWT_EXPIRES_IN ?? '8h',
  corsOrigin: env.CORS_ORIGIN ?? '*',
  admin: {
    nome: env.ADMIN_NOME ?? 'Administrador',
    email: env.ADMIN_EMAIL,
    senha: env.ADMIN_SENHA,
  },
};

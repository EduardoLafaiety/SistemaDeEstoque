import { app } from './app.js';
import { config } from './config.js';
import { db } from './db.js';

const server = app.listen(config.porta, () => {
  console.log(`API do Sistema de Estoque rodando em http://localhost:${config.porta}/api`);
});

function encerrar(sinal) {
  console.log(`\n${sinal} recebido, encerrando...`);
  server.close(() => {
    db.close();
    process.exit(0);
  });
  setTimeout(() => process.exit(1), 5000).unref();
}

process.on('SIGINT', () => encerrar('SIGINT'));
process.on('SIGTERM', () => encerrar('SIGTERM'));

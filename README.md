# Sistema de Estoque

Controle de estoque com cadastro de produtos e categorias, registro de entradas/saídas e dashboard.

| Parte      | Stack                                   | Porta |
| ---------- | --------------------------------------- | ----- |
| `backend`  | Node.js 22.13+ · Express 5 · SQLite (`node:sqlite`) | 3000  |
| `frontend` | Angular 22 (standalone, signals, zoneless) | 4200  |

## Como rodar

Em dois terminais:

```bash
cd backend && npm install && npm run dev
```

```bash
cd frontend && npm install && npm start
```

Acesse http://localhost:4200. O `ng serve` usa `frontend/proxy.conf.json` para encaminhar `/api` ao backend,
então não é preciso configurar URL nem CORS em desenvolvimento.

Na primeira execução o banco é criado em `backend/data/estoque.db` com algumas categorias e produtos de exemplo.

### Variáveis do backend

| Variável      | Padrão                  |
| ------------- | ----------------------- |
| `PORT`        | `3000`                  |
| `DB_PATH`     | `data/estoque.db`       |
| `CORS_ORIGIN` | `http://localhost:4200` |

## API

Erros retornam `{ "erro": "mensagem" }` com o status HTTP apropriado (400, 404, 409).

| Método | Rota                      | Descrição |
| ------ | ------------------------- | --------- |
| GET    | `/api/dashboard`          | Totais, produtos abaixo do mínimo e últimas movimentações |
| GET    | `/api/categorias`         | Lista categorias (com `totalProdutos`) |
| POST   | `/api/categorias`         | `{ nome }` |
| PUT    | `/api/categorias/:id`     | `{ nome }` |
| DELETE | `/api/categorias/:id`     | Produtos da categoria ficam sem categoria |
| GET    | `/api/produtos`           | Filtros: `busca`, `categoriaId`, `baixoEstoque=true` |
| GET    | `/api/produtos/:id`       | |
| POST   | `/api/produtos`           | `{ sku, nome, descricao?, categoriaId?, preco, estoqueMinimo?, quantidade? }` |
| PUT    | `/api/produtos/:id`       | Mesmo corpo, sem `quantidade` |
| DELETE | `/api/produtos/:id`       | Remove também o histórico |
| GET    | `/api/movimentacoes`      | Filtros: `produtoId`, `tipo`, `limite` (padrão 100) |
| POST   | `/api/movimentacoes`      | `{ produtoId, tipo: "ENTRADA" \| "SAIDA", quantidade, observacao? }` |

A quantidade de um produto só é definida na criação; depois disso muda apenas por movimentações,
que rodam em transação e recusam saídas maiores que o saldo.

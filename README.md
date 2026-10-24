# Sistema de Estoque

Aplicação web de controle de estoque com cadastro de produtos e categorias, registro de entradas e saídas
e um dashboard com indicadores e alertas de estoque baixo.

![Node.js](https://img.shields.io/badge/Node.js-22.13+-339933?logo=node.js&logoColor=white)
![Express](https://img.shields.io/badge/Express-5-000000?logo=express&logoColor=white)
![SQLite](https://img.shields.io/badge/SQLite-node%3Asqlite-003B57?logo=sqlite&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-22-DD0031?logo=angular&logoColor=white)
![TypeScript](https://img.shields.io/badge/TypeScript-6-3178C6?logo=typescript&logoColor=white)

---

## Sumário

- [Funcionalidades](#funcionalidades)
- [Arquitetura](#arquitetura)
- [Estrutura do repositório](#estrutura-do-repositório)
- [Pré-requisitos](#pré-requisitos)
- [Como rodar](#como-rodar)
- [Configuração](#configuração)
- [Modelo de dados](#modelo-de-dados)
- [API REST](#api-rest)
- [Frontend](#frontend)
- [Regras de negócio](#regras-de-negócio)
- [Módulo Spring Boot (em desenvolvimento)](#módulo-spring-boot-em-desenvolvimento)
- [Roadmap](#roadmap)

---

## Funcionalidades

- **Dashboard**: total de produtos, itens em estoque, valor total do inventário, quantidade de categorias,
  os produtos abaixo do estoque mínimo e as últimas movimentações.
- **Produtos**: cadastro com SKU único, descrição, categoria, preço, quantidade inicial e estoque mínimo.
  Busca por nome/SKU, filtro por categoria e filtro de "estoque baixo".
- **Categorias**: CRUD com contagem de produtos por categoria.
- **Movimentações**: registro de **entradas** e **saídas** com observação. O saldo é atualizado em transação,
  e saídas maiores que o saldo disponível são recusadas.
- **Dados de exemplo**: na primeira execução o banco é criado e populado com categorias e produtos.

## Arquitetura

```
┌──────────────────────────┐   /api (proxy)  ┌──────────────────────────┐        ┌──────────────┐
│ Frontend Angular 22      │ ──────────────▶ │ Backend Express 5        │ ─────▶ │ SQLite       │
│ localhost:4200           │                 │ localhost:3000/api       │        │ data/*.db    │
│ standalone · signals     │ ◀────────────── │ JSON REST                │ ◀───── │ (node:sqlite)│
└──────────────────────────┘      JSON       └──────────────────────────┘        └──────────────┘
```

| Parte      | Stack                                                    | Porta |
| ---------- | -------------------------------------------------------- | ----- |
| `backend`  | Node.js 22.13+ · Express 5 · SQLite nativo (`node:sqlite`) | 3000  |
| `frontend` | Angular 22 (standalone components, signals, zoneless)    | 4200  |

O backend não usa ORM nem driver externo: o SQLite vem embutido no Node (`node:sqlite`), então as únicas
dependências de runtime são `express` e `cors`.

## Estrutura do repositório

```
.
├── backend/                  # API REST (Node.js + Express + SQLite)
│   ├── src/
│   │   ├── server.js         # Ponto de entrada: sobe o HTTP e encerra com segurança
│   │   ├── app.js            # App Express: middlewares e registro das rotas
│   │   ├── config.js         # Lê .env / variáveis de ambiente
│   │   ├── db.js             # Conexão, schema, migrações, transações e dados de exemplo
│   │   ├── errors.js         # HttpError, validação e tratamento de erros
│   │   └── routes/           # categorias, produtos, movimentacoes, dashboard
│   ├── tests/                # Testes de integração da API (node:test)
│   ├── .env.example          # Modelo de configuração
│   └── data/                 # Banco SQLite (gerado em runtime, ignorado pelo git)
│
├── frontend/                 # SPA Angular
│   ├── proxy.conf.json       # Encaminha /api → http://localhost:3000
│   └── src/app/
│       ├── core/             # ApiService, modelos, interceptor de erros, toasts, pipes
│       └── pages/            # dashboard, produtos, produto-form, categorias, movimentacoes
│
├── src/ · pom.xml · mvnw     # Módulo Spring Boot (em desenvolvimento, veja abaixo)
└── README.md
```

## Pré-requisitos

- **Node.js 22.13 ou superior**: o backend usa o módulo nativo `node:sqlite`.
- **npm** (o frontend declara `npm@11`).

Para conferir:

```bash
node --version
```

## Como rodar

Em dois terminais:

**1. Backend**

```bash
cd backend
npm install
npm run dev
```

A API sobe em http://localhost:3000/api. O `npm run dev` usa `node --watch` e reinicia a cada alteração.
Para rodar sem watch, use `npm start`.

**2. Frontend**

```bash
cd frontend
npm install
npm start
```

Acesse **http://localhost:4200**.

Em desenvolvimento, o `ng serve` usa o `frontend/proxy.conf.json` para encaminhar as chamadas `/api` ao backend,
então não é preciso configurar URL nem CORS.

Para verificar se a API está no ar:

```bash
curl http://localhost:3000/api/health
```

### Build de produção do frontend

```bash
cd frontend
npm run build
```

Os arquivos gerados ficam em `frontend/dist/frontend/browser/`.

### Testes do backend

```bash
cd backend
npm test
```

Usa o runner nativo `node:test` com banco em memória: não toca em `backend/data/`.

### Testes do frontend

```bash
cd frontend
npm test
```

O projeto usa Vitest com jsdom.

### Resetar o banco

Pare o backend e apague a pasta `backend/data/`. Na próxima execução o banco é recriado com os dados de exemplo.

## Configuração

O backend lê variáveis de ambiente (todas opcionais). Elas também podem ficar em `backend/.env`
(copie de `backend/.env.example`); variáveis já definidas no ambiente têm prioridade.

| Variável      | Padrão                  | Descrição                                 |
| ------------- | ----------------------- | ----------------------------------------- |
| `PORT`        | `3000`                  | Porta HTTP da API                         |
| `DB_PATH`     | `data/estoque.db`       | Caminho do SQLite, relativo a `backend/` (ou `:memory:`) |
| `CORS_ORIGIN` | `http://localhost:4200` | Origem permitida pelo CORS                |
| `SEED`        | `true`                  | Popula dados de exemplo se o banco estiver vazio |

Exemplo (bash):

```bash
PORT=4000 DB_PATH=/tmp/estoque.db npm start
```

> Se mudar a porta do backend, ajuste também o `target` em `frontend/proxy.conf.json`.

## Modelo de dados

```
categorias 1 ──── N produtos 1 ──── N movimentacoes
```

**categorias**

| Coluna | Tipo    | Regras                                  |
| ------ | ------- | --------------------------------------- |
| `id`   | INTEGER | PK, autoincremento                      |
| `nome` | TEXT    | obrigatório, único (sem diferenciar maiúsculas) |

**produtos**

| Coluna           | Tipo    | Regras                                                  |
| ---------------- | ------- | ------------------------------------------------------- |
| `id`             | INTEGER | PK, autoincremento                                      |
| `sku`            | TEXT    | obrigatório, único (sem diferenciar maiúsculas)         |
| `nome`           | TEXT    | obrigatório                                             |
| `descricao`      | TEXT    | opcional                                                |
| `categoria_id`   | INTEGER | FK → `categorias`, `ON DELETE SET NULL`                 |
| `preco`          | REAL    | `>= 0`                                                  |
| `quantidade`     | INTEGER | `>= 0`                                                  |
| `estoque_minimo` | INTEGER | `>= 0`                                                  |
| `ativo`          | INTEGER | `1` ativo, `0` inativado (excluído com histórico)       |
| `criado_em`      | TEXT    | data/hora UTC                                           |
| `atualizado_em`  | TEXT    | data/hora UTC                                           |

**movimentacoes**

| Coluna       | Tipo    | Regras                                          |
| ------------ | ------- | ----------------------------------------------- |
| `id`         | INTEGER | PK, autoincremento                              |
| `produto_id` | INTEGER | FK → `produtos` (`RESTRICT` em bancos novos)    |
| `tipo`       | TEXT    | `ENTRADA` ou `SAIDA`                            |
| `quantidade` | INTEGER | `> 0`                                           |
| `quantidade_anterior`  | INTEGER | saldo antes da movimentação          |
| `quantidade_posterior` | INTEGER | saldo depois da movimentação         |
| `observacao` | TEXT    | opcional                                        |
| `criado_em`  | TEXT    | data/hora UTC                                   |

O banco roda com `foreign_keys = ON` e `journal_mode = WAL`. Bancos criados por versões anteriores
recebem as colunas novas automaticamente na inicialização (migração aditiva, sem perda de dados).

## API REST

Base: `http://localhost:3000/api`. Todas as requisições e respostas usam JSON.

### Endpoints

| Método | Rota                  | Descrição                                                    |
| ------ | --------------------- | ------------------------------------------------------------ |
| GET    | `/health`             | Verificação de saúde (`{ "status": "ok" }`)                  |
| GET    | `/dashboard`          | Totais, produtos abaixo do mínimo e últimas movimentações    |
| GET    | `/categorias`         | Lista as categorias (com `totalProdutos`)                    |
| GET    | `/categorias/:id`     | Detalha uma categoria                                        |
| POST   | `/categorias`         | Cria uma categoria: `{ nome }`                               |
| PUT    | `/categorias/:id`     | Renomeia uma categoria: `{ nome }`                           |
| DELETE | `/categorias/:id`     | Remove a categoria; os produtos dela ficam sem categoria     |
| GET    | `/produtos`           | Lista os ativos. Filtros: `busca`, `categoriaId`, `baixoEstoque=true`, `inativos=true` |
| GET    | `/produtos/:id`       | Detalha um produto                                           |
| POST   | `/produtos`           | Cria um produto (veja o corpo abaixo)                        |
| PUT    | `/produtos/:id`       | Atualiza um produto (mesmo corpo, sem `quantidade`)          |
| DELETE | `/produtos/:id`       | Inativa o produto se tiver movimentações; senão, exclui      |
| POST   | `/produtos/:id/reativar` | Reativa um produto inativado                              |
| GET    | `/movimentacoes`      | Lista. Filtros: `produtoId`, `tipo`, `inicio`/`fim` (AAAA-MM-DD), `limite` (padrão 100, máx. 500) |
| GET    | `/movimentacoes/:id`  | Detalha uma movimentação                                     |
| POST   | `/movimentacoes`      | Registra uma entrada ou saída                                |

### Exemplos

**Criar produto**

```http
POST /api/produtos
Content-Type: application/json

{
  "sku": "ELE-003",
  "nome": "Monitor 24\"",
  "descricao": "Full HD, HDMI",
  "categoriaId": 1,
  "preco": 899.9,
  "estoqueMinimo": 3,
  "quantidade": 10
}
```

Resposta `201 Created`:

```json
{
  "id": 6,
  "sku": "ELE-003",
  "nome": "Monitor 24\"",
  "descricao": "Full HD, HDMI",
  "preco": 899.9,
  "quantidade": 10,
  "estoqueMinimo": 3,
  "categoriaId": 1,
  "categoriaNome": "Eletrônicos",
  "ativo": true,
  "criadoEm": "2026-10-24 14:00:00",
  "atualizadoEm": "2026-10-24 14:00:00"
}
```

**Registrar saída**

```bash
curl -X POST http://localhost:3000/api/movimentacoes \
  -H "Content-Type: application/json" \
  -d '{ "produtoId": 6, "tipo": "SAIDA", "quantidade": 2, "observacao": "Pedido #123" }'
```

**Listar produtos com estoque baixo**

```bash
curl "http://localhost:3000/api/produtos?baixoEstoque=true"
```

### Erros

Os erros retornam `{ "erro": "mensagem" }` com o status HTTP correspondente:

| Status | Quando                                                                 |
| ------ | ---------------------------------------------------------------------- |
| `400`  | Campo obrigatório ausente, valor inválido, id inválido, JSON malformado ou produto inativo |
| `404`  | Recurso ou rota não encontrados                                        |
| `409`  | Violação de unicidade (SKU ou nome de categoria já existente) ou estoque insuficiente |
| `500`  | Erro inesperado                                                        |

```json
{ "erro": "Estoque insuficiente. Disponível: 4." }
```

## Frontend

SPA em Angular 22 com componentes standalone, signals e change detection sem Zone.js. Todas as páginas
usam lazy loading.

| Rota               | Página                                                |
| ------------------ | ----------------------------------------------------- |
| `/`                | Dashboard com indicadores, estoque baixo e últimas movimentações |
| `/produtos`        | Lista de produtos com busca e filtros                 |
| `/produtos/novo`   | Cadastro de produto                                   |
| `/produtos/:id`    | Edição de produto                                     |
| `/categorias`      | Gestão de categorias                                  |
| `/movimentacoes`   | Registro e histórico de entradas e saídas             |

Organização em `src/app/core/`:

- `api.service.ts`: acesso tipado à API REST.
- `models.ts`: interfaces de `Produto`, `Categoria`, `Movimentacao` e `Dashboard`.
- `error.interceptor.ts`: intercepta erros HTTP e mostra a mensagem `erro` da API.
- `toast.service.ts`: notificações de sucesso e erro.
- `data-utc.pipe.ts`: exibe no fuso local as datas UTC salvas pelo SQLite.

## Regras de negócio

- A **quantidade** de um produto só é informada na criação, e gera uma `ENTRADA` "Estoque inicial".
  Depois disso ela muda **apenas por movimentações**, o que mantém o histórico consistente com o saldo.
- Cada movimentação guarda o saldo **anterior** e **posterior**. Movimentações são imutáveis: para corrigir
  um lançamento, registre a movimentação inversa.
- Cada movimentação roda em **transação**: o saldo é atualizado e a movimentação é registrada juntos,
  ou nenhum dos dois acontece.
- **Saídas** maiores que o saldo disponível são recusadas com `409`.
- Um produto está com **estoque baixo** quando `quantidade <= estoque_minimo`.
- O **valor total** do inventário é `Σ quantidade × preço`.
- Excluir uma **categoria** não exclui os produtos dela: eles ficam sem categoria.
- Excluir um **produto** com movimentações apenas o **inativa** (o histórico é preservado e ele some das
  listagens e do dashboard); sem movimentações, ele é excluído de fato. Produtos inativos não podem ser movimentados.
- SKU e nome de categoria são únicos, sem diferenciar maiúsculas de minúsculas.

## Módulo Spring Boot (em desenvolvimento)

A raiz do repositório (`pom.xml`, `mvnw`, `src/main/java/com/estoque`) contém uma versão em
**Java/Spring Boot** do sistema, ainda em construção. A proposta é um controle de estoque mais completo,
inspirado no módulo de Inventário do Odoo:

- Armazéns e locais de estoque hierárquicos
- Saldos por local (quants) e movimentos de estoque
- Operações de recebimento e expedição (pickings)
- Pedidos de compra e ordens de produção
- **Regras de reposição** (estoque mínimo/máximo) com tela de reabastecimento e agendamento automático
- Parceiros (fornecedores/clientes) e usuários com papéis

| Stack prevista |                                                                 |
| -------------- | --------------------------------------------------------------- |
| Linguagem      | Java 25                                                         |
| Framework      | Spring Boot 4 (Web MVC, Data JPA, Security, Validation, Actuator) |
| Interface      | Thymeleaf + Bootstrap                                           |
| Banco          | PostgreSQL com migrações Flyway                                 |
| Documentação   | springdoc-openapi (Swagger UI)                                  |
| Testes         | JUnit + Testcontainers                                          |

> Esse módulo ainda não está funcional. A aplicação utilizável hoje é a dupla `backend/` + `frontend/`.

## Roadmap

- [ ] Autenticação com JWT e perfis de usuário
- [ ] Cadastro de fornecedores
- [ ] Paginação nas listagens
- [ ] Testes automatizados da API
- [ ] Exportação de relatórios (CSV)
- [ ] Concluir o módulo Spring Boot (reposição automática, compras e produção)

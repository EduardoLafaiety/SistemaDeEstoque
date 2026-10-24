import { test, before, after } from 'node:test';
import assert from 'node:assert/strict';

process.env.NODE_ENV = 'test';
process.env.DB_PATH = ':memory:';
process.env.SEED = 'false';

const { app } = await import('../src/app.js');

let server;
let base;

before(async () => {
  server = app.listen(0);
  await new Promise((r) => server.once('listening', r));
  base = `http://localhost:${server.address().port}/api`;
});

after(() => server.close());

async function api(metodo, caminho, corpo) {
  const res = await fetch(base + caminho, {
    method: metodo,
    headers: corpo ? { 'Content-Type': 'application/json' } : {},
    body: corpo ? JSON.stringify(corpo) : undefined,
  });
  const texto = await res.text();
  return { status: res.status, body: texto ? JSON.parse(texto) : null };
}

const produtoBase = { sku: 'tst-001', nome: 'Produto teste', descricao: null, preco: 10.5, estoqueMinimo: 5 };

test('health', async () => {
  assert.deepEqual((await api('GET', '/health')).body, { status: 'ok' });
});

test('fluxo completo: categoria, produto, movimentações e dashboard', async () => {
  const cat = await api('POST', '/categorias', { nome: 'Ferramentas' });
  assert.equal(cat.status, 201);
  assert.equal(cat.body.totalProdutos, 0);

  const dup = await api('POST', '/categorias', { nome: 'ferramentas' });
  assert.equal(dup.status, 409);

  const prod = await api('POST', '/produtos', { ...produtoBase, categoriaId: cat.body.id, quantidade: 10 });
  assert.equal(prod.status, 201);
  assert.equal(prod.body.sku, 'TST-001');
  assert.equal(prod.body.quantidade, 10);
  assert.equal(prod.body.categoriaNome, 'Ferramentas');
  const pid = prod.body.id;

  // Estoque inicial gera uma ENTRADA no histórico
  let hist = await api('GET', `/movimentacoes?produtoId=${pid}`);
  assert.equal(hist.body.length, 1);
  assert.equal(hist.body[0].observacao, 'Estoque inicial');

  const saida = await api('POST', '/movimentacoes', { produtoId: pid, tipo: 'saida', quantidade: 7 });
  assert.equal(saida.status, 201);
  assert.equal(saida.body.tipo, 'SAIDA');
  assert.equal(saida.body.quantidadeAnterior, 10);
  assert.equal(saida.body.quantidadePosterior, 3);

  const excesso = await api('POST', '/movimentacoes', { produtoId: pid, tipo: 'SAIDA', quantidade: 4 });
  assert.equal(excesso.status, 409);
  assert.match(excesso.body.erro, /insuficiente/);
  assert.equal((await api('GET', `/produtos/${pid}`)).body.quantidade, 3, 'saldo não muda após falha');

  const baixo = await api('GET', '/produtos?baixoEstoque=true');
  assert.deepEqual(baixo.body.map((p) => p.id), [pid]);

  const dash = await api('GET', '/dashboard');
  assert.equal(dash.body.totalProdutos, 1);
  assert.equal(dash.body.totalItens, 3);
  assert.equal(dash.body.valorTotal, 31.5);
  assert.equal(dash.body.produtosBaixoEstoque, 1);
  assert.equal(dash.body.ultimasMovimentacoes.length, 2);

  // Produto com histórico é inativado, não apagado
  assert.equal((await api('DELETE', `/produtos/${pid}`)).status, 204);
  assert.equal((await api('GET', '/produtos')).body.length, 0);
  assert.equal((await api('GET', `/produtos/${pid}`)).body.ativo, false);
  hist = await api('GET', `/movimentacoes?produtoId=${pid}`);
  assert.equal(hist.body.length, 2, 'histórico preservado');
  const mov = await api('POST', '/movimentacoes', { produtoId: pid, tipo: 'ENTRADA', quantidade: 1 });
  assert.equal(mov.status, 400);

  const reativado = await api('POST', `/produtos/${pid}/reativar`);
  assert.equal(reativado.body.ativo, true);
});

test('produto sem histórico é excluído de fato', async () => {
  const prod = await api('POST', '/produtos', { ...produtoBase, sku: 'TST-002' });
  assert.equal(prod.body.quantidade, 0);
  assert.equal((await api('DELETE', `/produtos/${prod.body.id}`)).status, 204);
  assert.equal((await api('GET', `/produtos/${prod.body.id}`)).status, 404);
});

test('validações', async () => {
  assert.equal((await api('POST', '/produtos', { ...produtoBase, sku: '' })).status, 400);
  assert.equal((await api('POST', '/produtos', { ...produtoBase, sku: 'X1', preco: -1 })).status, 400);
  assert.equal((await api('POST', '/produtos', { ...produtoBase, sku: 'X2', categoriaId: 999 })).status, 400);
  assert.equal((await api('POST', '/produtos', { ...produtoBase, sku: 'X3', quantidade: 1.5 })).status, 400);
  assert.equal((await api('POST', '/movimentacoes', { produtoId: 1, tipo: 'TROCA', quantidade: 1 })).status, 400);
  assert.equal((await api('POST', '/movimentacoes', { produtoId: 999, tipo: 'ENTRADA', quantidade: 1 })).status, 404);
  assert.equal((await api('GET', '/produtos/abc')).status, 400);
  assert.equal((await api('GET', '/movimentacoes?inicio=24/10/2026')).status, 400);
  assert.equal((await api('GET', '/nao-existe')).status, 404);

  const json = await fetch(`${base}/categorias`, {
    method: 'POST', headers: { 'Content-Type': 'application/json' }, body: '{quebrado',
  });
  assert.equal(json.status, 400);
});

test('busca trata % _ e \\ como texto literal', async () => {
  await api('POST', '/produtos', { ...produtoBase, sku: 'BUS-001', nome: 'Desconto 50% off' });
  await api('POST', '/produtos', { ...produtoBase, sku: 'BUS-002', nome: 'Cabo_USB' });
  await api('POST', '/produtos', { ...produtoBase, sku: 'BUS-003', nome: 'Pasta C:\\docs' });
  const nomes = async (q) =>
    (await api('GET', `/produtos?busca=${encodeURIComponent(q)}`)).body.map((p) => p.nome);

  assert.deepEqual(await nomes('%'), ['Desconto 50% off']);
  assert.deepEqual(await nomes('_'), ['Cabo_USB']);
  assert.deepEqual(await nomes('\\'), ['Pasta C:\\docs']);
  assert.deepEqual(await nomes('bus-00'), ['Cabo_USB', 'Desconto 50% off', 'Pasta C:\\docs']);
});

test('excluir categoria deixa produtos sem categoria', async () => {
  const cat = await api('POST', '/categorias', { nome: 'Temporária' });
  const prod = await api('POST', '/produtos', { ...produtoBase, sku: 'TST-003', categoriaId: cat.body.id });
  assert.equal((await api('DELETE', `/categorias/${cat.body.id}`)).status, 204);
  assert.equal((await api('GET', `/produtos/${prod.body.id}`)).body.categoriaId, null);
});

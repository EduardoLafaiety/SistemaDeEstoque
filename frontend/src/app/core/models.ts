export interface Categoria {
  id: number;
  nome: string;
  totalProdutos: number;
}

export interface Produto {
  id: number;
  sku: string;
  nome: string;
  descricao: string | null;
  preco: number;
  quantidade: number;
  estoqueMinimo: number;
  categoriaId: number | null;
  categoriaNome: string | null;
  criadoEm: string;
  atualizadoEm: string;
}

export interface ProdutoInput {
  sku: string;
  nome: string;
  descricao: string | null;
  preco: number;
  estoqueMinimo: number;
  categoriaId: number | null;
  /** Usado apenas na criação; depois a quantidade muda via movimentações. */
  quantidade?: number;
}

export type TipoMovimentacao = 'ENTRADA' | 'SAIDA';

export interface Movimentacao {
  id: number;
  tipo: TipoMovimentacao;
  quantidade: number;
  observacao: string | null;
  criadoEm: string;
  produtoId: number;
  produtoNome: string;
  produtoSku: string;
}

export interface MovimentacaoInput {
  produtoId: number;
  tipo: TipoMovimentacao;
  quantidade: number;
  observacao?: string | null;
}

export interface Dashboard {
  totalProdutos: number;
  totalItens: number;
  valorTotal: number;
  produtosBaixoEstoque: number;
  totalCategorias: number;
  baixoEstoque: Produto[];
  ultimasMovimentacoes: Movimentacao[];
}

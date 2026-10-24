import { Routes } from '@angular/router';

export const routes: Routes = [
  {
    path: '',
    title: 'Dashboard · Estoque',
    loadComponent: () => import('./pages/dashboard/dashboard').then((m) => m.DashboardPage),
  },
  {
    path: 'produtos',
    title: 'Produtos · Estoque',
    loadComponent: () => import('./pages/produtos/produtos').then((m) => m.ProdutosPage),
  },
  {
    path: 'produtos/novo',
    title: 'Novo produto · Estoque',
    loadComponent: () => import('./pages/produto-form/produto-form').then((m) => m.ProdutoFormPage),
  },
  {
    path: 'produtos/:id',
    title: 'Editar produto · Estoque',
    loadComponent: () => import('./pages/produto-form/produto-form').then((m) => m.ProdutoFormPage),
  },
  {
    path: 'categorias',
    title: 'Categorias · Estoque',
    loadComponent: () => import('./pages/categorias/categorias').then((m) => m.CategoriasPage),
  },
  {
    path: 'movimentacoes',
    title: 'Movimentações · Estoque',
    loadComponent: () =>
      import('./pages/movimentacoes/movimentacoes').then((m) => m.MovimentacoesPage),
  },
  { path: '**', redirectTo: '' },
];

import { HttpClient, HttpParams } from '@angular/common/http';
import { Injectable, inject } from '@angular/core';
import {
  Categoria,
  Dashboard,
  Movimentacao,
  MovimentacaoInput,
  Produto,
  ProdutoInput,
  TipoMovimentacao,
} from './models';

/** Base da API. Em dev, o proxy do `ng serve` encaminha /api para http://localhost:3000. */
export const API_URL = '/api';

/** Remove filtros vazios para não enviar `?busca=` na query string. */
function params(obj: Record<string, string | number | boolean | null | undefined>): HttpParams {
  let p = new HttpParams();
  for (const [k, v] of Object.entries(obj)) {
    if (v !== null && v !== undefined && v !== '' && v !== false) p = p.set(k, String(v));
  }
  return p;
}

@Injectable({ providedIn: 'root' })
export class DashboardService {
  private http = inject(HttpClient);
  obter() {
    return this.http.get<Dashboard>(`${API_URL}/dashboard`);
  }
}

@Injectable({ providedIn: 'root' })
export class CategoriaService {
  private http = inject(HttpClient);
  private url = `${API_URL}/categorias`;

  listar() {
    return this.http.get<Categoria[]>(this.url);
  }
  criar(nome: string) {
    return this.http.post<Categoria>(this.url, { nome });
  }
  atualizar(id: number, nome: string) {
    return this.http.put<Categoria>(`${this.url}/${id}`, { nome });
  }
  excluir(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

export interface FiltroProdutos {
  busca?: string;
  categoriaId?: number | null;
  baixoEstoque?: boolean;
}

@Injectable({ providedIn: 'root' })
export class ProdutoService {
  private http = inject(HttpClient);
  private url = `${API_URL}/produtos`;

  listar(filtro: FiltroProdutos = {}) {
    return this.http.get<Produto[]>(this.url, { params: params({ ...filtro }) });
  }
  obter(id: number) {
    return this.http.get<Produto>(`${this.url}/${id}`);
  }
  criar(produto: ProdutoInput) {
    return this.http.post<Produto>(this.url, produto);
  }
  atualizar(id: number, produto: ProdutoInput) {
    return this.http.put<Produto>(`${this.url}/${id}`, produto);
  }
  excluir(id: number) {
    return this.http.delete<void>(`${this.url}/${id}`);
  }
}

export interface FiltroMovimentacoes {
  produtoId?: number | null;
  tipo?: TipoMovimentacao | '';
  limite?: number;
}

@Injectable({ providedIn: 'root' })
export class MovimentacaoService {
  private http = inject(HttpClient);
  private url = `${API_URL}/movimentacoes`;

  listar(filtro: FiltroMovimentacoes = {}) {
    return this.http.get<Movimentacao[]>(this.url, { params: params({ ...filtro }) });
  }
  registrar(mov: MovimentacaoInput) {
    return this.http.post<Movimentacao>(this.url, mov);
  }
}

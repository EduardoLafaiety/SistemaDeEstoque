import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, of, switchMap } from 'rxjs';
import { CategoriaService, FiltroProdutos, ProdutoService } from '../../core/api.service';
import { Categoria, Produto } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-produtos',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './produtos.html',
})
export class ProdutosPage implements OnInit {
  private produtosApi = inject(ProdutoService);
  private categoriasApi = inject(CategoriaService);
  private toast = inject(ToastService);
  private destroyRef = inject(DestroyRef);

  /** Query param `?baixoEstoque=true` (vindo do dashboard). */
  readonly baixoEstoqueParam = input<string | undefined>(undefined, { alias: 'baixoEstoque' });

  protected readonly produtos = signal<Produto[]>([]);
  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly carregando = signal(true);

  protected busca = '';
  protected categoriaId: number | null = null;
  protected baixoEstoque = false;

  private recarregar$ = new Subject<FiltroProdutos>();

  ngOnInit() {
    this.baixoEstoque = this.baixoEstoqueParam() === 'true';
    this.categoriasApi.listar().subscribe((c) => this.categorias.set(c));

    this.recarregar$
      .pipe(
        debounceTime(250),
        switchMap((f) => {
          this.carregando.set(true);
          // Um erro na busca não pode encerrar o stream de filtros.
          return this.produtosApi.listar(f).pipe(catchError(() => of([])));
        }),
        takeUntilDestroyed(this.destroyRef),
      )
      .subscribe((p) => {
        this.produtos.set(p);
        this.carregando.set(false);
      });

    this.filtrar();
  }

  protected filtrar() {
    this.recarregar$.next({
      busca: this.busca.trim(),
      categoriaId: this.categoriaId,
      baixoEstoque: this.baixoEstoque,
    });
  }

  protected excluir(p: Produto) {
    if (!confirm(`Excluir o produto "${p.nome}"? O histórico de movimentações também será removido.`)) return;
    this.produtosApi.excluir(p.id).subscribe(() => {
      this.produtos.update((lista) => lista.filter((x) => x.id !== p.id));
      this.toast.sucesso('Produto excluído.');
    });
  }
}

import { CurrencyPipe } from '@angular/common';
import { Component, DestroyRef, OnInit, inject, input, signal } from '@angular/core';
import { takeUntilDestroyed } from '@angular/core/rxjs-interop';
import { FormsModule } from '@angular/forms';
import { RouterLink } from '@angular/router';
import { Subject, catchError, debounceTime, of, switchMap } from 'rxjs';
import {
  CategoriaService,
  FiltroProdutos,
  MovimentacaoService,
  ProdutoService,
} from '../../core/api.service';
import { Categoria, Produto } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-produtos',
  imports: [FormsModule, RouterLink, CurrencyPipe],
  templateUrl: './produtos.html',
  styleUrl: './produtos.scss',
})
export class ProdutosPage implements OnInit {
  private produtosApi = inject(ProdutoService);
  private categoriasApi = inject(CategoriaService);
  private movApi = inject(MovimentacaoService);
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
  protected inativos = false;

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
      inativos: this.inativos,
    });
  }

  /**
   * O backend só apaga de fato produtos sem movimentações; os demais são inativados
   * e mantêm o histórico. Consultamos antes para avisar o usuário do que vai acontecer.
   */
  protected excluir(p: Produto) {
    this.movApi.listar({ produtoId: p.id, limite: 1 }).subscribe((movs) => {
      const inativar = movs.length > 0;
      const mensagem = inativar
        ? `Inativar o produto "${p.nome}"? Ele tem movimentações, então o histórico será mantido e você poderá reativá-lo depois.`
        : `Excluir definitivamente o produto "${p.nome}"?`;
      if (!confirm(mensagem)) return;
      this.produtosApi.excluir(p.id).subscribe(() => {
        this.toast.sucesso(inativar ? 'Produto inativado.' : 'Produto excluído.');
        this.filtrar();
      });
    });
  }

  protected reativar(p: Produto) {
    this.produtosApi.reativar(p.id).subscribe((atualizado) => {
      this.produtos.update((lista) => lista.map((x) => (x.id === p.id ? atualizado : x)));
      this.toast.sucesso('Produto reativado.');
    });
  }
}

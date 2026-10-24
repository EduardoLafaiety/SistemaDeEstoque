import { DatePipe } from '@angular/common';
import { Component, OnInit, computed, inject, input, signal } from '@angular/core';
import { FormsModule, NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { MovimentacaoService, ProdutoService } from '../../core/api.service';
import { DataUtcPipe } from '../../core/data-utc.pipe';
import { Movimentacao, Produto, TipoMovimentacao } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-movimentacoes',
  imports: [ReactiveFormsModule, FormsModule, DatePipe, DataUtcPipe],
  templateUrl: './movimentacoes.html',
  styleUrl: './movimentacoes.scss',
})
export class MovimentacoesPage implements OnInit {
  private fb = inject(NonNullableFormBuilder);
  private movApi = inject(MovimentacaoService);
  private produtosApi = inject(ProdutoService);
  private toast = inject(ToastService);

  /** Query param `?produtoId=` para pré-selecionar o produto (vindo da lista). */
  readonly produtoIdParam = input<string | undefined>(undefined, { alias: 'produtoId' });

  /** Só os ativos podem ser movimentados; o filtro do histórico inclui os inativos. */
  protected readonly todosProdutos = signal<Produto[]>([]);
  protected readonly produtos = computed(() => this.todosProdutos().filter((p) => p.ativo));
  protected readonly movimentacoes = signal<Movimentacao[]>([]);
  protected readonly carregando = signal(true);
  protected readonly salvando = signal(false);
  protected readonly produtoSelecionadoId = signal<number | null>(null);

  protected readonly produtoSelecionado = computed(() =>
    this.produtos().find((p) => p.id === this.produtoSelecionadoId()),
  );

  protected filtroProdutoId: number | null = null;
  protected filtroTipo: TipoMovimentacao | '' = '';
  protected filtroInicio = '';
  protected filtroFim = '';

  protected readonly form = this.fb.group({
    produtoId: this.fb.control<number | null>(null, Validators.required),
    tipo: this.fb.control<TipoMovimentacao>('ENTRADA'),
    quantidade: this.fb.control<number | null>(null, [Validators.required, Validators.min(1)]),
    observacao: [''],
  });

  ngOnInit() {
    this.form.controls.produtoId.valueChanges.subscribe((id) => this.produtoSelecionadoId.set(id));

    const pre = Number(this.produtoIdParam()) || null;
    if (pre) {
      this.form.patchValue({ produtoId: pre });
      this.filtroProdutoId = pre;
    }

    this.carregarProdutos();
    this.carregarHistorico();
  }

  private carregarProdutos() {
    this.produtosApi.listar({ inativos: true }).subscribe((p) => this.todosProdutos.set(p));
  }

  protected carregarHistorico() {
    this.carregando.set(true);
    const filtro = {
      produtoId: this.filtroProdutoId,
      tipo: this.filtroTipo,
      inicio: this.filtroInicio,
      fim: this.filtroFim,
    };
    this.movApi.listar(filtro).subscribe({
      next: (m) => {
        this.movimentacoes.set(m);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  protected registrar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    this.salvando.set(true);
    this.movApi
      .registrar({
        produtoId: v.produtoId!,
        tipo: v.tipo,
        quantidade: v.quantidade!,
        observacao: v.observacao.trim() || null,
      })
      .subscribe({
        next: (m) => {
          this.toast.sucesso(
            `${m.tipo === 'ENTRADA' ? 'Entrada' : 'Saída'} de ${m.quantidade} un. registrada para ${m.produtoNome}.`,
          );
          this.form.patchValue({ quantidade: null, observacao: '' });
          this.form.controls.quantidade.markAsUntouched();
          this.salvando.set(false);
          this.carregarProdutos();
          this.carregarHistorico();
        },
        error: () => this.salvando.set(false),
      });
  }
}

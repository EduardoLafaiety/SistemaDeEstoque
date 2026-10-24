import { Component, OnInit, inject, input, signal } from '@angular/core';
import { NonNullableFormBuilder, ReactiveFormsModule, Validators } from '@angular/forms';
import { Router, RouterLink } from '@angular/router';
import { CategoriaService, ProdutoService } from '../../core/api.service';
import { Categoria, ProdutoInput } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-produto-form',
  imports: [ReactiveFormsModule, RouterLink],
  templateUrl: './produto-form.html',
  styleUrl: './produto-form.scss',
})
export class ProdutoFormPage implements OnInit {
  private fb = inject(NonNullableFormBuilder);
  private produtosApi = inject(ProdutoService);
  private categoriasApi = inject(CategoriaService);
  private toast = inject(ToastService);
  private router = inject(Router);

  /** Parâmetro de rota `:id` (ausente em /produtos/novo). */
  readonly id = input<string>();

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly salvando = signal(false);
  protected readonly quantidadeAtual = signal<number | null>(null);
  protected readonly ativo = signal(true);

  protected readonly form = this.fb.group({
    sku: ['', [Validators.required, Validators.maxLength(60)]],
    nome: ['', [Validators.required, Validators.maxLength(200)]],
    descricao: [''],
    categoriaId: this.fb.control<number | null>(null),
    preco: this.fb.control<number | null>(null, [Validators.required, Validators.min(0)]),
    quantidade: this.fb.control<number | null>(0, [Validators.min(0)]),
    estoqueMinimo: this.fb.control<number | null>(0, [Validators.required, Validators.min(0)]),
  });

  protected get editando() {
    return !!this.id();
  }

  ngOnInit() {
    this.categoriasApi.listar().subscribe((c) => this.categorias.set(c));

    const id = this.id();
    if (id) {
      this.produtosApi.obter(Number(id)).subscribe({
        next: (p) => {
          this.form.patchValue({ ...p, descricao: p.descricao ?? '' });
          this.quantidadeAtual.set(p.quantidade);
          this.ativo.set(p.ativo);
        },
        error: () => this.router.navigate(['/produtos']),
      });
    }
  }

  protected invalido(campo: keyof typeof this.form.controls) {
    const c = this.form.controls[campo];
    return c.invalid && c.touched;
  }

  protected reativar() {
    this.produtosApi.reativar(Number(this.id())).subscribe(() => {
      this.ativo.set(true);
      this.toast.sucesso('Produto reativado.');
    });
  }

  protected salvar() {
    if (this.form.invalid) {
      this.form.markAllAsTouched();
      return;
    }
    const v = this.form.getRawValue();
    const dados: ProdutoInput = {
      sku: v.sku.trim(),
      nome: v.nome.trim(),
      descricao: v.descricao.trim() || null,
      categoriaId: v.categoriaId,
      preco: v.preco ?? 0,
      estoqueMinimo: v.estoqueMinimo ?? 0,
    };
    if (!this.editando) dados.quantidade = v.quantidade ?? 0;

    this.salvando.set(true);
    const req = this.editando
      ? this.produtosApi.atualizar(Number(this.id()), dados)
      : this.produtosApi.criar(dados);

    req.subscribe({
      next: () => {
        this.toast.sucesso(this.editando ? 'Produto atualizado.' : 'Produto cadastrado.');
        this.router.navigate(['/produtos']);
      },
      error: () => this.salvando.set(false),
    });
  }
}

import { Component, OnInit, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { CategoriaService } from '../../core/api.service';
import { Categoria } from '../../core/models';
import { ToastService } from '../../core/toast.service';

@Component({
  selector: 'app-categorias',
  imports: [FormsModule],
  templateUrl: './categorias.html',
  styleUrl: './categorias.scss',
})
export class CategoriasPage implements OnInit {
  private api = inject(CategoriaService);
  private toast = inject(ToastService);

  protected readonly categorias = signal<Categoria[]>([]);
  protected readonly carregando = signal(true);
  protected readonly editandoId = signal<number | null>(null);

  protected novoNome = '';
  protected nomeEdicao = '';

  ngOnInit() {
    this.carregar();
  }

  private carregar() {
    this.api.listar().subscribe({
      next: (c) => {
        this.categorias.set(c);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }

  protected criar() {
    const nome = this.novoNome.trim();
    if (!nome) return;
    this.api.criar(nome).subscribe(() => {
      this.novoNome = '';
      this.toast.sucesso('Categoria criada.');
      this.carregar();
    });
  }

  protected iniciarEdicao(c: Categoria) {
    this.editandoId.set(c.id);
    this.nomeEdicao = c.nome;
  }

  protected cancelarEdicao() {
    this.editandoId.set(null);
  }

  protected salvarEdicao(c: Categoria) {
    const nome = this.nomeEdicao.trim();
    if (!nome || nome === c.nome) return this.cancelarEdicao();
    this.api.atualizar(c.id, nome).subscribe((atualizada) => {
      this.categorias.update((lista) => lista.map((x) => (x.id === c.id ? atualizada : x)));
      this.editandoId.set(null);
      this.toast.sucesso('Categoria atualizada.');
    });
  }

  protected excluir(c: Categoria) {
    const aviso = c.totalProdutos
      ? ` Os ${c.totalProdutos} produto(s) dela ficarão sem categoria.`
      : '';
    if (!confirm(`Excluir a categoria "${c.nome}"?${aviso}`)) return;
    this.api.excluir(c.id).subscribe(() => {
      this.categorias.update((lista) => lista.filter((x) => x.id !== c.id));
      this.toast.sucesso('Categoria excluída.');
    });
  }
}

import { Component, inject } from '@angular/core';
import { RouterLink, RouterLinkActive, RouterOutlet } from '@angular/router';
import { ToastService } from './core/toast.service';

@Component({
  imports: [RouterOutlet, RouterLink, RouterLinkActive],
  selector: 'app-root',
  styleUrl: './app.scss',
  templateUrl: './app.html',
})
export class App {
  protected readonly toast = inject(ToastService);

  protected readonly menu = [
    { rota: '/', rotulo: 'Dashboard', icone: '▦', exato: true },
    { rota: '/produtos', rotulo: 'Produtos', icone: '▤', exato: false },
    { rota: '/categorias', rotulo: 'Categorias', icone: '◫', exato: false },
    { rota: '/movimentacoes', rotulo: 'Movimentações', icone: '⇅', exato: false },
  ];
}

import { CurrencyPipe, DatePipe, DecimalPipe } from '@angular/common';
import { Component, OnInit, inject, signal } from '@angular/core';
import { RouterLink } from '@angular/router';
import { DashboardService } from '../../core/api.service';
import { DataUtcPipe } from '../../core/data-utc.pipe';
import { Dashboard } from '../../core/models';

@Component({
  selector: 'app-dashboard',
  imports: [RouterLink, CurrencyPipe, DecimalPipe, DatePipe, DataUtcPipe],
  templateUrl: './dashboard.html',
  styleUrl: './dashboard.scss',
})
export class DashboardPage implements OnInit {
  private api = inject(DashboardService);

  protected readonly dados = signal<Dashboard | null>(null);
  protected readonly carregando = signal(true);

  ngOnInit() {
    this.api.obter().subscribe({
      next: (d) => {
        this.dados.set(d);
        this.carregando.set(false);
      },
      error: () => this.carregando.set(false),
    });
  }
}

import { Injectable, signal } from '@angular/core';

export interface Toast {
  id: number;
  tipo: 'sucesso' | 'erro';
  mensagem: string;
}

@Injectable({ providedIn: 'root' })
export class ToastService {
  private seq = 0;
  readonly toasts = signal<Toast[]>([]);

  sucesso(mensagem: string) {
    this.mostrar('sucesso', mensagem);
  }

  erro(mensagem: string) {
    this.mostrar('erro', mensagem);
  }

  fechar(id: number) {
    this.toasts.update((ts) => ts.filter((t) => t.id !== id));
  }

  private mostrar(tipo: Toast['tipo'], mensagem: string) {
    const id = ++this.seq;
    this.toasts.update((ts) => [...ts, { id, tipo, mensagem }]);
    setTimeout(() => this.fechar(id), 4500);
  }
}

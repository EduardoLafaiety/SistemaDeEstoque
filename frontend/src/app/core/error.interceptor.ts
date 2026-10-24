import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, throwError } from 'rxjs';
import { ToastService } from './toast.service';

/** Mostra a mensagem de erro retornada pela API (`{ erro: string }`) em um toast. */
export const errorInterceptor: HttpInterceptorFn = (req, next) => {
  const toast = inject(ToastService);
  return next(req).pipe(
    catchError((err: HttpErrorResponse) => {
      const mensagem =
        err.status === 0
          ? 'Não foi possível conectar à API. O backend está rodando em http://localhost:3000?'
          : (err.error?.erro ?? `Erro ${err.status}: ${err.statusText}`);
      toast.erro(mensagem);
      return throwError(() => err);
    }),
  );
};

import { HttpErrorResponse, HttpInterceptorFn } from '@angular/common/http';
import { inject } from '@angular/core';
import { catchError, config as rxjsConfig, throwError } from 'rxjs';
import { ToastService } from './toast.service';

/**
 * O interceptor já mostra os erros HTTP ao usuário, então um `subscribe` sem `error`
 * não precisa virar exceção não tratada no console. Outros erros seguem o padrão do RxJS.
 */
rxjsConfig.onUnhandledError = (err) => {
  if (err instanceof HttpErrorResponse) return;
  setTimeout(() => {
    throw err;
  });
};

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

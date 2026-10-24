import { Pipe, PipeTransform } from '@angular/core';

/**
 * O SQLite grava datas como "YYYY-MM-DD HH:MM:SS" em UTC, sem fuso.
 * Este pipe converte para Date, para usar em seguida com o `date` pipe.
 */
@Pipe({ name: 'dataUtc' })
export class DataUtcPipe implements PipeTransform {
  transform(valor: string | null | undefined): Date | null {
    return valor ? new Date(valor.replace(' ', 'T') + 'Z') : null;
  }
}

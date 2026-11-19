import { ChangeDetectionStrategy, Component, input, output } from '@angular/core';

import { ModalComponent } from './modal.component';

/**
 * Diálogo de confirmación reutilizable (eliminar, cancelar, etc.). No ejecuta nada por sí mismo:
 * emite (confirmar) o (cancelar) y la pantalla decide. Mientras procesando() es true, no se puede cerrar.
 */
@Component({
  selector: 'app-confirm-dialog',
  imports: [ModalComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [abierto]="abierto()" [titulo]="titulo()" (cerrar)="alCerrar()">
      <div class="modal-icono"><i class="fas fa-triangle-exclamation"></i></div>
      <h2 class="modal-titulo">{{ titulo() }}</h2>
      <p class="modal-texto">{{ mensaje() }}</p>
      <div class="modal-acciones">
        <button type="button" class="btn-modal btn-modal-primario" [disabled]="procesando()" (click)="confirmar.emit()">
          {{ procesando() ? 'Procesando…' : textoConfirmar() }}
        </button>
        <button type="button" class="btn-modal btn-modal-secundario" [disabled]="procesando()" (click)="cancelar.emit()">
          {{ textoCancelar() }}
        </button>
      </div>
    </app-modal>
  `,
})
export class ConfirmDialogComponent {
  readonly abierto = input.required<boolean>();
  readonly titulo = input('¿Estás seguro?');
  readonly mensaje = input('Esta acción no se puede deshacer.');
  readonly textoConfirmar = input('Confirmar');
  readonly textoCancelar = input('Cancelar');
  readonly procesando = input(false);

  readonly confirmar = output<void>();
  readonly cancelar = output<void>();

  protected alCerrar(): void {
    if (!this.procesando()) {
      this.cancelar.emit();
    }
  }
}
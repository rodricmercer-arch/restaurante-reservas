import { ChangeDetectionStrategy, Component, ElementRef, effect, input, output, viewChild } from '@angular/core';

/** Ventana emergente propia: se cierra con Esc, con clic en el fondo o desde el contenido proyectado. */
@Component({
  selector: 'app-modal',
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    @if (abierto()) {
      <div class="modal-fondo" (click)="cerrar.emit()">
        <div
          #panel
          class="modal-panel"
          role="dialog"
          aria-modal="true"
          tabindex="-1"
          [attr.aria-label]="titulo()"
          (click)="$event.stopPropagation()"
          (keydown.escape)="cerrar.emit()"
        >
          <ng-content />
        </div>
      </div>
    }
  `,
})
export class ModalComponent {
  readonly abierto = input.required<boolean>();
  readonly titulo = input('Ventana emergente');
  readonly cerrar = output<void>();

  private readonly panel = viewChild<ElementRef<HTMLElement>>('panel');

  constructor() {
    // Al abrirse, el foco pasa al panel: así Esc funciona de inmediato y el teclado queda dentro de la ventana.
    effect(() => this.panel()?.nativeElement.focus());
  }
}
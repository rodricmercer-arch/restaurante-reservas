import { ChangeDetectionStrategy, Component, computed, input, signal } from '@angular/core';

import { urlRecurso } from '../core/config/recursos';
import { CategoriaPlato } from '../models/plato.model';

const ICONOS: Record<CategoriaPlato, string> = {
  PRINCIPAL: 'fas fa-utensils',
  POSTRE: 'fas fa-ice-cream',
  BEBIDA: 'fas fa-wine-glass',
};

/** Foto de un plato con ícono de respaldo: si no hay imagen o falla la carga, queda el ícono de la categoría. */
@Component({
  selector: 'app-plato-imagen',
  changeDetection: ChangeDetectionStrategy.OnPush,
  host: { class: 'plato-imagen-host' },
  template: `
    <div [class]="'dish-img ' + clase()">
      <i [class]="icono()"></i>
      @if (url(); as src) {
        @if (!fallo()) {
          <img [src]="src" [alt]="nombre()" loading="lazy" (error)="fallo.set(true)" />
        }
      }
    </div>
  `,
})
export class PlatoImagenComponent {
  readonly categoria = input.required<CategoriaPlato>();
  readonly imagen = input<string | null | undefined>(null);
  readonly nombre = input('');
  readonly clase = input<'dish-thumb' | 'dish-banner' | 'dish-mini'>('dish-thumb');

  protected readonly fallo = signal(false);
  protected readonly icono = computed(() => ICONOS[this.categoria()]);
  protected readonly url = computed(() => {
    const ruta = this.imagen();
    return ruta && ruta.trim() !== '' ? urlRecurso(ruta.trim()) : null;
  });
}
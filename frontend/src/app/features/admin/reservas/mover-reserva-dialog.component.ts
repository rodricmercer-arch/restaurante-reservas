import { ChangeDetectionStrategy, Component, inject, input, output, signal } from '@angular/core';
import { FormControl, ReactiveFormsModule, Validators } from '@angular/forms';

import { AdminMesaService } from '../../../core/services/admin-mesa.service';
import { AdminReservaService } from '../../../core/services/admin-reserva.service';
import { ApiError } from '../../../models/api-error.model';
import { Mesa } from '../../../models/mesa.model';
import { Reserva } from '../../../models/reserva.model';
import { extraerApiError } from '../../../shared/api-error';
import { ModalComponent } from '../../../shared/modal.component';

/**
 * Diálogo "Mover reserva": el padre lo crea con @if solo cuando hay una reserva por mover, así que al abrirse
 * carga las mesas LIBRES más recientes. Emite (movida) con el número de la mesa destino, (fallida) con el error
 * de la API (el padre lo muestra y recarga la lista) o (cerrar).
 */
@Component({
  selector: 'app-mover-reserva-dialog',
  imports: [ReactiveFormsModule, ModalComponent],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <app-modal [abierto]="true" titulo="Mover reserva" (cerrar)="cerrarSiLibre()">
      <div class="modal-icono"><i class="fas fa-right-left"></i></div>
      <h2 class="modal-titulo">Mover reserva #{{ reserva().id }}</h2>
      <p class="modal-texto">
        {{ reserva().cliente.nombre }} · actualmente en la mesa {{ reserva().mesa.numero }}. Elige una mesa libre de destino.
      </p>

      @if (cargando()) {
        <p class="estado-carga" role="status">Cargando mesas libres…</p>
      } @else if (libres().length === 0) {
        <p class="campo-error">No hay mesas libres disponibles en este momento.</p>
      } @else {
        <div class="campo">
          <label class="campo-label" for="mesa-destino">Mesa destino</label>
          <select id="mesa-destino" class="campo-input" [formControl]="mesaId" [class.invalido]="mesaId.touched && mesaId.invalid">
            <option [ngValue]="null" disabled>Selecciona una mesa</option>
            @for (m of libres(); track m.id) {
              <option [ngValue]="m.id">Mesa {{ m.numero }} · {{ m.capacidad }} personas</option>
            }
          </select>
          @if (mesaId.touched && mesaId.invalid) {
            <div class="campo-error">Selecciona la mesa de destino</div>
          }
        </div>
      }

      <div class="modal-acciones">
        <button type="button" class="btn-modal btn-modal-primario" [disabled]="moviendo() || cargando() || libres().length === 0" (click)="mover()">
          {{ moviendo() ? 'Moviendo…' : 'Mover reserva' }}
        </button>
        <button type="button" class="btn-modal btn-modal-secundario" [disabled]="moviendo()" (click)="cerrarSiLibre()">Cancelar</button>
      </div>
    </app-modal>
  `,
})
export class MoverReservaDialogComponent {
  private readonly mesasApi = inject(AdminMesaService);
  private readonly reservasApi = inject(AdminReservaService);

  readonly reserva = input.required<Reserva>();
  readonly movida = output<number>();
  readonly fallida = output<ApiError>();
  readonly cerrar = output<void>();

  protected readonly mesaId = new FormControl<number | null>(null, { validators: [Validators.required] });
  protected readonly libres = signal<Mesa[]>([]);
  protected readonly cargando = signal(true);
  protected readonly moviendo = signal(false);

  constructor() {
    this.mesasApi.listar().subscribe({
      next: (mesas) => {
        this.libres.set(mesas.filter((m) => m.estado === 'LIBRE'));
        this.cargando.set(false);
      },
      error: (e: unknown) => this.fallida.emit(extraerApiError(e)),
    });
  }

  protected cerrarSiLibre(): void {
    if (!this.moviendo()) {
      this.cerrar.emit();
    }
  }

  protected mover(): void {
    const destino = this.mesaId.value;
    if (destino === null) {
      this.mesaId.markAsTouched();
      return;
    }
    if (this.moviendo()) {
      return;
    }
    this.moviendo.set(true);
    this.reservasApi.mover(this.reserva().id, destino).subscribe({
      next: () => this.movida.emit(this.libres().find((m) => m.id === destino)?.numero ?? destino),
      error: (e: unknown) => {
        this.moviendo.set(false);
        this.fallida.emit(extraerApiError(e));
      },
    });
  }
}
package com.restaurante.reservas.service;

import com.restaurante.reservas.model.DetalleReserva;
import com.restaurante.reservas.model.Reserva;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URI;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.concurrent.CompletableFuture;

/**
 * Avisa por Telegram cuando se crea una reserva. Es el único canal de notificación.
 * Contrato: notificarReservaNueva NUNCA lanza excepciones, para que un fallo de red
 * no pueda hacer fallar (ni mostrar error de) una reserva que ya se guardó.
 */
@Service
public class NotificacionService {

    private static final Logger log = LoggerFactory.getLogger(NotificacionService.class);

    private static final HttpClient HTTP = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    private static final DateTimeFormatter FORMATO_FECHA = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private final boolean activo;
    private final String token;
    private final String chatId;

    public NotificacionService(@Value("${app.notificaciones.telegram.enabled:false}") boolean activo,
                               @Value("${app.notificaciones.telegram.token:}") String token,
                               @Value("${app.notificaciones.telegram.chatid:}") String chatId) {
        this.activo = activo;
        // trim(): en un .properties un espacio al final del valor se conserva y rompería la URL.
        this.token = (token == null) ? "" : token.trim();
        this.chatId = (chatId == null) ? "" : chatId.trim();

        // Nunca se loguea el token ni el chat id.
        log.info("Notificaciones de reserva -> Telegram: {}", activo ? "ACTIVO" : "INACTIVO");
        if (activo && (enBlanco(this.token) || enBlanco(this.chatId))) {
            log.warn("Telegram activo pero falta token o chatid");
        }
    }

    public void notificarReservaNueva(Reserva reserva) {
        if (!activo) {
            return;
        }

        // El texto se arma AQUÍ, en el hilo de la petición: después se pasa a otro hilo y ya no debe tocarse la entidad.
        String texto;
        try {
            texto = construirMensaje(reserva);
        } catch (Exception e) {
            log.warn("No se pudo construir el aviso de reserva por Telegram: {}", e.getClass().getSimpleName());
            return;
        }

        final String mensaje = texto;
        try {
            CompletableFuture.runAsync(() -> {
                try {
                    enviarTelegram(mensaje);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt(); // se restaura la marca de interrupción
                    log.warn("Fallo al enviar el aviso de reserva por Telegram: {}", e.getClass().getSimpleName());
                } catch (Exception e) {
                    // Solo el nombre de la clase: el mensaje de algunas excepciones de red incluye la URL, y la URL contiene el token.
                    log.warn("Fallo al enviar el aviso de reserva por Telegram: {}", e.getClass().getSimpleName());
                }
            });
        } catch (Exception e) {
            // Por si el ejecutor rechazara la tarea: el contrato es no lanzar nunca.
            log.warn("No se pudo programar el aviso de reserva por Telegram: {}", e.getClass().getSimpleName());
        }
    }

    /** Texto plano, sin emojis y sin parse_mode. No incluye el DNI del cliente. */
    static String construirMensaje(Reserva r) {
        StringBuilder sb = new StringBuilder();
        sb.append("Nueva reserva\n");
        sb.append("Cliente: ").append(r.getCliente().getNombre()).append('\n');
        sb.append("Mesa: ").append(r.getMesa().getNumero())
          .append(" (").append(r.getMesa().getCapacidad()).append(" personas)\n");
        sb.append("Fecha: ").append(r.getFechaHora().format(FORMATO_FECHA)).append('\n');
        sb.append("Pedido:\n");

        BigDecimal total = BigDecimal.ZERO;
        for (DetalleReserva d : r.getDetalles()) {
            BigDecimal subtotal = d.getPlato().getPrecio().multiply(BigDecimal.valueOf(d.getCantidad()));
            total = total.add(subtotal);
            sb.append("- ").append(d.getPlato().getNombre())
              .append(" x").append(d.getCantidad())
              .append(" (S/ ").append(importe(subtotal)).append(")\n");
        }
        sb.append("Total: S/ ").append(importe(total));
        return sb.toString();
    }

    private void enviarTelegram(String texto) throws IOException, InterruptedException {
        if (enBlanco(token) || enBlanco(chatId)) {
            log.warn("Telegram activo pero falta token o chatid");
            return;
        }

        // En formularios, "+" para los espacios es lo correcto: no se cambia por %20.
        String cuerpo = "chat_id=" + URLEncoder.encode(chatId, StandardCharsets.UTF_8)
                + "&text=" + URLEncoder.encode(texto, StandardCharsets.UTF_8);

        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("https://api.telegram.org/bot" + token + "/sendMessage"))
                .timeout(Duration.ofSeconds(10))
                .header("Content-Type", "application/x-www-form-urlencoded")
                .POST(HttpRequest.BodyPublishers.ofString(cuerpo))
                .build();

        HttpResponse<String> response = HTTP.send(request, HttpResponse.BodyHandlers.ofString());

        if (response.statusCode() == 200) {
            log.info("Aviso de reserva enviado por Telegram");
        } else {
            String cuerpoRespuesta = (response.body() == null) ? "" : response.body();
            String resumen = cuerpoRespuesta.substring(0, Math.min(200, cuerpoRespuesta.length()));
            // Nunca se loguea la URL ni el token.
            log.warn("Telegram respondió con código {}: {}", response.statusCode(), resumen);
        }
    }

    private static String importe(BigDecimal valor) {
        return valor.setScale(2, RoundingMode.HALF_UP).toPlainString();
    }

    private static boolean enBlanco(String s) {
        return s == null || s.isBlank();
    }
}
package cl.duoc.speedfast.concurrencia;

import cl.duoc.speedfast.modelo.EstadoPedido;
import cl.duoc.speedfast.modelo.Pedido;
import cl.duoc.speedfast.util.Consola;

import java.util.Random;

// Hilo repartidor: retira pedidos de la zona de carga, simula la entrega y los marca ENTREGADO.
public class Repartidor implements Runnable {

    private static final int DEMORA_MINIMA_MS = 600;
    private static final int DEMORA_MAXIMA_MS = 1500;

    private final String nombre;
    private final ZonaDeCarga zonaDeCarga;
    private final Random random = new Random();
    private final int demoraMinimaMs;
    private final int demoraMaximaMs;
    private int pedidosEntregados = 0;

    public Repartidor(String nombre, ZonaDeCarga zonaDeCarga) {
        this(nombre, zonaDeCarga, DEMORA_MINIMA_MS, DEMORA_MAXIMA_MS);
    }

    // Constructor con demora configurable (para la prueba de alta concurrencia).
    public Repartidor(String nombre, ZonaDeCarga zonaDeCarga, int demoraMinimaMs, int demoraMaximaMs) {
        if (nombre == null || nombre.trim().isEmpty()) {
            throw new IllegalArgumentException("El nombre del repartidor es obligatorio.");
        }
        if (zonaDeCarga == null) {
            throw new IllegalArgumentException("El repartidor necesita una zona de carga.");
        }
        if (demoraMinimaMs < 0 || demoraMaximaMs < demoraMinimaMs) {
            throw new IllegalArgumentException("El rango de demora informado no es válido.");
        }
        this.nombre = nombre;
        this.zonaDeCarga = zonaDeCarga;
        this.demoraMinimaMs = demoraMinimaMs;
        this.demoraMaximaMs = demoraMaximaMs;
    }

    @Override
    public void run() {
        Thread.currentThread().setName("Repartidor-" + nombre);
        Consola.log(prefijo() + " Inicia su turno.");
        try {
            Pedido pedido;
            while ((pedido = zonaDeCarga.retirarPedido()) != null) {
                atender(pedido);
            }
            Consola.log(prefijo() + " Termina su turno. Pedidos entregados: " + pedidosEntregados);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            Consola.log(prefijo() + " Turno interrumpido.");
        }
    }

    // Simula el reparto: verifica estado, viaja y marca entregado.
    private void atender(Pedido pedido) throws InterruptedException {
        if (!pedido.estaEnReparto()) {
            Consola.log(prefijo() + " Descarta el pedido #" + pedido.getId()
                    + ": su estado es " + pedido.getEstado() + " y no EN_REPARTO.");
            return;
        }

        Consola.log(prefijo() + " Retirando pedido #" + pedido.getId()
                + "... Destino: " + pedido.getDireccionEntrega());

        pedido.setEstado(EstadoPedido.EN_REPARTO);
        Consola.log(prefijo() + " Estado: " + pedido.getEstado());

        Consola.log(prefijo() + " Entregando pedido #" + pedido.getId() + "...");
        Thread.sleep(demoraEntrega());

        pedido.setEstado(EstadoPedido.ENTREGADO);
        Consola.log(prefijo() + " Estado: " + pedido.getEstado());

        zonaDeCarga.registrarEntrega(pedido);
        pedidosEntregados++;
    }

    private int demoraEntrega() {
        return demoraMinimaMs + random.nextInt(demoraMaximaMs - demoraMinimaMs + 1);
    }

    private String prefijo() {
        return "[Repartidor - " + nombre + "]";
    }

    public String getNombre() {
        return nombre;
    }

    public int getPedidosEntregados() {
        return pedidosEntregados;
    }

    @Override
    public String toString() {
        return String.format("Repartidor{nombre='%s', pedidosEntregados=%d}", nombre, pedidosEntregados);
    }
}

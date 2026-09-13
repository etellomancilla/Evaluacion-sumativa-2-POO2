package cl.duoc.speedfast.concurrencia;

import cl.duoc.speedfast.modelo.EstadoPedido;
import cl.duoc.speedfast.modelo.Pedido;
import cl.duoc.speedfast.util.Consola;

import java.util.ArrayDeque;
import java.util.LinkedHashSet;
import java.util.Queue;
import java.util.Set;

// Recurso compartido: cola de pedidos protegida con synchronized + wait/notifyAll.
// El retiro es atómico (sacar de la cola y reservar ocurre en la misma sección crítica).
public class ZonaDeCarga {

    private final Queue<Pedido> pedidosDisponibles = new ArrayDeque<>();
    private final Set<Integer> idsRetirados = new LinkedHashSet<>();
    private final Set<Integer> idsEntregados = new LinkedHashSet<>();

    private boolean recepcionCerrada = false;
    private int totalRecibidos = 0;
    private int retirosDoblesEvitados = 0;
    private int entregasDuplicadasDetectadas = 0;
    private int pedidosRechazados = 0;

    // Agrega un pedido y despierta a los repartidores en espera.
    // Rechaza nulos, repetidos y ya entregados.
    public synchronized void agregarPedido(Pedido p) {
        if (p == null) {
            pedidosRechazados++;
            Consola.log("[Zona de carga] Pedido rechazado: referencia nula.");
            return;
        }
        if (!p.esRetirable()) {
            pedidosRechazados++;
            Consola.log("[Zona de carga] Pedido #" + p.getId()
                    + " rechazado: ya se encuentra " + p.getEstado() + ".");
            return;
        }
        if (idsRetirados.contains(p.getId()) || contieneId(p.getId())) {
            pedidosRechazados++;
            Consola.log("[Zona de carga] Pedido #" + p.getId()
                    + " rechazado: el identificador ya existe en la zona de carga.");
            return;
        }

        p.setEstado(EstadoPedido.PENDIENTE);
        pedidosDisponibles.offer(p);
        totalRecibidos++;
        Consola.log("Pedido #" + p.getId() + " agregado. Destino: " + p.getDireccionEntrega());

        notifyAll();
    }

    // Retira un pedido ya reservado (EN_REPARTO + repartidor asignado).
    // Si la cola está vacía espera con wait(); devuelve null cuando no queda trabajo.
    public synchronized Pedido retirarPedido() {
        try {
            while (true) {
                while (pedidosDisponibles.isEmpty() && !recepcionCerrada) {
                    wait();
                }

                if (pedidosDisponibles.isEmpty()) {
                    return null;
                }

                Pedido pedido = pedidosDisponibles.poll();

                if (!idsRetirados.add(pedido.getId())) {
                    retirosDoblesEvitados++;
                    Consola.log("[Zona de carga] ALERTA: se evitó el retiro doble del pedido #"
                            + pedido.getId() + ".");
                    continue;
                }

                // Reserva atómica: estado + repartidor en la misma sección crítica.
                pedido.setEstado(EstadoPedido.EN_REPARTO);
                pedido.setRepartidorAsignado(Thread.currentThread().getName());
                return pedido;
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            return null;
        }
    }

    // Registra la entrega y detecta duplicados.
    public synchronized void registrarEntrega(Pedido p) {
        if (p == null || p.getEstado() != EstadoPedido.ENTREGADO) {
            Consola.log("[Zona de carga] ALERTA: intento de registrar una entrega inválida.");
            return;
        }
        if (!idsEntregados.add(p.getId())) {
            entregasDuplicadasDetectadas++;
            Consola.log("[Zona de carga] ALERTA: entrega duplicada del pedido #" + p.getId() + ".");
        }
    }

    // Cierra la recepción y despierta a todos los hilos para que terminen.
    public synchronized void cerrarRecepcion() {
        recepcionCerrada = true;
        notifyAll();
        Consola.log("[Zona de carga] Recepción de pedidos cerrada.");
    }

    // Consultas

    public synchronized boolean estaVacia() {
        return pedidosDisponibles.isEmpty();
    }

    public synchronized int getPedidosDisponibles() {
        return pedidosDisponibles.size();
    }

    public synchronized int getTotalRecibidos() {
        return totalRecibidos;
    }

    public synchronized int getTotalRetirados() {
        return idsRetirados.size();
    }

    public synchronized int getTotalEntregados() {
        return idsEntregados.size();
    }

    public synchronized int getRetirosDoblesEvitados() {
        return retirosDoblesEvitados;
    }

    public synchronized int getEntregasDuplicadasDetectadas() {
        return entregasDuplicadasDetectadas;
    }

    public synchronized int getPedidosRechazados() {
        return pedidosRechazados;
    }

    private boolean contieneId(int id) {
        for (Pedido pedido : pedidosDisponibles) {
            if (pedido.getId() == id) {
                return true;
            }
        }
        return false;
    }
}

package cl.duoc.speedfast.modelo;

// Estados del pedido dentro del flujo de despacho.
public enum EstadoPedido {

    PENDIENTE,
    EN_REPARTO,
    ENTREGADO;

    // Convierte texto a estado, sin distinguir mayúsculas.
    public static EstadoPedido desdeTexto(String texto) {
        if (texto == null) {
            throw new IllegalArgumentException("El estado no puede ser nulo.");
        }
        return EstadoPedido.valueOf(texto.trim().toUpperCase());
    }
}

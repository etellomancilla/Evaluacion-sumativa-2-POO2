package cl.duoc.speedfast.modelo;

// Encomienda que se retira de la zona de carga y se entrega.
// Acceso sincronizado porque el objeto es compartido entre hilos.
public class Pedido {

    private final int id;
    private String direccionEntrega;
    private EstadoPedido estado;
    private String repartidorAsignado;

    public Pedido(int id, String direccionEntrega) {
        this(id, direccionEntrega, EstadoPedido.PENDIENTE);
    }

    public Pedido(int id, String direccionEntrega, EstadoPedido estado) {
        if (direccionEntrega == null || direccionEntrega.trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria.");
        }
        if (estado == null) {
            throw new IllegalArgumentException("El estado inicial es obligatorio.");
        }
        this.id = id;
        this.direccionEntrega = direccionEntrega;
        this.estado = estado;
    }

    // Getters

    public int getId() {
        return id;
    }

    public synchronized String getDireccionEntrega() {
        return direccionEntrega;
    }

    public synchronized EstadoPedido getEstado() {
        return estado;
    }

    public synchronized String getRepartidorAsignado() {
        return repartidorAsignado;
    }

    // Setters

    public synchronized void setDireccionEntrega(String direccionEntrega) {
        if (direccionEntrega == null || direccionEntrega.trim().isEmpty()) {
            throw new IllegalArgumentException("La dirección de entrega es obligatoria.");
        }
        this.direccionEntrega = direccionEntrega;
    }

    public synchronized void setEstado(EstadoPedido nuevoEstado) {
        if (nuevoEstado == null) {
            throw new IllegalArgumentException("El nuevo estado no puede ser nulo.");
        }
        this.estado = nuevoEstado;
    }

    // Sobrecarga con String para cumplir con la firma del enunciado.
    public synchronized void setEstado(String nuevoEstado) {
        setEstado(EstadoPedido.desdeTexto(nuevoEstado));
    }

    public synchronized void setRepartidorAsignado(String repartidorAsignado) {
        this.repartidorAsignado = repartidorAsignado;
    }

    // Un pedido ya ENTREGADO no se puede volver a despachar.
    public synchronized boolean esRetirable() {
        return estado != EstadoPedido.ENTREGADO;
    }

    public synchronized boolean estaEnReparto() {
        return estado == EstadoPedido.EN_REPARTO;
    }

    @Override
    public synchronized String toString() {
        return String.format("Pedido{id=%d, direccionEntrega='%s', estado=%s, repartidor=%s}",
                id, direccionEntrega, estado,
                repartidorAsignado == null ? "sin asignar" : repartidorAsignado);
    }
}

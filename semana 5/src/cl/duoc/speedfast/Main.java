package cl.duoc.speedfast;

import cl.duoc.speedfast.concurrencia.Repartidor;
import cl.duoc.speedfast.concurrencia.ZonaDeCarga;
import cl.duoc.speedfast.modelo.EstadoPedido;
import cl.duoc.speedfast.modelo.Pedido;
import cl.duoc.speedfast.util.Consola;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

// Punto de entrada: orquesta la simulación de entregas en SpeedFast.
// Uso: java cl.duoc.speedfast.Main [estres]
public class Main {

    private static final int CANTIDAD_REPARTIDORES = 3;
    private static final List<String> NOMBRES_REPARTIDORES =
            Arrays.asList("Juan", "Camila", "Pedro");
    private static final int ESPERA_MAXIMA_SEGUNDOS = 60;

    public static void main(String[] args) throws InterruptedException {
        simulacionPrincipal();

        if (args.length > 0 && "estres".equalsIgnoreCase(args[0])) {
            pruebaAltaConcurrencia();
        }
    }

    private static void simulacionPrincipal() throws InterruptedException {
        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        Consola.resultado("[Zona de carga inicializada]");

        // Caso 1: lote inicial de pedidos
        Consola.seccion("Caso 1: ingreso del lote inicial de pedidos");
        List<Pedido> pedidos = new ArrayList<>();
        pedidos.add(new Pedido(1, "Santiago Centro"));
        pedidos.add(new Pedido(2, "Providencia"));
        pedidos.add(new Pedido(3, "Ñuñoa"));
        pedidos.add(new Pedido(4, "Recoleta"));
        pedidos.add(new Pedido(5, "Las Condes"));
        for (Pedido pedido : pedidos) {
            zonaDeCarga.agregarPedido(pedido);
        }

        // Lanzar 3 repartidores en paralelo
        Consola.seccion("Inicio de la jornada: " + CANTIDAD_REPARTIDORES
                + " repartidores trabajando en paralelo");
        List<Repartidor> repartidores = new ArrayList<>();
        ExecutorService jornada = Executors.newFixedThreadPool(CANTIDAD_REPARTIDORES);
        for (String nombre : NOMBRES_REPARTIDORES) {
            Repartidor repartidor = new Repartidor(nombre, zonaDeCarga);
            repartidores.add(repartidor);
            jornada.execute(repartidor);
        }

        // Caso 2: pedidos que llegan mientras los repartidores ya están trabajando
        Thread.sleep(1200);
        Consola.seccion("Caso 2: llegada tardía de pedidos con la jornada en curso");
        Pedido pedido6 = new Pedido(6, "Maipú");
        Pedido pedido7 = new Pedido(7, "La Florida");
        pedidos.add(pedido6);
        pedidos.add(pedido7);
        zonaDeCarga.agregarPedido(pedido6);
        zonaDeCarga.agregarPedido(pedido7);

        // Caso 3: validaciones (entregado, duplicado, nulo)
        Consola.seccion("Caso 3: validaciones del recurso compartido");
        zonaDeCarga.agregarPedido(new Pedido(99, "Dirección histórica", EstadoPedido.ENTREGADO));
        zonaDeCarga.agregarPedido(new Pedido(1, "Santiago Centro (duplicado)"));
        zonaDeCarga.agregarPedido(null);

        // Cierre y espera
        zonaDeCarga.cerrarRecepcion();
        jornada.shutdown();
        boolean finalizo = jornada.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);
        if (!finalizo) {
            jornada.shutdownNow();
            Consola.resultado("ADVERTENCIA: la jornada superó el tiempo máximo de espera.");
        }

        if (zonaDeCarga.estaVacia()) {
            Consola.salto();
            Consola.resultado("[Zona de carga vacía]");
        }

        informeFinal(zonaDeCarga, pedidos, repartidores);
    }

    private static void informeFinal(ZonaDeCarga zonaDeCarga,
                                     List<Pedido> pedidos,
                                     List<Repartidor> repartidores) {
        Consola.seccion("Verificación final de la jornada");

        Consola.resultado(String.format("%-6s %-28s %-12s %s", "ID", "DESTINO", "ESTADO", "REPARTIDOR"));
        for (Pedido pedido : pedidos) {
            Consola.resultado(String.format("#%-5d %-28s %-12s %s",
                    pedido.getId(),
                    pedido.getDireccionEntrega(),
                    pedido.getEstado(),
                    pedido.getRepartidorAsignado() == null ? "sin asignar" : pedido.getRepartidorAsignado()));
        }

        Consola.salto();
        Consola.resultado("Entregas por repartidor:");
        int sumaEntregas = 0;
        for (Repartidor repartidor : repartidores) {
            Consola.resultado("  - " + repartidor.getNombre() + ": "
                    + repartidor.getPedidosEntregados() + " pedido(s)");
            sumaEntregas += repartidor.getPedidosEntregados();
        }

        Consola.salto();
        Consola.resultado("Pedidos recibidos en la zona de carga : " + zonaDeCarga.getTotalRecibidos());
        Consola.resultado("Pedidos retirados (una sola vez)      : " + zonaDeCarga.getTotalRetirados());
        Consola.resultado("Pedidos entregados                    : " + zonaDeCarga.getTotalEntregados());
        Consola.resultado("Pedidos rechazados por validación     : " + zonaDeCarga.getPedidosRechazados());
        Consola.resultado("Retiros dobles evitados               : " + zonaDeCarga.getRetirosDoblesEvitados());
        Consola.resultado("Entregas duplicadas detectadas        : " + zonaDeCarga.getEntregasDuplicadasDetectadas());

        boolean sinDuplicados = zonaDeCarga.getEntregasDuplicadasDetectadas() == 0
                && zonaDeCarga.getRetirosDoblesEvitados() == 0;
        boolean cuadranTotales = zonaDeCarga.getTotalRecibidos() == zonaDeCarga.getTotalEntregados()
                && sumaEntregas == zonaDeCarga.getTotalEntregados();
        boolean todosEntregados = pedidos.stream()
                .allMatch(pedido -> pedido.getEstado() == EstadoPedido.ENTREGADO);

        Consola.salto();
        if (sinDuplicados && cuadranTotales && todosEntregados && zonaDeCarga.estaVacia()) {
            Consola.resultado("Todos los pedidos han sido entregados correctamente.");
        } else {
            Consola.resultado("ATENCIÓN: la verificación detectó inconsistencias en la jornada.");
        }
    }

    // Prueba con 500 pedidos y 12 repartidores para validar la sincronización bajo alta contención.
    private static void pruebaAltaConcurrencia() throws InterruptedException {
        final int totalPedidos = 500;
        final int totalRepartidores = 12;

        Consola.seccion("Caso 4: prueba de alta concurrencia ("
                + totalPedidos + " pedidos / " + totalRepartidores + " repartidores)");

        ZonaDeCarga zonaDeCarga = new ZonaDeCarga();
        List<Pedido> pedidos = new ArrayList<>();
        List<Repartidor> repartidores = new ArrayList<>();

        Consola.setModoSilencioso(true);
        ExecutorService jornada = Executors.newFixedThreadPool(totalRepartidores);
        for (int i = 1; i <= totalRepartidores; i++) {
            Repartidor repartidor = new Repartidor("R" + i, zonaDeCarga, 0, 2);
            repartidores.add(repartidor);
            jornada.execute(repartidor);
        }

        for (int i = 1; i <= totalPedidos; i++) {
            Pedido pedido = new Pedido(i, "Destino " + i);
            pedidos.add(pedido);
            zonaDeCarga.agregarPedido(pedido);
        }

        zonaDeCarga.cerrarRecepcion();
        jornada.shutdown();
        boolean finalizo = jornada.awaitTermination(ESPERA_MAXIMA_SEGUNDOS, TimeUnit.SECONDS);
        Consola.setModoSilencioso(false);

        if (!finalizo) {
            jornada.shutdownNow();
            Consola.resultado("ADVERTENCIA: la prueba de alta concurrencia no terminó a tiempo.");
        }

        int sumaEntregas = repartidores.stream().mapToInt(Repartidor::getPedidosEntregados).sum();
        long entregados = pedidos.stream()
                .filter(pedido -> pedido.getEstado() == EstadoPedido.ENTREGADO)
                .count();

        Consola.resultado("Pedidos ingresados                    : " + totalPedidos);
        Consola.resultado("Pedidos retirados (una sola vez)      : " + zonaDeCarga.getTotalRetirados());
        Consola.resultado("Pedidos en estado ENTREGADO           : " + entregados);
        Consola.resultado("Suma de entregas de los repartidores  : " + sumaEntregas);
        Consola.resultado("Retiros dobles evitados               : " + zonaDeCarga.getRetirosDoblesEvitados());
        Consola.resultado("Entregas duplicadas detectadas        : " + zonaDeCarga.getEntregasDuplicadasDetectadas());

        boolean correcto = entregados == totalPedidos
                && sumaEntregas == totalPedidos
                && zonaDeCarga.getTotalRetirados() == totalPedidos
                && zonaDeCarga.getRetirosDoblesEvitados() == 0
                && zonaDeCarga.getEntregasDuplicadasDetectadas() == 0
                && zonaDeCarga.estaVacia();

        Consola.salto();
        Consola.resultado(correcto
                ? "Prueba de alta concurrencia superada: sin condiciones de carrera ni bloqueos."
                : "ATENCIÓN: la prueba de alta concurrencia detectó inconsistencias.");
    }
}

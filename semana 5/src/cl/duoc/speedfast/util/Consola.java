package cl.duoc.speedfast.util;

// Salida sincronizada: evita que los mensajes de distintos hilos se mezclen.
public final class Consola {

    // En modo silencioso se omite el detalle paso a paso (para la prueba de estrés).
    private static boolean modoSilencioso = false;

    private Consola() { }

    public static synchronized void setModoSilencioso(boolean silencioso) {
        modoSilencioso = silencioso;
    }

    public static synchronized void log(String mensaje) {
        if (!modoSilencioso) {
            System.out.println(mensaje);
        }
    }

    // Siempre se imprime, incluso en modo silencioso.
    public static synchronized void resultado(String mensaje) {
        System.out.println(mensaje);
    }

    public static synchronized void seccion(String titulo) {
        System.out.println();
        System.out.println("========== " + titulo + " ==========");
    }

    public static synchronized void salto() {
        if (!modoSilencioso) {
            System.out.println();
        }
    }
}

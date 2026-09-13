# Semana 5 — Sincronización de procesos concurrentes

**Asignatura:** Desarrollo Orientado a Objetos II  
**Caso:** Coordinación de entregas en SpeedFast  
**Java 17+** | **IntelliJ IDEA**

## ¿De qué va esto?

SpeedFast tiene un problema: varios repartidores acceden al mismo tiempo a la zona de carga para retirar pedidos. Sin control, dos repartidores podrían llevarse el mismo paquete, o un pedido podría entregarse dos veces.

La idea es usar mecanismos de sincronización de Java para que cada pedido lo retire un solo repartidor, sin que ningún hilo quede colgado esperando al final.

## Estructura

```
semana 5/
├── README.md
├── salida-ejemplo.txt              ← salida real de una ejecución
└── src/cl/duoc/speedfast/
    ├── Main.java                   ← orquesta toda la simulación
    ├── modelo/
    │   ├── EstadoPedido.java       ← enum: PENDIENTE, EN_REPARTO, ENTREGADO
    │   └── Pedido.java             ← datos de la encomienda
    ├── concurrencia/
    │   ├── ZonaDeCarga.java        ← recurso compartido (la cola sincronizada)
    │   └── Repartidor.java         ← Runnable que ejecuta cada hilo
    └── util/
        └── Consola.java            ← para que no se mezclen los prints
```

## ¿Qué hace cada clase?

- **EstadoPedido**: enum con los tres estados posibles. Se usa en vez de Strings para evitar errores de tipeo.
- **Pedido**: el modelo de la encomienda. Tiene getters y setters sincronizados porque lo acceden varios hilos.
- **ZonaDeCarga**: acá está la sincronización. Usa `synchronized` para proteger la cola, y `wait()`/`notifyAll()` para que los repartidores esperen sin consumir CPU cuando no hay pedidos. Lo importante es que sacar un pedido de la cola y reservarlo ocurre en la misma sección crítica (retiro atómico), así no hay forma de que dos repartidores se lleven el mismo.
- **Repartidor**: implementa `Runnable`. En su `run()` pide pedidos a la zona de carga en un ciclo, simula el viaje con `Thread.sleep()` y los marca como entregados.
- **Consola**: centraliza los `println` con `synchronized` para que los mensajes no salgan mezclados.
- **Main**: crea la zona de carga, mete los pedidos, lanza los hilos con `ExecutorService`, espera a que terminen y al final verifica que todo cuadre.

## Cómo se sincroniza

1. **`synchronized`** en los métodos de `ZonaDeCarga` — solo un hilo a la vez puede tocar la cola.
2. **Retiro atómico** — sacar el pedido y marcarlo `EN_REPARTO` pasa dentro del mismo bloque sincronizado, así no hay ventana para race conditions.
3. **`wait()` / `notifyAll()`** — si no hay pedidos, el repartidor espera. Cuando llega uno nuevo o se cierra la recepción, se despierta a todos.
4. **Cierre limpio** — `cerrarRecepcion()` pone un flag y hace `notifyAll()`. Los repartidores que estaban esperando se despiertan, ven que no hay más trabajo y terminan solos. No queda ningún hilo bloqueado.
5. **Auditoría** — se guardan los IDs retirados y entregados en Sets, así se puede verificar al final que no hubo duplicados.

## Cómo ejecutar

### En IntelliJ

1. Abrir la carpeta `semana 5` como proyecto.
2. Configurar un JDK 17+ en Project Structure.
3. Click derecho en `Main.java` → Run.
4. Para la prueba de estrés, agregar `estres` como argumento del programa.

### Por terminal

```bash
javac -encoding UTF-8 -d out/production/semana5 $(find src -name "*.java")
java -cp out/production/semana5 cl.duoc.speedfast.Main
java -cp out/production/semana5 cl.duoc.speedfast.Main estres
```

## Qué se prueba

| Caso | Qué valida |
|------|------------|
| Caso 1 — 5 pedidos iniciales | Reparto normal con 3 hilos en paralelo |
| Caso 2 — 2 pedidos tardíos | Que los hilos que estaban en `wait()` se despiertan cuando llegan pedidos nuevos |
| Caso 3 — validaciones | Rechaza un pedido ya entregado, uno con ID repetido y una referencia nula |
| Caso 4 — estrés | 500 pedidos con 12 repartidores a velocidad máxima, para forzar condiciones de carrera |
| Verificación final | Tabla con cada pedido, su estado y repartidor. Cuadratura de totales. |

## Resultado

En `salida-ejemplo.txt` se puede ver una ejecución completa:

- Los 7 pedidos se entregan correctamente.
- 3 pedidos se rechazan por las validaciones.
- 0 retiros dobles, 0 entregas duplicadas.
- La prueba de estrés pasa con 500/500 pedidos sin ninguna inconsistencia.

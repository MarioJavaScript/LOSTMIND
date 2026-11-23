# LOSTMIND

**Lost in the maze, found by graphs.** Resolución de laberintos en Java con DFS, BFS y Dijkstra, paso a paso.

## Autor y entrega

- **Autor:** Mario Ramírez (GitHub: [MarioJavaScript](https://github.com/MarioJavaScript)).
- **Modalidad:** trabajo individual.
- **Fecha de entrega:** 28 de noviembre de 2026.

## Requisitos

JDK 17 o posterior y una terminal. No hay dependencias externas: solo se usa la biblioteca estándar de Java.

## Compilar y ejecutar

Desde la raíz del repositorio, en Windows PowerShell:

```powershell
javac -encoding UTF-8 src\*.java
java -cp src Main
```

En Linux o macOS:

```sh
javac -encoding UTF-8 src/*.java
java -cp src Main
```

Si en la opción 1 se deja la ruta vacía (ENTER), el programa lista los archivos de `ejemplos/` y permite elegir uno por número. Cuando el menú pida una ruta, se puede escribir, por ejemplo, `ejemplos/con_marcas.txt`. Las rutas relativas se interpretan desde el directorio donde se ejecuta Java.

## Formato de los laberintos

Cada archivo de texto UTF-8 contiene una matriz rectangular: todos los renglones deben tener la misma longitud y al menos una columna. Los saltos de línea no cuentan como columnas. No se eliminan los espacios al comienzo o al final de los renglones. Se admiten `+`, `-`, `|` como paredes y el espacio, `#`, `E`, `S` como celdas transitables; `#` funciona igual que un espacio. Las tabulaciones son inválidas. El BOM de UTF-8 se ignora; los saltos de línea CRLF, LF y CR se aceptan; un salto de línea final se acepta, pero una línea vacía extra se rechaza porque tendría 0 columnas. Si la ruta se pega entre comillas ("Copiar como ruta" de Windows), se quitan las comillas de los extremos.

Ejemplo con marcas explícitas:

```text
++E++++
+     +
+ +-+ +
+     +
++++S++
```

Si hay marcas, debe existir exactamente una `E` (entrada) y una `S` (salida). Si no hay marcas, debe haber exactamente dos celdas transitables en el borde. Se recorren de arriba abajo y, en cada renglón, de izquierda a derecha: la primera es la entrada y la segunda la salida. Cada esquina se cuenta una sola vez. Un laberinto sin camino también es válido. En `ejemplos/espacios_extremos.txt` hay espacios iniciales y finales que deben conservarse.

## Menú y despliegue

1. Cargar laberinto.
2. Resolver laberinto con DFS.
3. Resolver laberinto con BFS.
4. Resolver laberinto con Dijkstra (camino más corto).
5. Mostrar las tres soluciones.
6. Salir.

En la animación, pulsa **ENTER** para avanzar paso a paso, escribe un número para pausar esa cantidad de milisegundos entre pasos, (de 0 a 5000; fuera de rango se avisa y se vuelve a pedir) o `f` para saltar al resultado final. La leyenda es: `+ - |` pared original, espacio transitable, `E` entrada, `S` salida, `.` visitado, `@` vértice actual y `*` camino final. Los extremos conservan `E` y `S` en la vista final. Si no hay solución, se muestra la exploración y se informa que no existe camino, sin presentar una longitud.

## Representación y algoritmos

Se eligieron **listas ligadas de adyacencia** (`LinkedList`): cada celda transitable guarda solo sus vecinos transitables, lo que evita reservar espacio para pares de celdas sin conexión. El grafo es no dirigido y se representa con un arco en cada sentido; todos tienen costo 1 y conectan solo celdas horizontales o verticales. Se exploran en el orden **arriba, derecha, abajo, izquierda**. DFS usa una pila iterativa; BFS, una cola; Dijkstra, una cola de prioridad independiente de BFS.

Dijkstra desempata primero por distancia y después por el **id menor**, donde `id = fila * columnas + columna`. La longitud del camino se expresa en movimientos, es decir, número de vértices del camino menos uno. Los tiempos se miden con `System.nanoTime()` para la búsqueda y reconstrucción, y se muestran en **nanosegundos**. La impresión y la animación quedan fuera de esa medición.

## Pruebas

Después de compilar con el comando anterior:

```powershell
java -cp src PruebaCarga
java -cp src PruebaAlgoritmos
```

Los mismos comandos `java` funcionan en Linux y macOS. `PruebaCarga` verifica ejemplos válidos e inválidos; `PruebaAlgoritmos` comprueba los caminos, sus movimientos y la coincidencia de longitud entre BFS y Dijkstra.

## Responsabilidades

Proyecto realizado individualmente por Mario Ramírez.

## Declaración de uso de IA

| Empresa | Modelo | Uso |
| --- | --- | --- |
| OpenAI | Codex (GPT Sol) | Escribir código repetitivo. |
| Anthropic | Claude Opus 5.5 | Auditar el código. |

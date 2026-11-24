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

Para iniciar directamente con símbolos Unicode, usa `java -cp src Main --unicode`.

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

En la animación, pulsa **ENTER** para avanzar paso a paso, escribe un número para pausar esa cantidad de milisegundos entre pasos, (de 0 a 5000; fuera de rango se avisa y se vuelve a pedir) o `f` para saltar al resultado final. Con ASCII, la leyenda es: `+ - |` pared original, espacio transitable, `E` entrada, `S` salida, `.` visitado, `@` vértice actual y `*` camino final. Los extremos conservan sus símbolos en la vista final. Si no hay solución, se muestra la exploración y se informa que no existe camino, sin presentar una longitud.

## Caracteres alternativos

Al iniciar, el programa pregunta `Estilo de símbolos: 1) ASCII  2) Unicode (ENTER = ASCII)`.
Pulsa ENTER o escribe `1` para ASCII (predeterminado), o `2` para Unicode. El argumento
`--unicode` selecciona Unicode sin mostrar la pregunta. Las seis opciones del menú
siguen siendo las mismas.

| Elemento | ASCII | Unicode |
| --- | --- | --- |
| Pared | `+ - \|` | `─ │ ┌ ┐ └ ┘ ├ ┤ ┬ ┴ ┼` |
| Transitable | espacio | espacio |
| Entrada | `E` | `▶` |
| Salida | `S` | `◆` |
| Visitado | `.` | `·` |
| Vértice actual | `@` | `◉` |
| Camino final | `*` | `●` |

En Unicode, cada `+` se convierte en una esquina, unión o cruce según las paredes
vecinas. Cada celda sigue ocupando una posición; el archivo de entrada conserva
los caracteres ASCII originales y el grafo no cambia. En la vista final, entrada
y salida conservan sus símbolos. Si falla la salida Unicode, el programa avisa y
vuelve a ASCII.

En Windows, la terminal debe soportar UTF-8 y usar una fuente compatible con
estos símbolos. Antes de ejecutar, puedes escribir `chcp 65001` en la terminal.
Java configura la salida estándar con UTF-8.

## Interfaz gráfica (Swing)

La interfaz gráfica usa solo el JDK (Swing); no requiere dependencias externas ni
JavaFX. Desde la raíz del repositorio, en Windows PowerShell:

```powershell
javac -encoding UTF-8 src\*.java
java -cp src MainSwing
```

En Linux o macOS, cambia `src\*.java` por `src/*.java`. El botón **Cargar
laberinto** o el menú **Archivo > Cargar laberinto** abre un selector de archivos.
Si el archivo es inválido, se muestra el motivo y se conserva el laberinto anterior.
Los botones **DFS**, **BFS** y **Dijkstra** calculan y animan la exploración;
**Comparar los tres** abre tres tarjetas con los resultados completos y calcula
solo los que falten. **Volver** regresa a la vista principal. **Tiempos** muestra
los tiempos de búsqueda y reconstrucción en nanosegundos y milisegundos.

Durante la animación puedes usar **Pausar/Continuar**, **Siguiente paso**, el
control de **Velocidad** o **Saltar al final**. El control **Zoom** cambia el tamaño
de las celdas y **Ajustar** recupera el tamaño que cabe en el área visible, con
un mínimo de 24 píxeles por celda. Las barras de desplazamiento permiten recorrer
laberintos grandes. La leyenda bajo el laberinto explica los colores de cada capa.
**Salir** o el cierre de la ventana termina la aplicación.

## Representación y algoritmos

Se eligieron **listas ligadas de adyacencia** (`LinkedList`): cada celda transitable guarda solo sus vecinos transitables, lo que evita reservar espacio para pares de celdas sin conexión. El grafo es no dirigido y se representa con un arco en cada sentido; todos tienen costo 1 y conectan solo celdas horizontales o verticales. Se exploran en el orden **arriba, derecha, abajo, izquierda**. DFS usa una pila iterativa; BFS, una cola; Dijkstra, una cola de prioridad independiente de BFS.

Dijkstra desempata primero por distancia y después por el **id menor**, donde `id = fila * columnas + columna`. La longitud del camino se expresa en movimientos, es decir, número de vértices del camino menos uno. Los tiempos se miden con `System.nanoTime()` para la búsqueda y reconstrucción, y se muestran en **nanosegundos**. La impresión y la animación quedan fuera de esa medición.

## Pruebas

Después de compilar con el comando anterior:

```powershell
java -cp src PruebaCarga
java -cp src PruebaAlgoritmos
java -cp src PruebaTema
```

Los mismos comandos `java` funcionan en Linux y macOS. `PruebaCarga` verifica ejemplos válidos e inválidos; `PruebaAlgoritmos` comprueba los caminos, sus movimientos y la coincidencia de longitud entre BFS y Dijkstra. `PruebaTema` comprueba dimensiones, paredes, posiciones transitables y esquinas en ambos temas.

## Responsabilidades

Proyecto realizado individualmente por Mario Ramírez.

## Declaración de uso de IA

| Empresa | Modelo | Uso |
| --- | --- | --- |
| OpenAI | Codex (GPT Sol) | Escribir código repetitivo. |
| Anthropic | Claude Opus 5.5 | Auditar el código. |

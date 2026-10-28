# Contacto 3xtraterrestre

Entorno de desarrollo y compilador para tres lenguajes interconectados, proyecto de **Compiladores 2** (USAC / CUNOC), Fase 2:

- **Pig Latin** (`.pig`): lenguaje principal y punto de entrada; importa archivos de los otros dos.
- **Y?** (`.y`): lenguaje procedural con indentación significativa (funciones y estructuras).
- **Zetariano** (`.z`): lenguaje orientado a objetos de estilo Java (clases, herencia, sobrecarga, arreglos).

El sistema analiza los tres lenguajes (léxico, sintáctico y semántico), traduce un programa Pig con sus importaciones a **cuartetas** y luego a **ensamblador RISC-V de 64 bits**, lo ensambla, lo enlaza y lo ejecuta en un simulador, mostrando la salida en una consola interactiva.

## Estructura del repositorio

```
.
├── backend/                       API REST y compilador (Java 21, Spring Boot, ANTLR4)
│   ├── src/main/antlr4/           gramáticas de los tres lenguajes (lexer y parser de cada uno)
│   ├── src/main/java/             analizadores, generación de cuartetas, backend RISC-V, API
│   ├── src/main/resources/        application.properties (herramientas y límites de ejecución)
│   ├── src/test/java/             prueba de arranque de la aplicación
│   ├── DOCUMENTACION_TECNICA.md
│   └── MANUAL_USUARIO.md
├── frontend/                      IDE web (React, Vite, Material UI)
│   ├── src/                       editor, explorador de archivos, consola, estado del espacio de trabajo
│   ├── DOCUMENTACION_TECNICA.md
│   └── MANUAL_USUARIO.md
└── README.md
```

## Requisitos

| Componente | Para qué | Versión |
| :--- | :--- | :--- |
| JDK | compilar y ejecutar el backend | Java 21 o superior |
| Binutils de RISC-V | `riscv64-linux-gnu-as` y `riscv64-linux-gnu-ld` | cualquier versión reciente (probado con 2.38) |
| `qemu-riscv64` | ejecutar los programas generados (paquete `qemu-user`) | probado con 6.2.0 |
| Node.js y npm | compilar y servir el frontend | Node.js 20.19 o superior (o 22.12 o superior); probado con 22.20 |
| Linux | las sesiones interactivas leen `/proc` | — |

Maven no se instala: el backend incluye su wrapper (`./mvnw`). En Debian o Ubuntu:

```bash
sudo apt install openjdk-21-jdk binutils-riscv64-linux-gnu qemu-user
```

Sin las herramientas de RISC-V, la tokenización y la validación funcionan, pero no la compilación y ejecución.

## Cómo arrancar

### 1. Backend

```bash
cd backend
./mvnw clean package      # genera los analizadores ANTLR, compila y ejecuta las pruebas
./mvnw spring-boot:run    # la API queda en http://localhost:8080
```

### 2. Frontend

En otra terminal. El frontend lee la dirección del backend de la variable `VITE_API_BASE_URL`; créela en `frontend/.env`:

```bash
cd frontend
echo 'VITE_API_BASE_URL=http://localhost:8080' > .env
npm install
npm run dev               # el IDE queda en http://localhost:5173
```

Abra `http://localhost:5173` en el navegador. El backend solo acepta peticiones desde ese origen (CORS), así que el servidor de desarrollo debe usar el puerto 5173.

### 3. Un primer programa

En el IDE cree `hola.pig` con:

```
VARIABILES>
esto nombre : textum "mundo";
MAIOR>
>> "Hola " >> nombre;
FINIS;
```

y pulse **Compile & Run**: la pestaña *Output* muestra `Hola mundo`, y las pestañas *Quadruples* y *Assembly* muestran las cuartetas y el RISC-V generados.

## Documentación

| | Backend | Frontend |
| :--- | :--- | :--- |
| **Técnica** (arquitectura, diagramas, lenguajes, tipos, pipeline, API, errores, límites) | [backend/DOCUMENTACION_TECNICA.md](backend/DOCUMENTACION_TECNICA.md) | [frontend/DOCUMENTACION_TECNICA.md](frontend/DOCUMENTACION_TECNICA.md) |
| **De usuario** (instalación, operación, API con `curl`, solución de problemas / uso del IDE) | [backend/MANUAL_USUARIO.md](backend/MANUAL_USUARIO.md) | [frontend/MANUAL_USUARIO.md](frontend/MANUAL_USUARIO.md) |

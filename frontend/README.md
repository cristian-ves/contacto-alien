# Contacto Alien: frontend

Interfaz web del compilador de Contacto Alien: explorador de archivos, editor con coloreado en tiempo real para Pig Latin (`.pig`), Y? (`.y`) y Zetariano (`.z`), errores en el editor, y compilación y ejecución de programas Pig Latin con salida, cuartetas y ensamblador RISC-V. Es una SPA con React y Material UI; todo el análisis lo hace el backend.

## Requisitos
- Node.js 20.19 o superior (o 22.12 o superior) y npm.
- El backend en ejecución (por defecto en `http://localhost:8080`).

## Arrancar
```bash
npm ci
npm run dev
```
Abra `http://localhost:5173`. El backend solo acepta ese origen (CORS), así que el servidor de desarrollo debe usar ese puerto.

Otros comandos: `npm run build` (genera `dist/`), `npm run preview` y `npm run lint`.

## Variable de entorno
`VITE_API_BASE_URL` es la URL del backend. Si no está definida se usa `http://localhost:8080`. Para otro puerto, cree un archivo `.env` (ignorado por git) o páselo al ejecutar:
```bash
VITE_API_BASE_URL=http://localhost:9090 npm run dev
```

## Documentación
- [MANUAL_USUARIO.md](MANUAL_USUARIO.md): cómo usar la aplicación.
- [DOCUMENTACION_TECNICA.md](DOCUMENTACION_TECNICA.md): estructura, componentes y flujos.

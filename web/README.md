# Control Electrico Web

Version web de Control Electrico para usar desde un navegador.

## Funciones

- Resumen por periodo y usuario.
- Registro de recibos, lecturas, servicios y pagos.
- Exportacion de PDF, CSV y JSON.
- Importacion de respaldos CSV/JSON.
- Sincronizacion opcional con Supabase usando correo y contrasena.
- Respaldo opcional en Google Sheets usando permiso limitado `drive.file`.
- Modo diurno/nocturno.

## Ejecutar localmente

```powershell
pnpm install
pnpm run dev
```

Abre la URL que muestra Vite, normalmente `http://127.0.0.1:5173`.

## Compilar para publicar

```powershell
pnpm run build
```

La web compilada queda en `dist/`.

## Configurar Supabase

Puedes configurar Supabase desde la propia web, en `Cuenta y sincronizacion`.
Tambien puedes dejar variables de entorno para que la URL y clave publica
aparezcan precargadas:

```text
VITE_SUPABASE_URL=https://xxxxx.supabase.co
VITE_SUPABASE_ANON_KEY=tu_clave_publica
```

Usa solo la clave publica `publishable` o `anon`. Nunca uses `service_role`.
Ejecuta el SQL de `outputs/Supabase/setup.sql` en el proyecto Supabase.

## Publicacion recomendada

- Vercel: importa el repositorio, framework `Vite`, build `pnpm run build`, output `dist`.
- Netlify: build `pnpm run build`, publish directory `dist`.
- GitHub Pages: sube el contenido de `dist` o usa una accion de despliegue.

Los datos se guardan primero en el navegador (`localStorage`). Si configuras
Supabase e inicias sesion, la web puede sincronizar con Android y Windows.

## Google Sheets

Desde `Centro de respaldo` puedes crear o actualizar una hoja llamada
`Control Electrico` en Google Drive. La app guarda el respaldo completo en
`Respaldo_JSON` y crea pestañas legibles para usuarios, recibos, lecturas,
servicios y pagos.

La integracion usa Google OAuth con el alcance `drive.file`, por lo que la app
solo puede ver, crear o editar archivos de Drive abiertos o creados por ella.
Tambien puedes importar pegando el enlace o ID de una hoja ya vinculada.

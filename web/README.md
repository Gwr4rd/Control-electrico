# Control Electrico Web

Version web de Control Electrico para usar desde un navegador.

## Funciones

- Resumen por periodo y usuario.
- Registro de recibos, lecturas, servicios y pagos.
- Exportacion de PDF, CSV y JSON.
- Importacion de respaldos CSV/JSON.
- Sincronizacion opcional con Supabase usando correo y contrasena.
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

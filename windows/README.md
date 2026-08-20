# Control Electrico para Windows

Aplicacion de escritorio para Windows 10 y Windows 11 de 64 bits.

## Instalacion

1. Ejecutar `ControlElectrico-Windows-1.1.2.msi`.
2. Seguir el asistente de instalacion.
3. Abrir **Control Electrico** desde el escritorio o el menu Inicio.

Windows puede mostrar la advertencia **Editor desconocido** porque esta version no
incluye un certificado comercial de firma de codigo.

## Datos y respaldos

- Los datos se guardan localmente para poder trabajar sin conexion.
- El respaldo se puede exportar e importar desde CSV/JSON.
- La sincronizacion opcional usa una cuenta propia de Supabase con correo y
  contrasena.
- No se debe desinstalar la aplicacion ni borrar sus datos antes de confirmar que
  existe un respaldo reciente.

## Cuenta y sincronizacion

1. Ejecuta `outputs/Supabase/setup.sql` en el editor SQL de Supabase.
2. Abre `Cuenta y sincronizacion` desde el menu de la aplicacion.
3. Ingresa la URL del proyecto y la clave publica `publishable` o `anon`.
4. Crea una cuenta o inicia sesion con correo y contrasena.

Nunca uses la clave `service_role`. Cada cuenta solo puede acceder a su propio
respaldo mediante las politicas RLS incluidas en el script.

Cuando Supabase aun no esta configurado, esta pantalla incluye una guia,
un acceso al panel y un boton para copiar el script SQL.

## Compilar de nuevo

```powershell
pnpm install
pnpm run build:msi
```

El instalador se genera en `release/ControlElectrico-Windows-1.1.2.msi`.

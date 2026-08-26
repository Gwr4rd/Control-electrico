# Control Electrico v1.7.5

## Novedades

- Android 1.7.5: centro de respaldo con Google Sheets directo.
- Web 1.0.3: respaldo e importacion desde Google Sheets.
- Windows 1.1.6: respaldo e importacion desde Google Sheets usando el servidor local autorizado.
- Google Sheets crea o actualiza una hoja `Control Electrico`.
- La hoja incluye `Respaldo_JSON` con el respaldo completo dividido en fragmentos seguros para Sheets.
- Tambien se generan pestañas legibles: `Usuarios`, `Recibos`, `Lecturas`, `Servicios` y `Pagos`.
- Importacion desde Google Sheets permite usar la hoja vinculada o pegar un enlace/ID.
- El permiso de Google usado es `drive.file`, limitado a archivos creados o elegidos por la app.

## Verificacion

- Web: `pnpm test`
- Web: `pnpm build`
- Windows: `pnpm test`
- Windows: `pnpm run build:msi`
- Android: `gradle.bat testDebugUnitTest assembleRelease bundleRelease`

## Artefactos

- Android APK: `ControlElectrico-Android-1.7.5.apk`
- Android AAB: `ControlElectrico-Android-1.7.5.aab`
- Windows MSI: `ControlElectrico-Windows-1.1.6.msi`
- Web dist: `ControlElectrico-Web-1.0.3-dist.zip`

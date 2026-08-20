# Control Electrico - Release 2026-08-19

## Versiones

- Android: 1.7.4 (versionCode 12)
- Web: 1.0.2
- Windows: 1.1.5

## Cambios principales

- Pantalla Acerca de con nombre de la app, version instalada y web del creador.
- Resumen con alertas utiles: lecturas pendientes, deuda anterior, pagos abiertos, respaldo antiguo y consumo inusual.
- Historial de cambios local en Web/Windows para altas, ediciones, borrados, importaciones, respaldos y configuracion.
- Seguridad Web/Windows con bloqueo por PIN local.
- Respaldo JSON protegido en Web/Windows compatible con el formato cifrado de Android.
- Vista de resumen solo lectura en Web/Windows mediante URL con `?view=share`.
- Validacion visible de campos clave antes de guardar recibos importados.
- Versiones release nuevas para Android, Web y Windows.

## Artefactos generados localmente

Los archivos quedaron generados en la carpeta local `outputs`:

- `ControlElectrico-Android-1.7.4.apk`
- `ControlElectrico-Android-1.7.4.aab`
- `ControlElectrico-Windows-1.1.5.msi`
- `ControlElectricoWeb-1.0.2-dist.zip`

## SHA-256

- APK: `5BCE4DB2A5D19E064E61221126F1775E270C91B67BC628C796438EA629C69289`
- AAB: `435D324694F03BBB05B669314D6FB908675E1EBF68DB70811D6146F5FA2C6054`
- MSI: `9DB5F0ADC8BEDB64A7ED30EF6B7BB8F669B29587A6763191063F26C48EE2B04B`
- Web dist ZIP: `9E4698F8B4A75ACF02C121CD1993ADB94FEDC7981EB64224C55D9D93E931D6FE`

## Nota para GitHub Release

El MSI pesa mas de 100 MB, por lo que no debe subirse como archivo normal del repositorio. Debe adjuntarse como asset en un GitHub Release junto con el APK, AAB y ZIP web.

# Control Electrico Android

Aplicacion Android para registrar usuarios, recibos mensuales, lecturas internas y calcular pagos de consumo electrico.

## Requisitos

- Android Studio Ladybug o superior
- Android SDK 35
- Android 10 o superior en el telefono (`minSdk = 29`)

## Como abrir

1. Abre Android Studio.
2. Selecciona `Open`.
3. Elige esta carpeta: `ControlElectricoAndroid`.
4. Espera a que Gradle sincronice.
5. Ejecuta en un emulador o telefono Android 10+.

## Logica incluida

- Usuarios activos/inactivos.
- Usuario residual definido en la pestaña Usuarios, limitado a un solo usuario residual.
- Historial de estado por periodo para que crear, activar, inactivar o retirar usuarios no altere periodos anteriores.
- Pestaña Usuarios oculta del menu inferior; se abre desde el menu superior de tres puntos.
- Registro mensual del recibo externo.
- Importacion de recibo PDF con OCR local para autollenar los campos del recibo.
- Lecturas internas por usuario.
- Servicios y otros gastos por periodo con lista desplegable: Netflix, HBO Max, Disney, otros streaming, internet, agua/Sedapal y otros servicios escritos manualmente.
- Servicios activables/inactivables, editables y eliminables.
- Division de servicios por cantidad de usuarios participantes, con seleccion opcional de los usuarios especificos que pagan cada servicio.
- Los servicios sin monto no se guardan ni se muestran como registrados.
- Seleccion de fecha de lectura interna con calendario.
- Autollenado de lectura anterior usando la lectura actual previa del mismo usuario.
- Usuario residual opcional.
- Reparto de cargos fijos.
- Resumen por periodo seleccionable desde lista de periodos registrados.
- Resumen filtrable por usuario o visible para todos los usuarios.
- Resumen calculado con todos los usuarios activos, aunque alguno no tenga lectura en el periodo.
- Resumen redisenado como dashboard con total grande, metricas compactas y controles de periodo/usuario en la parte principal.
- Resumen con detalle de servicios activos, total general incluyendo electricidad y servicios del periodo.
- Al filtrar el resumen por un usuario, se muestra solo el total que debe pagar ese usuario, su consumo, su pago electrico, cada servicio dividido y el total final resaltado.
- Estado de pago por usuario y periodo: pagado total, pago parcial o no pagado.
- Los pagos parciales y no pagados generan un saldo que se suma al siguiente periodo disponible del mismo usuario.
- Los periodos sin estado de pago se consideran pendientes y se suman al siguiente periodo del usuario.
- Las deudas se identifican por periodo, conservan el importe original y muestran los abonos parciales aplicados.
- Los pagos se aplican primero a la deuda mas antigua.
- El estado de pago puede editarse o borrarse, y el saldo se recalcula en los periodos posteriores.
- Grafico de consumo electrico por usuario, o total del periodo al seleccionar todos los usuarios.
- Exportacion de PDF multipagina desde Resumen, con cabecera, totales destacados, deudas por periodo, servicios, grafico y pie de pagina.
- Centro de respaldo desde el menu superior.
- Exportacion de respaldo local en CSV o JSON.
- Exportacion de JSON protegido con clave opcional mediante cifrado AES-GCM.
- Exportacion de respaldo CSV o JSON a archivo local o Google Drive mediante el selector de documentos de Android.
- Importacion de respaldo CSV o JSON desde archivo local o Google Drive.
- Importacion de JSON protegido escribiendo la misma clave antes de seleccionar el archivo.
- Historial de respaldos locales dentro de la app.
- Copia automatica local antes de importar cualquier respaldo.
- Importacion con dos modos: reemplazar todo o fusionar datos sin duplicar claves existentes.
- Vista previa de cantidades antes de confirmar una importacion.
- Los respaldos CSV y JSON incluyen estados, montos y fechas de pago.
- Persistencia principal migrada a Room con migracion automatica desde los datos JSON locales anteriores.
- Pantalla de configuracion para IGV, redondeo, nombre del suministro, titular y recordatorio mensual.
- Recordatorio mensual mediante notificacion local.
- Alertas de validacion para lecturas invertidas, recibos incompletos, usuarios sin lectura, servicios sin participantes especificos y diferencias grandes.
- Historial comparativo en Resumen: consumo vs periodo anterior, pago vs periodo anterior, mayor consumidor y total de servicios.
- Funcionamiento local sin cuenta y sincronizacion opcional mediante una cuenta propia de Supabase.
- Guia integrada para crear el proyecto Supabase, abrir el panel y copiar el script SQL cuando la conexion aun no esta configurada.
- Opcion para compartir como imagen PNG el resumen de un usuario seleccionado, incluyendo consumo, servicios, deuda y total pendiente.
- Room conserva usuarios, recibos, lecturas, servicios, pagos y configuracion en el dispositivo.
- Confirmacion antes de eliminar usuarios, recibos, lecturas o servicios para evitar borrados accidentales.
- Modo AMOLED con cambio de colores y cambio de imagen superior segun el modo.
- Icono de launcher basado en la imagen original del medidor.
- Icono de launcher actualizado con una imagen mas clara para que se aprecie mejor.
- La app abre directamente en Resumen.
- El encabezado cambia segun la pestaña seleccionada: Recibo, Lecturas o Servicios; en Resumen mantiene `Control Electrico`.
- Estilo visual inspirado en Telegram: encabezado blanco en modo diurno, fondo claro y tarjetas limpias.
- Barra inferior compacta tipo pildora, con bordes redondeados y seleccion estilo liquid glass.
- Barra inferior semitranslucida con los textos debajo de cada icono.
- Servicios registrados con icono segun categoria: agua, internet, streaming u otros.
- Seleccion de participantes de servicios mediante chips visuales.
- Formularios agrupados por secciones para ingresar datos con menos confusion.
- Estados vacios redisenados con tarjetas de orientacion.
- Dialogo de revision despues de importar PDF, mostrando campos detectados y campos pendientes.
- El campo `Recibo del mes S/` se lee desde `TOTAL DEL MES`, no desde `TOTAL A PAGAR`, para evitar redondeos o importes pendientes.
- Modo de tarifa kWh seleccionable: `Precio unico por kWh`, `Dos bloques kWh` o estimacion solo si el recibo no muestra ningun precio kWh.
- `Electrificacion Rural` se muestra como `Electrificacion Rural (Ley N 28749)` y tambien reconoce `Aporte Ley N 28749`.
- `Electrificacion Rural (Ley N 28749)` puede activarse o desactivarse en el recibo.
- Mantenimiento y reposicion pueden ingresarse juntos o separados.
- Encabezado centrado.
- Encabezado blanco en modo diurno e icono superior ampliado.
- Menu superior redondeado con iconos por opcion.
- Menu superior simplificado: usuarios, centro de respaldo y cambio de modo diurno/nocturno.
- En Resumen, la lista de periodos muestra solo periodos con recibo y al menos una lectura.
- Tarjeta de inicio rapido en Resumen para guiar el primer uso: usuarios, recibo, lecturas y sincronizacion.
- Pantalla de Diagnostico con version, conteos de datos, ultimo respaldo, estado de sincronizacion y opcion para copiar el diagnostico.
- Pantalla de Privacidad con explicacion de almacenamiento local, respaldos y sincronizacion opcional.
- Vista previa de importacion de respaldo mejorada, comparando datos actuales contra datos importados antes de reemplazar o fusionar.
- Soporte para gesto de atras de Android en pantallas internas, ademas del boton superior.

En `Usuarios`, el campo `Periodo del cambio (YYYY-MM)` define desde que periodo aplica el estado activo/inactivo y residual. El boton eliminar retira al usuario desde el periodo actual sin borrar sus lecturas historicas.
- IGV 18%.
- Calculo de diferencia entre recibo y total asignado.

La persistencia principal usa Room (`control_electrico.db`). Si existian datos guardados en el formato anterior con `SharedPreferences` y JSON local, la app los migra automaticamente al abrirse cuando la base Room esta vacia. La aplicacion funciona sin cuenta; Internet solo se usa cuando el usuario configura Supabase y decide sincronizar.

## Cuenta y sincronizacion

La sincronizacion opcional utiliza correo y contrasena propios mediante Supabase Auth. Antes de usarla, ejecuta `outputs/Supabase/setup.sql` en el editor SQL de Supabase. Luego abre `Cuenta y sincronizacion` desde el menu superior e ingresa la URL del proyecto y su clave publica `publishable` o `anon`.

Nunca ingreses la clave `service_role`. Los datos se guardan primero en Room y se suben despues. La pantalla muestra porcentaje, ultima sincronizacion, revision, cambios pendientes y conflictos entre dispositivos.

## Centro de respaldo

En el menu superior de tres puntos puedes abrir `Centro de respaldo`. Desde ahi puedes:

- Guardar `JSON local`: respaldo completo recomendado para restaurar o migrar.
- Guardar `JSON local` protegido si escribes una clave en `Proteccion opcional`.
- Guardar `CSV local`: respaldo legible en Excel.
- Guardar `JSON en archivo/Drive`: permite escoger Google Drive o una carpeta del telefono.
- Guardar `CSV en archivo/Drive`: permite guardar una copia compatible con hojas de calculo.
- Importar desde archivo/Drive: acepta respaldos CSV o JSON.
- Restaurar desde el historial local de respaldos generados por la app.

Antes de importar, la app crea automaticamente un respaldo local en JSON con el prefijo `auto_antes_importar`. La importacion permite escoger entre `Reemplazar todo` o `Fusionar datos`. El modo fusionar conserva lo actual y agrega/actualiza registros usando claves como usuario, periodo, lectura y servicio para evitar duplicados.

El selector de documentos de Android permite guardar e importar respaldos desde una carpeta del telefono o Google Drive sin integrar una cuenta dentro de la aplicacion.

La opcion `Agregar/eliminar usuarios` se abre como pantalla completa sin barra inferior; usa el boton de atras en la parte superior para volver. Los servicios se administran desde la pestaña `Servicios`.

## Importar recibo PDF

En la pantalla `Recibo`, toca `Importar PDF` y selecciona el recibo. La app renderiza la primera pagina, ejecuta OCR local y completa los campos detectados:

- Periodo
- Fecha de lectura exterior
- Nro. suministro
- kWh exterior
- Total del recibo desde `TOTAL DEL MES`
- Precio por kWh cuando el recibo trae una sola tarifa
- Precio kWh hasta 30 y precio kWh mayor a 30 cuando el recibo trae dos bloques
- Cargo fijo
- Mantenimiento y reposicion
- Alumbrado publico
- Electrificacion Rural (Ley N 28749) o Aporte Ley N 28749

Los importes de `TOTAL DEL MES`, `Cargo Fijo`, `Mant. y Reposicion de Conexion`, `Alumbrado Publico`, `Electrificacion Rural (Ley N 28749)` y `Aporte Ley N 28749` se leen buscando la etiqueta y tomando el importe decimal ubicado visualmente a su derecha.

Si el recibo trae un solo `Precio por kWh`, selecciona `Precio unico por kWh`; el calculo multiplicara todo el consumo por esa tarifa y no mostrara `Umbral individual` en el resumen. Si trae dos bloques, usa `Dos bloques kWh`; ahi si se muestra el umbral individual para repartir los primeros 30 kWh entre los participantes. Usa `Estimar si no hay precio` solo cuando el recibo no muestra ningun precio kWh; en ese caso el calculo usa un precio promedio estimado desde `TOTAL DEL MES`. Si `Mantenimiento` y `Reposicion de Conexion` aparecen separados, activa `Mantenimiento y reposicion separados`.

La `Fecha de lectura exterior` se lee buscando el encabezado `Ultima lectura` y tomando la fecha ubicada visualmente debajo de ese encabezado.

Siempre revisa los datos antes de guardar, porque la precision depende de la calidad del PDF y del OCR.

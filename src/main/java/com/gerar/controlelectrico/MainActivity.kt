package com.gerar.controlelectrico

import android.Manifest
import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Typeface
import android.graphics.pdf.PdfDocument
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.util.Base64
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.result.IntentSenderRequest
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.Image
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDownload
import androidx.compose.material.icons.filled.CloudOff
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.ErrorOutline
import androidx.compose.material.icons.filled.FileDownload
import androidx.compose.material.icons.filled.FileUpload
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LightMode
import androidx.compose.material.icons.filled.List
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.NightsStay
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Save
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Button
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ElevatedCard
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.FileProvider
import com.gerar.controlelectrico.data.ElectricRepository
import com.gerar.controlelectrico.data.GoogleSheetsAuthorization
import com.gerar.controlelectrico.data.GoogleSheetsBackupManager
import com.gerar.controlelectrico.data.PdfReceiptReader
import com.gerar.controlelectrico.data.SupabaseConfig
import com.gerar.controlelectrico.data.SupabaseSyncManager
import com.gerar.controlelectrico.data.SupabaseSyncPhase
import com.gerar.controlelectrico.domain.AppSettings
import com.gerar.controlelectrico.domain.DebtItem
import com.gerar.controlelectrico.domain.ElectricCalculator
import com.gerar.controlelectrico.domain.ElectricUser
import com.gerar.controlelectrico.domain.MeterReading
import com.gerar.controlelectrico.domain.MonthlyReceipt
import com.gerar.controlelectrico.domain.PaymentBalance
import com.gerar.controlelectrico.domain.PaymentLedger
import com.gerar.controlelectrico.domain.PaymentResult
import com.gerar.controlelectrico.domain.PaymentStatus
import com.gerar.controlelectrico.domain.PeriodSummary
import com.gerar.controlelectrico.domain.ReceiptPdfData
import com.gerar.controlelectrico.domain.ServiceExpense
import com.gerar.controlelectrico.domain.UserPeriodState
import com.gerar.controlelectrico.domain.UserPayment
import com.gerar.controlelectrico.domain.isActiveInPeriod
import com.gerar.controlelectrico.domain.isResidualInPeriod
import com.gerar.controlelectrico.ui.theme.ControlElectricoTheme
import org.json.JSONArray
import org.json.JSONObject
import java.io.ByteArrayOutputStream
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.YearMonth
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cleanupInstalledUpdateIfNeeded(applicationContext)
        val repository = ElectricRepository(applicationContext)
        val syncManager = SupabaseSyncManager(applicationContext, repository)
        setContent {
            ControlElectricoTheme(amoled = repository.isAmoled.value) {
                ControlElectricoApp(repository, syncManager)
            }
        }
    }
}

private data class AppTab(
    val title: String,
    val icon: ImageVector
)

private data class UserOption(
    val id: String,
    val label: String
)

private data class ConsumptionPoint(
    val period: String,
    val kwh: Double
)

private data class BackupData(
    val users: List<ElectricUser>,
    val receipts: List<MonthlyReceipt>,
    val readings: List<MeterReading>,
    val services: List<ServiceExpense>,
    val payments: List<UserPayment> = emptyList()
)

private data class PendingImport(
    val backup: BackupData,
    val sourceLabel: String
)

private enum class GoogleSheetsAction {
    EXPORT,
    IMPORT
}

private data class LocalBackupInfo(
    val name: String,
    val path: String,
    val sizeBytes: Long,
    val lastModifiedMillis: Long
)

private data class AppUpdateInfo(
    val tagName: String,
    val versionName: String,
    val releaseName: String,
    val body: String,
    val apkAssetName: String,
    val apkDownloadUrl: String,
    val apkSizeBytes: Long,
    val publishedAt: String
)

private const val ALL_USERS_OPTION = "__all_users__"
private const val OTHER_SERVICE_OPTION = "Otro servicio"
private const val USERS_MANAGEMENT_TAB = 4
private const val SERVICES_MANAGEMENT_TAB = 5
private const val BACKUP_CENTER_TAB = 6
private const val SETTINGS_TAB = 7
private const val ACCOUNT_SYNC_TAB = 8
private const val DIAGNOSTICS_TAB = 9
private const val PRIVACY_TAB = 10
private const val ABOUT_TAB = 11
private const val KEY_QUICK_START_DISMISSED = "quick_start_dismissed"
private const val APP_CREATOR_WEBSITE = "https://github.com/Gwr4rd/Control-electrico"
private const val TARIFF_SINGLE = "Precio único por kWh"
private const val TARIFF_TWO_BLOCKS = "Dos bloques kWh"
private const val TARIFF_ESTIMATED = "Estimar si no hay precio"

private val serviceOptions = listOf(
    "Netflix",
    "HBO Max",
    "Disney",
    "Otros streaming",
    "Servicio de Internet",
    "Agua y alcantarillado (Sedapal)",
    OTHER_SERVICE_OPTION
)

private val tariffModeOptions = listOf(
    TARIFF_SINGLE,
    TARIFF_TWO_BLOCKS,
    TARIFF_ESTIMATED
)

private val appTabs = listOf(
    AppTab("Recibo", Icons.Default.Speed),
    AppTab("Lecturas", Icons.Default.Edit),
    AppTab("Servicios", Icons.Default.AttachMoney),
    AppTab("Resumen", Icons.Default.List)
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ControlElectricoApp(
    repository: ElectricRepository,
    syncManager: SupabaseSyncManager
) {
    val context = LocalContext.current
    var selectedTab by rememberSaveable { mutableStateOf(3) }
    var lastMainTab by rememberSaveable { mutableStateOf(3) }
    var menuExpanded by remember { mutableStateOf(false) }
    var pendingCsv by remember { mutableStateOf("") }
    var pendingJson by remember { mutableStateOf("") }
    var pendingImport by remember { mutableStateOf<PendingImport?>(null) }
    var backupPassword by rememberSaveable { mutableStateOf("") }
    var startupUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    var startupUpdateMessage by rememberSaveable { mutableStateOf("") }
    var startupUpdateDownloading by rememberSaveable { mutableStateOf(false) }
    var startupUpdateProgress by rememberSaveable { mutableStateOf(0) }
    val scope = rememberCoroutineScope()
    val logo = if (repository.isAmoled.value) R.drawable.medidor_amoled else R.drawable.medidor_original
    val isManagementScreen = selectedTab == USERS_MANAGEMENT_TAB ||
        selectedTab == SERVICES_MANAGEMENT_TAB ||
        selectedTab == BACKUP_CENTER_TAB ||
        selectedTab == SETTINGS_TAB ||
        selectedTab == ACCOUNT_SYNC_TAB ||
        selectedTab == DIAGNOSTICS_TAB ||
        selectedTab == PRIVACY_TAB ||
        selectedTab == ABOUT_TAB
    val title = when (selectedTab) {
        0 -> "Recibo"
        1 -> "Lecturas"
        2 -> "Servicios"
        3 -> "Control Eléctrico"
        USERS_MANAGEMENT_TAB -> "Usuarios"
        SERVICES_MANAGEMENT_TAB -> "Servicios"
        BACKUP_CENTER_TAB -> "Respaldo"
        SETTINGS_TAB -> "Configuracion"
        ACCOUNT_SYNC_TAB -> "Cuenta y sincronización"
        DIAGNOSTICS_TAB -> "Diagnóstico"
        PRIVACY_TAB -> "Privacidad"
        ABOUT_TAB -> "Acerca de"
        else -> "Control Eléctrico"
    }

    BackHandler(enabled = isManagementScreen) {
        selectedTab = lastMainTab
    }

    val csvLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("text/csv")) { uri ->
        if (uri != null && pendingCsv.isNotBlank()) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(pendingCsv.toByteArray(Charsets.UTF_8))
            }
        }
    }
    val jsonLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/json")) { uri ->
        if (uri != null && pendingJson.isNotBlank()) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(pendingJson.toByteArray(Charsets.UTF_8))
            }
        }
    }
    val backupImportLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)
                ?.bufferedReader(Charsets.UTF_8)
                ?.use { it.readText() }
                .orEmpty()
            pendingImport = PendingImport(parseBackupContent(raw, backupPassword), "archivo seleccionado")
        }.onFailure { error ->
            Toast.makeText(context, "No se pudo importar el respaldo: ${error.message}", Toast.LENGTH_LONG).show()
        }
    }
    val startupInstallPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Toast.makeText(
            context,
            "Si activaste el permiso, vuelve a tocar Descargar e instalar.",
            Toast.LENGTH_LONG
        ).show()
    }

    LaunchedEffect(repository.settings.value.updateRepositoryUrl) {
        val repositoryUrl = repository.settings.value.updateRepositoryUrl
        if (repositoryUrl.isBlank() || !shouldCheckGithubUpdate(context)) return@LaunchedEffect
        runCatching {
            checkForGithubUpdate(context, repositoryUrl)
        }.onSuccess { update ->
            markGithubUpdateChecked(context)
            if (update != null) {
                startupUpdate = update
                startupUpdateMessage = ""
            }
        }.onFailure {
            markGithubUpdateChecked(context)
        }
    }

    pendingImport?.let { pending ->
        ImportPreviewDialog(
            backup = pending.backup,
            currentBackup = repository.snapshotBackupData(),
            sourceLabel = pending.sourceLabel,
            onDismiss = { pendingImport = null },
            onConfirm = { replaceAll ->
                val autoBackup = saveAutomaticBackupBeforeImport(context, repository)
                if (replaceAll) {
                    repository.replaceAllData(
                        importedUsers = pending.backup.users,
                        importedReceipts = pending.backup.receipts,
                        importedReadings = pending.backup.readings,
                        importedServices = pending.backup.services,
                        importedPayments = pending.backup.payments
                    )
                } else {
                    repository.mergeAllData(
                        importedUsers = pending.backup.users,
                        importedReceipts = pending.backup.receipts,
                        importedReadings = pending.backup.readings,
                        importedServices = pending.backup.services,
                        importedPayments = pending.backup.payments
                    )
                }
                pendingImport = null
                val mode = if (replaceAll) "reemplazados" else "fusionados"
                val backupMessage = autoBackup?.let { " Copia previa: ${it.name}" }.orEmpty()
                Toast.makeText(context, "Datos $mode correctamente.$backupMessage", Toast.LENGTH_LONG).show()
            }
        )
    }

    startupUpdate?.let { update ->
        UpdateAvailableDialog(
            update = update,
            message = startupUpdateMessage,
            downloading = startupUpdateDownloading,
            progress = startupUpdateProgress,
            onDismiss = {
                startupUpdate = null
                startupUpdateMessage = ""
            },
            onInstall = {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                    !context.packageManager.canRequestPackageInstalls()
                ) {
                    startupUpdateMessage = "Activa Permitir desde esta fuente para instalar el APK descargado."
                    val intent = Intent(
                        Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                        Uri.parse("package:${context.packageName}")
                    )
                    startupInstallPermissionLauncher.launch(intent)
                    return@UpdateAvailableDialog
                }

                startupUpdateDownloading = true
                startupUpdateProgress = 0
                startupUpdateMessage = "Descargando APK..."
                scope.launch {
                    runCatching {
                        downloadAndInstallUpdate(context, update) { progress ->
                            startupUpdateProgress = progress
                        }
                    }.onSuccess {
                        startupUpdateMessage = "APK descargado. Confirma la instalacion en Android."
                    }.onFailure { error ->
                        startupUpdateMessage = "No se pudo instalar: ${error.message ?: "error desconocido"}"
                    }
                    startupUpdateDownloading = false
                }
            }
        )
    }

    Scaffold(
        topBar = {
            CenterAlignedTopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = if (repository.isAmoled.value) {
                        MaterialTheme.colorScheme.surface
                    } else {
                        MaterialTheme.colorScheme.surface
                    },
                    titleContentColor = if (repository.isAmoled.value) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.onSurface
                    },
                    navigationIconContentColor = if (repository.isAmoled.value) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    },
                    actionIconContentColor = if (repository.isAmoled.value) {
                        MaterialTheme.colorScheme.onSurface
                    } else {
                        MaterialTheme.colorScheme.primary
                    }
                ),
                navigationIcon = {
                    if (isManagementScreen) {
                        IconButton(onClick = { selectedTab = lastMainTab }) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Atras")
                        }
                    } else {
                        Image(
                            painter = painterResource(id = logo),
                            contentDescription = null,
                            modifier = Modifier
                                .padding(start = 12.dp)
                                .size(51.dp)
                        )
                    }
                },
                title = {
                    Text(
                        text = title,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp
                    )
                },
                actions = {
                    if (!isManagementScreen) {
                        IconButton(onClick = { menuExpanded = true }) {
                            Icon(Icons.Default.MoreVert, contentDescription = "Menu")
                        }
                        DropdownMenu(
                            expanded = menuExpanded,
                            onDismissRequest = { menuExpanded = false },
                            shape = RoundedCornerShape(24.dp),
                            containerColor = MaterialTheme.colorScheme.surface,
                            shadowElevation = 10.dp
                        ) {
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Person, contentDescription = null) },
                                text = { Text("Agregar/eliminar usuarios") },
                                onClick = {
                                    lastMainTab = selectedTab
                                    selectedTab = USERS_MANAGEMENT_TAB
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.CloudUpload, contentDescription = null) },
                                text = { Text("Centro de respaldo") },
                                onClick = {
                                    lastMainTab = selectedTab
                                    selectedTab = BACKUP_CENTER_TAB
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        if (syncManager.isSignedIn()) Icons.Default.CloudUpload else Icons.Default.CloudOff,
                                        contentDescription = null
                                    )
                                },
                                text = { Text("Cuenta y sincronización") },
                                onClick = {
                                    lastMainTab = selectedTab
                                    selectedTab = ACCOUNT_SYNC_TAB
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Edit, contentDescription = null) },
                                text = { Text("Configuracion") },
                                onClick = {
                                    lastMainTab = selectedTab
                                    selectedTab = SETTINGS_TAB
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.ErrorOutline, contentDescription = null) },
                                text = { Text("Diagnóstico") },
                                onClick = {
                                    lastMainTab = selectedTab
                                    selectedTab = DIAGNOSTICS_TAB
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.CheckCircle, contentDescription = null) },
                                text = { Text("Privacidad") },
                                onClick = {
                                    lastMainTab = selectedTab
                                    selectedTab = PRIVACY_TAB
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = { Icon(Icons.Default.Info, contentDescription = null) },
                                text = { Text("Acerca de") },
                                onClick = {
                                    lastMainTab = selectedTab
                                    selectedTab = ABOUT_TAB
                                    menuExpanded = false
                                }
                            )
                            DropdownMenuItem(
                                leadingIcon = {
                                    Icon(
                                        if (repository.isAmoled.value) Icons.Default.LightMode else Icons.Default.NightsStay,
                                        contentDescription = null
                                    )
                                },
                                text = { Text(if (repository.isAmoled.value) "Modo diurno" else "Modo nocturno") },
                                onClick = {
                                    repository.setAmoled(!repository.isAmoled.value)
                                    menuExpanded = false
                                }
                            )
                        }
                    }
                }
            )
        },
        bottomBar = {
            if (!isManagementScreen) {
                LiquidBottomBar(
                    selectedTab = selectedTab,
                    onTabSelected = { selectedTab = it }
                )
            }
        }
    ) { innerPadding ->
        val modifier = Modifier
            .fillMaxSize()
            .padding(innerPadding)

        when (selectedTab) {
            0 -> ReceiptScreen(repository, modifier)
            1 -> ReadingsScreen(repository, modifier)
            2 -> ServicesScreen(repository, modifier)
            3 -> SummaryScreen(
                repository = repository,
                modifier = modifier,
                onOpenReceipt = { selectedTab = 0 },
                onOpenReadings = { selectedTab = 1 },
                onOpenUsers = {
                    lastMainTab = selectedTab
                    selectedTab = USERS_MANAGEMENT_TAB
                },
                onOpenSync = {
                    lastMainTab = selectedTab
                    selectedTab = ACCOUNT_SYNC_TAB
                },
                syncConfigured = syncManager.isConfigured()
            )
            USERS_MANAGEMENT_TAB -> UsersScreen(repository, modifier)
            SERVICES_MANAGEMENT_TAB -> ServicesScreen(repository, modifier)
            BACKUP_CENTER_TAB -> BackupCenterScreen(
                repository = repository,
                modifier = modifier,
                onExportCsvDocument = {
                    pendingCsv = buildBackupCsv(repository)
                    csvLauncher.launch("${backupFileStem()}.csv")
                },
                onExportJsonDocument = {
                    pendingJson = buildBackupJsonForExport(repository, backupPassword)
                    jsonLauncher.launch("${backupFileStem()}.json")
                },
                onImportDocument = {
                    backupImportLauncher.launch("*/*")
                },
                onPreviewImport = { backup, source ->
                    pendingImport = PendingImport(backup, source)
                },
                backupPassword = backupPassword,
                onBackupPasswordChange = { backupPassword = it }
            )
            SETTINGS_TAB -> SettingsScreen(repository, modifier)
            ACCOUNT_SYNC_TAB -> AccountSyncScreen(syncManager, modifier)
            DIAGNOSTICS_TAB -> DiagnosticsScreen(repository, syncManager, modifier)
            PRIVACY_TAB -> PrivacyScreen(modifier)
            ABOUT_TAB -> AboutScreen(modifier)
        }
    }
}

@Composable
private fun AccountSyncScreen(
    syncManager: SupabaseSyncManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val currentConfig = syncManager.config.value
    val currentSession = syncManager.session.value
    val syncState = syncManager.state.value
    var projectUrl by rememberSaveable(currentConfig.url) { mutableStateOf(currentConfig.url) }
    var publicKey by rememberSaveable(currentConfig.anonKey) { mutableStateOf(currentConfig.anonKey) }
    var email by rememberSaveable(currentSession?.email) { mutableStateOf(currentSession?.email.orEmpty()) }
    var password by rememberSaveable { mutableStateOf("") }
    var createAccount by rememberSaveable { mutableStateOf(false) }
    var showSetupGuide by rememberSaveable { mutableStateOf(false) }
    val busy = syncState.phase == SupabaseSyncPhase.SYNCING

    if (showSetupGuide) {
        SupabaseSetupGuide(
            modifier = modifier,
            onBack = { showSetupGuide = false }
        )
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Icon(
                            imageVector = when (syncState.phase) {
                                SupabaseSyncPhase.ERROR,
                                SupabaseSyncPhase.UNCONFIGURED,
                                SupabaseSyncPhase.SIGNED_OUT -> Icons.Default.CloudOff
                                else -> Icons.Default.CloudUpload
                            },
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(30.dp)
                        )
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = syncStatusTitle(syncManager),
                                fontWeight = FontWeight.Bold,
                                fontSize = 17.sp
                            )
                            Text(
                                text = syncState.message,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 12.sp
                            )
                        }
                        if (busy) {
                            Text(
                                text = "${syncState.progress}%",
                                color = MaterialTheme.colorScheme.primary,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                    if (busy) {
                        LinearProgressIndicator(
                            progress = { syncState.progress / 100f },
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(100.dp))
                        )
                    }
                }
            }
        }

        if (!syncManager.isConfigured()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text("Conectar proyecto Supabase", fontWeight = FontWeight.Bold)
                        Text(
                            "Estos datos se solicitan una sola vez. Se encuentran en la configuración API del proyecto.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 12.sp
                        )
                        OutlinedTextField(
                            value = projectUrl,
                            onValueChange = { projectUrl = it },
                            label = { Text("URL del proyecto") },
                            placeholder = { Text("https://xxxxx.supabase.co") },
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = publicKey,
                            onValueChange = { publicKey = it },
                            label = { Text("Clave pública (publishable o anon)") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                syncManager.saveConfig(SupabaseConfig(projectUrl, publicKey))
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.Save, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Guardar conexión")
                        }
                        OutlinedButton(
                            onClick = { showSetupGuide = true },
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.List, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text("Cómo crear y configurar Supabase")
                        }
                        Text(
                            "Usa solamente la clave pública. Nunca ingreses la clave service_role.",
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 11.sp
                        )
                    }
                }
            }
        } else if (!syncManager.isSignedIn()) {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            FilterChip(
                                selected = !createAccount,
                                onClick = { createAccount = false },
                                label = { Text("Iniciar sesión") },
                                modifier = Modifier.weight(1f)
                            )
                            FilterChip(
                                selected = createAccount,
                                onClick = { createAccount = true },
                                label = { Text("Crear cuenta") },
                                modifier = Modifier.weight(1f)
                            )
                        }
                        OutlinedTextField(
                            value = email,
                            onValueChange = { email = it },
                            label = { Text("Correo") },
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("Contraseña") },
                            visualTransformation = PasswordVisualTransformation(),
                            singleLine = true,
                            modifier = Modifier.fillMaxWidth()
                        )
                        Button(
                            onClick = {
                                if (email.isBlank() || password.length < 6) {
                                    Toast.makeText(
                                        context,
                                        "Escribe un correo y una contraseña de al menos 6 caracteres",
                                        Toast.LENGTH_LONG
                                    ).show()
                                } else {
                                    scope.launch {
                                        runCatching {
                                            if (createAccount) {
                                                val confirmationRequired = syncManager.signUp(email, password)
                                                if (confirmationRequired) {
                                                    Toast.makeText(
                                                        context,
                                                        "Revisa tu correo para confirmar la cuenta",
                                                        Toast.LENGTH_LONG
                                                    ).show()
                                                }
                                            } else {
                                                syncManager.signIn(email, password)
                                            }
                                            password = ""
                                        }
                                    }
                                }
                            },
                            enabled = !busy,
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(Icons.Default.CloudUpload, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(if (createAccount) "Crear cuenta propia" else "Entrar y sincronizar")
                        }
                        TextButton(
                            onClick = {
                                syncManager.saveConfig(SupabaseConfig())
                                projectUrl = ""
                                publicKey = ""
                            },
                            modifier = Modifier.align(Alignment.CenterHorizontally)
                        ) {
                            Text("Cambiar conexión de Supabase")
                        }
                    }
                }
            }
        } else {
            item {
                ElevatedCard(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(
                            currentSession?.email.orEmpty().ifBlank { "Cuenta conectada" },
                            fontWeight = FontWeight.Bold
                        )
                        Divider()
                        MetricRow(
                            "Última sincronización",
                            syncManager.lastSyncedAt.value
                                .takeIf { it.isNotBlank() }
                                ?.take(19)
                                ?.replace("T", " ")
                                ?: "Aún no realizada"
                        )
                        MetricRow(
                            "Revisión en la nube",
                            syncManager.revision.value.takeIf { it > 0 }?.toString() ?: "Sin respaldo"
                        )
                        MetricRow(
                            "Cambios locales",
                            if (syncManager.hasPendingChanges()) "Pendientes de subir" else "Ninguno"
                        )
                    }
                }
            }

            if (syncState.phase == SupabaseSyncPhase.CONFLICT) {
                item {
                    ElevatedCard(
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(24.dp),
                        colors = CardDefaults.elevatedCardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(18.dp),
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Text(
                                "Cambios en dos dispositivos",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                "Fusionar conserva ambos respaldos y da prioridad a este dispositivo cuando un registro coincide.",
                                color = MaterialTheme.colorScheme.onErrorContainer,
                                fontSize = 12.sp
                            )
                            Button(
                                onClick = {
                                    scope.launch {
                                        syncManager.resolveConflict(SupabaseSyncManager.CONFLICT_CLOUD)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usar datos de la nube")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        syncManager.resolveConflict(SupabaseSyncManager.CONFLICT_MERGE)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Fusionar respaldos")
                            }
                            Button(
                                onClick = {
                                    scope.launch {
                                        syncManager.resolveConflict(SupabaseSyncManager.CONFLICT_DEVICE)
                                    }
                                },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Usar este dispositivo")
                            }
                        }
                    }
                }
            }

            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    TextButton(
                        onClick = { scope.launch { syncManager.signOut() } },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Cerrar sesión")
                    }
                    Button(
                        onClick = { scope.launch { syncManager.syncNow() } },
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(6.dp))
                        Text("Sincronizar")
                    }
                }
            }
        }

        if (syncState.error.isNotBlank()) {
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(18.dp))
                        .background(MaterialTheme.colorScheme.errorContainer)
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    Icon(
                        Icons.Default.ErrorOutline,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onErrorContainer
                    )
                    Text(
                        syncState.error,
                        color = MaterialTheme.colorScheme.onErrorContainer,
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SupabaseSetupGuide(
    modifier: Modifier = Modifier,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            TextButton(onClick = onBack) {
                Icon(Icons.Default.ArrowBack, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("Volver a la conexión")
            }
        }
        item {
            Text(
                "Configurar Supabase",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                "Sigue estos pasos una sola vez para dejar lista la sincronización segura por cuenta.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        item {
            SetupGuideStep(
                number = "1",
                title = "Crear el proyecto",
                text = "Abre Supabase, crea un proyecto llamado ControlElectrico, guarda la contraseña de base de datos y elige la región más cercana."
            )
        }
        item {
            Button(
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://database.new/"))
                    )
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Public, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Abrir Supabase")
            }
        }
        item {
            SetupGuideStep(
                number = "2",
                title = "Crear la tabla segura",
                text = "En SQL Editor pulsa New query, pega el script de configuración y selecciona Run."
            )
        }
        item {
            OutlinedButton(
                onClick = {
                    val sql = context.resources.openRawResource(R.raw.supabase_setup)
                        .bufferedReader(Charsets.UTF_8)
                        .use { it.readText() }
                    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                    clipboard.setPrimaryClip(ClipData.newPlainText("Supabase Control Electrico", sql))
                    Toast.makeText(context, "Script SQL copiado", Toast.LENGTH_SHORT).show()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Copiar script SQL")
            }
        }
        item {
            SetupGuideStep(
                number = "3",
                title = "Habilitar correo y contraseña",
                text = "En Authentication > Providers abre Email y activa Email provider. Para una primera prueba puedes desactivar Confirm email."
            )
        }
        item {
            SetupGuideStep(
                number = "4",
                title = "Copiar la conexión",
                text = "En Settings > API Keys copia Project URL y Publishable key. Nunca copies Secret key ni service_role."
            )
        }
        item {
            SetupGuideStep(
                number = "5",
                title = "Conectar tus dispositivos",
                text = "Regresa a esta pantalla, guarda la URL y la clave pública. Crea la cuenta en el primer dispositivo e inicia sesión con la misma cuenta en los demás."
            )
        }
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Default.CheckCircle, contentDescription = null)
                    Text(
                        "La configuración es correcta cuando Authentication > Users muestra tu correo y la tabla control_electrico_sync contiene una fila.",
                        fontSize = 12.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun SetupGuideStep(
    number: String,
    title: String,
    text: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.62f))
            .padding(14.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .size(34.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(MaterialTheme.colorScheme.primary),
            contentAlignment = Alignment.Center
        ) {
            Text(number, color = MaterialTheme.colorScheme.onPrimary, fontWeight = FontWeight.Bold)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.Bold)
            Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
    }
}

private fun syncStatusTitle(syncManager: SupabaseSyncManager): String {
    return when (syncManager.state.value.phase) {
        SupabaseSyncPhase.UNCONFIGURED -> "Supabase no configurado"
        SupabaseSyncPhase.SIGNED_OUT -> "Sin cuenta conectada"
        SupabaseSyncPhase.SYNCING -> "Sincronizando"
        SupabaseSyncPhase.SYNCED -> if (syncManager.hasPendingChanges()) {
            "Cambios pendientes"
        } else {
            "Sincronizado"
        }
        SupabaseSyncPhase.CONFLICT -> "Requiere revisión"
        SupabaseSyncPhase.ERROR -> "Error de sincronización"
    }
}

@Composable
private fun ImportPreviewDialog(
    backup: BackupData,
    currentBackup: BackupData,
    sourceLabel: String,
    onDismiss: () -> Unit,
    onConfirm: (Boolean) -> Unit
) {
    var replaceAll by rememberSaveable { mutableStateOf(true) }

    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Importar respaldo") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Origen: $sourceLabel",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                ImportDataSummary(title = "Datos actuales", backup = currentBackup)
                ImportDataSummary(title = "Datos del respaldo", backup = backup)
                Divider()
                ToggleRow("Reemplazar todo", replaceAll) { replaceAll = it }
                Text(
                    text = if (replaceAll) {
                        "Se creara una copia automatica local y luego se reemplazaran usuarios, recibos, lecturas, servicios y pagos actuales."
                    } else {
                        "Se creara una copia automatica local y luego se fusionaran registros. Si una clave coincide, el respaldo importado tendra prioridad."
                    },
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Revisa las cantidades antes de continuar. Esta accion puede cambiar el resumen de periodos anteriores.",
                    color = MaterialTheme.colorScheme.error
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(replaceAll) }) {
                Text(if (replaceAll) "Reemplazar datos" else "Fusionar datos")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun ImportDataSummary(title: String, backup: BackupData) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(title, fontWeight = FontWeight.SemiBold)
            MetricRow("Usuarios", backup.users.size.toString())
            MetricRow("Recibos", backup.receipts.size.toString())
            MetricRow("Lecturas", backup.readings.size.toString())
            MetricRow("Servicios", backup.services.size.toString())
            MetricRow("Pagos", backup.payments.size.toString())
            MetricRow("Ultimo periodo", backup.latestPeriod().ifBlank { "Sin periodo" })
        }
    }
}

@Composable
private fun UpdateAvailableDialog(
    update: AppUpdateInfo,
    message: String,
    downloading: Boolean,
    progress: Int,
    onDismiss: () -> Unit,
    onInstall: () -> Unit
) {
    AlertDialog(
        onDismissRequest = { if (!downloading) onDismiss() },
        icon = { Icon(Icons.Default.CloudDownload, contentDescription = null) },
        title = { Text("Actualizacion disponible") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("Hay una nueva version de Control Electrico.")
                MetricRow("Version", update.versionName)
                MetricRow("Archivo", update.apkAssetName)
                MetricRow("Tamaño", update.apkSizeBytes.fileSize())
                if (update.releaseName.isNotBlank()) {
                    MetricRow("Release", update.releaseName)
                }
                if (message.isNotBlank()) {
                    Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (downloading) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    Text("$progress%", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                Text(
                    text = "Android pedira confirmar la instalacion manualmente.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            Button(
                enabled = !downloading,
                onClick = onInstall,
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(Icons.Default.FileDownload, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text("Descargar e instalar")
            }
        },
        dismissButton = {
            TextButton(enabled = !downloading, onClick = onDismiss) {
                Text("Mas tarde")
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun ConfirmActionDialog(
    title: String,
    message: String,
    confirmText: String = "Eliminar",
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text(confirmText)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancelar")
            }
        }
    )
}

@Composable
private fun PdfImportReviewDialog(
    data: ReceiptPdfData,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        title = { Text("Revisar PDF importado") },
        text = {
            Column(
                modifier = Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Estos campos se cargaron al formulario. Los pendientes necesitan revision manual.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                data.reviewFields().forEach { field ->
                    PdfFieldStatusRow(field.first, field.second)
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Seguir editando")
            }
        }
    )
}

@Composable
private fun PdfFieldStatusRow(label: String, value: String?) {
    val detected = !value.isNullOrBlank()
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = if (detected) Icons.Default.CheckCircle else Icons.Default.ErrorOutline,
            contentDescription = null,
            tint = if (detected) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(20.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(label, fontWeight = FontWeight.Medium)
            Text(
                text = value?.takeIf { it.isNotBlank() } ?: "Pendiente de revisar",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun BackupCenterScreen(
    repository: ElectricRepository,
    modifier: Modifier = Modifier,
    onExportCsvDocument: () -> Unit,
    onExportJsonDocument: () -> Unit,
    onImportDocument: () -> Unit,
    onPreviewImport: (BackupData, String) -> Unit,
    backupPassword: String,
    onBackupPasswordChange: (String) -> Unit
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val googleSheetsManager = remember { GoogleSheetsBackupManager(context.applicationContext) }
    var refreshKey by rememberSaveable { mutableStateOf(0) }
    val localBackups = remember(refreshKey) { listLocalBackups(context) }
    var googleSheetInput by rememberSaveable { mutableStateOf(repository.settings.value.googleSheetId) }
    var googleSheetsBusy by rememberSaveable { mutableStateOf(false) }
    var googleSheetsMessage by rememberSaveable { mutableStateOf("") }
    var pendingGoogleAction by remember { mutableStateOf<GoogleSheetsAction?>(null) }

    fun runGoogleSheetsAction(action: GoogleSheetsAction, accessToken: String) {
        scope.launch {
            googleSheetsBusy = true
            googleSheetsMessage = if (action == GoogleSheetsAction.EXPORT) {
                "Subiendo respaldo a Google Sheets..."
            } else {
                "Leyendo respaldo desde Google Sheets..."
            }
            runCatching {
                when (action) {
                    GoogleSheetsAction.EXPORT -> {
                        val targetSheetId = GoogleSheetsBackupManager.extractSheetId(googleSheetInput)
                            .ifBlank { repository.settings.value.googleSheetId }
                        val result = googleSheetsManager.exportBackup(
                            accessToken = accessToken,
                            backupJson = buildBackupJson(repository),
                            existingSpreadsheetId = targetSheetId
                        )
                        googleSheetInput = result.spreadsheetId
                        repository.saveSettings(
                            repository.settings.value.copy(
                                googleSheetId = result.spreadsheetId,
                                googleSheetName = "Control Electrico",
                                googleSheetUpdatedAt = result.updatedAt
                            )
                        )
                        googleSheetsMessage = "Respaldo guardado en Google Sheets."
                        Toast.makeText(context, "Respaldo guardado en Google Sheets", Toast.LENGTH_LONG).show()
                    }
                    GoogleSheetsAction.IMPORT -> {
                        val targetSheetId = GoogleSheetsBackupManager.extractSheetId(googleSheetInput)
                            .ifBlank { repository.settings.value.googleSheetId }
                        if (targetSheetId.isBlank()) error("Pega el enlace o ID de una hoja de Google Sheets.")
                        val raw = googleSheetsManager.importBackup(accessToken, targetSheetId)
                        googleSheetInput = targetSheetId
                        repository.saveSettings(
                            repository.settings.value.copy(
                                googleSheetId = targetSheetId,
                                googleSheetName = "Control Electrico",
                                googleSheetUpdatedAt = LocalDateTime.now().toString()
                            )
                        )
                        onPreviewImport(parseBackupContent(raw, backupPassword), "Google Sheets")
                        googleSheetsMessage = "Respaldo de Google Sheets listo para revisar."
                    }
                }
            }.onFailure { error ->
                googleSheetsMessage = "Google Sheets: ${error.message}"
                Toast.makeText(context, googleSheetsMessage, Toast.LENGTH_LONG).show()
            }
            googleSheetsBusy = false
        }
    }

    val googleSheetsAuthLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartIntentSenderForResult()
    ) { result ->
        val action = pendingGoogleAction ?: return@rememberLauncherForActivityResult
        pendingGoogleAction = null
        if (result.resultCode != Activity.RESULT_OK) {
            googleSheetsMessage = "Permiso de Google Sheets cancelado."
            Toast.makeText(context, googleSheetsMessage, Toast.LENGTH_LONG).show()
            return@rememberLauncherForActivityResult
        }
        runCatching {
            googleSheetsManager.authorizationFromIntent(result.data)
        }.onSuccess { authorization ->
            runGoogleSheetsAction(action, authorization.accessToken)
        }.onFailure { error ->
            googleSheetsMessage = "Google Sheets: ${error.message}"
            Toast.makeText(context, googleSheetsMessage, Toast.LENGTH_LONG).show()
        }
    }

    fun requestGoogleSheets(action: GoogleSheetsAction) {
        val activity = context as? Activity
        if (activity == null) {
            googleSheetsMessage = "Google Sheets requiere abrir la app desde Android."
            Toast.makeText(context, googleSheetsMessage, Toast.LENGTH_LONG).show()
            return
        }
        pendingGoogleAction = action
        googleSheetsBusy = true
        googleSheetsMessage = "Solicitando permiso de Google Sheets..."
        scope.launch {
            runCatching {
                googleSheetsManager.authorize(activity)
            }.onSuccess { authorization ->
                when (authorization) {
                    is GoogleSheetsAuthorization.Ready -> {
                        runGoogleSheetsAction(action, authorization.accessToken)
                    }
                    is GoogleSheetsAuthorization.NeedsResolution -> {
                        googleSheetsBusy = false
                        googleSheetsMessage = "Autoriza Google Sheets para continuar."
                        googleSheetsAuthLauncher.launch(
                            IntentSenderRequest.Builder(authorization.pendingIntent.intentSender).build()
                        )
                    }
                }
            }.onFailure { error ->
                googleSheetsBusy = false
                googleSheetsMessage = "Google Sheets: ${error.message}"
                Toast.makeText(context, googleSheetsMessage, Toast.LENGTH_LONG).show()
            }
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SummaryBackupCard(repository)
        }

        item {
            FormCard(title = "Proteccion opcional") {
                Text(
                    text = "Si colocas una clave, los respaldos JSON se guardaran cifrados. Para importar un respaldo cifrado debes escribir la misma clave antes de seleccionarlo.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextInput("Clave para respaldo protegido", backupPassword) {
                    onBackupPasswordChange(it)
                }
            }
        }

        item {
            FormCard(title = "Guardar respaldo local") {
                Text(
                    text = "Se guarda dentro del almacenamiento de la app y queda disponible en el historial local.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        val file = saveLocalBackup(
                            context,
                            "${backupFileStem()}.json",
                            buildBackupJsonForExport(repository, backupPassword)
                        )
                        refreshKey++
                        val mode = if (backupPassword.isBlank()) "JSON local" else "JSON protegido local"
                        Toast.makeText(context, "Respaldo $mode: ${file.name}", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar JSON local")
                }
                Button(
                    onClick = {
                        val file = saveLocalBackup(context, "${backupFileStem()}.csv", buildBackupCsv(repository))
                        refreshKey++
                        Toast.makeText(context, "Respaldo CSV local: ${file.name}", Toast.LENGTH_LONG).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.FileDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar CSV local")
                }
            }
        }

        item {
            FormCard(title = "Archivo o Google Drive") {
                Text(
                    text = "Elige Google Drive en el selector de Android para guardar o importar desde la nube.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    onClick = {
                        Toast.makeText(context, "Elige Google Drive o una carpeta del telefono", Toast.LENGTH_SHORT).show()
                        onExportJsonDocument()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar JSON en archivo/Drive")
                }
                Button(
                    onClick = {
                        Toast.makeText(context, "Elige Google Drive o una carpeta del telefono", Toast.LENGTH_SHORT).show()
                        onExportCsvDocument()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Guardar CSV en archivo/Drive")
                }
                Button(
                    onClick = {
                        Toast.makeText(context, "Selecciona un respaldo CSV o JSON", Toast.LENGTH_SHORT).show()
                        onImportDocument()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar desde archivo/Drive")
                }
            }
        }

        item {
            FormCard(title = "Google Sheets") {
                Text(
                    text = "Crea o actualiza una hoja de calculo en tu Google Drive. La hoja guarda un respaldo completo y pestañas legibles para revisar usuarios, recibos, lecturas, servicios y pagos.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextInput("ID o enlace de la hoja", googleSheetInput) {
                    googleSheetInput = it
                }
                Button(
                    enabled = !googleSheetsBusy,
                    onClick = { requestGoogleSheets(GoogleSheetsAction.EXPORT) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.CloudUpload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Subir respaldo a Google Sheets")
                }
                Button(
                    enabled = !googleSheetsBusy,
                    onClick = { requestGoogleSheets(GoogleSheetsAction.IMPORT) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Importar desde Google Sheets")
                }
                if (googleSheetsBusy) {
                    LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                }
                if (googleSheetsMessage.isNotBlank()) {
                    Text(googleSheetsMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                if (repository.settings.value.googleSheetId.isNotBlank()) {
                    MetricRow("Hoja vinculada", repository.settings.value.googleSheetId)
                    MetricRow(
                        "Ultima actualizacion",
                        repository.settings.value.googleSheetUpdatedAt.ifBlank { "Pendiente" }
                    )
                }
            }
        }

        item { SectionTitle("Historial local") }

        if (localBackups.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Save,
                    title = "Sin respaldos locales",
                    message = "Guarda un respaldo local o importa datos para crear copias automaticas."
                )
            }
        }

        items(localBackups, key = { it.path }) { backup ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = if (backup.name.endsWith(".json", ignoreCase = true)) Icons.Default.Save else Icons.Default.FileDownload,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(backup.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "${backup.sizeBytes.fileSize()} - ${backup.lastModifiedMillis.fileDateTime()}",
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Button(
                        onClick = {
                            runCatching {
                                val file = File(backup.path)
                                onPreviewImport(
                                    parseBackupContent(file.readText(Charsets.UTF_8), backupPassword),
                                    "historial local: ${backup.name}"
                                )
                            }.onFailure { error ->
                                Toast.makeText(context, "No se pudo leer el respaldo: ${error.message}", Toast.LENGTH_LONG).show()
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(Icons.Default.FileUpload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Restaurar este respaldo")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryBackupCard(repository: ElectricRepository) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text("Datos actuales", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            MetricRow("Usuarios", repository.users.size.toString())
            MetricRow("Recibos", repository.receipts.size.toString())
            MetricRow("Lecturas", repository.readings.size.toString())
            MetricRow("Servicios", repository.serviceExpenses.count { it.amount > 0.0 }.toString())
        }
    }
}

@Composable
private fun SettingsScreen(repository: ElectricRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val current = repository.settings.value
    var igvPercent by rememberSaveable { mutableStateOf((current.igvRate * 100.0).input()) }
    var roundUp by rememberSaveable { mutableStateOf(current.roundUpToTenth) }
    var supplyAlias by rememberSaveable { mutableStateOf(current.supplyAlias) }
    var accountHolder by rememberSaveable { mutableStateOf(current.accountHolder) }
    var reminderEnabled by rememberSaveable { mutableStateOf(current.monthlyReminderEnabled) }
    var reminderDay by rememberSaveable { mutableStateOf(current.reminderDay.toString()) }
    var updateRepositoryUrl by rememberSaveable { mutableStateOf(current.updateRepositoryUrl) }
    var checkingUpdate by rememberSaveable { mutableStateOf(false) }
    var downloadingUpdate by rememberSaveable { mutableStateOf(false) }
    var updateMessage by rememberSaveable { mutableStateOf("") }
    var updateProgress by rememberSaveable { mutableStateOf(0) }
    var availableUpdate by remember { mutableStateOf<AppUpdateInfo?>(null) }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { granted ->
        if (!granted) {
            Toast.makeText(context, "Sin permiso de notificaciones no se mostrara el recordatorio.", Toast.LENGTH_LONG).show()
        }
    }
    val installPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) {
        Toast.makeText(
            context,
            "Si activaste el permiso, vuelve a tocar Descargar e instalar.",
            Toast.LENGTH_LONG
        ).show()
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard(title = "Calculo electrico") {
                DecimalInput("IGV (%)", igvPercent) { igvPercent = it }
                ToggleRow("Redondear hacia arriba a decimos", roundUp) { roundUp = it }
            }
        }
        item {
            FormCard(title = "Datos del suministro") {
                TextInput("Nombre del suministro", supplyAlias) { supplyAlias = it }
                TextInput("Titular o referencia", accountHolder) { accountHolder = it }
            }
        }
        item {
            FormCard(title = "Recordatorio mensual") {
                ToggleRow("Activar recordatorio", reminderEnabled) { reminderEnabled = it }
                DecimalInput("Dia del mes (1-28)", reminderDay) { reminderDay = it }
                SaveButton(enabled = true, text = "Guardar configuracion") {
                    val next = AppSettings(
                        igvRate = (igvPercent.toDoubleValue() / 100.0).coerceAtLeast(0.0),
                        roundUpToTenth = roundUp,
                        supplyAlias = supplyAlias.trim(),
                        accountHolder = accountHolder.trim(),
                        monthlyReminderEnabled = reminderEnabled,
                        reminderDay = reminderDay.toIntValue().coerceIn(1, 28),
                        googleSheetId = repository.settings.value.googleSheetId,
                        googleSheetName = repository.settings.value.googleSheetName,
                        googleSheetUpdatedAt = repository.settings.value.googleSheetUpdatedAt,
                        updateRepositoryUrl = normalizeGithubRepositoryUrl(updateRepositoryUrl.trim())
                    )
                    updateRepositoryUrl = next.updateRepositoryUrl
                    repository.saveSettings(next)
                    if (next.monthlyReminderEnabled) {
                        if (Build.VERSION.SDK_INT >= 33 &&
                            context.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
                        ) {
                            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        }
                        scheduleMonthlyReminder(context, next.reminderDay)
                    } else {
                        cancelMonthlyReminder(context)
                    }
                    Toast.makeText(context, "Configuracion guardada", Toast.LENGTH_SHORT).show()
                }
            }
        }
        item {
            FormCard(title = "Actualizaciones por GitHub") {
                TextInput("Repositorio GitHub", updateRepositoryUrl) { updateRepositoryUrl = it }
                Text(
                    text = "Ejemplo: https://github.com/tu_usuario/control-electrico. La app revisara el ultimo Release y buscara un APK.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Button(
                    enabled = !checkingUpdate && !downloadingUpdate && updateRepositoryUrl.isNotBlank(),
                    onClick = {
                        val normalized = normalizeGithubRepositoryUrl(updateRepositoryUrl.trim())
                        updateRepositoryUrl = normalized
                        repository.saveSettings(repository.settings.value.copy(updateRepositoryUrl = normalized))
                        availableUpdate = null
                        updateMessage = "Consultando GitHub..."
                        checkingUpdate = true
                        scope.launch {
                            runCatching {
                                checkForGithubUpdate(context, normalized)
                            }.onSuccess { update ->
                                availableUpdate = update
                                updateMessage = update?.let {
                                    "Nueva version disponible: ${it.versionName} (${it.apkAssetName})"
                                } ?: "La app ya esta actualizada."
                            }.onFailure { error ->
                                updateMessage = "No se pudo verificar: ${error.message ?: "error desconocido"}"
                            }
                            checkingUpdate = false
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.CloudDownload, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (checkingUpdate) "Buscando..." else "Buscar actualizacion")
                }

                if (updateMessage.isNotBlank()) {
                    Text(updateMessage, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }

                availableUpdate?.let { update ->
                    MetricRow("Version encontrada", update.versionName)
                    MetricRow("Tamaño APK", update.apkSizeBytes.fileSize())
                    if (update.releaseName.isNotBlank()) {
                        MetricRow("Release", update.releaseName)
                    }
                    Button(
                        enabled = !downloadingUpdate,
                        onClick = {
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O &&
                                !context.packageManager.canRequestPackageInstalls()
                            ) {
                                val intent = Intent(
                                    Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES,
                                    Uri.parse("package:${context.packageName}")
                                )
                                installPermissionLauncher.launch(intent)
                                updateMessage = "Activa Permitir desde esta fuente para instalar el APK descargado."
                                return@Button
                            }

                            downloadingUpdate = true
                            updateProgress = 0
                            updateMessage = "Descargando APK..."
                            scope.launch {
                                runCatching {
                                    downloadAndInstallUpdate(context, update) { progress ->
                                        updateProgress = progress
                                    }
                                }.onSuccess {
                                    updateMessage = "APK descargado. Confirma la instalacion en Android."
                                }.onFailure { error ->
                                    updateMessage = "No se pudo instalar: ${error.message ?: "error desconocido"}"
                                }
                                downloadingUpdate = false
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(22.dp)
                    ) {
                        Icon(Icons.Default.FileDownload, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(if (downloadingUpdate) "Descargando $updateProgress%" else "Descargar e instalar")
                    }
                    if (downloadingUpdate) {
                        LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsScreen(
    repository: ElectricRepository,
    syncManager: SupabaseSyncManager,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val backups = remember { listLocalBackups(context) }
    val latestBackup = backups.maxByOrNull { it.lastModifiedMillis }
    val syncState = syncManager.state.value
    val versionName = remember { appVersionName(context) }
    val diagnosticsText = buildDiagnosticsText(repository, syncManager, latestBackup, versionName)

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard(title = "Estado general") {
                MetricRow("Version Android", versionName)
                MetricRow("Usuarios", repository.users.size.toString())
                MetricRow("Recibos", repository.receipts.size.toString())
                MetricRow("Lecturas", repository.readings.size.toString())
                MetricRow("Servicios con monto", repository.serviceExpenses.count { it.amount > 0.0 }.toString())
                MetricRow("Pagos registrados", repository.payments.size.toString())
            }
        }

        item {
            FormCard(title = "Respaldo") {
                MetricRow("Respaldos locales", backups.size.toString())
                MetricRow(
                    "Ultimo respaldo",
                    latestBackup?.let { "${it.name} - ${it.lastModifiedMillis.fileDateTime()}" } ?: "Sin respaldos"
                )
                Text(
                    text = "Antes de importar datos, la app crea una copia automatica local para poder revisar el estado anterior.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        item {
            FormCard(title = "Sincronización") {
                MetricRow("Estado", syncStatusTitle(syncManager))
                MetricRow("Mensaje", syncState.message.ifBlank { "Sin mensaje" })
                MetricRow("Progreso", "${syncState.progress}%")
                MetricRow("Revision nube", syncManager.revision.value.toString())
                MetricRow(
                    "Ultima sincronización",
                    syncManager.lastSyncedAt.value.takeIf { it.isNotBlank() } ?: "Aun no realizada"
                )
                MetricRow("Cambios pendientes", if (syncManager.hasPendingChanges()) "Si" else "No")
                if (syncState.error.isNotBlank()) {
                    Text(syncState.error, color = MaterialTheme.colorScheme.error)
                }
            }
        }

        item {
            FormCard(title = "Acciones de soporte") {
                Button(
                    onClick = {
                        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                        clipboard.setPrimaryClip(ClipData.newPlainText("Diagnostico Control Electrico", diagnosticsText))
                        Toast.makeText(context, "Diagnostico copiado", Toast.LENGTH_SHORT).show()
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Save, contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text("Copiar diagnostico")
                }
                Text(
                    text = "No copia contrasenas ni claves de Supabase. Solo incluye conteos, version y estado visible.",
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun PrivacyScreen(modifier: Modifier = Modifier) {
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard(title = "Resumen de privacidad") {
                PrivacyBullet("Los datos principales se guardan en el telefono usando almacenamiento local de la app.")
                PrivacyBullet("La sincronización con Supabase es opcional y solo funciona si el usuario configura una cuenta.")
                PrivacyBullet("Los respaldos manuales se guardan donde el usuario elija: telefono, carpeta local o selector de Android.")
                PrivacyBullet("La app no debe recibir ni guardar claves secretas como service_role.")
            }
        }
        item {
            FormCard(title = "Datos que puede guardar") {
                PrivacyBullet("Usuarios, recibos, lecturas internas, servicios, pagos y configuracion.")
                PrivacyBullet("Archivos PDF seleccionados para lectura de recibo; los valores detectados se muestran para revision.")
                PrivacyBullet("Estado de sincronización, revision y fecha de ultimo respaldo en la pantalla de diagnostico.")
            }
        }
        item {
            FormCard(title = "Buenas practicas") {
                PrivacyBullet("Protege los respaldos JSON con clave si los vas a guardar fuera del telefono.")
                PrivacyBullet("No compartas la clave publica junto con contrasenas ni datos personales.")
                PrivacyBullet("Antes de reemplazar datos, revisa el conteo de usuarios, recibos, lecturas y pagos.")
            }
        }
    }
}

@Composable
private fun AboutScreen(modifier: Modifier = Modifier) {
    val context = LocalContext.current
    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            ElevatedCard(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(
                    modifier = Modifier.padding(22.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Image(
                        painter = painterResource(id = R.drawable.medidor_original),
                        contentDescription = null,
                        modifier = Modifier
                            .size(96.dp)
                            .clip(RoundedCornerShape(24.dp))
                    )
                    Text(
                        "Control Eléctrico",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        "Versión ${appVersionName(context)}",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Button(
                        onClick = {
                            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(APP_CREATOR_WEBSITE)))
                        },
                        shape = RoundedCornerShape(22.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Icon(Icons.Default.Public, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("Web del creador")
                    }
                }
            }
        }
        item {
            FormCard(title = "Qué hace la app") {
                PrivacyBullet("Controla recibos, lecturas internas, usuario residual, servicios compartidos y pagos pendientes.")
                PrivacyBullet("Permite respaldos JSON, CSV y JSON protegido con clave.")
                PrivacyBullet("Puede sincronizar datos con Supabase si el usuario configura su propia cuenta.")
            }
        }
    }
}

@Composable
private fun PrivacyBullet(text: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            Icons.Default.CheckCircle,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(18.dp)
        )
        Text(text, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun LiquidBottomBar(
    selectedTab: Int,
    onTabSelected: (Int) -> Unit
) {
    val barShape = RoundedCornerShape(26.dp)
    val itemShape = RoundedCornerShape(18.dp)
    val barColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.68f)
    val selectedColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
    val borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.24f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 6.dp),
        contentAlignment = Alignment.Center
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .shadow(18.dp, barShape, clip = false)
                .clip(barShape)
                .background(barColor)
                .border(1.dp, borderColor, barShape)
                .padding(horizontal = 5.dp, vertical = 5.dp),
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            appTabs.forEachIndexed { index, tab ->
                val selected = selectedTab == index
                val contentColor = if (selected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .height(40.dp)
                        .clip(itemShape)
                        .background(if (selected) selectedColor else MaterialTheme.colorScheme.surface.copy(alpha = 0.0f))
                        .clickable { onTabSelected(index) }
                        .padding(horizontal = 4.dp, vertical = 3.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(
                        imageVector = tab.icon,
                        contentDescription = tab.title,
                        tint = contentColor,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = tab.title,
                        color = contentColor,
                        fontSize = 9.sp,
                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun UsersScreen(repository: ElectricRepository, modifier: Modifier = Modifier) {
    var userId by rememberSaveable { mutableStateOf(nextUserId(repository.users.size + 1)) }
    var name by rememberSaveable { mutableStateOf("") }
    var internalMeter by rememberSaveable { mutableStateOf("") }
    var isActive by rememberSaveable { mutableStateOf(true) }
    var isResidual by rememberSaveable { mutableStateOf(false) }
    var statePeriod by rememberSaveable { mutableStateOf(currentPeriod()) }
    var notes by rememberSaveable { mutableStateOf("") }
    var pendingDeleteUser by remember { mutableStateOf<ElectricUser?>(null) }
    val todayPeriod = currentPeriod()

    pendingDeleteUser?.let { user ->
        ConfirmActionDialog(
            title = "Eliminar usuario",
            message = "Se inactivara a ${user.name.ifBlank { user.userId }} desde el periodo actual sin borrar su historial.",
            onDismiss = { pendingDeleteUser = null },
            onConfirm = {
                repository.deleteUser(user)
                pendingDeleteUser = null
            }
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard(title = "Usuario") {
                TextInput("ID usuario", userId) { userId = it.uppercase(Locale.US) }
                TextInput("Nombre", name) { name = it }
                TextInput("Medidor interno", internalMeter) { internalMeter = it }
                TextInput("Periodo del cambio (YYYY-MM)", statePeriod) { statePeriod = it }
                ToggleRow("Activo", isActive) { isActive = it }
                ToggleRow("Usuario residual", isResidual) { isResidual = it }
                TextInput("Notas", notes, singleLine = false) { notes = it }
                SaveButton(
                    enabled = userId.isNotBlank() && name.isNotBlank(),
                    text = "Guardar usuario"
                ) {
                    repository.saveUser(
                        user = ElectricUser(
                            userId = userId.trim(),
                            name = name.trim(),
                            internalMeter = internalMeter.trim(),
                            isActive = isActive,
                            isResidual = isResidual,
                            notes = notes.trim()
                        ),
                        statePeriod = statePeriod.trim()
                    )
                    userId = nextUserId(repository.users.size + 1)
                    name = ""
                    internalMeter = ""
                    isActive = true
                    isResidual = false
                    statePeriod = todayPeriod
                    notes = ""
                }
            }
        }

        item { SectionTitle("Usuarios registrados") }

        items(repository.users, key = { it.userId }) { user ->
            val activeNow = user.isActiveInPeriod(todayPeriod)
            val residualNow = user.isResidualInPeriod(todayPeriod)
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${user.userId} - ${user.name}", fontWeight = FontWeight.SemiBold)
                        Text(user.internalMeter.ifBlank { "Sin medidor interno" })
                        Text(
                            listOf(
                                if (activeNow) "Activo" else "Inactivo",
                                if (residualNow) "Residual" else ""
                            ).filter { it.isNotBlank() }.joinToString(" - ")
                        )
                        if (user.notes.isNotBlank()) Text(user.notes)
                    }
                    IconButton(
                        onClick = {
                            userId = user.userId
                            name = user.name
                            internalMeter = user.internalMeter
                            isActive = user.isActiveInPeriod(todayPeriod)
                            isResidual = user.isResidualInPeriod(todayPeriod)
                            statePeriod = todayPeriod
                            notes = user.notes
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar usuario")
                    }
                    IconButton(onClick = { pendingDeleteUser = user }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar usuario")
                    }
                }
            }
        }
    }
}

@Composable
private fun ReceiptScreen(repository: ElectricRepository, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    var editingOriginalPeriod by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(currentPeriod()) }
    var externalDate by rememberSaveable { mutableStateOf("") }
    var supplyNumber by rememberSaveable { mutableStateOf("") }
    var externalKwh by rememberSaveable { mutableStateOf("") }
    var monthlyBill by rememberSaveable { mutableStateOf("") }
    var singleKwhPrice by rememberSaveable { mutableStateOf("") }
    var priceUpTo30 by rememberSaveable { mutableStateOf("") }
    var priceOver30 by rememberSaveable { mutableStateOf("") }
    var fixedCharge by rememberSaveable { mutableStateOf("") }
    var maintenance by rememberSaveable { mutableStateOf("") }
    var maintenanceOnly by rememberSaveable { mutableStateOf("") }
    var replacementOnly by rememberSaveable { mutableStateOf("") }
    var publicLighting by rememberSaveable { mutableStateOf("") }
    var ruralElectrification by rememberSaveable { mutableStateOf("") }
    var tariffMode by rememberSaveable { mutableStateOf(TARIFF_TWO_BLOCKS) }
    var includeRuralLaw by rememberSaveable { mutableStateOf(true) }
    var splitMaintenanceReplacement by rememberSaveable { mutableStateOf(false) }
    var notes by rememberSaveable { mutableStateOf("") }
    var importStatus by rememberSaveable { mutableStateOf("") }
    var isImporting by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteReceipt by remember { mutableStateOf<MonthlyReceipt?>(null) }
    var pendingPdfReview by remember { mutableStateOf<ReceiptPdfData?>(null) }
    val receiptWarnings = listOfNotNull(
        "El total del recibo esta en cero".takeIf { monthlyBill.isNotBlank() && monthlyBill.toDoubleValue() <= 0.0 },
        "El kWh exterior esta en cero".takeIf { externalKwh.isNotBlank() && externalKwh.toDoubleValue() <= 0.0 },
        "Revisa el precio único por kWh".takeIf {
            tariffMode == TARIFF_SINGLE && singleKwhPrice.toDoubleValue() <= 0.0
        },
        "Revisa las tarifas kWh por bloques".takeIf {
            tariffMode == TARIFF_TWO_BLOCKS && (
                priceUpTo30.toDoubleValue() <= 0.0 ||
                priceOver30.toDoubleValue() <= 0.0
            )
        }
    )
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        isImporting = true
        importStatus = "Leyendo PDF..."
        PdfReceiptReader.readReceipt(
            context = context,
            uri = uri,
            onSuccess = { data ->
                data.period?.let { period = it }
                data.externalReadingDate?.let { externalDate = it }
                data.supplyNumber?.let { supplyNumber = it }
                data.externalKwh?.let { externalKwh = it.input() }
                data.monthlyBill?.let { monthlyBill = it.input() }
                if (data.priceKwhUpTo30 != null || data.priceKwhOver30 != null) {
                    tariffMode = if (data.priceKwhRateCount <= 1) TARIFF_SINGLE else TARIFF_TWO_BLOCKS
                    data.priceKwhUpTo30?.let { singleKwhPrice = it.input() }
                    data.priceKwhUpTo30?.let { priceUpTo30 = it.input() }
                    data.priceKwhOver30?.let { priceOver30 = it.input() }
                } else {
                    tariffMode = TARIFF_ESTIMATED
                    singleKwhPrice = ""
                    priceUpTo30 = ""
                    priceOver30 = ""
                }
                data.fixedCharge?.let { fixedCharge = it.input() }
                data.maintenance?.let { maintenance = it.input() }
                if (data.maintenanceOnly != null || data.replacementOnly != null) {
                    splitMaintenanceReplacement = true
                    data.maintenanceOnly?.let { maintenanceOnly = it.input() }
                    data.replacementOnly?.let { replacementOnly = it.input() }
                }
                data.publicLighting?.let { publicLighting = it.input() }
                if (data.ruralElectrification != null) {
                    includeRuralLaw = true
                    ruralElectrification = data.ruralElectrification.input()
                } else if (ruralElectrification.isBlank()) {
                    includeRuralLaw = false
                }
                importStatus = data.importMessage()
                pendingPdfReview = data
                isImporting = false
            },
            onError = { error ->
                importStatus = error
                isImporting = false
            }
        )
    }

    pendingPdfReview?.let { data ->
        PdfImportReviewDialog(
            data = data,
            onDismiss = { pendingPdfReview = null }
        )
    }

    pendingDeleteReceipt?.let { receipt ->
        ConfirmActionDialog(
            title = "Eliminar recibo",
            message = "Se eliminara el recibo del periodo ${receipt.period}.",
            onDismiss = { pendingDeleteReceipt = null },
            onConfirm = {
                repository.deleteReceipt(receipt)
                if (editingOriginalPeriod == receipt.period) {
                    editingOriginalPeriod = ""
                }
                pendingDeleteReceipt = null
            }
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard(title = "Recibo mensual") {
                Button(
                    enabled = !isImporting,
                    onClick = { pdfLauncher.launch("application/pdf") },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.Add, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (isImporting) "Leyendo PDF..." else "Importar PDF")
                }
                if (importStatus.isNotBlank()) {
                    Text(importStatus, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                FormSectionLabel("Datos principales")
                TextInput("Periodo (YYYY-MM)", period) { period = it }
                TextInput("Fecha lectura exterior", externalDate) { externalDate = it }
                TextInput("Nro suministro", supplyNumber) { supplyNumber = it }
                FormSectionLabel("Consumo y tarifas")
                DecimalInput("kWh medidor exterior", externalKwh) { externalKwh = it }
                DecimalInput("Recibo del mes S/", monthlyBill) { monthlyBill = it }
                PeriodDropdown(
                    label = "Modo de tarifa kWh",
                    selected = tariffMode,
                    options = tariffModeOptions,
                    onSelected = { tariffMode = it }
                )
                if (tariffMode == TARIFF_SINGLE) {
                    DecimalInput("Precio por kWh", singleKwhPrice) {
                        singleKwhPrice = it
                        priceUpTo30 = it
                        priceOver30 = it
                    }
                } else if (tariffMode == TARIFF_TWO_BLOCKS) {
                    DecimalInput("Precio kWh hasta 30", priceUpTo30) { priceUpTo30 = it }
                    DecimalInput("Precio kWh mayor a 30", priceOver30) { priceOver30 = it }
                } else {
                    Text(
                        text = "Usa esta opcion solo si el recibo no muestra ningun precio kWh; se estimara un precio promedio desde el TOTAL DEL MES.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                FormSectionLabel("Cargos del recibo")
                DecimalInput("Cargo fijo", fixedCharge) { fixedCharge = it }
                ToggleRow("Mantenimiento y reposicion separados", splitMaintenanceReplacement) {
                    splitMaintenanceReplacement = it
                }
                if (splitMaintenanceReplacement) {
                    DecimalInput("Mantenimiento", maintenanceOnly) { maintenanceOnly = it }
                    DecimalInput("Reposicion de conexion", replacementOnly) { replacementOnly = it }
                } else {
                    DecimalInput("Mantenimiento y reposicion", maintenance) { maintenance = it }
                }
                DecimalInput("Alumbrado publico", publicLighting) { publicLighting = it }
                ToggleRow("Incluir Electrificación Rural (Ley N° 28749)", includeRuralLaw) {
                    includeRuralLaw = it
                }
                if (includeRuralLaw) {
                    DecimalInput("Electrificación Rural (Ley N° 28749)", ruralElectrification) { ruralElectrification = it }
                }
                TextInput("Observaciones", notes, singleLine = false) { notes = it }
                if (receiptWarnings.isNotEmpty()) {
                    ValidationWarningsCard(receiptWarnings)
                }
                SaveButton(
                    enabled = period.isNotBlank(),
                    text = if (editingOriginalPeriod.isBlank()) "Guardar recibo" else "Actualizar recibo"
                ) {
                    repository.saveReceipt(
                        receipt = MonthlyReceipt(
                            period = period.trim(),
                            externalReadingDate = externalDate.trim(),
                            supplyNumber = supplyNumber.trim(),
                            externalKwh = externalKwh.toDoubleValue(),
                            monthlyBill = monthlyBill.toDoubleValue(),
                            priceKwhUpTo30 = when (tariffMode) {
                                TARIFF_SINGLE -> singleKwhPrice.toDoubleValue()
                                TARIFF_TWO_BLOCKS -> priceUpTo30.toDoubleValue()
                                else -> 0.0
                            },
                            priceKwhOver30 = when (tariffMode) {
                                TARIFF_SINGLE -> singleKwhPrice.toDoubleValue()
                                TARIFF_TWO_BLOCKS -> priceOver30.toDoubleValue()
                                else -> 0.0
                            },
                            fixedCharge = fixedCharge.toDoubleValue(),
                            maintenance = if (splitMaintenanceReplacement) {
                                maintenanceOnly.toDoubleValue() + replacementOnly.toDoubleValue()
                            } else {
                                maintenance.toDoubleValue()
                            },
                            publicLighting = publicLighting.toDoubleValue(),
                            ruralElectrification = if (includeRuralLaw) ruralElectrification.toDoubleValue() else 0.0,
                            notes = notes.trim()
                        ),
                        originalPeriod = editingOriginalPeriod.ifBlank { null }
                    )
                    editingOriginalPeriod = ""
                }
            }
        }

        item { SectionTitle("Recibos registrados") }

        if (repository.receipts.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.DateRange,
                    title = "Sin recibos guardados",
                    message = "Importa un PDF o registra manualmente el primer recibo mensual."
                )
            }
        }

        items(repository.receipts, key = { it.period }) { receipt ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(receipt.period, fontWeight = FontWeight.SemiBold)
                        Text("Recibo: ${receipt.monthlyBill.money()} - kWh: ${receipt.externalKwh.kwh()}")
                        Text(
                            if (receipt.priceKwhUpTo30 > 0.0 && receipt.priceKwhOver30 > 0.0) {
                                if (receipt.priceKwhUpTo30 == receipt.priceKwhOver30) {
                                    "Tarifa única: ${receipt.priceKwhUpTo30.decimal()}"
                                } else {
                                    "Tarifas: ${receipt.priceKwhUpTo30.decimal()} / ${receipt.priceKwhOver30.decimal()}"
                                }
                            } else {
                                "Tarifas: precio promedio estimado"
                            }
                        )
                    }
                    IconButton(
                        onClick = {
                            editingOriginalPeriod = receipt.period
                            period = receipt.period
                            externalDate = receipt.externalReadingDate
                            supplyNumber = receipt.supplyNumber
                            externalKwh = receipt.externalKwh.input()
                            monthlyBill = receipt.monthlyBill.input()
                            singleKwhPrice = if (receipt.priceKwhUpTo30 == receipt.priceKwhOver30) receipt.priceKwhUpTo30.input() else ""
                            priceUpTo30 = receipt.priceKwhUpTo30.input()
                            priceOver30 = receipt.priceKwhOver30.input()
                            fixedCharge = receipt.fixedCharge.input()
                            maintenance = receipt.maintenance.input()
                            maintenanceOnly = ""
                            replacementOnly = ""
                            splitMaintenanceReplacement = false
                            publicLighting = receipt.publicLighting.input()
                            ruralElectrification = receipt.ruralElectrification.input()
                            tariffMode = when {
                                receipt.priceKwhUpTo30 <= 0.0 && receipt.priceKwhOver30 <= 0.0 -> TARIFF_ESTIMATED
                                receipt.priceKwhUpTo30 == receipt.priceKwhOver30 -> TARIFF_SINGLE
                                else -> TARIFF_TWO_BLOCKS
                            }
                            includeRuralLaw = receipt.ruralElectrification > 0.0
                            notes = receipt.notes
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar recibo")
                    }
                    IconButton(
                        onClick = { pendingDeleteReceipt = receipt }
                    ) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar recibo")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun ReadingsScreen(repository: ElectricRepository, modifier: Modifier = Modifier) {
    var editingId by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(repository.receipts.firstOrNull()?.period ?: currentPeriod()) }
    var userId by rememberSaveable {
        mutableStateOf(
            repository.users.firstOrNull { it.isActiveInPeriod(period) && !it.isResidualInPeriod(period) }?.userId
                ?: repository.users.firstOrNull { it.isActiveInPeriod(period) }?.userId.orEmpty()
        )
    }
    var internalDate by rememberSaveable { mutableStateOf("") }
    var previousReading by rememberSaveable { mutableStateOf("") }
    var currentReading by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var showDatePicker by rememberSaveable { mutableStateOf(false) }
    var pendingDeleteReading by remember { mutableStateOf<MeterReading?>(null) }
    val readingWarnings = listOfNotNull(
        "La lectura actual es menor que la anterior".takeIf {
            previousReading.toNullableDoubleValue() != null &&
                currentReading.toNullableDoubleValue() != null &&
                currentReading.toDoubleValue() < previousReading.toDoubleValue()
        },
        "Selecciona una fecha de lectura interna".takeIf { internalDate.isBlank() }
    )
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = internalDate.toDateMillisOrNull() ?: LocalDate.now().toDateMillis()
    )

    fun applyPreviousReadingSuggestion(nextUserId: String = userId, nextDate: String = internalDate) {
        if (repository.users.firstOrNull { it.userId == nextUserId }?.isResidualInPeriod(period) == true) return
        suggestedPreviousReading(
            readings = repository.readings,
            userId = nextUserId,
            internalDate = nextDate,
            editingId = editingId
        )?.let { previousReading = it.input() }
    }

    if (showDatePicker) {
        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { selectedMillis ->
                            val selectedDate = selectedMillis.toIsoDate()
                            internalDate = selectedDate
                            applyPreviousReadingSuggestion(nextDate = selectedDate)
                        }
                        showDatePicker = false
                    }
                ) {
                    Text("Aceptar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text("Cancelar")
                }
            }
        ) {
            DatePicker(state = datePickerState)
        }
    }

    pendingDeleteReading?.let { reading ->
        ConfirmActionDialog(
            title = "Eliminar lectura",
            message = "Se eliminara la lectura de ${reading.userId} del periodo ${reading.period}.",
            onDismiss = { pendingDeleteReading = null },
            onConfirm = {
                repository.deleteReading(reading)
                pendingDeleteReading = null
            }
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard(title = "Lectura interna") {
                FormSectionLabel("Usuario y fecha")
                TextInput("Periodo (YYYY-MM)", period) { period = it }
                TextInput("ID usuario", userId) {
                    val nextUserId = it.uppercase(Locale.US)
                    userId = nextUserId
                    if (previousReading.isBlank()) applyPreviousReadingSuggestion(nextUserId = nextUserId)
                }
                if (repository.users.isNotEmpty()) {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        repository.users.filter { it.isActiveInPeriod(period) && !it.isResidualInPeriod(period) }.take(3).forEach { user ->
                            AssistChip(
                                onClick = {
                                    userId = user.userId
                                    applyPreviousReadingSuggestion(nextUserId = user.userId)
                                },
                                label = { Text("${user.userId} ${user.name}") }
                            )
                        }
                    }
                }
                FormSectionLabel("Lectura")
                TextInput("Fecha lectura interna", internalDate) {
                    internalDate = it
                    if (previousReading.isBlank()) applyPreviousReadingSuggestion(nextDate = it)
                }
                Button(
                    onClick = {
                        datePickerState.selectedDateMillis =
                            internalDate.toDateMillisOrNull() ?: LocalDate.now().toDateMillis()
                        showDatePicker = true
                    },
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Icon(Icons.Default.DateRange, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Seleccionar fecha en calendario")
                }
                DecimalInput("Lectura anterior", previousReading) { previousReading = it }
                DecimalInput("Lectura actual", currentReading) { currentReading = it }
                TextInput("Observaciones", notes, singleLine = false) { notes = it }
                if (readingWarnings.isNotEmpty()) {
                    ValidationWarningsCard(readingWarnings)
                }
                SaveButton(
                    enabled = period.isNotBlank() && userId.isNotBlank() && readingWarnings.none { it.contains("menor") },
                    text = "Guardar lectura"
                ) {
                    repository.saveReading(
                        MeterReading(
                            id = editingId,
                            period = period.trim(),
                            userId = userId.trim(),
                            isResidual = false,
                            internalReadingDate = internalDate.trim(),
                            previousReading = previousReading.toNullableDoubleValue(),
                            currentReading = currentReading.toNullableDoubleValue(),
                            notes = notes.trim()
                        )
                    )
                    editingId = ""
                    previousReading = ""
                    currentReading = ""
                    notes = ""
                }
            }
        }

        item { SectionTitle("Lecturas registradas") }

        if (repository.readings.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.Edit,
                    title = "Sin lecturas internas",
                    message = "Registra una lectura por usuario para activar el resumen del periodo."
                )
            }
        }

        items(repository.readings, key = { it.id }) { reading ->
            val user = repository.users.firstOrNull { it.userId == reading.userId }
            val isResidualUserInReadingPeriod = user?.isResidualInPeriod(reading.period) == true
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("${reading.period} - ${reading.userId}", fontWeight = FontWeight.SemiBold)
                        Text(user?.name.orEmpty().ifBlank { "Usuario no encontrado" })
                        Text(if (isResidualUserInReadingPeriod) "Usuario residual" else "Lectura directa")
                        Text("Anterior: ${reading.previousReading?.input().orEmpty()}  Actual: ${reading.currentReading?.input().orEmpty()}")
                    }
                    IconButton(
                        onClick = {
                            editingId = reading.id
                            period = reading.period
                            userId = reading.userId
                            internalDate = reading.internalReadingDate
                            previousReading = reading.previousReading?.input().orEmpty()
                            currentReading = reading.currentReading?.input().orEmpty()
                            notes = reading.notes
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar lectura")
                    }
                    IconButton(onClick = { pendingDeleteReading = reading }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar lectura")
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun ParticipantChips(
    users: List<ElectricUser>,
    selectedIds: List<String>,
    onToggle: (ElectricUser, Boolean) -> Unit
) {
    FlowRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        users.forEach { user ->
            val selected = user.userId in selectedIds
            FilterChip(
                selected = selected,
                onClick = { onToggle(user, !selected) },
                label = { Text(user.name.ifBlank { user.userId }) },
                leadingIcon = {
                    Icon(
                        imageVector = if (selected) Icons.Default.CheckCircle else Icons.Default.Person,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp)
                    )
                },
                shape = RoundedCornerShape(18.dp)
            )
        }
    }
}

@Composable
private fun ServiceAvatar(serviceName: String) {
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = serviceIcon(serviceName),
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary
        )
    }
}

private fun serviceIcon(serviceName: String): ImageVector {
    val normalized = serviceName.lowercase(Locale.US)
    return when {
        "agua" in normalized || "sedapal" in normalized -> Icons.Default.WaterDrop
        "internet" in normalized -> Icons.Default.Wifi
        "netflix" in normalized || "hbo" in normalized || "disney" in normalized || "streaming" in normalized -> Icons.Default.Movie
        "otro" in normalized -> Icons.Default.Public
        else -> Icons.Default.AttachMoney
    }
}

@Composable
private fun ServicesScreen(repository: ElectricRepository, modifier: Modifier = Modifier) {
    var editingId by rememberSaveable { mutableStateOf("") }
    var period by rememberSaveable { mutableStateOf(repository.receipts.firstOrNull()?.period ?: currentPeriod()) }
    var selectedService by rememberSaveable { mutableStateOf(serviceOptions.first()) }
    var customServiceName by rememberSaveable { mutableStateOf("") }
    var amount by rememberSaveable { mutableStateOf("") }
    var isActive by rememberSaveable { mutableStateOf(true) }
    var splitCost by rememberSaveable { mutableStateOf(true) }
    var participantCount by rememberSaveable {
        mutableStateOf(repository.users.count { it.isActiveInPeriod(period) }.coerceAtLeast(1).toString())
    }
    var participantUserIdsRaw by rememberSaveable { mutableStateOf("") }
    var notes by rememberSaveable { mutableStateOf("") }
    var pendingDeleteService by remember { mutableStateOf<ServiceExpense?>(null) }
    val serviceName = selectedServiceName(selectedService, customServiceName)
    val visibleServices = repository.serviceExpenses.filter { it.amount > 0.0 }
    val selectedParticipantIds = participantUserIdsRaw.toParticipantIds()
    val activeUsersForService = repository.users.filter { it.isActiveInPeriod(period) }

    pendingDeleteService?.let { service ->
        ConfirmActionDialog(
            title = "Eliminar servicio",
            message = "Se eliminara ${service.name} del periodo ${service.period}.",
            onDismiss = { pendingDeleteService = null },
            onConfirm = {
                repository.deleteServiceExpense(service)
                pendingDeleteService = null
            }
        )
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            FormCard(title = "Servicios y otros gastos") {
                FormSectionLabel("Datos del servicio")
                TextInput("Periodo (YYYY-MM)", period) { period = it }
                PeriodDropdown(
                    label = "Servicio",
                    selected = selectedService,
                    options = serviceOptions,
                    onSelected = { selected ->
                        selectedService = selected
                        if (selected != OTHER_SERVICE_OPTION) customServiceName = ""
                    }
                )
                if (selectedService == OTHER_SERVICE_OPTION) {
                    TextInput("Nombre del servicio", customServiceName) { customServiceName = it }
                }
                DecimalInput("Monto total S/", amount) { amount = it }
                FormSectionLabel("Distribucion")
                ToggleRow("Activo", isActive) { isActive = it }
                ToggleRow("Dividir costo", splitCost) { splitCost = it }
                if (splitCost) {
                    DecimalInput("Usuarios que usan/pagan", participantCount) { participantCount = it }
                    if (activeUsersForService.isNotEmpty()) {
                        Text("Participantes especificos", style = MaterialTheme.typography.labelLarge)
                        ParticipantChips(
                            users = activeUsersForService,
                            selectedIds = selectedParticipantIds,
                            onToggle = { user, checked ->
                                participantUserIdsRaw = selectedParticipantIds
                                    .toMutableSet()
                                    .apply {
                                        if (checked) add(user.userId) else remove(user.userId)
                                    }
                                    .sorted()
                                    .joinToString("|")
                                val selectedCount = participantUserIdsRaw.toParticipantIds().size
                                if (selectedCount > 0) participantCount = selectedCount.toString()
                            }
                        )
                    }
                }
                TextInput("Notas", notes, singleLine = false) { notes = it }
                SaveButton(
                    enabled = period.isNotBlank() && serviceName.isNotBlank() && amount.toDoubleValue() > 0.0,
                    text = if (editingId.isBlank()) "Guardar servicio" else "Actualizar servicio"
                ) {
                    repository.saveServiceExpense(
                        ServiceExpense(
                            id = editingId,
                            period = period.trim(),
                            name = serviceName,
                            amount = amount.toDoubleValue(),
                            isActive = isActive,
                            splitCost = splitCost,
                            participantCount = if (splitCost) selectedParticipantIds.size.takeIf { it > 0 } ?: participantCount.toIntValue() else 1,
                            participantUserIds = if (splitCost) selectedParticipantIds else emptyList(),
                            notes = notes.trim()
                        )
                    )
                    editingId = ""
                    selectedService = serviceOptions.first()
                    customServiceName = ""
                    amount = ""
                    isActive = true
                    splitCost = true
                    participantCount = repository.users.count { it.isActiveInPeriod(period) }.coerceAtLeast(1).toString()
                    participantUserIdsRaw = ""
                    notes = ""
                }
            }
        }

        item { SectionTitle("Servicios registrados") }

        if (visibleServices.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.AttachMoney,
                    title = "Sin servicios con monto",
                    message = "Agrega internet, agua, streaming u otro gasto para incluirlo en el resumen."
                )
            }
        }

        items(visibleServices, key = { it.id }) { service ->
            ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ServiceAvatar(service.name)
                    Spacer(modifier = Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text("${service.period} - ${service.name}", fontWeight = FontWeight.SemiBold)
                        Text("Monto: ${service.amount.money()}")
                        Text(
                            if (service.splitCost) {
                                "Divide entre ${service.serviceParticipantCount()} usuario(s): ${safeServiceShare(service).money()} c/u"
                            } else {
                                "No dividido"
                            }
                        )
                        if (service.participantUserIds.isNotEmpty()) {
                            Text("Participantes: ${service.participantUserIds.joinToString(", ")}")
                        }
                        Text(if (service.isActive) "Activo" else "Inactivo")
                        if (service.notes.isNotBlank()) Text(service.notes)
                    }
                    Switch(
                        checked = service.isActive,
                        onCheckedChange = { checked ->
                            repository.saveServiceExpense(service.copy(isActive = checked))
                        }
                    )
                    IconButton(
                        onClick = {
                            editingId = service.id
                            period = service.period
                            val option = matchingServiceOption(service.name)
                            selectedService = option
                            customServiceName = if (option == OTHER_SERVICE_OPTION) service.name else ""
                            amount = service.amount.input()
                            isActive = service.isActive
                            splitCost = service.splitCost
                            participantCount = service.participantCount.toString()
                            participantUserIdsRaw = service.participantUserIds.joinToString("|")
                            notes = service.notes
                        }
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = "Editar servicio")
                    }
                    IconButton(onClick = { pendingDeleteService = service }) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar servicio")
                    }
                }
            }
        }
    }
}

@Composable
private fun SummaryScreen(
    repository: ElectricRepository,
    modifier: Modifier = Modifier,
    onOpenReceipt: () -> Unit = {},
    onOpenReadings: () -> Unit = {},
    onOpenUsers: () -> Unit = {},
    onOpenSync: () -> Unit = {},
    syncConfigured: Boolean = false
) {
    val context = LocalContext.current
    val uiPrefs = remember { context.getSharedPreferences("control_electrico_ui", Context.MODE_PRIVATE) }
    var showQuickStart by rememberSaveable {
        mutableStateOf(!uiPrefs.getBoolean(KEY_QUICK_START_DISMISSED, false))
    }
    var period by rememberSaveable { mutableStateOf(repository.receipts.firstOrNull()?.period ?: currentPeriod()) }
    var selectedUserId by rememberSaveable { mutableStateOf(ALL_USERS_OPTION) }
    var pendingPaymentUserId by rememberSaveable { mutableStateOf<String?>(null) }
    var pendingPdf by remember { mutableStateOf<ByteArray?>(null) }
    val pdfLauncher = rememberLauncherForActivityResult(ActivityResultContracts.CreateDocument("application/pdf")) { uri ->
        val bytes = pendingPdf
        if (uri != null && bytes != null) {
            context.contentResolver.openOutputStream(uri)?.use { output ->
                output.write(bytes)
            }
        }
    }
    val users = repository.users.toList()
    val receipts = repository.receipts.toList()
    val readings = repository.readings.toList()
    val services = repository.serviceExpenses.toList()
    val payments = repository.payments.toList()
    val settings = repository.settings.value
    val periodOptions = remember(receipts, readings) {
        val periodsWithReadings = readings
            .map { it.period }
            .filter { it.isNotBlank() }
            .toSet()

        receipts
            .map { it.period }
            .filter { it.isNotBlank() && it in periodsWithReadings }
            .distinct()
            .sortedDescending()
    }
    val selectedPeriod = when {
        periodOptions.isEmpty() -> ""
        period in periodOptions -> period
        else -> periodOptions.first()
    }
    val receipt = receipts.firstOrNull { it.period == selectedPeriod }
    val summariesByPeriod = remember(periodOptions, users, receipts, readings, services, settings) {
        periodOptions.associateWith { summaryPeriod ->
            ElectricCalculator.calculatePeriod(
                period = summaryPeriod,
                users = users,
                receipt = receipts.firstOrNull { it.period == summaryPeriod },
                readings = readings,
                services = services,
                settings = settings
            )
        }
    }
    val summary = summariesByPeriod[selectedPeriod] ?: ElectricCalculator.calculatePeriod(
        period = selectedPeriod,
        users = users,
        receipt = receipt,
        readings = readings,
        services = services,
        settings = settings
    )
    val paymentBalances = remember(summariesByPeriod, payments) {
        PaymentLedger.calculate(summariesByPeriod, payments)
    }
    val userOptions = remember(summary.results) {
        listOf(UserOption(ALL_USERS_OPTION, "Todos los usuarios")) +
            summary.results.map { result ->
                UserOption(
                    id = result.userId,
                    label = "${result.userId} - ${result.userName.ifBlank { "Sin nombre" }}"
                )
            }
    }
    val selectedUser = if (userOptions.any { it.id == selectedUserId }) {
        selectedUserId
    } else {
        ALL_USERS_OPTION
    }
    val visibleResults = if (selectedUser == ALL_USERS_OPTION) {
        summary.results
    } else {
        summary.results.filter { it.userId == selectedUser }
    }
    val isUserSummary = selectedUser != ALL_USERS_OPTION
    val selectedResult = visibleResults.firstOrNull()
    val selectedPaymentBalance = if (isUserSummary) selectedResult?.let {
        paymentBalances[selectedPeriod to it.userId]
    } else null
    val generalPaymentTotal = remember(selectedPeriod, summary, paymentBalances) {
        PaymentLedger.outstandingTotalForPeriod(
            period = selectedPeriod,
            summary = summary,
            balances = paymentBalances
        )
    }
    val selectedUserLabel = userOptions.firstOrNull { it.id == selectedUser }?.label ?: "Todos los usuarios"
    val activeServices = remember(selectedPeriod, services) {
        services.filter { it.period == selectedPeriod && it.isActive && it.amount > 0.0 }
    }
    val previousPeriod = periodOptions
        .sortedDescending()
        .dropWhile { it != selectedPeriod }
        .drop(1)
        .firstOrNull()
    val previousSummary = previousPeriod?.let { summariesByPeriod[it] }
    val validationWarnings = remember(selectedPeriod, receipt, users, readings, services, summary) {
        buildValidationWarnings(
            period = selectedPeriod,
            users = users,
            receipt = receipt,
            readings = readings,
            services = services,
            summary = summary
        )
    }
    val selectedServiceTotal = if (isUserSummary) {
        activeServices.sumOf { serviceDisplayAmount(it, selectedUser) }
    } else {
        summary.serviceExpensesTotal
    }
    val chartPoints = remember(selectedUser, periodOptions, users, receipts, readings, services, settings) {
        buildConsumptionPoints(
            periods = periodOptions,
            selectedUserId = selectedUser,
            users = users,
            receipts = receipts,
            readings = readings,
            services = services,
            settings = settings
        )
    }

    pendingPaymentUserId?.let { userId ->
        val result = summary.results.firstOrNull { it.userId == userId }
        val balance = paymentBalances[selectedPeriod to userId]
        if (result != null && balance != null) {
            PaymentRegistrationDialog(
                result = result,
                balance = balance,
                existingPayment = payments.firstOrNull {
                    it.period == selectedPeriod && it.userId == userId
                },
                onDismiss = { pendingPaymentUserId = null },
                onSave = { payment ->
                    repository.savePayment(payment)
                    pendingPaymentUserId = null
                },
                onClear = {
                    repository.deletePayment(selectedPeriod, userId)
                    pendingPaymentUserId = null
                }
            )
        }
    }

    LazyColumn(
        modifier = modifier,
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        if (showQuickStart) {
            item {
                QuickStartCard(
                    hasReceipt = receipts.isNotEmpty(),
                    hasReading = readings.isNotEmpty(),
                    hasUsers = users.isNotEmpty(),
                    syncConfigured = syncConfigured,
                    onOpenReceipt = onOpenReceipt,
                    onOpenReadings = onOpenReadings,
                    onOpenUsers = onOpenUsers,
                    onOpenSync = onOpenSync,
                    onDismiss = {
                        showQuickStart = false
                        uiPrefs.edit().putBoolean(KEY_QUICK_START_DISMISSED, true).apply()
                    }
                )
            }
        }

        item {
            SummaryDashboardCard(
                periodOptions = periodOptions,
                selectedPeriod = selectedPeriod,
                onPeriodSelected = { period = it },
                userOptions = userOptions,
                selectedUser = selectedUser,
                onUserSelected = { selectedUserId = it },
                summary = summary,
                selectedResult = selectedResult,
                selectedPaymentBalance = selectedPaymentBalance,
                generalPaymentTotal = generalPaymentTotal,
                activeServices = activeServices,
                selectedServiceTotal = selectedServiceTotal,
                isUserSummary = isUserSummary,
                canDownloadPdf = visibleResults.isNotEmpty(),
                onShareImage = {
                    val result = selectedResult
                    if (result != null) {
                        runCatching {
                            shareUserSummaryImage(
                                context = context,
                                period = selectedPeriod,
                                result = result,
                                balance = selectedPaymentBalance,
                                activeServices = activeServices
                            )
                        }.onFailure { error ->
                            Toast.makeText(
                                context,
                                "No se pudo compartir la imagen: ${error.message}",
                                Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                },
                onDownloadPdf = {
                    pendingPdf = buildSummaryPdfBytes(
                        period = selectedPeriod,
                        userLabel = selectedUserLabel,
                        summary = summary,
                        results = visibleResults,
                        activeServices = activeServices,
                        chartPoints = chartPoints,
                        selectedUserId = selectedUser,
                        paymentBalances = paymentBalances
                    )
                    pdfLauncher.launch("resumen_${selectedPeriod}_${selectedUserLabel.fileNameSafe()}.pdf")
                }
            )
        }

        if (validationWarnings.isNotEmpty()) {
            item {
                ValidationWarningsCard(validationWarnings)
            }
        }

        item {
            HistoryInsightsCard(
                summary = summary,
                previousSummary = previousSummary,
                activeServices = activeServices
            )
        }

        item {
            FormCard(title = "Consumo electrico") {
                ConsumptionChart(chartPoints)
            }
        }

        item { SectionTitle("Pagos por usuario") }

        if (summary.results.isEmpty()) {
            item {
                EmptyStateCard(
                    icon = Icons.Default.List,
                    title = "Resumen pendiente",
                    message = "Guarda un recibo y al menos una lectura para calcular los pagos."
                )
            }
        }

        items(visibleResults, key = { it.userId }) { result ->
            PaymentCard(
                result = result,
                activeServices = activeServices,
                userSummary = isUserSummary,
                paymentBalance = paymentBalances[selectedPeriod to result.userId],
                onManagePayment = { pendingPaymentUserId = result.userId }
            )
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun QuickStartCard(
    hasReceipt: Boolean,
    hasReading: Boolean,
    hasUsers: Boolean,
    syncConfigured: Boolean,
    onOpenReceipt: () -> Unit,
    onOpenReadings: () -> Unit,
    onOpenUsers: () -> Unit,
    onOpenSync: () -> Unit,
    onDismiss: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Spacer(modifier = Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text("Inicio rápido", fontWeight = FontWeight.Bold, fontSize = 18.sp)
                    Text(
                        "Completa estos pasos para obtener tu primer resumen.",
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            QuickStartStepRow(
                icon = Icons.Default.Person,
                title = "Usuarios",
                message = "Revisa usuarios activos y define si uno es residual.",
                completed = hasUsers
            )
            QuickStartStepRow(
                icon = Icons.Default.Speed,
                title = "Recibo",
                message = "Carga el PDF o registra el recibo del mes.",
                completed = hasReceipt
            )
            QuickStartStepRow(
                icon = Icons.Default.Edit,
                title = "Lecturas",
                message = "Ingresa las lecturas internas del periodo.",
                completed = hasReading
            )

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(onClick = onOpenReceipt, shape = RoundedCornerShape(22.dp)) {
                    Icon(Icons.Default.Speed, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Recibo")
                }
                OutlinedButton(onClick = onOpenReadings, shape = RoundedCornerShape(22.dp)) {
                    Icon(Icons.Default.Edit, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Lecturas")
                }
                OutlinedButton(onClick = onOpenUsers, shape = RoundedCornerShape(22.dp)) {
                    Icon(Icons.Default.Person, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text("Usuarios")
                }
                OutlinedButton(onClick = onOpenSync, shape = RoundedCornerShape(22.dp)) {
                    Icon(if (syncConfigured) Icons.Default.CloudUpload else Icons.Default.CloudOff, contentDescription = null)
                    Spacer(Modifier.width(6.dp))
                    Text(if (syncConfigured) "Cuenta" else "Sincronizar")
                }
            }

            TextButton(onClick = onDismiss, modifier = Modifier.align(Alignment.End)) {
                Text("No volver a mostrar")
            }
        }
    }
}

@Composable
private fun QuickStartStepRow(
    icon: ImageVector,
    title: String,
    message: String,
    completed: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.Top,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            imageVector = if (completed) Icons.Default.CheckCircle else icon,
            contentDescription = null,
            tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(22.dp)
        )
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
        }
        Text(
            text = if (completed) "Listo" else "Pendiente",
            color = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun ValidationWarningsCard(warnings: List<String>) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.ErrorOutline, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Alertas de revision", fontWeight = FontWeight.SemiBold)
            }
            warnings.forEach { warning ->
                Text("- $warning", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun HistoryInsightsCard(
    summary: PeriodSummary,
    previousSummary: PeriodSummary?,
    activeServices: List<ServiceExpense>
) {
    FormCard(title = "Historial y comparacion") {
        val topConsumer = summary.results.maxByOrNull { it.consumptionKwh }
        if (previousSummary == null || previousSummary.results.isEmpty()) {
            Text(
                text = "Aun no hay un periodo anterior comparable.",
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        } else {
            val consumptionDelta = summary.results.sumOf { it.consumptionKwh } -
                previousSummary.results.sumOf { it.consumptionKwh }
            val paymentDelta = summary.totalAssignedWithServices - previousSummary.totalAssignedWithServices
            MetricRow("Consumo vs periodo anterior", consumptionDelta.kwhSigned())
            MetricRow("Pago vs periodo anterior", paymentDelta.moneySigned())
        }
        MetricRow("Mayor consumo", topConsumer?.let { "${it.userName.ifBlank { it.userId }} - ${it.consumptionKwh.kwh()}" } ?: "Sin datos")
        MetricRow("Servicios activos", activeServices.size.toString())
        MetricRow("Total servicios", activeServices.sumOf { it.amount }.money())
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SummaryDashboardCard(
    periodOptions: List<String>,
    selectedPeriod: String,
    onPeriodSelected: (String) -> Unit,
    userOptions: List<UserOption>,
    selectedUser: String,
    onUserSelected: (String) -> Unit,
    summary: PeriodSummary,
    selectedResult: PaymentResult?,
    selectedPaymentBalance: PaymentBalance?,
    generalPaymentTotal: Double,
    activeServices: List<ServiceExpense>,
    selectedServiceTotal: Double,
    isUserSummary: Boolean,
    canDownloadPdf: Boolean,
    onShareImage: () -> Unit,
    onDownloadPdf: () -> Unit
) {
    val dashboardTotal = if (isUserSummary && selectedResult != null) {
        selectedPaymentBalance?.outstandingAmount()
            ?: (selectedResult.finalTotal + selectedServiceTotal)
    } else {
        generalPaymentTotal
    }
    val dashboardLabel = if (isUserSummary) "Total a pagar" else "Total general del periodo"
    val userServices = if (selectedResult != null) {
        activeServices
            .map { service -> service to serviceShareForUser(service, selectedResult.userId) }
            .filter { (_, amount) -> amount > 0.0 }
    } else {
        emptyList()
    }

    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(28.dp),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Text(dashboardLabel, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                text = dashboardTotal.money(),
                style = MaterialTheme.typography.titleLarge,
                fontSize = 34.sp,
                fontWeight = FontWeight.Bold
            )

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DashboardMetricChip(
                    icon = Icons.Default.Speed,
                    label = if (isUserSummary) "Consumo" else "Usuarios",
                    value = if (isUserSummary) {
                        selectedResult?.consumptionKwh?.kwh() ?: "0.00 kWh"
                    } else {
                        summary.participants.toString()
                    }
                )
                DashboardMetricChip(
                    icon = Icons.Default.AttachMoney,
                    label = if (isUserSummary) "Electricidad" else "Recibo asignado",
                    value = if (isUserSummary) {
                        selectedResult?.finalTotal?.money() ?: "S/ 0.00"
                    } else {
                        summary.totalAssigned.money()
                    }
                )
                DashboardMetricChip(
                    icon = Icons.Default.List,
                    label = "Servicios",
                    value = if (isUserSummary) selectedServiceTotal.money() else summary.serviceExpensesTotal.money()
                )
            }

            if (periodOptions.isEmpty()) {
                EmptyStateCard(
                    icon = Icons.Default.DateRange,
                    title = "Sin periodo disponible",
                    message = "Registra un recibo y al menos una lectura para habilitar el resumen."
                )
            } else {
                PeriodDropdown(
                    label = "Periodo",
                    selected = selectedPeriod,
                    options = periodOptions,
                    onSelected = onPeriodSelected
                )
            }

            if (userOptions.size > 1) {
                UserDropdown(
                    label = "Usuario",
                    selected = selectedUser,
                    options = userOptions,
                    onSelected = onUserSelected
                )
            }

            if (isUserSummary && selectedResult != null) {
                MetricRow("Consumo en kWh", selectedResult.consumptionKwh.kwh())
                MetricRow("Total a pagar por consumo eléctrico", selectedResult.finalTotal.money())
                if (userServices.isNotEmpty()) {
                    Divider()
                    Text("Servicios incluidos", fontWeight = FontWeight.SemiBold)
                    userServices.forEach { (service, amount) ->
                        ServiceAmountRow(service, amount)
                    }
                }
                selectedPaymentBalance?.let { balance ->
                    if (balance.previousBalance > 0.0) {
                        Divider()
                        Text("Deudas de pagos anteriores", fontWeight = FontWeight.SemiBold)
                        balance.previousDebtItems.forEach { item ->
                            DebtPeriodRow(item)
                        }
                        MetricRow("Total de deuda anterior", balance.previousBalance.money())
                    }
                    MetricRow("Estado de pago", balance.status.displayName())
                    if (balance.status != null) {
                        MetricRow("Monto pagado", balance.amountPaid.money())
                    }
                }
                Divider()
                MetricRow("Total a pagar", dashboardTotal.money(), important = true)
            } else {
                MetricRow("Estado residual", summary.residualStatus)
                if (summary.thresholdKwhPerUser > 0.0) {
                    MetricRow("Umbral individual", summary.thresholdKwhPerUser.kwh())
                }
                MetricRow("Cargos fijos por usuario", summary.fixedChargesPerUser.money())
                MetricRow("Electrificación Rural (Ley N° 28749) por usuario", summary.ruralElectrificationPerUser.money())
                if (activeServices.isNotEmpty()) {
                    Divider()
                    Text("Servicios adicionales", fontWeight = FontWeight.SemiBold)
                    activeServices.forEach { service ->
                        ServiceAmountRow(service, service.amount)
                    }
                }
                Divider()
                MetricRow("Diferencia recibo - total", summary.receiptDifference.money())
            }

            if (isUserSummary && selectedResult != null) {
                OutlinedButton(
                    onClick = onShareImage,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(22.dp)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Compartir resumen como imagen")
                }
            }

            Button(
                enabled = canDownloadPdf,
                onClick = onDownloadPdf,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Icon(Icons.Default.Save, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Descargar PDF")
            }
        }
    }
}

@Composable
private fun DashboardMetricChip(
    icon: ImageVector,
    label: String,
    value: String
) {
    Row(
        modifier = Modifier
            .width(156.dp)
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.72f))
            .padding(10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(20.dp)
        )
        Column {
            Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp)
            Text(value, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun ServiceAmountRow(service: ServiceExpense, amount: Double) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        ServiceAvatar(service.name)
        Column(modifier = Modifier.weight(1f)) {
            Text(service.summaryServiceName(), fontWeight = FontWeight.Medium)
            Text(
                text = if (service.splitCost) "Servicio dividido" else "Servicio completo",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Text(amount.money(), fontWeight = FontWeight.SemiBold)
    }
}

@Composable
private fun DebtPeriodRow(item: DebtItem) {
    val paid = (item.originalAmount - item.remainingAmount).coerceAtLeast(0.0)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(MaterialTheme.colorScheme.tertiaryContainer.copy(alpha = 0.42f))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text("Periodo ${item.period}", fontWeight = FontWeight.Medium)
            Text(
                if (paid > 0.005) {
                    "Original ${item.originalAmount.money()} - abonado ${paid.money()}"
                } else {
                    "Original ${item.originalAmount.money()} - sin abonos"
                },
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp
            )
        }
        Text(item.remainingAmount.money(), fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun EmptyStateCard(
    icon: ImageVector,
    title: String,
    message: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.58f))
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(message, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
private fun PaymentCard(
    result: PaymentResult,
    activeServices: List<ServiceExpense>,
    userSummary: Boolean,
    paymentBalance: PaymentBalance?,
    onManagePayment: () -> Unit
) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = "${result.userId} - ${result.userName.ifBlank { "Sin nombre" }}",
                fontWeight = FontWeight.SemiBold
            )
            if (userSummary) {
                val userServices = activeServices
                    .map { service -> service to serviceShareForUser(service, result.userId) }
                    .filter { (_, amount) -> amount > 0.0 }
                MetricRow("Consumo en kWh", result.consumptionKwh.kwh())
                MetricRow("Total a pagar por consumo eléctrico", result.finalTotal.money())
                userServices.forEach { (service, amount) ->
                    MetricRow(service.summaryServiceName(), amount.money())
                }
                Divider()
                MetricRow(
                    "Total del periodo",
                    paymentBalance?.currentPeriodAmount?.money()
                        ?: (result.finalTotal + userServices.sumOf { (_, amount) -> amount }).money(),
                    important = true
                )
            } else {
                Text(if (result.isResidual) "Pago residual" else "Pago por lectura interna")
                MetricRow("Consumo", result.consumptionKwh.kwh())
                MetricRow("Importe consumo", result.energyAmount.money())
                MetricRow("Subtotal", result.subtotal.money())
                MetricRow("IGV 18%", result.igv.money())
                MetricRow("Total final", result.finalTotal.money(), important = true)
                if (result.serviceShare > 0.0) {
                    MetricRow("Otros gastos", result.serviceShare.money())
                    MetricRow("Total con servicios", result.finalTotalWithServices.money(), important = true)
                }
            }
            paymentBalance?.let { balance ->
                Divider()
                if (balance.previousBalance > 0.0) {
                    Text("Deudas de pagos anteriores", fontWeight = FontWeight.SemiBold)
                    balance.previousDebtItems.forEach { item ->
                        DebtPeriodRow(item)
                    }
                    MetricRow("Total de deuda anterior", balance.previousBalance.money())
                }
                MetricRow("Estado de pago", balance.status.displayName())
                if (balance.status != null) {
                    MetricRow("Monto pagado", balance.amountPaid.money())
                }
                MetricRow("Saldo por pagar", balance.outstandingAmount().money(), important = true)
            }
            if (!userSummary && result.notes.isNotBlank()) {
                Text(result.notes, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Button(
                onClick = onManagePayment,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp)
            ) {
                Icon(Icons.Default.AttachMoney, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (paymentBalance?.status == null) "Registrar pago" else "Editar pago")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PaymentRegistrationDialog(
    result: PaymentResult,
    balance: PaymentBalance,
    existingPayment: UserPayment?,
    onDismiss: () -> Unit,
    onSave: (UserPayment) -> Unit,
    onClear: () -> Unit
) {
    var selectedStatus by remember(existingPayment) {
        mutableStateOf(existingPayment?.status)
    }
    var partialAmount by remember(existingPayment) {
        mutableStateOf(existingPayment?.amountPaid?.input().orEmpty())
    }
    var paymentDate by remember(existingPayment) {
        mutableStateOf(existingPayment?.paymentDate.orEmpty().ifBlank { LocalDate.now().toString() })
    }
    var notes by remember(existingPayment) {
        mutableStateOf(existingPayment?.notes.orEmpty())
    }
    val partialValue = partialAmount.toDoubleOrNull() ?: 0.0
    val canSave = selectedStatus != null &&
        (selectedStatus != PaymentStatus.PARTIAL ||
            (partialValue > 0.0 && partialValue < balance.totalDue))

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Pago de ${result.userName.ifBlank { result.userId }}")
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                MetricRow("Periodo", balance.period)
                MetricRow("Consumo y servicios", balance.currentPeriodAmount.money())
                if (balance.previousBalance > 0.0) {
                    MetricRow("Saldo anterior", balance.previousBalance.money())
                }
                MetricRow("Total comprometido", balance.totalDue.money(), important = true)

                Text("Estado", fontWeight = FontWeight.SemiBold)
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    listOf(
                        PaymentStatus.PAID,
                        PaymentStatus.PARTIAL,
                        PaymentStatus.UNPAID
                    ).forEach { status ->
                        FilterChip(
                            selected = selectedStatus == status,
                            onClick = { selectedStatus = status },
                            label = { Text(status.displayName()) }
                        )
                    }
                }

                if (selectedStatus == PaymentStatus.PARTIAL) {
                    OutlinedTextField(
                        value = partialAmount,
                        onValueChange = { partialAmount = it },
                        label = { Text("Monto pagado parcialmente") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                        supportingText = {
                            Text("Debe ser mayor a S/ 0.00 y menor a ${balance.totalDue.money()}")
                        }
                    )
                }
                TextInput("Fecha de pago", paymentDate) { paymentDate = it }
                TextInput("Notas", notes) { notes = it }
            }
        },
        confirmButton = {
            TextButton(
                enabled = canSave,
                onClick = {
                    val status = selectedStatus ?: return@TextButton
                    onSave(
                        UserPayment(
                            id = existingPayment?.id ?: "${balance.period}|${result.userId}",
                            period = balance.period,
                            userId = result.userId,
                            status = status,
                            amountPaid = when (status) {
                                PaymentStatus.PAID -> balance.totalDue
                                PaymentStatus.PARTIAL -> partialValue
                                PaymentStatus.UNPAID -> 0.0
                            },
                            paymentDate = paymentDate.trim(),
                            notes = notes.trim()
                        )
                    )
                }
            ) {
                Text("Guardar")
            }
        },
        dismissButton = {
            Row {
                if (existingPayment != null) {
                    TextButton(onClick = onClear) {
                        Text("Borrar estado")
                    }
                }
                TextButton(onClick = onDismiss) {
                    Text("Cancelar")
                }
            }
        },
        shape = RoundedCornerShape(28.dp)
    )
}

@Composable
private fun ConsumptionChart(points: List<ConsumptionPoint>) {
    if (points.isEmpty()) {
        Text(
            text = "Aun no hay consumos suficientes para graficar.",
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        return
    }

    val barColor = MaterialTheme.colorScheme.primary
    val axisColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.7f)
    val maxKwh = (points.maxOfOrNull { it.kwh } ?: 0.0).coerceAtLeast(1.0)

    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(180.dp)
            .padding(top = 6.dp)
    ) {
        val horizontalPadding = 8.dp.toPx()
        val gap = 7.dp.toPx()
        val chartTop = 8.dp.toPx()
        val chartBottom = size.height - 16.dp.toPx()
        val chartHeight = chartBottom - chartTop
        val availableWidth = size.width - (horizontalPadding * 2)
        val totalGaps = gap * (points.size - 1).coerceAtLeast(0)
        val barWidth = ((availableWidth - totalGaps) / points.size.coerceAtLeast(1)).coerceAtLeast(6.dp.toPx())

        drawLine(
            color = axisColor,
            start = Offset(horizontalPadding, chartBottom),
            end = Offset(size.width - horizontalPadding, chartBottom),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = axisColor,
            start = Offset(horizontalPadding, chartTop),
            end = Offset(horizontalPadding, chartBottom),
            strokeWidth = 1.dp.toPx()
        )

        points.forEachIndexed { index, point ->
            val ratio = (point.kwh / maxKwh).coerceIn(0.0, 1.0).toFloat()
            val barHeight = chartHeight * ratio
            val left = horizontalPadding + index * (barWidth + gap)
            drawRect(
                color = barColor,
                topLeft = Offset(left, chartBottom - barHeight),
                size = Size(barWidth, barHeight)
            )
        }
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(points.first().period, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(points.last().period, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
    MetricRow("Mayor consumo", maxKwh.kwh())
    MetricRow("Ultimo consumo", points.last().kwh.kwh())
}

@Composable
private fun FormCard(title: String, content: @Composable ColumnScope.() -> Unit) {
    ElevatedCard(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.elevatedCardColors(containerColor = MaterialTheme.colorScheme.surface),
        elevation = CardDefaults.elevatedCardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp),
            content = {
                Text(title, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
                content()
            }
        )
    }
}

@Composable
private fun SectionTitle(text: String) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun FormSectionLabel(text: String) {
    Text(
        text = text.uppercase(Locale.US),
        color = MaterialTheme.colorScheme.primary,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold
    )
}

@Composable
private fun PeriodDropdown(
    label: String,
    selected: String,
    options: List<String>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(selected)
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option) },
                        onClick = {
                            onSelected(option)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun UserDropdown(
    label: String,
    selected: String,
    options: List<UserOption>,
    onSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val selectedLabel = options.firstOrNull { it.id == selected }?.label.orEmpty()
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Text(label, style = MaterialTheme.typography.labelLarge)
        Box(modifier = Modifier.fillMaxWidth()) {
            Button(
                onClick = { expanded = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(22.dp)
            ) {
                Text(selectedLabel.ifBlank { "Seleccionar usuario" })
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                shape = RoundedCornerShape(22.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                shadowElevation = 10.dp
            ) {
                options.forEach { option ->
                    DropdownMenuItem(
                        text = { Text(option.label) },
                        onClick = {
                            onSelected(option.id)
                            expanded = false
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun TextInput(
    label: String,
    value: String,
    singleLine: Boolean = true,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = singleLine,
        minLines = if (singleLine) 1 else 2,
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun DecimalInput(
    label: String,
    value: String,
    onValueChange: (String) -> Unit
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
        shape = RoundedCornerShape(18.dp),
        modifier = Modifier.fillMaxWidth()
    )
}

@Composable
private fun ToggleRow(label: String, value: Boolean, onValueChange: (Boolean) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Switch(checked = value, onCheckedChange = onValueChange)
    }
}

@Composable
private fun SaveButton(enabled: Boolean, text: String, onClick: () -> Unit) {
    Button(
        enabled = enabled,
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp)
    ) {
        Icon(Icons.Default.Save, contentDescription = null)
        Spacer(modifier = Modifier.width(8.dp))
        Text(text)
    }
}

@Composable
private fun MetricRow(label: String, value: String, important: Boolean = false) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        Text(
            text = value,
            fontWeight = if (important) FontWeight.Bold else FontWeight.Medium
        )
    }
}

private fun nextUserId(nextNumber: Int): String {
    return "U" + nextNumber.toString().padStart(2, '0')
}

private fun currentPeriod(): String = YearMonth.now().toString()

private fun appVersionName(context: Context): String {
    return runCatching {
        context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty()
    }.getOrDefault("desconocida")
}

private suspend fun checkForGithubUpdate(context: Context, repositoryUrl: String): AppUpdateInfo? {
    val repo = githubRepoPath(repositoryUrl)
        ?: error("Ingresa una URL valida de GitHub, por ejemplo https://github.com/usuario/repositorio")
    val currentVersion = appVersionName(context)
    val release = withContext(Dispatchers.IO) {
        val connection = (URL("https://api.github.com/repos/$repo/releases/latest").openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 20000
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "Control-Electrico-Android")
        }
        try {
            val response = if (connection.responseCode in 200..299) {
                connection.inputStream.bufferedReader().use { it.readText() }
            } else {
                val errorBody = connection.errorStream?.bufferedReader()?.use { it.readText() }.orEmpty()
                error("GitHub respondio ${connection.responseCode}: ${githubErrorMessage(errorBody)}")
            }
            JSONObject(response)
        } finally {
            connection.disconnect()
        }
    }

    val tag = release.optString("tag_name")
    val latestVersion = tag.trim().removePrefix("v").removePrefix("V")
    if (compareVersions(latestVersion, currentVersion) <= 0) return null
    val assets = release.optJSONArray("assets").jsonObjects()
    val apkAsset = assets
        .filter { it.optString("name").endsWith(".apk", ignoreCase = true) }
        .sortedWith(
            compareByDescending<JSONObject> { it.optString("name").contains("android", ignoreCase = true) }
                .thenByDescending { it.optString("name").contains(latestVersion, ignoreCase = true) }
        )
        .firstOrNull()
        ?: error("El ultimo Release no tiene un archivo APK adjunto.")

    return AppUpdateInfo(
        tagName = tag,
        versionName = latestVersion.ifBlank { tag },
        releaseName = release.optString("name"),
        body = release.optString("body"),
        apkAssetName = apkAsset.optString("name"),
        apkDownloadUrl = apkAsset.optString("browser_download_url"),
        apkSizeBytes = apkAsset.optLong("size", 0L),
        publishedAt = release.optString("published_at")
    )
}

private suspend fun downloadAndInstallUpdate(
    context: Context,
    update: AppUpdateInfo,
    onProgress: suspend (Int) -> Unit
) {
    val apkFile = withContext(Dispatchers.IO) {
        val updatesDir = File(context.cacheDir, "github_updates").apply { mkdirs() }
        updatesDir.listFiles()?.forEach { file ->
            if (file.extension.equals("apk", ignoreCase = true)) file.delete()
        }
        val safeApkName = update.apkAssetName
            .removeSuffix(".apk")
            .fileNameSafe()
            .ifBlank { "control_electrico_${update.versionName.fileNameSafe()}" } + ".apk"
        val target = File(updatesDir, safeApkName)
        val connection = (URL(update.apkDownloadUrl).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15000
            readTimeout = 60000
            setRequestProperty("User-Agent", "Control-Electrico-Android")
        }
        try {
            if (connection.responseCode !in 200..299) {
                error("Descarga rechazada por GitHub (${connection.responseCode}).")
            }
            val total = connection.contentLengthLong.coerceAtLeast(update.apkSizeBytes)
            var downloaded = 0L
            connection.inputStream.use { input ->
                target.outputStream().use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read
                        if (total > 0) {
                            withContext(Dispatchers.Main) {
                                onProgress(((downloaded * 100) / total).toInt().coerceIn(0, 100))
                            }
                        }
                    }
                }
            }
            target
        } finally {
            connection.disconnect()
        }
    }

    rememberPendingUpdateApk(context, apkFile, update.versionName)
    val apkUri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        apkFile
    )
    val intent = Intent(Intent.ACTION_VIEW).apply {
        setDataAndType(apkUri, "application/vnd.android.package-archive")
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    try {
        context.startActivity(intent)
    } catch (error: ActivityNotFoundException) {
        throw IllegalStateException("No se encontro instalador de paquetes en este dispositivo.", error)
    }
}

private fun githubRepoPath(rawUrl: String): String? {
    val cleaned = rawUrl.trim()
        .removeSuffix("/")
        .removeSuffix(".git")
        .removeSuffix("/releases")
        .removeSuffix("/releases/latest")
    val match = Regex("""github\.com[:/]+([^/\s]+)/([^/\s]+)""", RegexOption.IGNORE_CASE)
        .find(cleaned)
        ?: return null
    return "${match.groupValues[1]}/${match.groupValues[2].removeSuffix(".git")}"
}

private fun normalizeGithubRepositoryUrl(rawUrl: String): String {
    if (rawUrl.isBlank()) return ""
    return githubRepoPath(rawUrl)?.let { "https://github.com/$it" } ?: rawUrl.trim()
}

private fun compareVersions(left: String, right: String): Int {
    val leftParts = versionParts(left)
    val rightParts = versionParts(right)
    val maxSize = maxOf(leftParts.size, rightParts.size, 1)
    repeat(maxSize) { index ->
        val comparison = (leftParts.getOrElse(index) { 0 }).compareTo(rightParts.getOrElse(index) { 0 })
        if (comparison != 0) return comparison
    }
    return 0
}

private fun versionParts(version: String): List<Int> {
    return Regex("""\d+""")
        .findAll(version)
        .map { it.value.toIntOrNull() ?: 0 }
        .toList()
}

private fun githubErrorMessage(raw: String): String {
    return runCatching { JSONObject(raw).optString("message") }
        .getOrDefault("")
        .ifBlank { "no se pudo leer el ultimo Release" }
}

private fun rememberPendingUpdateApk(context: Context, apkFile: File, targetVersion: String) {
    context.getSharedPreferences("control_electrico_updates", Context.MODE_PRIVATE)
        .edit()
        .putString("pending_apk_path", apkFile.absolutePath)
        .putString("pending_target_version", targetVersion)
        .apply()
}

private fun shouldCheckGithubUpdate(context: Context): Boolean {
    val prefs = context.getSharedPreferences("control_electrico_updates", Context.MODE_PRIVATE)
    val lastChecked = prefs.getLong("last_github_check_millis", 0L)
    val oneDayMillis = 24L * 60L * 60L * 1000L
    return System.currentTimeMillis() - lastChecked > oneDayMillis
}

private fun markGithubUpdateChecked(context: Context) {
    context.getSharedPreferences("control_electrico_updates", Context.MODE_PRIVATE)
        .edit()
        .putLong("last_github_check_millis", System.currentTimeMillis())
        .apply()
}

private fun cleanupInstalledUpdateIfNeeded(context: Context) {
    val prefs = context.getSharedPreferences("control_electrico_updates", Context.MODE_PRIVATE)
    val path = prefs.getString("pending_apk_path", "").orEmpty()
    val targetVersion = prefs.getString("pending_target_version", "").orEmpty()
    val updatesDir = File(context.cacheDir, "github_updates")
    if (path.isBlank() || targetVersion.isBlank()) {
        updatesDir.listFiles()?.forEach { file ->
            if (file.extension.equals("apk", ignoreCase = true) && file.lastModified() < System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L) {
                file.delete()
            }
        }
        return
    }

    if (compareVersions(appVersionName(context), targetVersion) >= 0) {
        File(path).delete()
        updatesDir.listFiles()?.forEach { file ->
            if (file.extension.equals("apk", ignoreCase = true)) file.delete()
        }
        prefs.edit().clear().apply()
    }
}

private fun suggestedPreviousReading(
    readings: List<MeterReading>,
    userId: String,
    internalDate: String,
    editingId: String
): Double? {
    val selectedDate = internalDate.toLocalDateOrNull()
    val candidates = readings
        .filter { it.id != editingId }
        .filter { it.userId.equals(userId, ignoreCase = true) }
        .filter { !it.isResidual }
        .filter { it.currentReading != null }

    val datedCandidate = candidates
        .mapNotNull { reading ->
            val date = reading.internalReadingDate.toLocalDateOrNull() ?: return@mapNotNull null
            if (selectedDate != null && !date.isBefore(selectedDate)) return@mapNotNull null
            reading to date
        }
        .maxByOrNull { it.second }
        ?.first

    return datedCandidate?.currentReading
        ?: candidates.maxByOrNull { it.period }?.currentReading
}

private fun String.toLocalDateOrNull(): LocalDate? {
    return runCatching { LocalDate.parse(this.trim()) }.getOrNull()
}

private fun String.toDateMillisOrNull(): Long? {
    return toLocalDateOrNull()?.toDateMillis()
}

private fun LocalDate.toDateMillis(): Long {
    return atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
}

private fun Long.toIsoDate(): String {
    return Instant.ofEpochMilli(this).atZone(ZoneOffset.UTC).toLocalDate().toString()
}

private fun String.toDoubleValue(): Double {
    return replace(",", ".").toDoubleOrNull() ?: 0.0
}

private fun String.toNullableDoubleValue(): Double? {
    return replace(",", ".").toDoubleOrNull()
}

private fun String.toIntValue(): Int {
    return replace(",", ".").toDoubleOrNull()?.toInt()?.coerceAtLeast(1) ?: 1
}

private fun safeServiceShare(service: ServiceExpense): Double {
    return if (service.splitCost) {
        service.amount / service.serviceParticipantCount()
    } else {
        service.amount
    }
}

private fun serviceShareForUser(service: ServiceExpense, userId: String): Double {
    val participantIds = service.participantUserIds.filter { it.isNotBlank() }
    if (participantIds.isEmpty()) return safeServiceShare(service)
    if (userId !in participantIds) return 0.0
    return if (service.splitCost) {
        service.amount / participantIds.size.coerceAtLeast(1)
    } else {
        service.amount
    }
}

private fun ServiceExpense.serviceParticipantCount(): Int {
    return participantUserIds.size.takeIf { it > 0 } ?: participantCount.coerceAtLeast(1)
}

private fun String.toParticipantIds(): List<String> {
    return split("|")
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun selectedServiceName(selectedService: String, customServiceName: String): String {
    return if (selectedService == OTHER_SERVICE_OPTION) {
        customServiceName.trim()
    } else {
        selectedService.trim()
    }
}

private fun matchingServiceOption(serviceName: String): String {
    return serviceOptions.firstOrNull { it.equals(serviceName, ignoreCase = true) } ?: OTHER_SERVICE_OPTION
}

private fun serviceDisplayAmount(service: ServiceExpense, selectedUserId: String): Double {
    return if (selectedUserId == ALL_USERS_OPTION) service.amount else serviceShareForUser(service, selectedUserId)
}

private fun ServiceExpense.summaryServiceName(): String {
    val normalized = name.lowercase(Locale.US)
    return when {
        "agua" in normalized || "sedapal" in normalized -> "Agua y alcantarillado"
        "netflix" in normalized -> "Netflix"
        "internet" in normalized -> "Internet"
        "hbo" in normalized -> "HBO Max"
        "disney" in normalized -> "Disney"
        "streaming" in normalized -> name.ifBlank { "Streaming" }
        "otro" in normalized -> "Otros"
        else -> name.ifBlank { "Otros" }
    }
}

private fun buildConsumptionPoints(
    periods: List<String>,
    selectedUserId: String,
    users: List<ElectricUser>,
    receipts: List<MonthlyReceipt>,
    readings: List<MeterReading>,
    services: List<ServiceExpense>,
    settings: AppSettings
): List<ConsumptionPoint> {
    return periods
        .sorted()
        .mapNotNull { period ->
            val receipt = receipts.firstOrNull { it.period == period } ?: return@mapNotNull null
            val summary = ElectricCalculator.calculatePeriod(
                period = period,
                users = users,
                receipt = receipt,
                readings = readings,
                services = services,
                settings = settings
            )
            val kwh = if (selectedUserId == ALL_USERS_OPTION) {
                summary.results.sumOf { it.consumptionKwh }
            } else {
                summary.results.firstOrNull { it.userId == selectedUserId }?.consumptionKwh
                    ?: return@mapNotNull null
            }
            ConsumptionPoint(period = period, kwh = kwh)
        }
        .takeLast(12)
}

private fun buildValidationWarnings(
    period: String,
    users: List<ElectricUser>,
    receipt: MonthlyReceipt?,
    readings: List<MeterReading>,
    services: List<ServiceExpense>,
    summary: PeriodSummary
): List<String> {
    val warnings = mutableListOf<String>()
    if (period.isBlank()) return warnings
    if (receipt == null) {
        warnings.add("Falta registrar el recibo del periodo.")
    } else {
        if (receipt.monthlyBill <= 0.0) warnings.add("El recibo del mes esta en cero.")
        if (receipt.externalKwh <= 0.0) warnings.add("El consumo exterior esta en cero.")
        val hasAnyKwhRate = receipt.priceKwhUpTo30 > 0.0 || receipt.priceKwhOver30 > 0.0
        val hasBothKwhRates = receipt.priceKwhUpTo30 > 0.0 && receipt.priceKwhOver30 > 0.0
        if (hasAnyKwhRate && !hasBothKwhRates) {
            warnings.add("Revisa las tarifas kWh: solo una tarifa tiene valor.")
        } else if (!hasAnyKwhRate) {
            warnings.add("Sin tarifas kWh por bloque: se usara precio promedio estimado.")
        }
    }

    val readingsByUser = readings.filter { it.period == period }.associateBy { it.userId }
    users.filter { it.isActiveInPeriod(period) && !it.isResidualInPeriod(period) }.forEach { user ->
        val reading = readingsByUser[user.userId]
        if (reading == null) {
            warnings.add("${user.userId} - ${user.name.ifBlank { "usuario" }} no tiene lectura en este periodo.")
        } else if (reading.previousReading != null && reading.currentReading != null && reading.currentReading < reading.previousReading) {
            warnings.add("${user.userId} tiene lectura actual menor que la anterior.")
        }
    }

    services.filter { it.period == period && it.isActive && it.amount > 0.0 }.forEach { service ->
        if (service.splitCost && service.participantUserIds.isEmpty()) {
            warnings.add("${service.name} se divide solo por cantidad; puedes elegir participantes especificos.")
        }
    }

    if (kotlin.math.abs(summary.receiptDifference) > 1.0 && summary.results.isNotEmpty()) {
        warnings.add("La diferencia entre recibo y total asignado supera S/ 1.00.")
    }
    if (summary.residualStatus.startsWith("Error")) {
        warnings.add(summary.residualStatus)
    }
    return warnings.distinct()
}

private fun shareUserSummaryImage(
    context: Context,
    period: String,
    result: PaymentResult,
    balance: PaymentBalance?,
    activeServices: List<ServiceExpense>
) {
    val userServices = activeServices
        .map { service -> service to serviceShareForUser(service, result.userId) }
        .filter { (_, amount) -> amount > 0.0 }
    val previousDebts = balance?.previousDebtItems.orEmpty()
    val totalToPay = balance?.outstandingAmount()
        ?: (result.finalTotal + userServices.sumOf { (_, amount) -> amount })
    val rowCount = 5 + userServices.size + previousDebts.size +
        if ((balance?.amountPaid ?: 0.0) > 0.0) 1 else 0
    val width = 1080
    val height = maxOf(1240, 720 + rowCount * 82)
    val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
    val canvas = android.graphics.Canvas(bitmap)
    val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    val green = android.graphics.Color.rgb(27, 137, 83)
    val darkGreen = android.graphics.Color.rgb(16, 96, 59)
    val textColor = android.graphics.Color.rgb(30, 39, 34)
    val muted = android.graphics.Color.rgb(103, 116, 109)
    val line = android.graphics.Color.rgb(222, 229, 225)
    val surface = android.graphics.Color.WHITE
    val background = android.graphics.Color.rgb(244, 247, 245)

    fun configureText(
        size: Float,
        color: Int = textColor,
        bold: Boolean = false,
        align: Paint.Align = Paint.Align.LEFT
    ) {
        paint.style = Paint.Style.FILL
        paint.color = color
        paint.textSize = size
        paint.typeface = if (bold) Typeface.DEFAULT_BOLD else Typeface.DEFAULT
        paint.textAlign = align
    }

    fun fittedText(value: String, maxWidth: Float): String {
        if (paint.measureText(value) <= maxWidth) return value
        var shortened = value
        while (shortened.length > 3 && paint.measureText("$shortened...") > maxWidth) {
            shortened = shortened.dropLast(1)
        }
        return "$shortened..."
    }

    fun drawRow(label: String, value: String, y: Float, important: Boolean = false): Float {
        configureText(if (important) 31f else 27f, if (important) darkGreen else textColor, important)
        canvas.drawText(fittedText(label, 620f), 84f, y, paint)
        configureText(if (important) 33f else 28f, if (important) darkGreen else textColor, true, Paint.Align.RIGHT)
        canvas.drawText(fittedText(value, 300f), width - 84f, y, paint)
        if (!important) {
            paint.color = line
            paint.strokeWidth = 2f
            canvas.drawLine(84f, y + 25f, width - 84f, y + 25f, paint)
        }
        return y + if (important) 88f else 72f
    }

    canvas.drawColor(background)
    paint.color = green
    paint.style = Paint.Style.FILL
    canvas.drawRect(0f, 0f, width.toFloat(), 230f, paint)

    configureText(48f, android.graphics.Color.WHITE, true)
    canvas.drawText("Control Eléctrico", 64f, 82f, paint)
    configureText(27f, android.graphics.Color.WHITE)
    canvas.drawText("Resumen individual de pago", 64f, 130f, paint)
    configureText(26f, android.graphics.Color.WHITE, true, Paint.Align.RIGHT)
    canvas.drawText(period, width - 64f, 82f, paint)

    paint.color = surface
    canvas.drawRoundRect(
        RectF(48f, 178f, width - 48f, height - 74f),
        34f,
        34f,
        paint
    )

    configureText(42f, textColor, true)
    canvas.drawText(
        fittedText(result.userName.ifBlank { result.userId }, 870f),
        84f,
        270f,
        paint
    )
    configureText(24f, muted)
    val userLine = listOf(result.userId, result.internalMeter)
        .filter { it.isNotBlank() }
        .joinToString(" · ")
    canvas.drawText(fittedText(userLine, 850f), 84f, 314f, paint)

    var y = 385f
    configureText(25f, green, true)
    canvas.drawText("DETALLE DEL PERIODO", 84f, y, paint)
    y += 60f
    y = drawRow("Consumo en kWh", result.consumptionKwh.kwh(), y)
    y = drawRow("Consumo eléctrico", result.finalTotal.money(), y)

    if (userServices.isNotEmpty()) {
        y += 18f
        configureText(25f, green, true)
        canvas.drawText("SERVICIOS", 84f, y, paint)
        y += 58f
        userServices.forEach { (service, amount) ->
            y = drawRow(service.summaryServiceName(), amount.money(), y)
        }
    }

    if (previousDebts.isNotEmpty()) {
        y += 18f
        configureText(25f, green, true)
        canvas.drawText("DEUDAS ANTERIORES", 84f, y, paint)
        y += 58f
        previousDebts.forEach { debt ->
            y = drawRow("Periodo ${debt.period}", debt.remainingAmount.money(), y)
        }
    }

    balance?.let {
        y += 10f
        y = drawRow("Estado de pago", it.status.displayName(), y)
        if (it.amountPaid > 0.0) {
            y = drawRow("Monto pagado", it.amountPaid.money(), y)
        }
    }

    y += 20f
    paint.color = android.graphics.Color.rgb(225, 244, 233)
    canvas.drawRoundRect(
        RectF(76f, y - 42f, width - 76f, y + 72f),
        24f,
        24f,
        paint
    )
    configureText(27f, darkGreen, true)
    canvas.drawText("TOTAL A PAGAR", 104f, y + 25f, paint)
    configureText(42f, darkGreen, true, Paint.Align.RIGHT)
    canvas.drawText(totalToPay.money(), width - 104f, y + 29f, paint)

    configureText(21f, muted, false, Paint.Align.CENTER)
    canvas.drawText(
        "Generado por Control Eléctrico · ${LocalDate.now()}",
        width / 2f,
        height - 28f,
        paint
    )

    val directory = File(context.cacheDir, "shared_summaries").apply { mkdirs() }
    directory.listFiles()?.filter { it.isFile }?.forEach { it.delete() }
    val file = File(
        directory,
        "resumen_${period}_${result.userId.fileNameSafe()}.png"
    )
    file.outputStream().use { output ->
        check(bitmap.compress(Bitmap.CompressFormat.PNG, 100, output)) {
            "No se pudo crear la imagen."
        }
    }
    bitmap.recycle()

    val uri = FileProvider.getUriForFile(
        context,
        "${context.packageName}.fileprovider",
        file
    )
    val shareIntent = Intent(Intent.ACTION_SEND).apply {
        type = "image/png"
        putExtra(Intent.EXTRA_STREAM, uri)
        putExtra(
            Intent.EXTRA_TEXT,
            "Resumen ${result.userName.ifBlank { result.userId }} · $period · Total ${totalToPay.money()}"
        )
        clipData = ClipData.newUri(context.contentResolver, "Resumen Control Eléctrico", uri)
        addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
    }
    context.startActivity(Intent.createChooser(shareIntent, "Compartir resumen"))
}

private fun buildSummaryPdfBytes(
    period: String,
    userLabel: String,
    summary: PeriodSummary,
    results: List<PaymentResult>,
    activeServices: List<ServiceExpense>,
    chartPoints: List<ConsumptionPoint>,
    selectedUserId: String,
    paymentBalances: Map<Pair<String, String>, PaymentBalance>
): ByteArray {
    val document = PdfDocument()
    val pageWidth = 595f
    val pageHeight = 842f
    val margin = 40f
    val green = android.graphics.Color.rgb(24, 160, 94)
    val ink = android.graphics.Color.rgb(30, 41, 48)
    val muted = android.graphics.Color.rgb(98, 111, 120)
    val soft = android.graphics.Color.rgb(241, 247, 244)
    val pale = android.graphics.Color.rgb(248, 250, 251)
    val titlePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = android.graphics.Color.WHITE
        textSize = 20f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val sectionPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 11f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val bodyPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 10f
    }
    val mutedPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = muted
        textSize = 9f
    }
    val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = ink
        textSize = 10f
        typeface = Typeface.create(Typeface.DEFAULT, Typeface.BOLD)
    }
    val fillPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        style = Paint.Style.FILL
    }
    var pageNumber = 1
    var page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
    var pdfCanvas = page.canvas
    var y = 0f

    fun fitText(text: String, maxWidth: Float, paint: Paint): String {
        if (paint.measureText(text) <= maxWidth) return text
        var shortened = text
        while (shortened.length > 3 && paint.measureText("$shortened...") > maxWidth) {
            shortened = shortened.dropLast(1)
        }
        return "$shortened..."
    }

    fun drawHeader() {
        fillPaint.color = green
        pdfCanvas.drawRect(0f, 0f, pageWidth, 96f, fillPaint)
        pdfCanvas.drawText("Control Eléctrico", margin, 42f, titlePaint)
        val subtitlePaint = Paint(mutedPaint).apply { color = android.graphics.Color.WHITE }
        pdfCanvas.drawText("Resumen de consumo, servicios y pagos", margin, 65f, subtitlePaint)
        val periodPaint = Paint(sectionPaint).apply { color = android.graphics.Color.WHITE }
        val periodText = period.ifBlank { "Sin periodo" }
        pdfCanvas.drawText(
            periodText,
            pageWidth - margin - periodPaint.measureText(periodText),
            46f,
            periodPaint
        )
        y = 118f
    }

    fun drawFooter() {
        val linePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(225, 230, 233)
            strokeWidth = 1f
        }
        pdfCanvas.drawLine(margin, pageHeight - 30f, pageWidth - margin, pageHeight - 30f, linePaint)
        val generated = "Generado ${LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm"))}"
        pdfCanvas.drawText(generated, margin, pageHeight - 14f, mutedPaint)
        val pageText = "Página $pageNumber"
        pdfCanvas.drawText(
            pageText,
            pageWidth - margin - mutedPaint.measureText(pageText),
            pageHeight - 14f,
            mutedPaint
        )
    }

    fun newPage() {
        drawFooter()
        document.finishPage(page)
        pageNumber += 1
        page = document.startPage(PdfDocument.PageInfo.Builder(595, 842, pageNumber).create())
        pdfCanvas = page.canvas
        drawHeader()
    }

    fun ensureSpace(height: Float) {
        if (y + height > pageHeight - 46f) newPage()
    }

    fun drawSection(title: String) {
        ensureSpace(30f)
        y += 6f
        fillPaint.color = soft
        pdfCanvas.drawRoundRect(margin, y, pageWidth - margin, y + 25f, 6f, 6f, fillPaint)
        pdfCanvas.drawText(title, margin + 10f, y + 17f, sectionPaint)
        y += 36f
    }

    fun drawRow(
        label: String,
        value: String,
        important: Boolean = false,
        indent: Float = 0f
    ) {
        ensureSpace(if (important) 24f else 19f)
        val labelPaint = if (important) sectionPaint else bodyPaint
        val rightPaint = if (important) sectionPaint else valuePaint
        pdfCanvas.drawText(
            fitText(label, 330f - indent, labelPaint),
            margin + indent,
            y,
            labelPaint
        )
        pdfCanvas.drawText(
            fitText(value, 145f, rightPaint),
            pageWidth - margin - rightPaint.measureText(fitText(value, 145f, rightPaint)),
            y,
            rightPaint
        )
        y += if (important) 22f else 18f
    }

    fun drawDebtItems(balance: PaymentBalance) {
        if (balance.previousDebtItems.isEmpty()) return
        drawSection("Deudas de periodos anteriores")
        balance.previousDebtItems.forEach { item ->
            val paid = (item.originalAmount - item.remainingAmount).coerceAtLeast(0.0)
            val detail = if (paid > 0.005) {
                "Periodo ${item.period} - abonado ${paid.money()}"
            } else {
                "Periodo ${item.period} - sin abonos"
            }
            drawRow(detail, item.remainingAmount.money(), indent = 6f)
        }
        drawRow("Total de deuda anterior", balance.previousBalance.money(), important = true)
    }

    fun drawPaymentState(balance: PaymentBalance, fallbackTotal: Double) {
        drawSection("Estado del pago")
        drawRow("Estado registrado", balance.status.displayName())
        if (balance.amountPaid > 0.0) drawRow("Pago aplicado", balance.amountPaid.money())
        drawRow(
            "Total por pagar",
            balance.outstandingAmount().money(),
            important = true
        )
        if (balance.totalDue <= 0.0) drawRow("Total del periodo", fallbackTotal.money())
    }

    drawHeader()
    ensureSpace(70f)
    fillPaint.color = pale
    pdfCanvas.drawRoundRect(margin, y, pageWidth - margin, y + 62f, 8f, 8f, fillPaint)
    pdfCanvas.drawText("VISTA DEL RESUMEN", margin + 14f, y + 19f, mutedPaint)
    val subject = if (selectedUserId == ALL_USERS_OPTION) "Todos los usuarios" else userLabel
    val subjectPaint = Paint(sectionPaint).apply { textSize = 13f }
    pdfCanvas.drawText(fitText(subject, 320f, subjectPaint), margin + 14f, y + 43f, subjectPaint)
    val headlineTotal = if (selectedUserId == ALL_USERS_OPTION) {
        results.sumOf { result ->
            paymentBalances[period to result.userId]?.outstandingAmount()
                ?: result.finalTotalWithServices
        }
    } else {
        results.firstOrNull()?.let { result ->
            paymentBalances[period to result.userId]?.outstandingAmount()
                ?: result.finalTotalWithServices
        } ?: 0.0
    }
    val totalPaint = Paint(titlePaint).apply {
        color = green
        textSize = 18f
    }
    val totalText = headlineTotal.money()
    pdfCanvas.drawText(
        totalText,
        pageWidth - margin - 14f - totalPaint.measureText(totalText),
        y + 43f,
        totalPaint
    )
    y += 78f

    if (selectedUserId != ALL_USERS_OPTION) {
        val result = results.firstOrNull()
        if (result != null) {
            val userServiceShares = activeServices
                .map { service -> service to serviceShareForUser(service, result.userId) }
                .filter { (_, amount) -> amount > 0.0 }
            drawSection("Detalle del usuario")
            drawRow("Consumo en kWh", result.consumptionKwh.kwh())
            drawRow("Total a pagar por consumo eléctrico", result.finalTotal.money())
            if (userServiceShares.isNotEmpty()) {
                userServiceShares.forEach { (service, amount) ->
                    drawRow(service.summaryServiceName(), amount.money(), indent = 6f)
                }
            }
            val balance = paymentBalances[period to result.userId]
            if (balance != null) {
                drawRow("Total del periodo", balance.currentPeriodAmount.money(), important = true)
                drawDebtItems(balance)
                drawPaymentState(balance, result.finalTotalWithServices)
            } else {
                drawRow("Total a pagar", result.finalTotalWithServices.money(), important = true)
            }
        } else {
            drawSection("Detalle del usuario")
            drawRow("Estado", "Sin datos para el usuario seleccionado")
        }
    } else {
        drawSection("Datos del periodo")
        drawRow("Estado residual", summary.residualStatus)
        drawRow("Usuarios del periodo", summary.participants.toString())
        if (summary.thresholdKwhPerUser > 0.0) {
            drawRow("Umbral individual", summary.thresholdKwhPerUser.kwh())
        }
        drawRow("Cargos fijos por usuario", summary.fixedChargesPerUser.money())
        drawRow(
            "Electrificación Rural (Ley N° 28749) por usuario",
            summary.ruralElectrificationPerUser.money()
        )
        if (activeServices.isNotEmpty()) {
            drawSection("Servicios adicionales")
            activeServices.forEach { service ->
                val mode = if (service.splitCost) "dividido" else "sin dividir"
                drawRow("${service.name} ($mode)", service.amount.money(), indent = 6f)
            }
        }

        drawSection("Detalle por usuario")
        results.forEach { result ->
            val balance = paymentBalances[period to result.userId]
            ensureSpace(76f + (balance?.previousDebtItems?.size ?: 0) * 18f)
            drawRow(
                "${result.userId} - ${result.userName.ifBlank { "Sin nombre" }}",
                balance?.outstandingAmount()?.money() ?: result.finalTotalWithServices.money(),
                important = true
            )
            drawRow("Consumo", result.consumptionKwh.kwh(), indent = 8f)
            drawRow("Electricidad", result.finalTotal.money(), indent = 8f)
            if (result.serviceShare > 0.0) drawRow("Servicios", result.serviceShare.money(), indent = 8f)
            if (balance != null) {
                balance.previousDebtItems.forEach { item ->
                    drawRow("Deuda ${item.period}", item.remainingAmount.money(), indent = 8f)
                }
                drawRow("Estado", balance.status.displayName(), indent = 8f)
            }
            y += 4f
        }
    }

    drawSection("Gráfico de consumo")
    if (chartPoints.isEmpty()) {
        drawRow("Estado", "Sin datos suficientes para graficar")
    } else {
        val points = chartPoints.takeLast(12)
        ensureSpace(158f)
        val chartLeft = margin + 8f
        val chartTop = y + 4f
        val chartWidth = pageWidth - margin * 2 - 16f
        val chartHeight = 105f
        val chartBottom = chartTop + chartHeight
        val maxKwh = (points.maxOfOrNull { it.kwh } ?: 0.0).coerceAtLeast(1.0)
        val axisPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(120, 130, 138)
            strokeWidth = 1.2f
        }
        val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(30, 125, 82)
        }
        val labelPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            color = android.graphics.Color.rgb(70, 80, 86)
            textSize = 8f
        }
        val gap = 7f
        val barWidth = (
            (chartWidth - gap * (points.size - 1).coerceAtLeast(0)) /
                points.size.coerceAtLeast(1)
            ).coerceIn(8f, 34f)
        val groupWidth = barWidth * points.size + gap * (points.size - 1).coerceAtLeast(0)
        val chartStart = chartLeft + (chartWidth - groupWidth) / 2f

        pdfCanvas.drawLine(chartLeft, chartBottom, chartLeft + chartWidth, chartBottom, axisPaint)
        pdfCanvas.drawLine(chartLeft, chartTop, chartLeft, chartBottom, axisPaint)
        points.forEachIndexed { index, point ->
            val ratio = (point.kwh / maxKwh).coerceIn(0.0, 1.0).toFloat()
            val left = chartStart + index * (barWidth + gap)
            val barHeight = chartHeight * ratio
            pdfCanvas.drawRect(left, chartBottom - barHeight, left + barWidth, chartBottom, barPaint)
            val label = point.period.takeLast(5)
            pdfCanvas.drawText(
                label,
                left + (barWidth - labelPaint.measureText(label)) / 2f,
                chartBottom + 14f,
                labelPaint
            )
        }
        pdfCanvas.drawText("Máximo: ${maxKwh.kwh()}", chartLeft, chartBottom + 31f, mutedPaint)
        y = chartBottom + 40f
    }

    drawFooter()
    document.finishPage(page)
    val output = ByteArrayOutputStream()
    document.writeTo(output)
    document.close()
    return output.toByteArray()
}

private fun buildBackupCsv(repository: ElectricRepository): String {
    return buildString {
        appendCsvRow(listOf("seccion", "campo_1", "campo_2", "campo_3", "campo_4", "campo_5", "campo_6", "campo_7", "campo_8", "campo_9", "campo_10", "campo_11", "campo_12"))
        repository.users.forEach { user ->
            appendCsvRow(
                listOf(
                    "usuario",
                    user.userId,
                    user.name,
                    user.internalMeter,
                    user.isActive,
                    user.isResidual,
                    user.periodStates.joinToString("|") { "${it.period}:${it.isActive}:${it.isResidual}" },
                    user.notes
                )
            )
        }
        repository.receipts.forEach { receipt ->
            appendCsvRow(
                listOf(
                    "recibo",
                    receipt.period,
                    receipt.externalReadingDate,
                    receipt.supplyNumber,
                    receipt.externalKwh,
                    receipt.monthlyBill,
                    receipt.priceKwhUpTo30,
                    receipt.priceKwhOver30,
                    receipt.fixedCharge,
                    receipt.maintenance,
                    receipt.publicLighting,
                    receipt.ruralElectrification,
                    receipt.notes
                )
            )
        }
        repository.readings.forEach { reading ->
            appendCsvRow(
                listOf(
                    "lectura",
                    reading.id,
                    reading.period,
                    reading.userId,
                    reading.isResidual,
                    reading.internalReadingDate,
                    reading.previousReading,
                    reading.currentReading,
                    reading.notes
                )
            )
        }
        repository.serviceExpenses.filter { it.amount > 0.0 }.forEach { service ->
            appendCsvRow(
                listOf(
                    "servicio",
                    service.id,
                    service.period,
                    service.name,
                    service.amount,
                    service.isActive,
                    service.splitCost,
                    service.participantCount,
                    service.participantUserIds.joinToString("|"),
                    service.notes
                )
            )
        }
        repository.payments.forEach { payment ->
            appendCsvRow(
                listOf(
                    "pago",
                    payment.id,
                    payment.period,
                    payment.userId,
                    payment.status.name,
                    payment.amountPaid,
                    payment.paymentDate,
                    payment.notes
                )
            )
        }
    }
}

private fun buildBackupJson(repository: ElectricRepository): String {
    return JSONObject()
        .put("format", "control_electrico_backup")
        .put("version", 3)
        .put("createdAt", LocalDateTime.now().toString())
        .put("users", JSONArray(repository.users.map { it.toBackupJson() }))
        .put("receipts", JSONArray(repository.receipts.map { it.toBackupJson() }))
        .put("readings", JSONArray(repository.readings.map { it.toBackupJson() }))
        .put("services", JSONArray(repository.serviceExpenses.filter { it.amount > 0.0 }.map { it.toBackupJson() }))
        .put("payments", JSONArray(repository.payments.map { it.toBackupJson() }))
        .put("settings", repository.settings.value.toBackupJson())
        .toString(2)
}

private fun buildBackupJsonForExport(repository: ElectricRepository, password: String): String {
    val plainJson = buildBackupJson(repository)
    return if (password.isBlank()) plainJson else encryptBackupJson(plainJson, password)
}

private fun ElectricRepository.snapshotBackupData(): BackupData {
    return BackupData(
        users = users.toList(),
        receipts = receipts.toList(),
        readings = readings.toList(),
        services = serviceExpenses.filter { it.amount > 0.0 },
        payments = payments.toList()
    )
}

private fun AppSettings.toBackupJson(): JSONObject = JSONObject()
    .put("igvRate", igvRate)
    .put("roundUpToTenth", roundUpToTenth)
    .put("supplyAlias", supplyAlias)
    .put("accountHolder", accountHolder)
    .put("monthlyReminderEnabled", monthlyReminderEnabled)
    .put("reminderDay", reminderDay)
    .put("googleSheetId", googleSheetId)
    .put("googleSheetName", googleSheetName)
    .put("googleSheetUpdatedAt", googleSheetUpdatedAt)
    .put("updateRepositoryUrl", updateRepositoryUrl)

private fun buildDiagnosticsText(
    repository: ElectricRepository,
    syncManager: SupabaseSyncManager,
    latestBackup: LocalBackupInfo?,
    versionName: String
): String {
    val syncState = syncManager.state.value
    return buildString {
        appendLine("Control Electrico - Diagnostico")
        appendLine("Version Android: $versionName")
        appendLine("Usuarios: ${repository.users.size}")
        appendLine("Recibos: ${repository.receipts.size}")
        appendLine("Lecturas: ${repository.readings.size}")
        appendLine("Servicios con monto: ${repository.serviceExpenses.count { it.amount > 0.0 }}")
        appendLine("Pagos: ${repository.payments.size}")
        appendLine("Ultimo periodo con recibo: ${repository.receipts.firstOrNull()?.period ?: "Sin recibos"}")
        appendLine("Ultimo respaldo local: ${latestBackup?.let { "${it.name} - ${it.lastModifiedMillis.fileDateTime()}" } ?: "Sin respaldos"}")
        appendLine("Supabase configurado: ${if (syncManager.isConfigured()) "Si" else "No"}")
        appendLine("Cuenta conectada: ${if (syncManager.isSignedIn()) "Si" else "No"}")
        appendLine("Estado sincronizacion: ${syncStatusTitle(syncManager)}")
        appendLine("Mensaje sincronizacion: ${syncState.message}")
        appendLine("Progreso: ${syncState.progress}%")
        appendLine("Revision nube: ${syncManager.revision.value}")
        appendLine("Ultima sincronizacion: ${syncManager.lastSyncedAt.value.ifBlank { "Aun no realizada" }}")
        appendLine("Cambios pendientes: ${if (syncManager.hasPendingChanges()) "Si" else "No"}")
        if (syncState.error.isNotBlank()) appendLine("Error: ${syncState.error}")
    }
}

private fun parseBackupContent(raw: String, password: String = ""): BackupData {
    val trimmed = raw.trimStart()
    if (!trimmed.startsWith("{")) return parseBackupCsv(raw)
    val root = JSONObject(trimmed)
    return if (root.optString("format") == "control_electrico_encrypted_backup") {
        if (password.isBlank()) error("respaldo protegido: escribe la clave antes de importar")
        parseBackupJson(decryptBackupJson(root, password))
    } else {
        parseBackupJson(trimmed)
    }
}

private fun encryptBackupJson(plainJson: String, password: String): String {
    val salt = ByteArray(16)
    val iv = ByteArray(12)
    SecureRandom().nextBytes(salt)
    SecureRandom().nextBytes(iv)
    val secretKey = backupSecretKey(password, salt)
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.ENCRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
    val encrypted = cipher.doFinal(plainJson.toByteArray(Charsets.UTF_8))
    return JSONObject()
        .put("format", "control_electrico_encrypted_backup")
        .put("version", 1)
        .put("createdAt", LocalDateTime.now().toString())
        .put("salt", salt.base64())
        .put("iv", iv.base64())
        .put("payload", encrypted.base64())
        .toString(2)
}

private fun decryptBackupJson(root: JSONObject, password: String): String {
    val salt = root.optString("salt").fromBase64()
    val iv = root.optString("iv").fromBase64()
    val payload = root.optString("payload").fromBase64()
    val cipher = Cipher.getInstance("AES/GCM/NoPadding")
    cipher.init(Cipher.DECRYPT_MODE, backupSecretKey(password, salt), GCMParameterSpec(128, iv))
    return cipher.doFinal(payload).toString(Charsets.UTF_8)
}

private fun backupSecretKey(password: String, salt: ByteArray): SecretKeySpec {
    val spec = PBEKeySpec(password.toCharArray(), salt, 120_000, 256)
    val keyBytes = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        .generateSecret(spec)
        .encoded
    return SecretKeySpec(keyBytes, "AES")
}

private fun ByteArray.base64(): String = Base64.encodeToString(this, Base64.NO_WRAP)

private fun String.fromBase64(): ByteArray = Base64.decode(this, Base64.NO_WRAP)

private fun parseBackupJson(raw: String): BackupData {
    val root = JSONObject(raw)
    val users = root.optJSONArray("users").jsonObjects().map { item ->
        ElectricUser(
            userId = item.optString("userId"),
            name = item.optString("name"),
            internalMeter = item.optString("internalMeter"),
            isActive = item.optBoolean("isActive", true),
            isResidual = item.optBoolean("isResidual", false),
            periodStates = item.optJSONArray("periodStates").jsonObjects().map { state ->
                UserPeriodState(
                    period = state.optString("period"),
                    isActive = state.optBoolean("isActive", true),
                    isResidual = state.optBoolean("isResidual", false)
                )
            },
            notes = item.optString("notes")
        )
    }
    val receipts = root.optJSONArray("receipts").jsonObjects().map { item ->
        MonthlyReceipt(
            period = item.optString("period"),
            externalReadingDate = item.optString("externalReadingDate"),
            supplyNumber = item.optString("supplyNumber"),
            externalKwh = item.optDouble("externalKwh", 0.0),
            monthlyBill = item.optDouble("monthlyBill", 0.0),
            priceKwhUpTo30 = item.optDouble("priceKwhUpTo30", 0.0),
            priceKwhOver30 = item.optDouble("priceKwhOver30", 0.0),
            fixedCharge = item.optDouble("fixedCharge", 0.0),
            maintenance = item.optDouble("maintenance", 0.0),
            publicLighting = item.optDouble("publicLighting", 0.0),
            ruralElectrification = item.optDouble("ruralElectrification", 0.0),
            notes = item.optString("notes")
        )
    }
    val readings = root.optJSONArray("readings").jsonObjects().map { item ->
        MeterReading(
            id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            period = item.optString("period"),
            userId = item.optString("userId"),
            isResidual = item.optBoolean("isResidual", false),
            internalReadingDate = item.optString("internalReadingDate"),
            previousReading = item.optionalDouble("previousReading"),
            currentReading = item.optionalDouble("currentReading"),
            notes = item.optString("notes")
        )
    }
    val services = root.optJSONArray("services").jsonObjects().map { item ->
        ServiceExpense(
            id = item.optString("id").ifBlank { java.util.UUID.randomUUID().toString() },
            period = item.optString("period"),
            name = item.optString("name"),
            amount = item.optDouble("amount", 0.0),
            isActive = item.optBoolean("isActive", true),
            splitCost = item.optBoolean("splitCost", true),
            participantCount = item.optInt("participantCount", 1).coerceAtLeast(1),
            participantUserIds = item.optJSONArray("participantUserIds").jsonStrings(),
            notes = item.optString("notes")
        )
    }
    val payments = root.optJSONArray("payments").jsonObjects().mapNotNull { item ->
        val period = item.optString("period")
        val userId = item.optString("userId")
        if (period.isBlank() || userId.isBlank()) return@mapNotNull null
        UserPayment(
            id = item.optString("id").ifBlank { "$period|$userId" },
            period = period,
            userId = userId,
            status = item.optString("status").toPaymentStatus(),
            amountPaid = item.optDouble("amountPaid", 0.0).coerceAtLeast(0.0),
            paymentDate = item.optString("paymentDate"),
            notes = item.optString("notes")
        )
    }

    if (
        users.isEmpty() &&
        receipts.isEmpty() &&
        readings.isEmpty() &&
        services.isEmpty() &&
        payments.isEmpty()
    ) {
        error("archivo JSON sin datos reconocibles")
    }
    return BackupData(users, receipts, readings, services, payments)
}

private fun ElectricUser.toBackupJson(): JSONObject = JSONObject()
    .put("userId", userId)
    .put("name", name)
    .put("internalMeter", internalMeter)
    .put("isActive", isActive)
    .put("isResidual", isResidual)
    .put("periodStates", JSONArray(periodStates.map { it.toBackupJson() }))
    .put("notes", notes)

private fun UserPeriodState.toBackupJson(): JSONObject = JSONObject()
    .put("period", period)
    .put("isActive", isActive)
    .put("isResidual", isResidual)

private fun MonthlyReceipt.toBackupJson(): JSONObject = JSONObject()
    .put("period", period)
    .put("externalReadingDate", externalReadingDate)
    .put("supplyNumber", supplyNumber)
    .put("externalKwh", externalKwh)
    .put("monthlyBill", monthlyBill)
    .put("priceKwhUpTo30", priceKwhUpTo30)
    .put("priceKwhOver30", priceKwhOver30)
    .put("fixedCharge", fixedCharge)
    .put("maintenance", maintenance)
    .put("publicLighting", publicLighting)
    .put("ruralElectrification", ruralElectrification)
    .put("notes", notes)

private fun MeterReading.toBackupJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("period", period)
    .put("userId", userId)
    .put("isResidual", isResidual)
    .put("internalReadingDate", internalReadingDate)
    .put("previousReading", previousReading ?: JSONObject.NULL)
    .put("currentReading", currentReading ?: JSONObject.NULL)
    .put("notes", notes)

private fun ServiceExpense.toBackupJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("period", period)
    .put("name", name)
    .put("amount", amount)
    .put("isActive", isActive)
    .put("splitCost", splitCost)
    .put("participantCount", participantCount)
    .put("participantUserIds", JSONArray(participantUserIds))
    .put("notes", notes)

private fun UserPayment.toBackupJson(): JSONObject = JSONObject()
    .put("id", id)
    .put("period", period)
    .put("userId", userId)
    .put("status", status.name)
    .put("amountPaid", amountPaid)
    .put("paymentDate", paymentDate)
    .put("notes", notes)

private fun saveAutomaticBackupBeforeImport(context: Context, repository: ElectricRepository): File? {
    return runCatching {
        saveLocalBackup(
            context = context,
            fileName = "auto_antes_importar_${backupTimestamp()}.json",
            content = buildBackupJson(repository)
        )
    }.getOrNull()
}

private fun saveLocalBackup(context: Context, fileName: String, content: String): File {
    val directory = backupDirectory(context)
    val file = File(directory, fileName)
    file.writeText(content, Charsets.UTF_8)
    return file
}

private fun listLocalBackups(context: Context): List<LocalBackupInfo> {
    return backupDirectory(context)
        .listFiles()
        .orEmpty()
        .filter { it.isFile && (it.extension.equals("json", true) || it.extension.equals("csv", true)) }
        .sortedByDescending { it.lastModified() }
        .map {
            LocalBackupInfo(
                name = it.name,
                path = it.absolutePath,
                sizeBytes = it.length(),
                lastModifiedMillis = it.lastModified()
            )
        }
}

private fun backupDirectory(context: Context): File {
    val directory = context.getExternalFilesDir("backups") ?: File(context.filesDir, "backups")
    if (!directory.exists()) directory.mkdirs()
    return directory
}

private fun backupFileStem(): String = "ControlElectrico_${backupTimestamp()}"

private fun backupTimestamp(): String {
    return LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd_HH-mm-ss", Locale.US))
}

private fun JSONArray?.jsonObjects(): List<JSONObject> {
    if (this == null) return emptyList()
    return List(length()) { index -> optJSONObject(index) }
        .filterNotNull()
}

private fun JSONArray?.jsonStrings(): List<String> {
    if (this == null) return emptyList()
    return List(length()) { index -> optString(index) }
        .filter { it.isNotBlank() }
}

private fun JSONObject.optionalDouble(key: String): Double? {
    return if (!has(key) || isNull(key)) null else optDouble(key)
}

private fun BackupData.latestPeriod(): String {
    return (
        receipts.map { it.period } +
            readings.map { it.period } +
            services.map { it.period } +
            payments.map { it.period }
        )
        .filter { it.isNotBlank() }
        .maxOrNull()
        .orEmpty()
}

private fun Long.fileSize(): String {
    return when {
        this >= 1_048_576L -> "${String.format(Locale.US, "%.1f", this / 1_048_576.0)} MB"
        this >= 1024L -> "${String.format(Locale.US, "%.1f", this / 1024.0)} KB"
        else -> "$this B"
    }
}

private fun Long.fileDateTime(): String {
    return LocalDateTime.ofInstant(Instant.ofEpochMilli(this), ZoneId.systemDefault())
        .format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm", Locale.US))
}

private fun scheduleMonthlyReminder(context: Context, dayOfMonth: Int) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val intent = Intent(context, ReminderReceiver::class.java)
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        801,
        intent,
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    val calendar = java.util.Calendar.getInstance().apply {
        set(java.util.Calendar.DAY_OF_MONTH, dayOfMonth.coerceIn(1, 28))
        set(java.util.Calendar.HOUR_OF_DAY, 19)
        set(java.util.Calendar.MINUTE, 0)
        set(java.util.Calendar.SECOND, 0)
        set(java.util.Calendar.MILLISECOND, 0)
        if (timeInMillis <= System.currentTimeMillis()) {
            add(java.util.Calendar.MONTH, 1)
        }
    }
    alarmManager.setInexactRepeating(
        AlarmManager.RTC_WAKEUP,
        calendar.timeInMillis,
        AlarmManager.INTERVAL_DAY * 30,
        pendingIntent
    )
}

private fun cancelMonthlyReminder(context: Context) {
    val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    val pendingIntent = PendingIntent.getBroadcast(
        context,
        801,
        Intent(context, ReminderReceiver::class.java),
        PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
    )
    alarmManager.cancel(pendingIntent)
}

private fun parseBackupCsv(raw: String): BackupData {
    val users = mutableListOf<ElectricUser>()
    val receipts = mutableListOf<MonthlyReceipt>()
    val readings = mutableListOf<MeterReading>()
    val services = mutableListOf<ServiceExpense>()
    val payments = mutableListOf<UserPayment>()

    parseCsvRows(raw)
        .filter { row -> row.any { it.isNotBlank() } }
        .forEach { row ->
            when (row.valueAt(0).lowercase(Locale.US)) {
                "usuario" -> {
                    val userId = row.valueAt(1).trim()
                    if (userId.isNotBlank()) {
                        users.add(
                            ElectricUser(
                                userId = userId,
                                name = row.valueAt(2).trim(),
                                internalMeter = row.valueAt(3).trim(),
                                isActive = row.valueAt(4).toBooleanValue(true),
                                isResidual = row.valueAt(5).toBooleanValue(false),
                                periodStates = parseUserPeriodStates(row.valueAt(6)),
                                notes = row.valueAt(7).trim()
                            )
                        )
                    }
                }

                "recibo" -> {
                    val period = row.valueAt(1).trim()
                    if (period.isNotBlank()) {
                        receipts.add(
                            MonthlyReceipt(
                                period = period,
                                externalReadingDate = row.valueAt(2).trim(),
                                supplyNumber = row.valueAt(3).trim(),
                                externalKwh = row.valueAt(4).toDoubleValue(),
                                monthlyBill = row.valueAt(5).toDoubleValue(),
                                priceKwhUpTo30 = row.valueAt(6).toDoubleValue(),
                                priceKwhOver30 = row.valueAt(7).toDoubleValue(),
                                fixedCharge = row.valueAt(8).toDoubleValue(),
                                maintenance = row.valueAt(9).toDoubleValue(),
                                publicLighting = row.valueAt(10).toDoubleValue(),
                                ruralElectrification = row.valueAt(11).toDoubleValue(),
                                notes = row.valueAt(12).trim()
                            )
                        )
                    }
                }

                "lectura" -> {
                    val period = row.valueAt(2).trim()
                    val userId = row.valueAt(3).trim()
                    if (period.isNotBlank() && userId.isNotBlank()) {
                        readings.add(
                            MeterReading(
                                id = row.valueAt(1).ifBlank { java.util.UUID.randomUUID().toString() },
                                period = period,
                                userId = userId,
                                isResidual = row.valueAt(4).toBooleanValue(false),
                                internalReadingDate = row.valueAt(5).trim(),
                                previousReading = row.valueAt(6).toNullableDoubleValue(),
                                currentReading = row.valueAt(7).toNullableDoubleValue(),
                                notes = row.valueAt(8).trim()
                            )
                        )
                    }
                }

                "servicio" -> {
                    val period = row.valueAt(2).trim()
                    val name = row.valueAt(3).trim()
                    val amount = row.valueAt(4).toDoubleValue()
                    if (period.isNotBlank() && name.isNotBlank() && amount > 0.0) {
                        val hasParticipantColumn = row.size > 9
                        services.add(
                            ServiceExpense(
                                id = row.valueAt(1).ifBlank { java.util.UUID.randomUUID().toString() },
                                period = period,
                                name = name,
                                amount = amount,
                                isActive = row.valueAt(5).toBooleanValue(true),
                                splitCost = row.valueAt(6).toBooleanValue(true),
                                participantCount = row.valueAt(7).toIntValue(),
                                participantUserIds = if (hasParticipantColumn) row.valueAt(8).toParticipantIds() else emptyList(),
                                notes = row.valueAt(if (hasParticipantColumn) 9 else 8).trim()
                            )
                        )
                    }
                }

                "pago" -> {
                    val period = row.valueAt(2).trim()
                    val userId = row.valueAt(3).trim()
                    if (period.isNotBlank() && userId.isNotBlank()) {
                        payments.add(
                            UserPayment(
                                id = row.valueAt(1).ifBlank { "$period|$userId" },
                                period = period,
                                userId = userId,
                                status = row.valueAt(4).toPaymentStatus(),
                                amountPaid = row.valueAt(5).toDoubleValue().coerceAtLeast(0.0),
                                paymentDate = row.valueAt(6).trim(),
                                notes = row.valueAt(7).trim()
                            )
                        )
                    }
                }
            }
        }

    if (
        users.isEmpty() &&
        receipts.isEmpty() &&
        readings.isEmpty() &&
        services.isEmpty() &&
        payments.isEmpty()
    ) {
        error("archivo sin datos reconocibles")
    }
    return BackupData(users, receipts, readings, services, payments)
}

private fun parseCsvRows(raw: String): List<List<String>> {
    val rows = mutableListOf<List<String>>()
    val currentRow = mutableListOf<String>()
    val currentCell = StringBuilder()
    var inQuotes = false
    var index = 0

    fun finishCell() {
        currentRow.add(currentCell.toString())
        currentCell.clear()
    }

    fun finishRow() {
        finishCell()
        rows.add(currentRow.toList())
        currentRow.clear()
    }

    while (index < raw.length) {
        val char = raw[index]
        when {
            char == '"' && inQuotes && index + 1 < raw.length && raw[index + 1] == '"' -> {
                currentCell.append('"')
                index++
            }

            char == '"' -> inQuotes = !inQuotes
            char == ',' && !inQuotes -> finishCell()
            char == '\n' && !inQuotes -> finishRow()
            char == '\r' -> Unit
            else -> currentCell.append(char)
        }
        index++
    }

    if (currentCell.isNotEmpty() || currentRow.isNotEmpty()) {
        finishRow()
    }
    return rows
}

private fun parseUserPeriodStates(raw: String): List<UserPeriodState> {
    return raw.split("|")
        .filter { it.isNotBlank() }
        .mapNotNull { token ->
            val parts = token.split(":")
            if (parts.size < 3) return@mapNotNull null
            UserPeriodState(
                period = parts[0],
                isActive = parts[1].toBooleanValue(true),
                isResidual = parts[2].toBooleanValue(false)
            )
        }
}

private fun List<String>.valueAt(index: Int): String {
    return getOrNull(index).orEmpty()
}

private fun String.toBooleanValue(default: Boolean): Boolean {
    return when (trim().lowercase(Locale.US)) {
        "true", "1", "si", "sí", "yes" -> true
        "false", "0", "no" -> false
        else -> default
    }
}

private fun String.toPaymentStatus(): PaymentStatus {
    return when (trim().uppercase(Locale.US)) {
        "PAID", "PAGADO", "PAGADO TOTAL" -> PaymentStatus.PAID
        "PARTIAL", "PARCIAL", "PAGO PARCIAL" -> PaymentStatus.PARTIAL
        else -> PaymentStatus.UNPAID
    }
}

private fun StringBuilder.appendCsvRow(values: List<Any?>) {
    appendLine(values.joinToString(",") { csvCell(it?.toString().orEmpty()) })
}

private fun csvCell(value: String): String {
    return "\"${value.replace("\"", "\"\"")}\""
}

private fun String.fileNameSafe(): String {
    return lowercase(Locale.US)
        .replace(Regex("[^a-z0-9_-]+"), "_")
        .trim('_')
        .ifBlank { "resumen" }
}

private fun Double.money(): String = "S/ ${String.format(Locale.US, "%.2f", this)}"

private fun PaymentBalance.outstandingAmount(): Double {
    return remainingBalance
}

private fun PaymentStatus?.displayName(): String {
    return when (this) {
        PaymentStatus.PAID -> "Pagado total"
        PaymentStatus.PARTIAL -> "Pago parcial"
        PaymentStatus.UNPAID -> "No pagado"
        null -> "Sin registrar"
    }
}

private fun Double.moneySigned(): String {
    val prefix = if (this >= 0.0) "+" else "-"
    return "$prefix S/ ${String.format(Locale.US, "%.2f", kotlin.math.abs(this))}"
}

private fun Double.kwh(): String = "${String.format(Locale.US, "%.2f", this)} kWh"

private fun Double.kwhSigned(): String {
    val prefix = if (this >= 0.0) "+" else "-"
    return "$prefix ${String.format(Locale.US, "%.2f", kotlin.math.abs(this))} kWh"
}

private fun Double.decimal(): String = String.format(Locale.US, "%.4f", this)

private fun Double.input(): String {
    return if (this % 1.0 == 0.0) {
        String.format(Locale.US, "%.0f", this)
    } else {
        String.format(Locale.US, "%.4f", this).trimEnd('0').trimEnd('.')
    }
}

private fun ReceiptPdfData.importMessage(): String {
    val fields = listOf(
        period,
        externalReadingDate,
        supplyNumber,
        externalKwh,
        monthlyBill,
        priceKwhUpTo30,
        priceKwhOver30,
        fixedCharge,
        maintenance,
        maintenanceOnly,
        replacementOnly,
        publicLighting,
        ruralElectrification
    )
    val detected = fields.count { it != null }
    return "PDF leido: $detected de ${fields.size} campos detectados. Revisa los datos antes de guardar."
}

private fun ReceiptPdfData.reviewFields(): List<Pair<String, String?>> {
    return listOf(
        "Periodo" to period,
        "Fecha lectura exterior" to externalReadingDate,
        "Nro suministro" to supplyNumber,
        "kWh exterior" to externalKwh?.kwh(),
        "Recibo del mes" to monthlyBill?.money(),
        if (priceKwhRateCount <= 1) {
            "Precio por kWh" to priceKwhUpTo30?.decimal()
        } else {
            "Precio kWh hasta 30" to priceKwhUpTo30?.decimal()
        },
        if (priceKwhRateCount <= 1) {
            "Modo tarifa" to "Precio único por kWh".takeIf { priceKwhUpTo30 != null }
        } else {
            "Precio kWh mayor a 30" to priceKwhOver30?.decimal()
        },
        "Cargo fijo" to fixedCharge?.money(),
        "Mantenimiento y reposicion" to maintenance?.money(),
        "Mantenimiento" to maintenanceOnly?.money(),
        "Reposicion de conexion" to replacementOnly?.money(),
        "Alumbrado publico" to publicLighting?.money(),
        "Electrificación Rural (Ley N° 28749)" to ruralElectrification?.money()
    )
}

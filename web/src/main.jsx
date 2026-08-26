import React, { useMemo, useRef, useState } from "react";
import { createRoot } from "react-dom/client";
import {
  Activity,
  AlertTriangle,
  ArrowLeft,
  ArrowDownToLine,
  BookOpen,
  CheckCircle2,
  ChevronDown,
  CircleDollarSign,
  Cloud,
  CloudOff,
  Database,
  Download,
  Droplets,
  Edit3,
  FileDown,
  FileText,
  Gauge,
  HardDrive,
  History,
  Info,
  KeyRound,
  ListChecks,
  LockKeyhole,
  Menu,
  Moon,
  MoreHorizontal,
  Plus,
  RefreshCw,
  Save,
  Settings,
  ShieldCheck,
  Share2,
  Sun,
  Trash2,
  Upload,
  UserRound,
  Users,
  Wifi,
  X
} from "lucide-react";
import {
  Bar,
  BarChart,
  CartesianGrid,
  ResponsiveContainer,
  Tooltip,
  XAxis,
  YAxis
} from "recharts";
import { jsPDF } from "jspdf";
import {
  ALL_USERS,
  APP_NAME,
  CREATOR_WEBSITE,
  availablePeriods,
  buildSmartAlerts,
  buildPaymentLedger,
  calculatePeriod,
  isActiveInPeriod,
  isResidualInPeriod,
  kwh,
  money,
  paymentStatusLabel,
  serviceShareForUser,
  totalOutstandingForPeriod,
  validateReceipt
} from "./domain";
import { useElectricData } from "./useElectricData";
import { useSupabaseSync } from "./useSupabaseSync";
import { SUPABASE_SETUP_SQL } from "./supabaseSetupSql";
import { parseReceiptPdf } from "./pdfParser";
import { downloadCsv, downloadEncryptedJson, downloadJson, importBackup } from "./backup";
import {
  extractGoogleSheetId,
  exportToGoogleSheets,
  googleSheetUrl,
  googleSheetsEnvironmentStatus,
  importFromGoogleSheets
} from "./googleSheetsBackup";
import "./styles.css";

const APP_VERSION = "1.0.3";

const NAV_ITEMS = [
  { id: "summary", label: "Resumen", icon: Activity },
  { id: "receipts", label: "Recibos", icon: FileText },
  { id: "readings", label: "Lecturas", icon: Gauge },
  { id: "services", label: "Servicios", icon: CircleDollarSign }
];

const SERVICE_OPTIONS = [
  "Netflix",
  "HBO Max",
  "Disney",
  "Otros streaming",
  "Servicio de Internet",
  "Agua y alcantarillado (Sedapal)",
  "Otro servicio"
];

function App() {
  const store = useElectricData();
  const { data } = store;
  const sync = useSupabaseSync(data, store.replaceData);
  const [tab, setTab] = useState("summary");
  const [modal, setModal] = useState(null);
  const [menuOpen, setMenuOpen] = useState(false);
  const [dark, setDark] = useState(() => localStorage.getItem("control-theme") === "dark");
  const [unlocked, setUnlocked] = useState(() => !data.settings.appLockPinHash);
  const [showOnboarding, setShowOnboarding] = useState(
    () => data.settings.onboardingComplete !== true && data.receipts.length === 0 && data.readings.length === 0
  );
  const [toast, setToast] = useState("");
  const currentTitle = tab === "summary" ? APP_NAME : NAV_ITEMS.find((item) => item.id === tab)?.label || APP_NAME;
  const urlParams = new URLSearchParams(window.location.search);
  const shareMode = urlParams.get("view") === "share";

  const notify = (message) => {
    setToast(message);
    window.setTimeout(() => setToast(""), 3200);
  };

  const toggleTheme = () => {
    const next = !dark;
    setDark(next);
    localStorage.setItem("control-theme", next ? "dark" : "light");
  };
  const openMenuModal = (type) => {
    setMenuOpen(false);
    setModal({ type });
  };

  if (data.settings.appLockPinHash && !unlocked) {
    return (
      <div className={dark ? "app dark lock-app" : "app lock-app"}>
        <LockScreen expectedHash={data.settings.appLockPinHash} onUnlock={() => setUnlocked(true)} />
      </div>
    );
  }

  if (shareMode) {
    return <ShareSummaryPage data={data} dark={dark} />;
  }

  return (
    <div className={dark ? "app dark" : "app"}>
      <aside className="sidebar">
        <div className="brand">
          <img src="/app-icon.png" alt="" />
          <div>
            <strong>{APP_NAME}</strong>
            <span>Web</span>
          </div>
        </div>
        <nav>
          {NAV_ITEMS.map(({ id, label, icon: Icon }) => (
            <button
              key={id}
              className={tab === id ? "nav-item active" : "nav-item"}
              onClick={() => setTab(id)}
            >
              <Icon size={20} />
              <span>{label}</span>
            </button>
          ))}
        </nav>
        <div className="sidebar-bottom">
          <button className={`sync-data-badge ${sync.state.phase}`} onClick={() => setModal({ type: "account" })}>
            {sync.state.phase === "syncing" ? (
              <RefreshCw className="spin" size={16} />
            ) : sync.signedIn ? (
              <Cloud size={16} />
            ) : (
              <CloudOff size={16} />
            )}
            <span>
              <strong>{syncStatusLabel(sync)}</strong>
              <small>{sync.state.phase === "syncing" ? `${sync.state.progress}%` : "Datos guardados localmente"}</small>
            </span>
          </button>
        </div>
      </aside>

      <section className="workspace">
        <header className="topbar">
          <div>
            <h1>{currentTitle}</h1>
            <p>{subtitleFor(tab)}</p>
          </div>
          <div className="top-actions">
            <button className="icon-button" title={dark ? "Modo diurno" : "Modo nocturno"} onClick={toggleTheme}>
              {dark ? <Sun size={19} /> : <Moon size={19} />}
            </button>
            <div className="menu-wrap">
              <button className="icon-button" title="Más opciones" onClick={() => setMenuOpen(!menuOpen)}>
                <MoreHorizontal size={21} />
              </button>
              {menuOpen && (
                <div className="context-menu">
                  <MenuButton icon={Users} label="Administrar usuarios" onClick={() => openMenuModal("users")} />
                  <MenuButton icon={HardDrive} label="Centro de respaldo" onClick={() => openMenuModal("backup")} />
                  <MenuButton icon={Cloud} label="Cuenta y sincronización" onClick={() => openMenuModal("account")} />
                  <MenuButton icon={History} label="Historial de cambios" onClick={() => openMenuModal("audit")} />
                  <MenuButton icon={LockKeyhole} label="Seguridad" onClick={() => openMenuModal("security")} />
                  <MenuButton icon={Settings} label="Configuración" onClick={() => openMenuModal("settings")} />
                  <MenuButton icon={Info} label="Acerca de" onClick={() => openMenuModal("about")} />
                </div>
              )}
            </div>
          </div>
        </header>

        <main>
          {tab === "summary" && <SummaryPage store={store} setModal={setModal} notify={notify} />}
          {tab === "receipts" && <ReceiptsPage store={store} setModal={setModal} notify={notify} />}
          {tab === "readings" && <ReadingsPage store={store} setModal={setModal} />}
          {tab === "services" && <ServicesPage store={store} setModal={setModal} />}
        </main>
      </section>

      {modal?.type === "receipt" && (
        <ReceiptModal
          receipt={modal.item}
          initial={modal.initial}
          onClose={() => setModal(null)}
          onSave={(receipt) => {
            store.upsert("receipts", receipt);
            setModal(null);
            notify("Recibo guardado");
          }}
        />
      )}
      {modal?.type === "reading" && (
        <ReadingModal
          reading={modal.item}
          data={data}
          onClose={() => setModal(null)}
          onSave={(reading) => {
            store.upsert("readings", reading);
            setModal(null);
            notify("Lectura guardada");
          }}
        />
      )}
      {modal?.type === "service" && (
        <ServiceModal
          service={modal.item}
          data={data}
          onClose={() => setModal(null)}
          onSave={(service) => {
            store.upsert("services", service);
            setModal(null);
            notify("Servicio guardado");
          }}
        />
      )}
      {modal?.type === "payment" && (
        <PaymentModal
          result={modal.result}
          balance={modal.balance}
          existing={modal.existing}
          onClose={() => setModal(null)}
          onDelete={() => {
            store.remove("payments", `${modal.balance.period}|${modal.result.userId}`);
            setModal(null);
          }}
          onSave={(payment) => {
            store.upsert("payments", payment);
            setModal(null);
            notify("Pago actualizado");
          }}
        />
      )}
      {modal?.type === "users" && <UsersModal store={store} onClose={() => setModal(null)} notify={notify} />}
      {modal?.type === "backup" && <BackupModal store={store} onClose={() => setModal(null)} notify={notify} />}
      {modal?.type === "account" && <AccountSyncModal sync={sync} onClose={() => setModal(null)} notify={notify} />}
      {modal?.type === "audit" && <AuditModal data={data} onClose={() => setModal(null)} />}
      {modal?.type === "security" && <SecurityModal store={store} onClose={() => setModal(null)} notify={notify} />}
      {modal?.type === "settings" && <SettingsModal store={store} onClose={() => setModal(null)} notify={notify} />}
      {modal?.type === "about" && <AboutModal onClose={() => setModal(null)} />}

      {showOnboarding && (
        <OnboardingModal
          onClose={() => {
            store.saveSettings({ ...data.settings, onboardingComplete: true });
            setShowOnboarding(false);
          }}
          onOpen={(target) => {
            store.saveSettings({ ...data.settings, onboardingComplete: true });
            setShowOnboarding(false);
            if (target === "receipt") setTab("receipts");
            if (target === "reading") setTab("readings");
            if (target === "users") setModal({ type: "users" });
            if (target === "backup") setModal({ type: "backup" });
          }}
        />
      )}

      {toast && <div className="toast"><CheckCircle2 size={18} />{toast}</div>}
    </div>
  );
}

function subtitleFor(tab) {
  if (tab === "summary") return "Distribución mensual, servicios y estado de pagos";
  if (tab === "receipts") return "Importa recibos PDF o registra sus datos manualmente";
  if (tab === "readings") return "Control de medidores internos por usuario";
  return "Gastos adicionales y participantes por periodo";
}

function MenuButton({ icon: Icon, label, onClick }) {
  return (
    <button onClick={onClick}>
      <Icon size={18} />
      {label}
    </button>
  );
}

function SummaryPage({ store, setModal, notify }) {
  const { data } = store;
  const periods = useMemo(() => availablePeriods(data), [data]);
  const [period, setPeriod] = useState(periods[0] || "");
  const [userId, setUserId] = useState(ALL_USERS);
  const selectedPeriod = periods.includes(period) ? period : periods[0] || "";
  const summary = useMemo(() => calculatePeriod(selectedPeriod, data), [selectedPeriod, data]);
  const ledger = useMemo(() => buildPaymentLedger(periods, data), [periods, data]);
  const alerts = useMemo(() => buildSmartAlerts(data, selectedPeriod, ledger), [data, selectedPeriod, ledger]);
  const visibleResults =
    userId === ALL_USERS ? summary.results : summary.results.filter((item) => item.userId === userId);
  const isUserSummary = userId !== ALL_USERS;
  const selectedResult = visibleResults[0];
  const selectedBalance =
    isUserSummary && selectedResult ? ledger.get(`${selectedPeriod}|${selectedResult.userId}`) : null;
  const outstanding =
    isUserSummary && selectedResult
      ? selectedBalance?.remainingBalance ?? selectedResult.finalTotalWithServices
      : totalOutstandingForPeriod(selectedPeriod, summary.results, ledger);
  const chartData = [...periods]
    .reverse()
    .map((chartPeriod) => {
      const chartSummary = calculatePeriod(chartPeriod, data);
      const value =
        userId === ALL_USERS
          ? chartSummary.results.reduce((sum, item) => sum + item.consumptionKwh, 0)
          : chartSummary.results.find((item) => item.userId === userId)?.consumptionKwh || 0;
      return { period: chartPeriod.slice(2), kWh: Number(value.toFixed(2)) };
    })
    .slice(-12);

  const exportPdf = () => {
    const pdf = new jsPDF({ unit: "mm", format: "a4" });
    const pageWidth = pdf.internal.pageSize.getWidth();
    const pageHeight = pdf.internal.pageSize.getHeight();
    const margin = 16;
    const green = [24, 160, 94];
    const ink = [30, 41, 48];
    const muted = [98, 111, 120];
    const soft = [241, 247, 244];
    let y = 0;

    const drawHeader = () => {
      pdf.setFillColor(...green);
      pdf.rect(0, 0, pageWidth, 34, "F");
      pdf.setTextColor(255, 255, 255);
      pdf.setFont("helvetica", "bold");
      pdf.setFontSize(20);
      pdf.text("Control Electrico", margin, 15);
      pdf.setFont("helvetica", "normal");
      pdf.setFontSize(10);
      pdf.text("Resumen de consumo, servicios y pagos", margin, 23);
      pdf.setFont("helvetica", "bold");
      pdf.text(selectedPeriod || "Sin periodo", pageWidth - margin, 17, { align: "right" });
      y = 44;
    };
    const ensureSpace = (height) => {
      if (y + height <= pageHeight - 18) return;
      pdf.addPage();
      drawHeader();
    };
    const row = (label, value, options = {}) => {
      ensureSpace(8);
      pdf.setFont("helvetica", options.important ? "bold" : "normal");
      pdf.setFontSize(options.important ? 11 : 9.5);
      pdf.setTextColor(...(options.important ? ink : muted));
      pdf.text(label, margin + (options.indent || 0), y);
      pdf.setTextColor(...ink);
      pdf.text(value, pageWidth - margin, y, { align: "right" });
      y += options.important ? 8 : 6;
    };
    const section = (title) => {
      ensureSpace(13);
      y += 3;
      pdf.setFillColor(...soft);
      pdf.roundedRect(margin, y - 5, pageWidth - margin * 2, 10, 2, 2, "F");
      pdf.setTextColor(...ink);
      pdf.setFont("helvetica", "bold");
      pdf.setFontSize(10.5);
      pdf.text(title, margin + 4, y + 1.5);
      y += 10;
    };

    drawHeader();
    pdf.setFillColor(248, 250, 251);
    pdf.roundedRect(margin, y, pageWidth - margin * 2, 22, 3, 3, "F");
    pdf.setTextColor(...muted);
    pdf.setFont("helvetica", "normal");
    pdf.setFontSize(9);
    pdf.text("VISTA DEL RESUMEN", margin + 5, y + 7);
    pdf.setTextColor(...ink);
    pdf.setFont("helvetica", "bold");
    pdf.setFontSize(12);
    pdf.text(
      userId === ALL_USERS
        ? "Todos los usuarios"
        : `${selectedResult?.userId || ""} - ${selectedResult?.userName || "Sin nombre"}`,
      margin + 5,
      y + 15
    );
    pdf.setFontSize(16);
    pdf.setTextColor(...green);
    pdf.text(money(outstanding), pageWidth - margin - 5, y + 15, { align: "right" });
    y += 29;

    visibleResults.forEach((result) => {
      const balance = ledger.get(`${selectedPeriod}|${result.userId}`);
      const services = data.services
        .filter((service) => service.period === selectedPeriod && service.isActive !== false)
        .map((service) => ({ name: service.name, amount: serviceShareForUser(service, result.userId) }))
        .filter((service) => service.amount > 0);
      ensureSpace(48);
      section(`${result.userId} - ${result.userName || "Sin nombre"}`);
      row("Consumo", kwh(result.consumptionKwh));
      row("Consumo electrico", money(result.finalTotal));
      services.forEach((service) => row(service.name, money(service.amount), { indent: 3 }));
      row("Total del periodo", money(balance?.currentPeriodAmount ?? result.finalTotalWithServices), {
        important: true
      });

      if (balance?.previousDebtItems?.length) {
        section("Deudas de periodos anteriores");
        balance.previousDebtItems.forEach((item) => {
          const paid = Math.max(item.originalAmount - item.remainingAmount, 0);
          row(
            `Periodo ${item.period}${paid > 0.005 ? ` (abonado ${money(paid)})` : ""}`,
            money(item.remainingAmount),
            { indent: 3 }
          );
        });
        row("Subtotal deuda anterior", money(balance.previousBalance), { important: true });
      }

      section("Estado del pago");
      row("Estado registrado", paymentStatusLabel(balance?.status));
      if (balance?.amountPaid > 0) row("Pago aplicado", money(balance.amountPaid));
      row("Total por pagar", money(balance?.remainingBalance ?? result.finalTotalWithServices), {
        important: true
      });
      y += 4;
    });

    if (chartData.length) {
      ensureSpace(65);
      section("Consumo de los ultimos periodos");
      const chartLeft = margin + 6;
      const chartTop = y + 3;
      const chartWidth = pageWidth - margin * 2 - 12;
      const chartHeight = 38;
      const maxValue = Math.max(...chartData.map((item) => item.kWh), 1);
      const gap = 3;
      const barWidth = Math.min(
        Math.max((chartWidth - gap * (chartData.length - 1)) / chartData.length, 4),
        18
      );
      const groupWidth = barWidth * chartData.length + gap * (chartData.length - 1);
      const chartStart = chartLeft + (chartWidth - groupWidth) / 2;
      pdf.setDrawColor(210, 218, 222);
      pdf.line(chartLeft, chartTop + chartHeight, chartLeft + chartWidth, chartTop + chartHeight);
      chartData.forEach((item, index) => {
        const height = (item.kWh / maxValue) * chartHeight;
        const left = chartStart + index * (barWidth + gap);
        pdf.setFillColor(...green);
        pdf.roundedRect(left, chartTop + chartHeight - height, barWidth, height, 1, 1, "F");
        pdf.setTextColor(...muted);
        pdf.setFontSize(7);
        pdf.text(item.period, left + barWidth / 2, chartTop + chartHeight + 5, { align: "center" });
      });
      y = chartTop + chartHeight + 12;
    }

    const pages = pdf.getNumberOfPages();
    for (let page = 1; page <= pages; page += 1) {
      pdf.setPage(page);
      pdf.setDrawColor(225, 230, 233);
      pdf.line(margin, pageHeight - 12, pageWidth - margin, pageHeight - 12);
      pdf.setFont("helvetica", "normal");
      pdf.setFontSize(8);
      pdf.setTextColor(...muted);
      pdf.text(`Generado ${new Date().toLocaleString("es-PE")}`, margin, pageHeight - 7);
      pdf.text(`Pagina ${page} de ${pages}`, pageWidth - margin, pageHeight - 7, { align: "right" });
    }
    pdf.save(`resumen_${selectedPeriod || "sin_periodo"}.pdf`);
    notify("PDF generado");
  };
  const shareReadOnlyLink = async () => {
    const url = new URL(window.location.href);
    url.searchParams.set("view", "share");
    url.searchParams.set("period", selectedPeriod);
    url.searchParams.set("user", userId);
    await copyText(url.toString());
    notify("Enlace de vista copiado");
  };

  if (!periods.length) {
    return (
      <EmptyState
        icon={BookOpen}
        title="El resumen todavía no está disponible"
        text="Registra un recibo y al menos una lectura del mismo periodo."
      />
    );
  }

  return (
    <div className="page-stack">
      <section className="summary-toolbar">
        <div className="select-field">
          <label>Periodo</label>
          <select value={selectedPeriod} onChange={(event) => setPeriod(event.target.value)}>
            {periods.map((item) => <option key={item}>{item}</option>)}
          </select>
          <ChevronDown size={16} />
        </div>
        <div className="select-field">
          <label>Usuario</label>
          <select value={userId} onChange={(event) => setUserId(event.target.value)}>
            <option value={ALL_USERS}>Todos los usuarios</option>
            {summary.results.map((result) => (
              <option key={result.userId} value={result.userId}>
                {result.userId} - {result.userName}
              </option>
            ))}
          </select>
          <ChevronDown size={16} />
        </div>
        <button className="secondary-button" onClick={exportPdf}>
          <FileDown size={18} /> Exportar PDF
        </button>
        <button className="secondary-button" onClick={shareReadOnlyLink}>
          <Share2 size={18} /> Vista solo lectura
        </button>
      </section>

      <AlertsPanel alerts={alerts} />

      <section className="metrics-grid">
        <MetricCard
          icon={CircleDollarSign}
          label={userId === ALL_USERS ? "Total general" : "Saldo por pagar"}
          value={money(outstanding)}
          tone="green"
        />
        <MetricCard
          icon={Gauge}
          label={userId === ALL_USERS ? "Consumo total" : "Consumo"}
          value={kwh(
            userId === ALL_USERS
              ? summary.results.reduce((sum, result) => sum + result.consumptionKwh, 0)
              : selectedResult?.consumptionKwh
          )}
          tone="blue"
        />
        <MetricCard
          icon={Users}
          label="Participantes"
          value={String(summary.participants)}
          tone="amber"
        />
        <MetricCard
          icon={ListChecks}
          label="Servicios"
          value={money(
            userId === ALL_USERS ? summary.serviceExpensesTotal : selectedResult?.serviceShare
          )}
          tone="gray"
        />
      </section>

      <section className="dashboard-grid">
        <Panel title="Consumo de los últimos periodos" className="chart-panel">
          <ResponsiveContainer width="100%" height={260}>
            <BarChart data={chartData} margin={{ top: 12, right: 8, left: 0, bottom: 0 }}>
              <CartesianGrid vertical={false} strokeDasharray="4 4" />
              <XAxis dataKey="period" tickLine={false} axisLine={false} />
              <YAxis tickLine={false} axisLine={false} width={42} />
              <Tooltip formatter={(value) => [`${value} kWh`, "Consumo"]} />
              <Bar dataKey="kWh" fill="#18a05e" radius={[5, 5, 0, 0]} maxBarSize={44} />
            </BarChart>
          </ResponsiveContainer>
        </Panel>

        <Panel title="Datos del periodo">
          <DetailRow label="Estado residual" value={summary.residualStatus} />
          {summary.thresholdKwhPerUser > 0 && (
            <DetailRow label="Umbral individual" value={kwh(summary.thresholdKwhPerUser)} />
          )}
          <DetailRow label="Cargos fijos por usuario" value={money(summary.fixedChargesPerUser)} />
          <DetailRow
            label="Electrificación Rural por usuario"
            value={money(summary.ruralElectrificationPerUser)}
          />
          <DetailRow label="Diferencia del recibo" value={money(summary.receiptDifference)} />
        </Panel>
      </section>

      {userId !== ALL_USERS && selectedBalance?.previousDebtItems?.length > 0 && (
        <Panel title="Deudas de periodos anteriores" className="debt-panel">
          <p className="panel-helper">
            Estos importes se suman al periodo actual. Los pagos se aplican primero a la deuda más antigua.
          </p>
          <div className="debt-list">
            {selectedBalance.previousDebtItems.map((item) => {
              const paid = Math.max(item.originalAmount - item.remainingAmount, 0);
              return (
                <div className="debt-item" key={item.period}>
                  <span>
                    <strong>Periodo {item.period}</strong>
                    <small>
                      Importe original {money(item.originalAmount)}
                      {paid > 0.005 ? ` · Abonado ${money(paid)}` : " · Sin abonos"}
                    </small>
                  </span>
                  <strong>{money(item.remainingAmount)}</strong>
                </div>
              );
            })}
          </div>
          <DetailRow label="Total de deuda anterior" value={money(selectedBalance.previousBalance)} important />
        </Panel>
      )}

      <Panel title={userId === ALL_USERS ? "Pagos por usuario" : "Detalle del usuario"}>
        <div className="table-wrap">
          <table>
            <thead>
              <tr>
                <th>Usuario</th>
                <th>Consumo</th>
                <th>Electricidad</th>
                <th>Servicios</th>
                <th>Saldo anterior</th>
                <th>Estado</th>
                <th>Saldo por pagar</th>
                <th aria-label="Acciones" />
              </tr>
            </thead>
            <tbody>
              {visibleResults.map((result) => {
                const balance = ledger.get(`${selectedPeriod}|${result.userId}`);
                const payment = data.payments.find(
                  (item) => item.period === selectedPeriod && item.userId === result.userId
                );
                const balanceDue = balance?.remainingBalance;
                return (
                  <tr key={result.userId}>
                    <td>
                      <strong>{result.userName || result.userId}</strong>
                      <small>{result.internalMeter}</small>
                      {balance?.previousDebtItems?.length > 0 && (
                        <small className="debt-periods">
                          Deuda: {balance.previousDebtItems.map((item) => item.period).join(", ")}
                        </small>
                      )}
                    </td>
                    <td>{kwh(result.consumptionKwh)}</td>
                    <td>{money(result.finalTotal)}</td>
                    <td>{money(result.serviceShare)}</td>
                    <td>{money(balance?.previousBalance)}</td>
                    <td><StatusPill status={balance?.status} /></td>
                    <td><strong>{money(balanceDue)}</strong></td>
                    <td>
                      <button
                        className="icon-button small"
                        title="Registrar o editar pago"
                        onClick={() => setModal({ type: "payment", result, balance, existing: payment })}
                      >
                        <Edit3 size={16} />
                      </button>
                    </td>
                  </tr>
                );
              })}
            </tbody>
          </table>
        </div>
      </Panel>
    </div>
  );
}

function ReceiptsPage({ store, setModal, notify }) {
  const inputRef = useRef();
  const [importing, setImporting] = useState(false);
  const receipts = [...store.data.receipts].sort((a, b) => b.period.localeCompare(a.period));
  const importPdf = async (file) => {
    if (!file) return;
    setImporting(true);
    try {
      const parsed = await parseReceiptPdf(file);
      setModal({ type: "receipt", initial: parsed });
      notify("PDF leído; revisa los campos antes de guardar");
    } catch (error) {
      notify(`No se pudo leer el PDF: ${error.message}`);
    } finally {
      setImporting(false);
      inputRef.current.value = "";
    }
  };
  return (
    <div className="page-stack">
      <section className="action-strip">
        <div>
          <strong>{receipts.length} recibos registrados</strong>
          <span>Los importes pueden revisarse antes de guardar.</span>
        </div>
        <div>
          <input ref={inputRef} hidden type="file" accept=".pdf,application/pdf" onChange={(e) => importPdf(e.target.files[0])} />
          <button className="secondary-button" disabled={importing} onClick={() => inputRef.current.click()}>
            <Upload size={18} /> {importing ? "Leyendo..." : "Importar PDF"}
          </button>
          <button className="primary-button" onClick={() => setModal({ type: "receipt" })}>
            <Plus size={18} /> Nuevo recibo
          </button>
        </div>
      </section>
      <Panel title="Historial de recibos">
        {receipts.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Periodo</th><th>Fecha lectura</th><th>Suministro</th><th>Consumo exterior</th><th>Total del mes</th><th>Tarifa</th><th /></tr></thead>
              <tbody>
                {receipts.map((receipt) => (
                  <tr key={receipt.period}>
                    <td><strong>{receipt.period}</strong></td>
                    <td>{receipt.externalReadingDate || "—"}</td>
                    <td>{receipt.supplyNumber || "—"}</td>
                    <td>{kwh(receipt.externalKwh)}</td>
                    <td><strong>{money(receipt.monthlyBill)}</strong></td>
                    <td>{Number(receipt.priceKwhOver30) > 0 ? "Dos bloques" : "Precio único"}</td>
                    <td className="row-actions">
                      <button className="icon-button small" title="Editar" onClick={() => setModal({ type: "receipt", item: receipt })}><Edit3 size={16} /></button>
                      <button className="icon-button small danger" title="Eliminar" onClick={() => confirm("¿Eliminar este recibo?") && store.remove("receipts", receipt.period)}><Trash2 size={16} /></button>
                    </td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        ) : <EmptyState icon={FileText} title="Sin recibos" text="Importa un PDF o registra el primer recibo." compact />}
      </Panel>
    </div>
  );
}

function ReadingsPage({ store, setModal }) {
  const readings = [...store.data.readings].sort((a, b) => b.period.localeCompare(a.period));
  return (
    <div className="page-stack">
      <section className="action-strip">
        <div><strong>{readings.length} lecturas registradas</strong><span>La lectura anterior se completa desde el registro previo.</span></div>
        <button className="primary-button" onClick={() => setModal({ type: "reading" })}><Plus size={18} /> Nueva lectura</button>
      </section>
      <Panel title="Lecturas internas">
        {readings.length ? (
          <div className="table-wrap">
            <table>
              <thead><tr><th>Periodo</th><th>Usuario</th><th>Fecha</th><th>Lectura anterior</th><th>Lectura actual</th><th>Consumo</th><th /></tr></thead>
              <tbody>
                {readings.map((reading) => {
                  const user = store.data.users.find((item) => item.userId === reading.userId);
                  const consumption =
                    Number.isFinite(Number(reading.previousReading)) && Number.isFinite(Number(reading.currentReading))
                      ? Math.max(Number(reading.currentReading) - Number(reading.previousReading), 0)
                      : 0;
                  return (
                    <tr key={reading.id}>
                      <td><strong>{reading.period}</strong></td>
                      <td>{user?.name || reading.userId}<small>{user?.internalMeter}</small></td>
                      <td>{reading.internalReadingDate}</td>
                      <td>{reading.previousReading ?? "—"}</td>
                      <td><strong>{reading.currentReading ?? "—"}</strong></td>
                      <td>{kwh(consumption)}</td>
                      <td className="row-actions">
                        <button className="icon-button small" title="Editar" onClick={() => setModal({ type: "reading", item: reading })}><Edit3 size={16} /></button>
                        <button className="icon-button small danger" title="Eliminar" onClick={() => confirm("¿Eliminar esta lectura?") && store.remove("readings", reading.id)}><Trash2 size={16} /></button>
                      </td>
                    </tr>
                  );
                })}
              </tbody>
            </table>
          </div>
        ) : <EmptyState icon={Gauge} title="Sin lecturas" text="Agrega la lectura de un medidor interno." compact />}
      </Panel>
    </div>
  );
}

function ServicesPage({ store, setModal }) {
  const services = [...store.data.services].sort((a, b) => b.period.localeCompare(a.period));
  return (
    <div className="page-stack">
      <section className="action-strip">
        <div><strong>{services.length} servicios registrados</strong><span>Divide el importe solo entre quienes utilizan cada servicio.</span></div>
        <button className="primary-button" onClick={() => setModal({ type: "service" })}><Plus size={18} /> Nuevo servicio</button>
      </section>
      <Panel title="Servicios adicionales">
        {services.length ? (
          <div className="service-grid">
            {services.map((service) => (
              <article className="service-item" key={service.id}>
                <span className="service-icon">{service.name.toLowerCase().includes("agua") ? <Droplets /> : service.name.toLowerCase().includes("internet") ? <Wifi /> : <ListChecks />}</span>
                <div className="service-copy">
                  <strong>{service.name}</strong>
                  <span>{service.period} · {service.splitCost ? `Dividido entre ${service.participantUserIds?.length || service.participantCount}` : "Importe completo"}</span>
                </div>
                <strong className="service-price">{money(service.amount)}</strong>
                <StatusPill status={service.isActive === false ? "INACTIVE" : "ACTIVE"} />
                <button className="icon-button small" title="Editar" onClick={() => setModal({ type: "service", item: service })}><Edit3 size={16} /></button>
                <button className="icon-button small danger" title="Eliminar" onClick={() => confirm("¿Eliminar este servicio?") && store.remove("services", service.id)}><Trash2 size={16} /></button>
              </article>
            ))}
          </div>
        ) : <EmptyState icon={CircleDollarSign} title="Sin servicios" text="Registra internet, agua, streaming u otro gasto." compact />}
      </Panel>
    </div>
  );
}

function ReceiptModal({ receipt, initial, onClose, onSave }) {
  const source = receipt || initial || {};
  const [form, setForm] = useState({
    period: source.period || new Date().toISOString().slice(0, 7),
    externalReadingDate: source.externalReadingDate || "",
    supplyNumber: source.supplyNumber || "",
    externalKwh: source.externalKwh ?? "",
    monthlyBill: source.monthlyBill ?? "",
    priceKwhUpTo30: source.priceKwhUpTo30 ?? "",
    priceKwhOver30: source.priceKwhOver30 ?? "",
    fixedCharge: source.fixedCharge ?? "",
    maintenance: source.maintenance ?? "",
    publicLighting: source.publicLighting ?? "",
    ruralElectrification: source.ruralElectrification ?? "",
    notes: source.notes || ""
  });
  const update = (key) => (event) => {
    const value = event.target.value;
    setForm((current) => ({ ...current, [key]: value }));
  };
  const normalizedReceipt = {
    ...form,
    externalKwh: Number(form.externalKwh) || 0,
    monthlyBill: Number(form.monthlyBill) || 0,
    priceKwhUpTo30: Number(form.priceKwhUpTo30) || 0,
    priceKwhOver30: Number(form.priceKwhOver30) || 0,
    fixedCharge: Number(form.fixedCharge) || 0,
    maintenance: Number(form.maintenance) || 0,
    publicLighting: Number(form.publicLighting) || 0,
    ruralElectrification: Number(form.ruralElectrification) || 0
  };
  const receiptWarnings = validateReceipt(normalizedReceipt);
  const save = () => onSave(normalizedReceipt);
  return (
    <Modal title={receipt ? "Editar recibo" : initial ? "Revisar recibo importado" : "Nuevo recibo"} onClose={onClose} wide>
      <div className="form-grid three">
        <Field label="Periodo" type="month" value={form.period} onChange={update("period")} required />
        <Field label="Fecha lectura exterior" value={form.externalReadingDate} onChange={update("externalReadingDate")} />
        <Field label="N.º de suministro" value={form.supplyNumber} onChange={update("supplyNumber")} />
        <Field label="Consumo exterior (kWh)" type="number" value={form.externalKwh} onChange={update("externalKwh")} />
        <Field label="Total del mes (S/)" type="number" value={form.monthlyBill} onChange={update("monthlyBill")} />
        <div />
        <Field label="Precio kWh / hasta 30" type="number" value={form.priceKwhUpTo30} onChange={update("priceKwhUpTo30")} />
        <Field label="Precio kWh mayor a 30" type="number" value={form.priceKwhOver30} onChange={update("priceKwhOver30")} />
        <div className="field-note">Deja el segundo precio en cero cuando exista una tarifa única.</div>
        <Field label="Cargo fijo" type="number" value={form.fixedCharge} onChange={update("fixedCharge")} />
        <Field label="Mantenimiento y reposición" type="number" value={form.maintenance} onChange={update("maintenance")} />
        <Field label="Alumbrado público" type="number" value={form.publicLighting} onChange={update("publicLighting")} />
        <Field label="Electrificación Rural (Ley N° 28749)" type="number" value={form.ruralElectrification} onChange={update("ruralElectrification")} />
        <Field label="Notas" value={form.notes} onChange={update("notes")} span={2} />
      </div>
      {receiptWarnings.length > 0 && (
        <div className="validation-panel">
          <AlertTriangle size={18} />
          <div>
            <strong>Revisa estos datos antes de guardar</strong>
            {receiptWarnings.map((warning) => <span key={warning}>{warning}</span>)}
          </div>
        </div>
      )}
      <ModalActions onClose={onClose} onSave={save} disabled={!form.period} />
    </Modal>
  );
}

function ReadingModal({ reading, data, onClose, onSave }) {
  const [userId, setUserId] = useState(reading?.userId || data.users[0]?.userId || "");
  const [period, setPeriod] = useState(reading?.period || new Date().toISOString().slice(0, 7));
  const latest = data.readings
    .filter((item) => item.userId === userId && item.period < period && item.currentReading != null)
    .sort((a, b) => b.period.localeCompare(a.period))[0];
  const [previous, setPrevious] = useState(reading?.previousReading ?? latest?.currentReading ?? "");
  const [current, setCurrent] = useState(reading?.currentReading ?? "");
  const [date, setDate] = useState(reading?.internalReadingDate || new Date().toISOString().slice(0, 10));
  const [notes, setNotes] = useState(reading?.notes || "");
  const selectUser = (event) => {
    const next = event.target.value;
    setUserId(next);
    const suggestion = data.readings
      .filter((item) => item.userId === next && item.period < period && item.currentReading != null)
      .sort((a, b) => b.period.localeCompare(a.period))[0];
    setPrevious(suggestion?.currentReading ?? "");
  };
  return (
    <Modal title={reading ? "Editar lectura" : "Nueva lectura"} onClose={onClose}>
      <div className="form-grid">
        <SelectField label="Usuario" value={userId} onChange={selectUser}>
          {data.users.map((user) => <option key={user.userId} value={user.userId}>{user.userId} - {user.name}</option>)}
        </SelectField>
        <Field label="Periodo" type="month" value={period} onChange={(e) => setPeriod(e.target.value)} />
        <Field label="Fecha lectura interna" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
        <Field label="Lectura anterior" type="number" value={previous} onChange={(e) => setPrevious(e.target.value)} />
        <Field label="Lectura actual" type="number" value={current} onChange={(e) => setCurrent(e.target.value)} />
        <Field label="Notas" value={notes} onChange={(e) => setNotes(e.target.value)} />
      </div>
      <ModalActions
        onClose={onClose}
        disabled={!userId || !period || current === ""}
        onSave={() => onSave({
          id: reading?.id || crypto.randomUUID(),
          period,
          userId,
          isResidual: false,
          internalReadingDate: date,
          previousReading: previous === "" ? null : Number(previous),
          currentReading: current === "" ? null : Number(current),
          notes
        })}
      />
    </Modal>
  );
}

function ServiceModal({ service, data, onClose, onSave }) {
  const matched = SERVICE_OPTIONS.includes(service?.name) ? service.name : service ? "Otro servicio" : SERVICE_OPTIONS[0];
  const [category, setCategory] = useState(matched);
  const [customName, setCustomName] = useState(matched === "Otro servicio" ? service?.name || "" : "");
  const [period, setPeriod] = useState(service?.period || new Date().toISOString().slice(0, 7));
  const [amount, setAmount] = useState(service?.amount ?? "");
  const [active, setActive] = useState(service?.isActive !== false);
  const [split, setSplit] = useState(service?.splitCost !== false);
  const [participants, setParticipants] = useState(service?.participantUserIds || []);
  const activeUsers = data.users.filter((user) => isActiveInPeriod(user, period));
  const toggleParticipant = (id) =>
    setParticipants((current) => current.includes(id) ? current.filter((item) => item !== id) : [...current, id]);
  const name = category === "Otro servicio" ? customName.trim() : category;
  return (
    <Modal title={service ? "Editar servicio" : "Nuevo servicio"} onClose={onClose}>
      <div className="form-grid">
        <SelectField label="Tipo de servicio" value={category} onChange={(e) => setCategory(e.target.value)}>
          {SERVICE_OPTIONS.map((option) => <option key={option}>{option}</option>)}
        </SelectField>
        {category === "Otro servicio" && <Field label="Nombre del servicio" value={customName} onChange={(e) => setCustomName(e.target.value)} />}
        <Field label="Periodo" type="month" value={period} onChange={(e) => setPeriod(e.target.value)} />
        <Field label="Importe total (S/)" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />
      </div>
      <div className="switch-list">
        <SwitchRow label="Servicio activo" checked={active} onChange={setActive} />
        <SwitchRow label="Dividir el importe" checked={split} onChange={setSplit} />
      </div>
      {split && (
        <div className="participants">
          <label>Usuarios participantes</label>
          <div>
            {activeUsers.map((user) => (
              <button key={user.userId} className={participants.includes(user.userId) ? "participant selected" : "participant"} onClick={() => toggleParticipant(user.userId)}>
                <UserRound size={15} /> {user.name || user.userId}
              </button>
            ))}
          </div>
        </div>
      )}
      <ModalActions
        onClose={onClose}
        disabled={!name || !period || Number(amount) <= 0}
        onSave={() => onSave({
          id: service?.id || crypto.randomUUID(),
          period,
          name,
          amount: Number(amount),
          isActive: active,
          splitCost: split,
          participantCount: Math.max(participants.length || activeUsers.length, 1),
          participantUserIds: participants,
          notes: service?.notes || ""
        })}
      />
    </Modal>
  );
}

function PaymentModal({ result, balance, existing, onClose, onSave, onDelete }) {
  const [status, setStatus] = useState(existing?.status || "");
  const [amount, setAmount] = useState(existing?.amountPaid ?? "");
  const [date, setDate] = useState(existing?.paymentDate || new Date().toISOString().slice(0, 10));
  const partialValid = status !== "PARTIAL" || (Number(amount) > 0 && Number(amount) < balance.totalDue);
  return (
    <Modal title={`Pago de ${result.userName || result.userId}`} onClose={onClose}>
      <div className="payment-summary">
        <DetailRow label="Periodo" value={balance.period} />
        <DetailRow label="Consumo y servicios" value={money(balance.currentPeriodAmount)} />
        <DetailRow label="Saldo anterior" value={money(balance.previousBalance)} />
        <DetailRow label="Total comprometido" value={money(balance.totalDue)} important />
      </div>
      <div className="segmented">
        {[["PAID", "Pagado total"], ["PARTIAL", "Pago parcial"], ["UNPAID", "No pagado"]].map(([value, label]) => (
          <button key={value} className={status === value ? "selected" : ""} onClick={() => setStatus(value)}>{label}</button>
        ))}
      </div>
      {status === "PARTIAL" && <Field label="Monto pagado parcialmente" type="number" value={amount} onChange={(e) => setAmount(e.target.value)} />}
      <Field label="Fecha de pago" type="date" value={date} onChange={(e) => setDate(e.target.value)} />
      <div className="modal-actions split-actions">
        {existing && <button className="text-button danger-text" onClick={onDelete}>Borrar estado</button>}
        <span />
        <button className="secondary-button" onClick={onClose}>Cancelar</button>
        <button
          className="primary-button"
          disabled={!status || !partialValid}
          onClick={() => onSave({
            id: `${balance.period}|${result.userId}`,
            period: balance.period,
            userId: result.userId,
            status,
            amountPaid: status === "PAID" ? balance.totalDue : status === "PARTIAL" ? Number(amount) : 0,
            paymentDate: date,
            notes: existing?.notes || ""
          })}
        ><Save size={17} /> Guardar</button>
      </div>
    </Modal>
  );
}

function UsersModal({ store, onClose, notify }) {
  const [editing, setEditing] = useState(null);
  const [form, setForm] = useState(null);
  const start = (user = null) => {
    setEditing(user);
    setForm({
      userId: user?.userId || `U${String(store.data.users.length + 1).padStart(2, "0")}`,
      name: user?.name || "",
      internalMeter: user?.internalMeter || "",
      statePeriod: new Date().toISOString().slice(0, 7),
      isActive: user?.isActive !== false,
      isResidual: Boolean(user?.isResidual),
      notes: user?.notes || ""
    });
  };
  const save = () => {
    const states = (editing?.periodStates || []).filter((state) => state.period !== form.statePeriod);
    const user = {
      userId: form.userId.trim(),
      name: form.name.trim(),
      internalMeter: form.internalMeter.trim(),
      isActive: form.isActive,
      isResidual: form.isResidual && form.isActive,
      periodStates: [...states, { period: form.statePeriod, isActive: form.isActive, isResidual: form.isResidual && form.isActive }].sort((a, b) => a.period.localeCompare(b.period)),
      notes: form.notes
    };
    if (user.isResidual) {
      store.data.users
        .filter((item) => item.userId !== user.userId && isResidualInPeriod(item, form.statePeriod))
        .forEach((item) => {
          const nextStates = (item.periodStates || []).filter((state) => state.period !== form.statePeriod);
          store.upsert("users", {
            ...item,
            isResidual: false,
            periodStates: [...nextStates, { period: form.statePeriod, isActive: isActiveInPeriod(item, form.statePeriod), isResidual: false }]
          });
        });
    }
    store.upsert("users", user);
    setForm(null);
    notify("Usuario guardado");
  };
  return (
    <Modal title="Administrar usuarios" onClose={onClose} wide>
      {!form ? (
        <>
          <div className="modal-toolbar">
            <p>Los cambios de estado se guardan desde el periodo indicado.</p>
            <button className="primary-button" onClick={() => start()}><Plus size={17} /> Nuevo usuario</button>
          </div>
          <div className="table-wrap">
            <table>
              <thead><tr><th>Código</th><th>Nombre</th><th>Medidor</th><th>Estado actual</th><th>Tipo</th><th /></tr></thead>
              <tbody>
                {store.data.users.map((user) => (
                  <tr key={user.userId}>
                    <td><strong>{user.userId}</strong></td>
                    <td>{user.name}</td>
                    <td>{user.internalMeter}</td>
                    <td><StatusPill status={user.isActive ? "ACTIVE" : "INACTIVE"} /></td>
                    <td>{user.isResidual ? "Residual" : "Lectura interna"}</td>
                    <td><button className="icon-button small" onClick={() => start(user)}><Edit3 size={16} /></button></td>
                  </tr>
                ))}
              </tbody>
            </table>
          </div>
        </>
      ) : (
        <>
          <div className="form-grid">
            <Field label="Código" value={form.userId} disabled={Boolean(editing)} onChange={(e) => setForm({ ...form, userId: e.target.value })} />
            <Field label="Nombre" value={form.name} onChange={(e) => setForm({ ...form, name: e.target.value })} />
            <Field label="Medidor interno" value={form.internalMeter} onChange={(e) => setForm({ ...form, internalMeter: e.target.value })} />
            <Field label="Periodo del cambio" type="month" value={form.statePeriod} onChange={(e) => setForm({ ...form, statePeriod: e.target.value })} />
          </div>
          <div className="switch-list">
            <SwitchRow label="Usuario activo" checked={form.isActive} onChange={(value) => setForm({ ...form, isActive: value })} />
            <SwitchRow label="Usuario residual" checked={form.isResidual} onChange={(value) => setForm({ ...form, isResidual: value })} />
          </div>
          <div className="modal-actions">
            <button className="secondary-button" onClick={() => setForm(null)}>Volver</button>
            <button className="primary-button" disabled={!form.userId || !form.name || !form.statePeriod} onClick={save}><Save size={17} /> Guardar</button>
          </div>
        </>
      )}
    </Modal>
  );
}

function BackupModal({ store, onClose, notify }) {
  const inputRef = useRef();
  const [password, setPassword] = useState("");
  const [googleBusy, setGoogleBusy] = useState(false);
  const googleStatus = googleSheetsEnvironmentStatus();
  const sheetId = store.data.settings.googleSheetId || "";
  const sheetUrl = googleSheetUrl(sheetId);
  const restore = async (file) => {
    if (!file) return;
    try {
      const imported = await importBackup(file, password);
      if (confirm("¿Reemplazar todos los datos actuales con este respaldo?")) {
        store.replaceData(imported, "replace");
        notify("Respaldo restaurado");
        onClose();
      }
    } catch (error) {
      notify(`No se pudo importar: ${error.message}`);
    }
  };
  const uploadToSheets = async () => {
    setGoogleBusy(true);
    try {
      const result = await exportToGoogleSheets(store.data, sheetId);
      store.saveSettings({
        ...store.data.settings,
        googleSheetId: result.spreadsheetId,
        googleSheetName: "Control Electrico",
        googleSheetUpdatedAt: result.updatedAt
      });
      store.markBackup("Google Sheets");
      notify("Respaldo guardado en Google Sheets");
    } catch (error) {
      notify(`Google Sheets: ${error.message}`);
    } finally {
      setGoogleBusy(false);
    }
  };
  const importFromSheets = async () => {
    const typed = sheetId || prompt("Pega el enlace o ID de la hoja de Google Sheets que contiene el respaldo:");
    const targetSheetId = extractGoogleSheetId(typed);
    if (!targetSheetId) {
      notify("No se indicó una hoja válida de Google Sheets");
      return;
    }
    setGoogleBusy(true);
    try {
      const result = await importFromGoogleSheets(targetSheetId);
      if (confirm("¿Reemplazar todos los datos actuales con el respaldo de Google Sheets?")) {
        store.replaceData({
          ...result.data,
          settings: {
            ...result.data.settings,
            googleSheetId: result.spreadsheetId,
            googleSheetName: "Control Electrico",
            googleSheetUpdatedAt: new Date().toISOString()
          }
        });
        notify("Respaldo de Google Sheets restaurado");
        onClose();
      }
    } catch (error) {
      notify(`Google Sheets: ${error.message}`);
    } finally {
      setGoogleBusy(false);
    }
  };
  return (
    <Modal title="Centro de respaldo" onClose={onClose}>
      <div className="backup-security">
        <Field
          label="Clave opcional para JSON protegido"
          type="password"
          value={password}
          onChange={(event) => setPassword(event.target.value)}
        />
        <p>Déjala vacía para respaldos normales. Escríbela antes de importar si el archivo está protegido.</p>
      </div>
      <div className="backup-options">
        <button onClick={() => { downloadJson(store.data); store.markBackup("JSON"); notify("JSON guardado"); }}><span><Download /></span><strong>Guardar JSON</strong><small>Respaldo completo recomendado</small></button>
        <button onClick={async () => {
          try {
            await downloadEncryptedJson(store.data, password);
            store.markBackup("JSON protegido");
            notify("JSON protegido guardado");
          } catch (error) {
            notify(error.message);
          }
        }}><span><KeyRound /></span><strong>Guardar protegido</strong><small>JSON cifrado con clave</small></button>
        <button onClick={() => { downloadCsv(store.data); store.markBackup("CSV"); notify("CSV guardado"); }}><span><ArrowDownToLine /></span><strong>Guardar CSV</strong><small>Compatible con Excel</small></button>
        <button onClick={() => inputRef.current.click()}><span><Upload /></span><strong>Importar respaldo</strong><small>Admite JSON y CSV de Android</small></button>
        <button disabled={!googleStatus.supported || googleBusy} onClick={uploadToSheets}><span><Cloud /></span><strong>Subir a Sheets</strong><small>{sheetId ? "Actualiza la hoja recordada" : "Crea una hoja privada"}</small></button>
        <button disabled={!googleStatus.supported || googleBusy} onClick={importFromSheets}><span><Database /></span><strong>Importar Sheets</strong><small>Restaura desde Google Drive</small></button>
      </div>
      <input ref={inputRef} hidden type="file" accept=".json,.csv" onChange={(e) => restore(e.target.files[0])} />
      <div className="info-box">
        <HardDrive size={19} />
        <span>Último respaldo: {formatSyncDate(store.data.settings.lastBackupAt)}. Los archivos se guardan en la carpeta que selecciones desde el navegador.</span>
      </div>
      <div className={`info-box ${googleStatus.supported ? "" : "danger"}`}>
        {googleStatus.supported ? <Cloud size={19} /> : <CloudOff size={19} />}
        <span>
          {googleStatus.supported
            ? `Google Sheets: ${sheetId ? `hoja vinculada, última actualización ${formatSyncDate(store.data.settings.googleSheetUpdatedAt)}` : "sin hoja vinculada todavía"}.`
            : googleStatus.message}
        </span>
        {sheetUrl && (
          <button className="inline-action" type="button" onClick={() => window.open(sheetUrl, "_blank", "noopener,noreferrer")}>
            Abrir hoja
          </button>
        )}
      </div>
    </Modal>
  );
}

function AccountSyncModal({ sync, onClose, notify }) {
  const [config, setConfig] = useState(sync.config);
  const [email, setEmail] = useState(sync.session?.user?.email || "");
  const [password, setPassword] = useState("");
  const [mode, setMode] = useState("signin");
  const [busy, setBusy] = useState(false);
  const [formError, setFormError] = useState("");
  const [showGuide, setShowGuide] = useState(false);

  const run = async (action) => {
    setBusy(true);
    setFormError("");
    try {
      await action();
    } catch (error) {
      setFormError(error.message || String(error));
    } finally {
      setBusy(false);
    }
  };

  const saveConfig = () =>
    run(async () => {
      await sync.configure(config);
      notify("Configuración de Supabase guardada");
    });

  const authenticate = () =>
    run(async () => {
      if (!email.trim() || password.length < 6) {
        throw new Error("Escribe un correo válido y una contraseña de al menos 6 caracteres.");
      }
      if (mode === "signup") {
        const result = await sync.signUp(email, password);
        notify(result.confirmationRequired ? "Revisa tu correo para confirmar la cuenta" : "Cuenta creada");
      } else {
        await sync.signIn(email, password);
        notify("Cuenta conectada");
      }
      setPassword("");
    });

  if (showGuide) {
    return (
      <Modal title="Cómo configurar Supabase" onClose={onClose}>
        <div className="setup-guide">
          <button className="text-button setup-back" onClick={() => setShowGuide(false)}>
            <ArrowLeft size={17} /> Volver a la conexión
          </button>
          <SetupStep number="1" title="Crear el proyecto">
            Abre Supabase, crea un proyecto llamado ControlElectrico, guarda la contraseña de base de datos y elige la región más cercana.
          </SetupStep>
          <a className="primary-button full-button" href="https://database.new/" target="_blank" rel="noreferrer">
            <Database size={17} /> Abrir Supabase
          </a>
          <SetupStep number="2" title="Crear la tabla segura">
            En SQL Editor pulsa New query, pega el script de configuración y selecciona Run.
          </SetupStep>
          <button
            className="secondary-button full-button"
            onClick={async () => {
              try {
                await copyText(SUPABASE_SETUP_SQL);
                notify("Script SQL copiado");
              } catch {
                notify("No se pudo copiar el script");
              }
            }}
          >
            <Save size={17} /> Copiar script SQL
          </button>
          <SetupStep number="3" title="Habilitar correo y contraseña">
            En Authentication &gt; Providers abre Email y activa Email provider. Para una primera prueba puedes desactivar Confirm email.
          </SetupStep>
          <SetupStep number="4" title="Copiar la conexión">
            En Settings &gt; API Keys copia Project URL y Publishable key. Nunca uses Secret key ni service_role.
          </SetupStep>
          <SetupStep number="5" title="Conectar los dispositivos">
            Regresa, guarda la URL y la clave pública. Crea la cuenta en el primer dispositivo e inicia sesión con la misma cuenta en los demás.
          </SetupStep>
        </div>
      </Modal>
    );
  }

  return (
    <Modal title="Cuenta y sincronización" onClose={onClose}>
      {!sync.configured && (
        <div className="sync-section">
          <div className="section-heading">
            <span><Database size={19} /></span>
            <div><strong>Conectar proyecto Supabase</strong><small>Configuración necesaria una sola vez</small></div>
          </div>
          <div className="form-grid">
            <Field
              label="URL del proyecto"
              span={2}
              placeholder="https://xxxxx.supabase.co"
              value={config.url}
              onChange={(event) => setConfig({ ...config, url: event.target.value })}
            />
            <Field
              label="Clave pública (publishable o anon)"
              span={2}
              type="password"
              value={config.anonKey}
              onChange={(event) => setConfig({ ...config, anonKey: event.target.value })}
            />
          </div>
          <button className="primary-button full-button" disabled={busy} onClick={saveConfig}>
            <Save size={17} /> Guardar conexión
          </button>
          <button className="secondary-button full-button" onClick={() => setShowGuide(true)}>
            <BookOpen size={17} /> Cómo crear y configurar Supabase
          </button>
          <div className="info-box">
            <ShieldCheck size={19} />
            <span>Usa solamente la clave pública. Nunca coloques aquí la clave service_role.</span>
          </div>
        </div>
      )}

      {sync.configured && !sync.signedIn && (
        <div className="sync-section">
          <div className="segmented">
            <button className={mode === "signin" ? "selected" : ""} onClick={() => setMode("signin")}>Iniciar sesión</button>
            <button className={mode === "signup" ? "selected" : ""} onClick={() => setMode("signup")}>Crear cuenta</button>
          </div>
          <div className="form-grid">
            <Field label="Correo" span={2} type="email" value={email} onChange={(event) => setEmail(event.target.value)} />
            <Field label="Contraseña" span={2} type="password" value={password} onChange={(event) => setPassword(event.target.value)} />
          </div>
          <button className="primary-button full-button" disabled={busy} onClick={authenticate}>
            {busy ? <RefreshCw className="spin" size={17} /> : <Cloud size={17} />}
            {mode === "signup" ? "Crear cuenta propia" : "Entrar y sincronizar"}
          </button>
          <button
            className="text-button"
            onClick={() => run(async () => {
              await sync.configure({ url: "", anonKey: "" });
              setConfig({ url: "", anonKey: "" });
            })}
          >
            Cambiar conexión de Supabase
          </button>
        </div>
      )}

      {sync.configured && sync.signedIn && (
        <div className="sync-section">
          <div className={`sync-account-card ${sync.state.phase}`}>
            <span><Cloud size={23} /></span>
            <div>
              <strong>{sync.session.user.email || "Cuenta conectada"}</strong>
              <small>{sync.state.message}</small>
            </div>
            <b>{sync.state.phase === "syncing" ? `${sync.state.progress}%` : sync.dirty ? "Pendiente" : "Al día"}</b>
          </div>
          {sync.state.phase === "syncing" && (
            <div className="sync-progress" aria-label={`Sincronización ${sync.state.progress}%`}>
              <i style={{ width: `${sync.state.progress}%` }} />
            </div>
          )}
          <div className="sync-details">
            <DetailRow label="Última sincronización" value={formatSyncDate(sync.meta.lastSyncedAt)} />
            <DetailRow label="Revisión en la nube" value={sync.meta.revision || "Sin respaldo"} />
            <DetailRow label="Cambios locales" value={sync.dirty ? "Pendientes de subir" : "Ninguno"} />
          </div>
          {sync.state.phase === "conflict" && (
            <div className="conflict-box">
              <strong>Se encontraron cambios en dos dispositivos</strong>
              <p>Elige qué información debe conservarse. Fusionar mantiene los registros de ambos y da prioridad a este dispositivo cuando coinciden.</p>
              <div>
                <button className="secondary-button" onClick={() => sync.resolveConflict("cloud")}>Usar nube</button>
                <button className="secondary-button" onClick={() => sync.resolveConflict("merge")}>Fusionar</button>
                <button className="primary-button" onClick={() => sync.resolveConflict("device")}>Usar este equipo</button>
              </div>
            </div>
          )}
          <div className="modal-actions">
            <button className="danger-text secondary-button" onClick={() => sync.signOut()}>Cerrar sesión</button>
            <button className="primary-button" disabled={sync.state.phase === "syncing"} onClick={() => sync.syncNow()}>
              <RefreshCw className={sync.state.phase === "syncing" ? "spin" : ""} size={17} /> Sincronizar ahora
            </button>
          </div>
        </div>
      )}

      {(formError || sync.state.error) && (
        <div className="sync-error"><CloudOff size={18} /><span>{formError || sync.state.error}</span></div>
      )}
    </Modal>
  );
}

function SetupStep({ number, title, children }) {
  return (
    <div className="setup-step">
      <span>{number}</span>
      <div><strong>{title}</strong><p>{children}</p></div>
    </div>
  );
}

async function copyText(value) {
  if (navigator.clipboard?.writeText) {
    await navigator.clipboard.writeText(value);
    return;
  }
  const textarea = document.createElement("textarea");
  textarea.value = value;
  textarea.style.position = "fixed";
  textarea.style.opacity = "0";
  document.body.appendChild(textarea);
  textarea.select();
  const copied = document.execCommand("copy");
  textarea.remove();
  if (!copied) throw new Error("Portapapeles no disponible");
}

async function sha256Hex(value) {
  if (!crypto.subtle) throw new Error("Hash seguro no disponible en este navegador.");
  const buffer = await crypto.subtle.digest("SHA-256", new TextEncoder().encode(value));
  return [...new Uint8Array(buffer)]
    .map((byte) => byte.toString(16).padStart(2, "0"))
    .join("");
}

function syncStatusLabel(sync) {
  if (!sync.configured) return "Supabase no configurado";
  if (!sync.signedIn) return "Sin cuenta conectada";
  if (sync.state.phase === "syncing") return "Sincronizando";
  if (sync.state.phase === "conflict") return "Requiere revisión";
  if (sync.state.phase === "error") return "Error de sincronización";
  if (sync.dirty) return "Cambios pendientes";
  return "Sincronizado";
}

function formatSyncDate(value) {
  if (!value) return "Aún no realizada";
  const date = new Date(value);
  return Number.isNaN(date.getTime()) ? "Aún no realizada" : date.toLocaleString("es-PE");
}

function SettingsModal({ store, onClose, notify }) {
  const [form, setForm] = useState(store.data.settings);
  return (
    <Modal title="Configuración" onClose={onClose}>
      <div className="form-grid">
        <Field label="IGV" type="number" value={form.igvRate} onChange={(e) => setForm({ ...form, igvRate: Number(e.target.value) })} />
        <Field label="Alias del suministro" value={form.supplyAlias} onChange={(e) => setForm({ ...form, supplyAlias: e.target.value })} />
        <Field label="Titular" value={form.accountHolder} onChange={(e) => setForm({ ...form, accountHolder: e.target.value })} />
        <Field label="Día de recordatorio" type="number" value={form.reminderDay} onChange={(e) => setForm({ ...form, reminderDay: Number(e.target.value) })} />
        <Field label="Avisar respaldo cada N días" type="number" value={form.backupReminderDays} onChange={(e) => setForm({ ...form, backupReminderDays: Number(e.target.value) })} />
      </div>
      <SwitchRow label="Redondear pagos hacia arriba a décimas" checked={form.roundUpToTenth} onChange={(value) => setForm({ ...form, roundUpToTenth: value })} />
      <ModalActions onClose={onClose} onSave={() => { store.saveSettings(form); notify("Configuración guardada"); onClose(); }} />
    </Modal>
  );
}

function AlertsPanel({ alerts }) {
  if (!alerts.length) {
    return (
      <section className="alerts-panel clear">
        <CheckCircle2 size={18} />
        <div><strong>Todo listo para este periodo</strong><span>No hay lecturas pendientes, deudas anteriores ni avisos urgentes.</span></div>
      </section>
    );
  }
  return (
    <section className="alerts-grid">
      {alerts.map((alert) => (
        <article className={`alert-card ${alert.tone}`} key={`${alert.title}-${alert.text}`}>
          <AlertTriangle size={18} />
          <div><strong>{alert.title}</strong><span>{alert.text}</span></div>
        </article>
      ))}
    </section>
  );
}

function AuditModal({ data, onClose }) {
  const events = data.auditEvents || [];
  const actionLabel = {
    create: "Creado",
    edit: "Editado",
    delete: "Eliminado",
    settings: "Configuración",
    import: "Importación",
    backup: "Respaldo"
  };
  const collectionLabel = {
    users: "Usuarios",
    receipts: "Recibos",
    readings: "Lecturas",
    services: "Servicios",
    payments: "Pagos",
    settings: "Ajustes",
    backup: "Respaldo"
  };
  return (
    <Modal title="Historial de cambios" onClose={onClose} wide>
      {events.length ? (
        <div className="audit-list">
          {events.map((event) => (
            <article key={event.id}>
              <span><History size={17} /></span>
              <div>
                <strong>{actionLabel[event.action] || event.action} · {collectionLabel[event.collection] || event.collection}</strong>
                <small>{event.label || "Sin detalle"} · {formatSyncDate(event.createdAt)}</small>
              </div>
            </article>
          ))}
        </div>
      ) : (
        <EmptyState icon={History} title="Aún no hay historial" text="Los cambios nuevos aparecerán aquí automáticamente." compact />
      )}
    </Modal>
  );
}

function AboutModal({ onClose }) {
  return (
    <Modal title="Acerca de" onClose={onClose}>
      <div className="about-card">
        <img src="/app-icon.png" alt="" />
        <div>
          <h3>{APP_NAME}</h3>
          <p>Versión {APP_VERSION}</p>
          <a href={CREATOR_WEBSITE} target="_blank" rel="noreferrer">{CREATOR_WEBSITE}</a>
        </div>
      </div>
      <div className="info-box">
        <Info size={19} />
        <span>Aplicación para controlar consumos eléctricos, servicios compartidos, pagos pendientes y respaldos locales o protegidos.</span>
      </div>
    </Modal>
  );
}

function SecurityModal({ store, onClose, notify }) {
  const hasPin = Boolean(store.data.settings.appLockPinHash);
  const [currentPin, setCurrentPin] = useState("");
  const [pin, setPin] = useState("");
  const [confirmPin, setConfirmPin] = useState("");
  const [error, setError] = useState("");
  const save = async () => {
    setError("");
    if (pin.length < 4 || pin !== confirmPin) {
      setError("El PIN debe tener al menos 4 dígitos y coincidir.");
      return;
    }
    if (hasPin && (await sha256Hex(currentPin)) !== store.data.settings.appLockPinHash) {
      setError("El PIN actual no coincide.");
      return;
    }
    store.saveSettings({
      ...store.data.settings,
      appLockPinHash: await sha256Hex(pin),
      appLockUpdatedAt: new Date().toISOString()
    });
    notify("Bloqueo por PIN actualizado");
    onClose();
  };
  const remove = async () => {
    setError("");
    if (hasPin && (await sha256Hex(currentPin)) !== store.data.settings.appLockPinHash) {
      setError("El PIN actual no coincide.");
      return;
    }
    store.saveSettings({ ...store.data.settings, appLockPinHash: "", appLockUpdatedAt: "" });
    notify("Bloqueo por PIN desactivado");
    onClose();
  };
  return (
    <Modal title="Seguridad" onClose={onClose}>
      <div className="security-panel">
        <LockKeyhole size={22} />
        <div>
          <strong>{hasPin ? "Bloqueo activo" : "Bloqueo desactivado"}</strong>
          <span>Protege la apertura de la app con un PIN local. Este PIN no se sube a la nube.</span>
        </div>
      </div>
      <div className="form-grid">
        {hasPin && <Field label="PIN actual" type="password" value={currentPin} onChange={(e) => setCurrentPin(e.target.value)} />}
        <Field label="Nuevo PIN" type="password" value={pin} onChange={(e) => setPin(e.target.value)} />
        <Field label="Repetir PIN" type="password" value={confirmPin} onChange={(e) => setConfirmPin(e.target.value)} />
      </div>
      {error && <div className="sync-error"><AlertTriangle size={18} /><span>{error}</span></div>}
      <div className="modal-actions split-actions">
        {hasPin && <button className="text-button danger-text" onClick={remove}>Desactivar PIN</button>}
        <span />
        <button className="secondary-button" onClick={onClose}>Cancelar</button>
        <button className="primary-button" onClick={save}><Save size={17} /> Guardar PIN</button>
      </div>
    </Modal>
  );
}

function LockScreen({ expectedHash, onUnlock }) {
  const [pin, setPin] = useState("");
  const [error, setError] = useState("");
  const unlock = async (event) => {
    event.preventDefault();
    if ((await sha256Hex(pin)) === expectedHash) {
      onUnlock();
      return;
    }
    setError("PIN incorrecto");
    setPin("");
  };
  return (
    <main className="lock-screen">
      <form onSubmit={unlock}>
        <img src="/app-icon.png" alt="" />
        <h1>{APP_NAME}</h1>
        <p>Ingresa el PIN para abrir tus datos.</p>
        <input value={pin} onChange={(event) => setPin(event.target.value)} type="password" inputMode="numeric" autoFocus />
        {error && <span>{error}</span>}
        <button className="primary-button" type="submit"><LockKeyhole size={17} /> Desbloquear</button>
      </form>
    </main>
  );
}

function OnboardingModal({ onClose, onOpen }) {
  return (
    <Modal title="Primeros pasos" onClose={onClose}>
      <div className="onboarding-list">
        <button onClick={() => onOpen("users")}><Users size={18} /><span><strong>Crear usuarios</strong><small>Define quién paga, quién está activo y quién es residual.</small></span></button>
        <button onClick={() => onOpen("receipt")}><FileText size={18} /><span><strong>Cargar recibo</strong><small>Importa el PDF o completa los importes manualmente.</small></span></button>
        <button onClick={() => onOpen("reading")}><Gauge size={18} /><span><strong>Registrar lecturas</strong><small>La lectura anterior se sugiere desde el periodo previo.</small></span></button>
        <button onClick={() => onOpen("backup")}><HardDrive size={18} /><span><strong>Guardar respaldo</strong><small>Conserva una copia JSON, CSV o protegida con clave.</small></span></button>
      </div>
      <div className="modal-actions">
        <button className="primary-button" onClick={onClose}>Empezar</button>
      </div>
    </Modal>
  );
}

function ShareSummaryPage({ data, dark }) {
  const params = new URLSearchParams(window.location.search);
  const periods = availablePeriods(data);
  const selectedPeriod = periods.includes(params.get("period")) ? params.get("period") : periods[0] || "";
  const selectedUser = params.get("user") || ALL_USERS;
  const summary = calculatePeriod(selectedPeriod, data);
  const ledger = buildPaymentLedger(periods, data);
  const visibleResults = selectedUser === ALL_USERS
    ? summary.results
    : summary.results.filter((result) => result.userId === selectedUser);
  const total = selectedUser === ALL_USERS
    ? totalOutstandingForPeriod(selectedPeriod, summary.results, ledger)
    : ledger.get(`${selectedPeriod}|${selectedUser}`)?.remainingBalance || visibleResults[0]?.finalTotalWithServices || 0;
  const activeServices = data.services.filter((service) => service.period === selectedPeriod && service.isActive !== false);

  return (
    <div className={dark ? "app dark share-root" : "app share-root"}>
      <main className="share-shell">
        <header>
          <img src="/app-icon.png" alt="" />
          <div>
            <h1>{APP_NAME}</h1>
            <p>Resumen de solo lectura · {selectedPeriod || "Sin periodo"}</p>
          </div>
          <strong>{money(total)}</strong>
        </header>
        {visibleResults.length ? (
          <section className="share-list">
            {visibleResults.map((result) => {
              const balance = ledger.get(`${selectedPeriod}|${result.userId}`);
              const services = activeServices
                .map((service) => ({ name: service.name, amount: serviceShareForUser(service, result.userId) }))
                .filter((service) => service.amount > 0);
              return (
                <article key={result.userId}>
                  <h2>{result.userName || result.userId}</h2>
                  <DetailRow label="Consumo en kWh" value={kwh(result.consumptionKwh)} />
                  <DetailRow label="Total a pagar por consumo eléctrico" value={money(result.finalTotal)} />
                  {services.map((service) => <DetailRow key={service.name} label={service.name} value={money(service.amount)} />)}
                  {balance?.previousDebtItems?.map((item) => (
                    <DetailRow key={item.period} label={`Deuda anterior ${item.period}`} value={money(item.remainingAmount)} />
                  ))}
                  <DetailRow label="Total a pagar" value={money(balance?.remainingBalance ?? result.finalTotalWithServices)} important />
                </article>
              );
            })}
          </section>
        ) : (
          <EmptyState icon={BookOpen} title="Resumen no disponible" text="No hay datos suficientes para esta vista." compact />
        )}
      </main>
    </div>
  );
}

function MetricCard({ icon: Icon, label, value, tone }) {
  return <article className={`metric-card ${tone}`}><span><Icon size={21} /></span><div><small>{label}</small><strong>{value}</strong></div></article>;
}

function Panel({ title, children, className = "" }) {
  return <section className={`panel ${className}`}><div className="panel-title"><h2>{title}</h2></div>{children}</section>;
}

function DetailRow({ label, value, important }) {
  return <div className={important ? "detail-row important" : "detail-row"}><span>{label}</span><strong>{value}</strong></div>;
}

function StatusPill({ status }) {
  const label = status === "ACTIVE" ? "Activo" : status === "INACTIVE" ? "Inactivo" : paymentStatusLabel(status);
  return <span className={`status-pill ${String(status || "empty").toLowerCase()}`}>{label}</span>;
}

function EmptyState({ icon: Icon, title, text, compact }) {
  return <section className={compact ? "empty-state compact" : "empty-state"}><span><Icon size={30} /></span><h2>{title}</h2><p>{text}</p></section>;
}

function Modal({ title, onClose, children, wide }) {
  return (
    <div className="modal-backdrop" onMouseDown={(e) => e.target === e.currentTarget && onClose()}>
      <section className={wide ? "modal wide" : "modal"}>
        <header><h2>{title}</h2><button className="icon-button" onClick={onClose}><X size={20} /></button></header>
        <div className="modal-body">{children}</div>
      </section>
    </div>
  );
}

function ModalActions({ onClose, onSave, disabled }) {
  return <div className="modal-actions"><button className="secondary-button" onClick={onClose}>Cancelar</button><button className="primary-button" disabled={disabled} onClick={onSave}><Save size={17} /> Guardar</button></div>;
}

function Field({ label, span, ...props }) {
  return <label className={span ? `field span-${span}` : "field"}><span>{label}</span><input step="any" {...props} /></label>;
}

function SelectField({ label, children, ...props }) {
  return <label className="field"><span>{label}</span><select {...props}>{children}</select></label>;
}

function SwitchRow({ label, checked, onChange }) {
  return <label className="switch-row"><span>{label}</span><input type="checkbox" checked={checked} onChange={(e) => onChange(e.target.checked)} /><i /></label>;
}

createRoot(document.getElementById("root")).render(<App />);

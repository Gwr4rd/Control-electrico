import * as pdfjsLib from "pdfjs-dist";
import workerUrl from "pdfjs-dist/build/pdf.worker.min.mjs?url";

pdfjsLib.GlobalWorkerOptions.workerSrc = workerUrl;

const MONTHS = {
  ENERO: "01",
  FEBRERO: "02",
  MARZO: "03",
  ABRIL: "04",
  MAYO: "05",
  JUNIO: "06",
  JULIO: "07",
  AGOSTO: "08",
  SETIEMBRE: "09",
  SEPTIEMBRE: "09",
  OCTUBRE: "10",
  NOVIEMBRE: "11",
  DICIEMBRE: "12"
};

function normalize(value) {
  return String(value || "")
    .normalize("NFD")
    .replace(/[\u0300-\u036f]/g, "")
    .replace(/\s+/g, " ")
    .trim();
}

function numberAtRight(lines, labels) {
  const wanted = labels.map((label) => normalize(label).toLowerCase());
  for (const line of lines) {
    const text = normalize(line.text).toLowerCase();
    if (!wanted.some((label) => text.includes(label))) continue;
    const matches = line.text.match(/\d+(?:[.,]\d{1,4})/g) || [];
    if (matches.length) return Number(matches.at(-1).replace(",", "."));
  }
  return null;
}

export async function parseReceiptPdf(file) {
  const bytes = new Uint8Array(await file.arrayBuffer());
  const document = await pdfjsLib.getDocument({ data: bytes }).promise;
  const page = await document.getPage(1);
  const content = await page.getTextContent();
  const rawItems = content.items
    .filter((item) => item.str?.trim())
    .map((item) => ({
      text: item.str.trim(),
      x: item.transform[4],
      y: item.transform[5]
    }));
  const grouped = [];
  rawItems
    .sort((a, b) => b.y - a.y || a.x - b.x)
    .forEach((item) => {
      let line = grouped.find((candidate) => Math.abs(candidate.y - item.y) <= 3);
      if (!line) {
        line = { y: item.y, items: [] };
        grouped.push(line);
      }
      line.items.push(item);
    });
  const lines = grouped
    .sort((a, b) => b.y - a.y)
    .map((line) => ({
      y: line.y,
      text: line.items.sort((a, b) => a.x - b.x).map((item) => item.text).join(" ")
    }));
  const fullText = lines.map((line) => line.text).join("\n");
  const normalized = normalize(fullText);

  const periodMatch = normalized.match(
    /MES FACTURADO\s+(ENERO|FEBRERO|MARZO|ABRIL|MAYO|JUNIO|JULIO|AGOSTO|SETIEMBRE|SEPTIEMBRE|OCTUBRE|NOVIEMBRE|DICIEMBRE)\s+(\d{4})/i
  );
  const period = periodMatch
    ? `${periodMatch[2]}-${MONTHS[periodMatch[1].toUpperCase()]}`
    : "";
  const supplyNumber =
    normalized.match(/N[°º]?\s*DE\s*SUMINISTRO\s*(\d{5,12})/i)?.[1] || "";
  const dateCandidates = fullText.match(/\d{1,2}\/(?:Ene|Feb|Mar|Abr|May|Jun|Jul|Ago|Set|Sep|Oct|Nov|Dic)\/\d{2,4}/gi) || [];
  const externalReadingDate = dateCandidates[0] || "";

  let priceUpTo30 = numberAtRight(lines, ["Valor de los primeros 30 kWh", "Precio kWh"]);
  let priceOver30 = numberAtRight(lines, ["Valor consumo mayor a los primeros 30 kWh"]);
  const singlePrice = numberAtRight(lines, ["Precio por kWh"]);
  if (singlePrice && !priceOver30) priceUpTo30 = singlePrice;

  const externalKwh =
    numberAtRight(lines, ["Energía a facturar", "Energia a facturar"]) ||
    Number(normalized.match(/=\s*(\d+(?:[.,]\d+)?)\s*kWh/i)?.[1]?.replace(",", ".")) ||
    0;

  return {
    period,
    externalReadingDate,
    supplyNumber,
    externalKwh,
    monthlyBill: numberAtRight(lines, ["TOTAL DEL MES"]) || 0,
    priceKwhUpTo30: priceUpTo30 || 0,
    priceKwhOver30: priceOver30 || 0,
    fixedCharge: numberAtRight(lines, ["Cargo Fijo"]) || 0,
    maintenance:
      numberAtRight(lines, ["Mant. y Reposición de Conexión", "Mant. y Reposicion de Conexion"]) || 0,
    publicLighting: numberAtRight(lines, ["Alumbrado Público", "Alumbrado Publico"]) || 0,
    ruralElectrification:
      numberAtRight(lines, [
        "Electrificación Rural",
        "Electrificacion Rural",
        "Aporte Ley N° 28749",
        "Aporte Ley N 28749"
      ]) || 0,
    notes: `Importado desde ${file.name}`
  };
}

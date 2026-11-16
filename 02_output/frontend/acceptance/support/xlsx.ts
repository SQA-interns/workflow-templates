// Reads the first sheet of an .xlsx file into rows of text (export-xlsx.schema.json), with Node's
// zlib only: an .xlsx is a zip of XML parts. Enough for the Export files of this product.
import { inflateRawSync } from "node:zlib";

function entries(zip: Buffer): Map<string, Buffer> {
  let end = zip.length - 22;
  while (end >= 0 && zip.readUInt32LE(end) !== 0x06054b50) end -= 1;
  if (end < 0) throw new Error("not a zip file");
  const count = zip.readUInt16LE(end + 10);
  let offset = zip.readUInt32LE(end + 16);
  const files = new Map<string, Buffer>();
  for (let i = 0; i < count; i += 1) {
    const method = zip.readUInt16LE(offset + 10);
    const compressedSize = zip.readUInt32LE(offset + 20);
    const nameLength = zip.readUInt16LE(offset + 28);
    const extraLength = zip.readUInt16LE(offset + 30);
    const commentLength = zip.readUInt16LE(offset + 32);
    const local = zip.readUInt32LE(offset + 42);
    const name = zip.toString("utf8", offset + 46, offset + 46 + nameLength);
    const dataStart = local + 30 + zip.readUInt16LE(local + 26) + zip.readUInt16LE(local + 28);
    const data = zip.subarray(dataStart, dataStart + compressedSize);
    files.set(name, method === 8 ? inflateRawSync(data) : Buffer.from(data));
    offset += 46 + nameLength + extraLength + commentLength;
  }
  return files;
}

function unescapeXml(text: string): string {
  return text
    .replace(/&lt;/g, "<")
    .replace(/&gt;/g, ">")
    .replace(/&quot;/g, '"')
    .replace(/&apos;/g, "'")
    .replace(/&#(\d+);/g, (_, code: string) => String.fromCodePoint(Number(code)))
    .replace(/&amp;/g, "&");
}

function texts(xml: string): string {
  return [...xml.matchAll(/<t\b[^>]*>([^<]*)<\/t>/g)].map((m) => unescapeXml(m[1] ?? "")).join("");
}

function column(reference: string): number {
  const letters = /^[A-Z]+/.exec(reference)?.[0] ?? "A";
  return [...letters].reduce((n, c) => n * 26 + c.charCodeAt(0) - 64, 0) - 1;
}

/** The rows of the first sheet, every cell as text; the first row is the header. */
export function readXlsx(file: Buffer): string[][] {
  const files = entries(file);
  const shared = [
    ...(files.get("xl/sharedStrings.xml")?.toString("utf8") ?? "").matchAll(
      /<si>([\s\S]*?)<\/si>/g,
    ),
  ].map((m) => texts(m[1] ?? ""));
  const sheetName = [...files.keys()]
    .filter((n) => /^xl\/worksheets\/sheet\d+\.xml$/.test(n))
    .sort()[0];
  if (!sheetName) throw new Error("no sheet");
  const sheet = files.get(sheetName)?.toString("utf8") ?? "";
  const rows: string[][] = [];
  for (const row of sheet.matchAll(/<row[^>]*>([\s\S]*?)<\/row>/g)) {
    const cells: string[] = [];
    for (const cell of (row[1] ?? "").matchAll(/<c\s([^>]*?)(?:\/>|>([\s\S]*?)<\/c>)/g)) {
      const attributes = cell[1] ?? "";
      const body = cell[2] ?? "";
      const index = column(/r="([A-Z]+\d+)"/.exec(attributes)?.[1] ?? "A1");
      const type = /t="([^"]+)"/.exec(attributes)?.[1];
      const value = /<v>([\s\S]*?)<\/v>/.exec(body)?.[1];
      let text = "";
      if (type === "s" && value !== undefined) text = shared[Number(value)] ?? "";
      else if (type === "inlineStr") text = texts(body);
      else if (value !== undefined) text = unescapeXml(value);
      while (cells.length < index) cells.push("");
      cells.splice(index, 1, text);
    }
    rows.push(cells);
  }
  return rows;
}

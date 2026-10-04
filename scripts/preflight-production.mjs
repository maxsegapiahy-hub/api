import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const pkg = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
const eas = JSON.parse(fs.readFileSync(path.join(root, "eas.json"), "utf8"));
const envFile = path.join(root, ".env.production");

function loadEnv(file) {
  const out = {};
  if (!fs.existsSync(file)) return out;
  for (const line of fs.readFileSync(file, "utf8").split(/\\r?\\n/)) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith("#")) continue;
    const match = trimmed.match(/^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/);
    if (!match) continue;
    let value = match[2].trim();
    if ((value.startsWith('"') && value.endsWith('"')) || (value.startsWith("'") && value.endsWith("'"))) value = value.slice(1, -1);
    out[match[1]] = value;
  }
  return out;
}

const env = { ...loadEnv(envFile), ...process.env };
const errors = [];
const warnings = [];

function required(name, predicate, message) {
  const value = String(env[name] || "").trim();
  if (!value) errors.push(`${name}: ausente`);
  else if (!predicate(value)) errors.push(`${name}: ${message}`);
}

required("EXPO_PROJECT_ID", v => /^[0-9a-f]{8}-[0-9a-f]{4}-[1-5][0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$/i.test(v), "deve ser um UUID de projeto Expo/EAS real");
required("MAXSEG_AUTH_SECRET", v => v.length >= 32, "deve ter pelo menos 32 caracteres");
required("MAXSEG_API_KEY", v => v.length >= 24, "deve ter pelo menos 24 caracteres");
required("MAXSEG_ADMIN_KEY", v => v.length >= 24, "deve ter pelo menos 24 caracteres");
required("MAXSEG_PUBLIC_BASE_URL", v => /^https:\\/\\//i.test(v) && !v.endsWith("/"), "deve usar HTTPS e não terminar com /");
required("EXPO_PUBLIC_API_URL", v => /^https:\\/\\//i.test(v) && !v.endsWith("/"), "deve usar HTTPS e não terminar com /");
required("MAXSEG_CORS_ORIGIN", v => /^https:\\/\\//i.test(v) && v !== "*", "deve ser uma origem HTTPS explícita, não *");

if (env.MAXSEG_API_KEY && env.MAXSEG_ADMIN_KEY && env.MAXSEG_API_KEY === env.MAXSEG_ADMIN_KEY) {
  errors.push("MAXSEG_API_KEY e MAXSEG_ADMIN_KEY: devem ser diferentes");
}
if (env.MAXSEG_PUBLIC_BASE_URL && env.MAXSEG_CORS_ORIGIN && env.MAXSEG_PUBLIC_BASE_URL !== env.MAXSEG_CORS_ORIGIN) {
  warnings.push("MAXSEG_PUBLIC_BASE_URL e MAXSEG_CORS_ORIGIN são diferentes; confirme se isso é intencional.");
}
if (eas.build?.production?.android?.buildType !== "app-bundle") errors.push("eas.json: production deve gerar app-bundle");
if (eas.build?.production?.distribution !== "store") errors.push("eas.json: production deve usar distribution=store");

console.log(`MaxSeg produção preflight — versão ${pkg.version}`);
console.log(`Arquivo .env.production: ${fs.existsSync(envFile) ? "encontrado" : "não encontrado"}`);
console.log(`EAS production Android: ${eas.build?.production?.android?.buildType || "ausente"}`);
console.log(`EAS production distribution: ${eas.build?.production?.distribution || "ausente"}`);

if (warnings.length) {
  console.log("\nAvisos:");
  for (const warning of warnings) console.log(`- ${warning}`);
}
if (errors.length) {
  console.log("\nFalhas:");
  for (const error of errors) console.log(`- ${error}`);
  process.exit(1);
}
console.log("\nPRODUCTION PREFLIGHT: PASS");

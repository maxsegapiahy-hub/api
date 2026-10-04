import fs from "node:fs";
import path from "node:path";
import { execFileSync } from "node:child_process";

const root = process.cwd();
const pkg = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
const eas = JSON.parse(fs.readFileSync(path.join(root, "eas.json"), "utf8"));
const errors = [];
const warnings = [];

function commandExists(name) {
  try {
    execFileSync(process.platform === "win32" ? "where" : "which", [name], { stdio: "ignore" });
    return true;
  } catch { return false; }
}

const easAvailable = commandExists("eas");
const envFile = path.join(root, ".env.production");
const hasEnvFile = fs.existsSync(envFile);
const projectId = String(process.env.EXPO_PROJECT_ID || "").trim();

if (!eas.build?.production || eas.build.production.distribution !== "store") errors.push("eas.json: perfil production inválido");
if (eas.build?.production?.android?.buildType !== "app-bundle") errors.push("eas.json: production.android.buildType deve ser app-bundle");
if (!hasEnvFile && !projectId) warnings.push(".env.production não encontrado e EXPO_PROJECT_ID não foi fornecido no ambiente.");
if (!easAvailable) warnings.push("EAS CLI não está instalado/disponível neste ambiente; a build EAS real precisa ser executada no computador/CI autenticado.");

console.log(`MaxSeg Android build gate — v${pkg.version}`);
console.log(`EAS CLI: ${easAvailable ? "disponível" : "não disponível"}`);
console.log(`.env.production: ${hasEnvFile ? "presente" : "ausente"}`);
console.log(`EXPO_PROJECT_ID em runtime: ${projectId ? "presente" : "ausente"}`);
console.log(`EAS production: ${eas.build?.production?.distribution || "ausente"} / ${eas.build?.production?.android?.buildType || "ausente"}`);

if (warnings.length) {
  console.log("\nAvisos:");
  for (const item of warnings) console.log(`- ${item}`);
}
if (errors.length) {
  console.log("\nFalhas:");
  for (const item of errors) console.log(`- ${item}`);
  process.exit(1);
}

if (!easAvailable) {
  console.log("\nBUILD GATE: BLOCKED (infraestrutura local)");
  console.log("A configuração do projeto está pronta, mas a execução do EAS exige a CLI e autenticação em um ambiente com acesso ao Expo.");
  process.exit(2);
}

console.log("\nBUILD GATE: PASS");

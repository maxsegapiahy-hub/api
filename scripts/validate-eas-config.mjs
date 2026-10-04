import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const pkg = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
const eas = JSON.parse(fs.readFileSync(path.join(root, "eas.json"), "utf8"));
const appConfig = fs.readFileSync(path.join(root, "app.config.ts"), "utf8");
const errors = [];
const warnings = [];

const production = eas.build?.production;
if (!production) errors.push("eas.json: perfil production ausente");
if (production?.distribution !== "store") errors.push("eas.json: production.distribution deve ser store");
if (production?.android?.buildType !== "app-bundle") errors.push("eas.json: production.android.buildType deve ser app-bundle");
if (!appConfig.includes("process.env.EXPO_PROJECT_ID")) errors.push("app.config.ts: EXPO_PROJECT_ID não está conectado ao extra.eas.projectId");
if (!/^\\s*version:\s*"[^"]+"/m.test(appConfig)) errors.push("app.config.ts: versão não encontrada");
if (!pkg.version) errors.push("package.json: versão ausente");
if (appConfig.includes("SEU_PROJECT_ID")) errors.push("app.config.ts: placeholder de projectId detectado");

const bundle = appConfig.match(/rawBundleId\\s*=\\s*"([^"]+)"/i)?.[1] ?? appConfig.match(/bundleId\\s*=\\s*"([^"]+)"/i)?.[1];
if (!bundle || !/^[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)+$/.test(bundle)) {
  errors.push("app.config.ts: bundle identifier inválido ou ausente");
}

console.log(`MaxSeg EAS config validation — versão ${pkg.version}`);
console.log(`Android/iOS bundle: ${bundle || "ausente"}`);
console.log(`Production distribution: ${production?.distribution || "ausente"}`);
console.log(`Production Android build type: ${production?.android?.buildType || "ausente"}`);
console.log("projectId: fornecido em runtime via EXPO_PROJECT_ID");

if (warnings.length) {
  console.log("\nAvisos:");
  for (const item of warnings) console.log(`- ${item}`);
}
if (errors.length) {
  console.log("\nFalhas:");
  for (const item of errors) console.log(`- ${item}`);
  process.exit(1);
}
console.log("\nEAS CONFIG VALIDATION: PASS");

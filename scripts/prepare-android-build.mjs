import fs from "node:fs";
import path from "node:path";

const root = process.cwd();
const envPath = path.join(root, ".env.production");
const outDir = path.join(root, "build-artifacts");
const metadataPath = path.join(outDir, "android-production-preflight.json");

function loadEnv(file) {
  const out = {};
  if (!fs.existsSync(file)) return out;
  for (const line of fs.readFileSync(file, "utf8").split(/\\r?\\n/)) {
    const m = line.trim().match(/^([A-Za-z_][A-Za-z0-9_]*)=(.*)$/);
    if (!m) continue;
    out[m[1]] = m[2].trim().replace(/^(['"])(.*)\\1$/, "$2");
  }
  return out;
}

const env = { ...loadEnv(envPath), ...process.env };
const required = ["EXPO_PROJECT_ID", "EXPO_PUBLIC_API_URL", "MAXSEG_PUBLIC_BASE_URL"];
const missing = required.filter(key => {
  const value = String(env[key] || "").trim();
  return !value || value.includes("SEU_") || value.includes("seu-dominio");
});

if (missing.length) {
  console.error("ANDROID BUILD PREPARATION: BLOCKED");
  console.error(`Configure antes: ${missing.join(", ")}`);
  process.exit(1);
}
if (!/^https:\\/\\//i.test(env.EXPO_PUBLIC_API_URL) || !/^https:\\/\\//i.test(env.MAXSEG_PUBLIC_BASE_URL)) {
  console.error("ANDROID BUILD PREPARATION: BLOCKED");
  console.error("As URLs de produção precisam usar HTTPS.");
  process.exit(1);
}

const pkg = JSON.parse(fs.readFileSync(path.join(root, "package.json"), "utf8"));
fs.mkdirSync(outDir, { recursive: true });
const metadata = {
  version: pkg.version,
  platform: "android",
  profile: "production",
  distribution: "store",
  buildType: "app-bundle",
  preparedAt: new Date().toISOString(),
  projectIdConfigured: true,
  apiUrlConfigured: true,
  publicUrlConfigured: true,
  note: "Preflight local concluído. A build EAS real ainda exige login no Expo/EAS e execução do comando eas build."
};
fs.writeFileSync(metadataPath, JSON.stringify(metadata, null, 2) + "\n");
console.log(`ANDROID BUILD PREPARATION: PASS\\nMetadata: ${path.relative(root, metadataPath)}`);

import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const pkgPath = path.join(root, 'package.json');
const easPath = path.join(root, 'eas.json');
const appConfigPath = path.join(root, 'app.config.ts');
const errors = [];

for (const [label, file] of [
  ['package.json', pkgPath],
  ['eas.json', easPath],
  ['app.config.ts', appConfigPath],
]) {
  if (!fs.existsSync(file)) errors.push(`Arquivo obrigatório ausente: ${label}`);
}

if (errors.length) {
  console.error('EAS CONFIG VALIDATION: FAIL');
  for (const error of errors) console.error(` - ${error}`);
  process.exit(1);
}

let pkg;
let eas;
try {
  pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
  eas = JSON.parse(fs.readFileSync(easPath, 'utf8'));
} catch {
  console.error('package.json ou eas.json contém JSON inválido.');
  process.exit(1);
}

const appConfig = fs.readFileSync(appConfigPath, 'utf8');
const production = eas.build?.production;
const preview = eas.build?.preview;

if (!pkg.version) errors.push('package.json: versão ausente');
if (!/^\\s*version:\\s*["'][^"']+["']/m.test(appConfig)) errors.push('app.config.ts: versão ausente');
if (!appConfig.includes('process.env.EXPO_PROJECT_ID')) errors.push('app.config.ts: extra.eas.projectId deve usar EXPO_PROJECT_ID');
if (!preview) errors.push('eas.json: perfil preview ausente');
if (!production) errors.push('eas.json: perfil production ausente');
if (preview?.android?.buildType !== 'apk') errors.push('eas.json: preview.android.buildType deve ser apk');
if (production?.distribution !== 'store') errors.push('eas.json: production.distribution deve ser store');
if (production?.android?.buildType !== 'app-bundle') errors.push('eas.json: production.android.buildType deve ser app-bundle');

const rawBundleId = appConfig.match(/rawBundleId\\s*=\\s*["']([^"']+)["']/)?.[1];
if (!rawBundleId || !/^[a-zA-Z][a-zA-Z0-9]*(\\.[a-zA-Z][a-zA-Z0-9]*)+$/.test(rawBundleId)) {
  errors.push('app.config.ts: rawBundleId ausente ou inválido');
}

console.log(`MaxSeg EAS config validation — versão ${pkg.version}`);
console.log(`Project ID via ambiente: ${String(process.env.EXPO_PROJECT_ID || '').trim() ? 'configurado no terminal' : 'ainda não configurado (necessário para build)'}`);
console.log('Esta verificação não lê nem exige segredos do arquivo .env.');

if (errors.length) {
  console.error('\\nFalhas:');
  for (const error of errors) console.error(` - ${error}`);
  process.exit(1);
}

console.log('\\nEAS CONFIG VALIDATION: PASS');

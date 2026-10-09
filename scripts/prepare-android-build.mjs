import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const easPath = path.join(root, 'eas.json');
const appConfigPath = path.join(root, 'app.config.ts');
const pkgPath = path.join(root, 'package.json');
const errors = [];

for (const [label, file] of [
  ['package.json', pkgPath],
  ['eas.json', easPath],
  ['app.config.ts', appConfigPath],
]) {
  if (!fs.existsSync(file)) errors.push(`Arquivo obrigatório ausente: ${label}`);
}

if (errors.length) {
  console.error('EAS hosted-build preparation: BLOCKED');
  for (const error of errors) console.error(` - ${error}`);
  process.exit(1);
}

const pkg = JSON.parse(fs.readFileSync(pkgPath, 'utf8'));
let eas;
try {
  eas = JSON.parse(fs.readFileSync(easPath, 'utf8'));
} catch {
  console.error('EAS hosted-build preparation: eas.json contém JSON inválido.');
  process.exit(1);
}

for (const profile of ['preview', 'production']) {
  if (!eas.build?.[profile]) errors.push(`Perfil EAS ausente: ${profile}`);
}
if (eas.build?.preview?.android?.buildType !== 'apk') {
  errors.push('O perfil preview do Android deve gerar APK para teste.');
}
if (eas.build?.production?.android?.buildType !== 'app-bundle') {
  errors.push('O perfil production do Android deve gerar app-bundle.');
}

if (errors.length) {
  console.error('EAS hosted-build preparation: BLOCKED');
  for (const error of errors) console.error(` - ${error}`);
  process.exit(1);
}

const appConfig = fs.readFileSync(appConfigPath, 'utf8');
const projectIdPresent = !!String(process.env.EXPO_PROJECT_ID || '').trim();
const projectIdWired = appConfig.includes('process.env.EXPO_PROJECT_ID');

console.log(`MaxSeg EAS preparation — version ${pkg.version}`);
console.log('Build mode: EAS hosted build (compilation on Expo servers)');
console.log('Android SDK locally: not required for hosted EAS builds');
console.log('JDK locally: not required for hosted EAS builds');
console.log('Preview Android: APK');
console.log('Production Android: app-bundle');
console.log(`EXPO_PROJECT_ID wired in app.config.ts: ${projectIdWired ? 'yes' : 'no'}`);
console.log(`EXPO_PROJECT_ID set in current terminal: ${projectIdPresent ? 'yes' : 'no'}`);

if (!projectIdWired) {
  console.error('Ajuste app.config.ts para vincular extra.eas.projectId ao ID real do projeto EAS.');
  process.exit(1);
}

if (!projectIdPresent) {
  console.warn('\nAtenção: a preparação de arquivos passou, mas ainda NÃO é possível iniciar o build até vincular o projeto Expo existente e configurar EXPO_PROJECT_ID.');
}

console.log('\nEAS HOSTED-BUILD PREPARATION: PASS');
console.log('Esta verificação não executa nem confirma uma build real.');

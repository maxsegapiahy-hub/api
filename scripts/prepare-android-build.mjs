import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const androidHome = process.env.ANDROID_HOME || process.env.ANDROID_SDK_ROOT;
const javaHome = process.env.JAVA_HOME;

const checks = [
  { label: 'Android SDK', ok: !!androidHome && fs.existsSync(String(androidHome)) },
  { label: 'JDK', ok: !!javaHome && fs.existsSync(String(javaHome)) },
  { label: 'eas.json', ok: fs.existsSync(path.join(root, 'eas.json')) },
  { label: 'app.config.ts', ok: fs.existsSync(path.join(root, 'app.config.ts')) },
];

const failed = checks.filter((entry) => !entry.ok);

if (failed.length > 0) {
  console.error('Android local preparation checks failed:');
  for (const entry of failed) {
    console.error(` - ${entry.label}`);
  }
  console.error('\nInstall Android SDK / JDK locally or configure ANDROID_HOME and JAVA_HOME before building.');
  process.exit(1);
}

console.log('Android local preparation checks passed.');
console.log('ANDROID_HOME/JAVA_HOME are configured and required files exist.');

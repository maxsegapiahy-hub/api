import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const easPath = path.join(root, 'eas.json');
const appConfigPath = path.join(root, 'app.config.ts');

if (!fs.existsSync(easPath)) {
  console.error('Missing eas.json');
  process.exit(1);
}

if (!fs.existsSync(appConfigPath)) {
  console.error('Missing app.config.ts');
  process.exit(1);
}

let eas;
try {
  eas = JSON.parse(fs.readFileSync(easPath, 'utf8'));
} catch (error) {
  console.error('eas.json is invalid JSON.');
  process.exit(1);
}

const production = eas?.build?.production;
if (!production) {
  console.error('Missing build.production section in eas.json.');
  process.exit(1);
}

const appConfigText = fs.readFileSync(appConfigPath, 'utf8');
const requiredAppKeys = ['slug', 'version', 'extra', 'ios', 'android'];
const missingAppKeys = requiredAppKeys.filter((key) => !appConfigText.includes(key));

if (missingAppKeys.length > 0) {
  console.error('app.config.ts does not appear to include required Expo configuration keys:');
  for (const key of missingAppKeys) {
    console.error(` - ${key}`);
  }
  process.exit(1);
}

console.log('Production EAS profile is configured.');
console.log('App config contains the required Expo configuration sections.');

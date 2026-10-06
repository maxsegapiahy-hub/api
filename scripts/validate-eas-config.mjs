import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const required = [
  'EXPO_PROJECT_ID',
  'MAXSEG_AUTH_SECRET',
  'MAXSEG_API_KEY',
  'MAXSEG_ADMIN_KEY',
  'MAXSEG_PUBLIC_BASE_URL',
  'EXPO_PUBLIC_API_URL',
  'MAXSEG_CORS_ORIGIN',
  'EXPO_PUBLIC_APP_ID',
  'EXPO_PUBLIC_OAUTH_PORTAL_URL',
  'EXPO_PUBLIC_OAUTH_SERVER_URL',
  'EXPO_PUBLIC_OWNER_OPEN_ID',
  'EXPO_PUBLIC_OWNER_NAME',
];

const missing = required.filter((key) => !process.env[key] || String(process.env[key]).trim() === '');

if (fs.existsSync(path.join(root, '.env'))) {
  const envText = fs.readFileSync(path.join(root, '.env'), 'utf8');
  const lines = envText.split(/\r?\n/);
  for (const line of lines) {
    const trimmed = line.trim();
    if (!trimmed || trimmed.startsWith('#')) continue;
    const match = trimmed.match(/^([^=]+)=(.*)$/);
    if (!match) continue;
    const key = match[1].trim();
    const value = match[2].trim();
    if (required.includes(key) && value && !process.env[key]) {
      process.env[key] = value;
    }
  }
}

const finalMissing = required.filter((key) => !process.env[key] || String(process.env[key]).trim() === '');

if (finalMissing.length > 0) {
  console.error('Missing required EAS environment variables:');
  for (const key of finalMissing) {
    console.error(` - ${key}`);
  }
  console.error('\nCreate a local .env file or set the variables in your shell before running the EAS build.');
  process.exit(1);
}

const easPath = path.join(root, 'eas.json');
const configPath = path.join(root, 'app.config.ts');

if (!fs.existsSync(easPath)) {
  console.error('Missing eas.json at project root.');
  process.exit(1);
}

if (!fs.existsSync(configPath)) {
  console.error('Missing app.config.ts at project root.');
  process.exit(1);
}

try {
  JSON.parse(fs.readFileSync(easPath, 'utf8'));
} catch (error) {
  console.error('eas.json is not valid JSON.');
  process.exit(1);
}

console.log('EAS configuration validation passed.');
console.log('Required environment variables are present and non-empty.');

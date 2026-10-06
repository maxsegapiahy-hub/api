import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const envPath = path.join(root, '.env');

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

if (!fs.existsSync(envPath)) {
  console.error('❌ Local .env not found. Create it before runtime validation.');
  process.exit(1);
}

const envText = fs.readFileSync(envPath, 'utf8');
const missing = [];

for (const key of required) {
  const regex = new RegExp(`(?:^|\\n)${key}=(.*)`, 'm');
  const match = envText.match(regex);
  const value = match ? match[1].trim() : '';

  if (!value) {
    missing.push(key);
  }
}

if (missing.length > 0) {
  console.error('❌ Missing runtime environment variables:');
  for (const key of missing) console.error(`   - ${key}`);
  process.exit(1);
}

console.log('✓ Runtime environment validation passed.');

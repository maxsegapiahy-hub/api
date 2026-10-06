import fs from 'node:fs';
import path from 'node:path';

const root = process.cwd();
const required = ['eas.json', 'app.config.ts', 'package.json'];
const missing = required.filter((file) => !fs.existsSync(path.join(root, file)));

if (missing.length > 0) {
  console.error('Missing required files for the EAS gate:');
  for (const file of missing) {
    console.error(` - ${file}`);
  }
  process.exit(1);
}

const envFile = path.join(root, '.env');
if (fs.existsSync(envFile)) {
  const envText = fs.readFileSync(envFile, 'utf8');
  const requiredVars = [
    'EXPO_PROJECT_ID',
    'MAXSEG_AUTH_SECRET',
    'MAXSEG_API_KEY',
    'MAXSEG_ADMIN_KEY',
    'MAXSEG_PUBLIC_BASE_URL',
    'EXPO_PUBLIC_API_URL',
    'MAXSEG_CORS_ORIGIN',
  ];
  const missingVars = requiredVars.filter((key) => !envText.includes(`${key}=`) || envText.includes(`${key}=`) && envText.split(`${key}=`)[1]?.split(/\r?\n/)[0]?.trim() === '');
  if (missingVars.length > 0) {
    console.error('Missing required variables in .env:');
    for (const key of missingVars) {
      console.error(` - ${key}`);
    }
    process.exit(1);
  }
} else {
  console.warn('.env is not present locally. Create it before running the real build.');
  process.exit(1);
}

console.log('Build gate passed.');
console.log('The project has the required files and the required EAS environment variables defined locally.');

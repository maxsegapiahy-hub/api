# Preparação Local do EAS — Guia Completo

## ⚠️ Segurança Crítica
- **Nunca faça commit do `.env`** — ele está protegido no `.gitignore`
- **Nunca comita credenciais reais** — EXPO_PROJECT_ID, secrets, keys, certificados
- **Variáveis sensíveis** são configuradas **apenas localmente** ou via Expo Dashboard

---

## 1. Instalação e Login no EAS CLI

### 1.1 Instalar o EAS CLI globalmente
```bash
npm install -g eas-cli
```

Ou, se usar pnpm:
```bash
pnpm add -g eas-cli
```

### 1.2 Fazer login na conta Expo
```bash
eas login
```

Será solicitado:
- Email da conta Expo
- Senha

Se não tem conta Expo, criar em: https://expo.dev

---

## 2. Configurar EXPO_PROJECT_ID Localmente

### 2.1 Opção A: Inicializar novo projeto no Expo (primeira vez)
```bash
eas init
```

O CLI vai:
1. Perguntar se quer criar um novo projeto ou usar um existente
2. Gerar um **único `EXPO_PROJECT_ID`** para este app
3. Associar o projeto ao seu account Expo
4. **NÃO vai modificar `.env`** (apenas cria `.easrc`)

### 2.2 Opção B: Usar projeto existente
Se já tem um projeto Expo criado:
```bash
eas project:configure
```

Vai solicitar o `projectId` manualmente.

### 2.3 Copiar o projectId para `.env` LOCAL (NÃO fazer commit)

Após `eas init` ou `eas project:configure`, você terá um `projectId` como:
```
abc12345-def6-7890-ghij-klmnopqrstuv
```

**Edite o arquivo `.env` APENAS no seu computador:**
```env
EXPO_PROJECT_ID=abc12345-def6-7890-ghij-klmnopqrstuv
```

**Salve o arquivo. NÃO faça commit.**

---

## 3. Configurar Variáveis de Produção no `.env`

O arquivo `.env` já foi criado no repositório com placeholders. Você precisa preencher:

### 3.1 Abrir o arquivo `.env` no seu editor local

```bash
cat .env
```

Você verá:
```env
EXPO_PROJECT_ID=
VITE_APP_ID=space.manus.max.seg.apiahy.t20260910020105
EXPO_PUBLIC_APP_ID=space.manus.max.seg.apiahy.t20260910020105

VITE_OAUTH_PORTAL_URL=
EXPO_PUBLIC_OAUTH_PORTAL_URL=

OAUTH_SERVER_URL=
EXPO_PUBLIC_OAUTH_SERVER_URL=

OWNER_OPEN_ID=
EXPO_PUBLIC_OWNER_OPEN_ID=

OWNER_NAME=
EXPO_PUBLIC_OWNER_NAME=

NODE_ENV=development
```

### 3.2 Preencher cada variável

Para **desenvolvimento local**, use valores de teste:

```env
EXPO_PROJECT_ID=abc12345-def6-7890-ghij-klmnopqrstuv
VITE_APP_ID=space.manus.max.seg.apiahy.t20260910020105
EXPO_PUBLIC_APP_ID=space.manus.max.seg.apiahy.t20260910020105

VITE_OAUTH_PORTAL_URL=https://seu-oauth-dev.com.br
EXPO_PUBLIC_OAUTH_PORTAL_URL=https://seu-oauth-dev.com.br

OAUTH_SERVER_URL=https://seu-auth-dev.com.br
EXPO_PUBLIC_OAUTH_SERVER_URL=https://seu-auth-dev.com.br

OWNER_OPEN_ID=seu-owner-id-dev
EXPO_PUBLIC_OWNER_OPEN_ID=seu-owner-id-dev

OWNER_NAME=Max Seg Dev
EXPO_PUBLIC_OWNER_NAME=Max Seg Dev

NODE_ENV=development
```

**Não faça commit. Este arquivo é seu, localmente.**

---

## 4. Validar Configuração EAS

Antes de iniciar o build, execute os scripts de validação:

### 4.1 Instalar dependências
```bash
npm install
# ou
pnpm install
```

### 4.2 Validar configuração EAS
```bash
npm run validate:eas
```

**Saída esperada:**
```
✓ EXPO_PROJECT_ID está definido
✓ eas.json existe
✓ app.config.ts está correto
✓ Bundle ID matches: space.manus.max.seg.apiahy
```

**Se houver erro:**
- Verifique se `.env` foi preenchido corretamente
- Verifique se `EXPO_PROJECT_ID` não está vazio

### 4.3 Preflight para produção
```bash
npm run preflight:production
```

**Saída esperada:**
```
✓ Versão: 2.4.17
✓ Build profile 'production' existe em eas.json
✓ Android bundleId está configurado
✓ iOS bundleIdentifier está configurado
✓ Todos os ícones e imagens existem
```

**Se houver erro:**
- Verifique se `app.config.ts` não foi alterado
- Verifique se `eas.json` contém o perfil `production`

### 4.4 Preparar build Android
```bash
npm run prepare:android
```

**Saída esperada:**
```
✓ Android SDK encontrado
✓ Java Development Kit (JDK) disponível
✓ Gradle está configurado
```

**Se houver erro:**
- Verifique se Android SDK está instalado
- Verifique a variável `ANDROID_HOME`

### 4.5 Gate de build (verificação final)
```bash
npm run build:gate
```

**Saída esperada:**
```
✓ Todas as validações passaram
✓ Pronto para build EAS
✓ Nenhum segredo detectado em arquivos públicos
```

**Se houver erro:**
- Corrija os problemas listados antes de prosseguir

---

## 5. Executar o Build EAS Real

**Somente após passar em todas as validações acima.**

### 5.1 Primeiro build: Preview (teste)

Para **Android**:
```bash
eas build --platform android --profile preview
```

Para **iOS** (requer Apple Developer Account):
```bash
eas build --platform ios --profile preview
```

**O que vai acontecer:**
1. EAS vai fazer upload do seu código
2. EAS vai compilar o Android APK ou iOS IPA
3. Você receberá um link para download do artefato
4. Você pode instalar e testar no seu device

**Duração:** 10-15 minutos (primeira vez pode levar mais)

### 5.2 Monitorar o build
```bash
eas build --status
```

Ou acompanhe em: https://expo.dev/projects

### 5.3 Build de Produção (somente após preview bem-sucedido)

Para **Android** com upload automático para Play Store:
```bash
eas build --platform android --profile production
```

Para **iOS** com upload automático para App Store:
```bash
eas build --platform ios --profile production
```

---

## 6. Identificar e Resolver Erros

### Erro: "EXPO_PROJECT_ID não definido"
```
Error: projectId is not defined
```
**Solução:**
1. Verifique se `.env` foi preenchido com `EXPO_PROJECT_ID=seu-id`
2. Rode: `eas init` novamente se o `.env` não estiver correto

### Erro: "Unauthorized"
```
Error: Unauthorized (401)
```
**Solução:**
1. Rode: `eas logout`
2. Rode: `eas login` novamente

### Erro: "Bundle ID mismatch"
```
Error: Bundle ID in app.config.ts doesn't match provisioning profile
```
**Solução:**
1. Verificar `bundleIdentifier` em `app.config.ts`
2. Configurar um novo provisioning profile no Expo Dashboard
3. Ou usar um bundleId novo no Expo Dashboard

### Erro: "Keystore not found"
```
Error: Keystore not configured for Android
```
**Solução:**
1. Acesse: https://expo.dev/projects → Android → Keystore
2. Gere um novo keystore (Expo vai fazer automaticamente)
3. Repita o build

### Erro: "Certificate not found"
```
Error: Apple certificate not configured
```
**Solução:**
1. Acesse: https://expo.dev/projects → iOS → Certificates
2. Gere um novo certificado (requer Apple Developer Account)
3. Upload do provisioning profile
4. Repita o build

---

## 7. Confirmar Sucesso do Build

Um build é considerado **concluído com sucesso** quando:

✅ **Erro 0 retornado pelo EAS**
```bash
eas build --status
# Output: "Status: FINISHED"
# Exit code: 0
```

✅ **Artefato disponível para download**
```
Android: app-2.4.17.aab (ou .apk)
iOS: app-2.4.17.ipa
```

✅ **Pronto para distribuição**
- Android (.aab): Enviar para Play Store
- iOS (.ipa): Enviar para App Store

---

## 8. Próximos Passos

1. ✅ Instalar EAS CLI
2. ✅ Login no Expo (eas login)
3. ✅ eas init (associar projeto)
4. ✅ Preencher `.env` localmente
5. ✅ npm run validate:eas
6. ✅ npm run preflight:production
7. ✅ npm run prepare:android
8. ✅ npm run build:gate
9. ✅ eas build --platform android --profile preview (teste)
10. ✅ eas build --platform android --profile production (real)

---

## ❌ Erros Comuns

| Erro | Causa | Solução |
|------|-------|---------|
| `EXPO_PROJECT_ID is empty` | .env não preenchido | Preencha `.env` com projectId do Expo |
| `Cannot find module` | npm install não executado | Rode: `npm install` |
| `Unauthorized (401)` | Token expirado | Rode: `eas logout` → `eas login` |
| `EACCES: permission denied` | Permissão de arquivo | Rode: `chmod +x scripts/*.mjs` |
| `Command not found: eas` | EAS CLI não instalado globalmente | Rode: `npm install -g eas-cli` |

---

## ⚠️ Recapitulação de Segurança

- ✅ `.env` NUNCA é commitado (protegido por `.gitignore`)
- ✅ `.env.example` SIM, é commitado (contém apenas nomes, sem valores)
- ✅ EXPO_PROJECT_ID é **único por máquina** (gerado por `eas init`)
- ✅ Credenciais Apple/Android são geridas pelo Expo Dashboard
- ✅ Nunca compartilhe seu `.env` ou EXPO_PROJECT_ID

---

## 📞 Suporte

- Documentação oficial: https://docs.expo.dev/eas-update/introduction/
- Expo dashboard: https://expo.dev/projects
- GitHub Issues: https://github.com/maxsegapiahy-hub/api/issues

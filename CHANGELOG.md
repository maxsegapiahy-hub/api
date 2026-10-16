# Histórico de versões — Max Seg & Max Saúde (Apiahy)

## 2.4.18 — 16/10/2026

- Sincroniza a versão declarada em `package.json` e `app.config.ts`.
- Corrige o guia local do EAS para orientar a vinculação ao projeto Expo existente, sem incentivar a criação de projeto duplicado.
- Mantém a configuração do Project ID dinâmica via `EXPO_PROJECT_ID`; nenhum identificador fictício ou segredo foi adicionado.
- Mantém os perfis EAS existentes: `preview` gera APK Android e `production` gera Android App Bundle (AAB).

### Estado da publicação

Esta alteração no GitHub não executa uma build EAS, não publica nas lojas e não confirma o resultado da build existente. Depois de confirmar o Project ID correto e validar as dependências, será necessário executar e acompanhar uma nova build para a versão 2.4.18.

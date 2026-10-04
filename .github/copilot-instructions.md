# Max Seg & Max Saúde — instruções de colaboração

## Branch base
A branch de desenvolvimento principal deste ciclo é:
feature/affiliate-single-registration

Não alterar diretamente a branch de produção/default sem autorização explícita.

## Regras para agentes de IA
- Antes de alterar código, leia package.json, app.config.ts, eas.json e a documentação relevante.
- Preserve funcionalidades existentes, especialmente o módulo de afiliados.
- Não substituir o projeto por versões antigas de ZIP.
- Para tarefas novas, prefira criar uma branch própria e Pull Request.
- Não mesclar Pull Requests automaticamente.
- Nunca gravar segredos, tokens, API keys ou Project IDs privados em arquivos versionados.
- Atualizar testes/documentação quando a mudança exigir.
- Validar TypeScript e testes disponíveis antes de concluir.
- Para EAS, não afirmar que uma build foi concluída sem execução real do EAS.
- Manter a versão do app alinhada entre package.json e app.config.ts.
- Responder em português quando a solicitação do projeto estiver em português.

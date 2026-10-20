# Rascunho local — chore: establish Yume fork foundation

**Base prevista:** `main`. **Head:** `chore/session-1-foundation`. Nenhum PR foi aberto ou enviado ao remoto.

Yume precisa de uma fundação que distinga o fork do Animiru e preserve a origem do código. Esta mudança apresenta o nome aprovado, documenta status e limites, conserva licença/créditos e registra o plano para separar pacote, marca, updater e callbacks OAuth antes de distribuir um APK.

Inclui README/CONTRIBUTING/SECURITY/NOTICE, seção própria de changelog, templates, `.gitignore`, documentação/evidências e ADR da organização local. Snapshots dos documentos upstream e seu histórico permanecem preservados. Código Android, SQL, recursos, dependências e workflows não mudam.

Validação executada e resultados finais: [registro da sessão 1](evidence/SESSION_1_COMMANDS.md) e [captura estruturada](evidence/SESSION_1.json). Compilação, testes Android, instalação e CI remoto não foram executados; baseline é a sessão 2.

- [x] Nome Yume confirmado pelo proprietário.
- [x] Licença e créditos conservados; autoria assistida por IA explicitada.
- [x] Mudanças funcionais excluídas do escopo desta fundação.
- [ ] Confirmar conta/organização e criar/verificar vínculo do fork remoto.
- [ ] Revisar plano de identidade, ícone, pacote, updater e clientes OAuth.
- [ ] Confirmar canais privados de segurança/conduta antes de abrir contribuições públicas.
- [ ] Autorizar envio/abertura de PR quando aplicável.

Riscos: identidade Android ainda herdada, restauração de backup sem baseline, inventário de licenças transitivas e avisos no APK pendentes. Nenhuma release deve ser inferida desta fundação. Documentação produzida com assistência do Codex, sem atribuir a ele recursos herdados ou revisão humana ainda não realizada.

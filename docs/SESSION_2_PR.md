# Rascunho local — chore: record Android reference build and regression baseline

Base prevista: `chore/session-1-foundation`. Head: `chore/session-2-baseline`. Nenhum envio ao remoto ou abertura de PR está autorizado por este documento.

O fork ainda não tinha um build Android executado no ambiente local. Esta mudança registra a preparação do JDK/SDK, a compilação debug do código funcional herdado e as evidências das verificações de baseline. Acrescenta um script PowerShell para repetir os comandos com logs, SHA, duração e códigos de saída, além de um checklist manual para os fluxos principais e backup/restauração.

Resultados, ferramentas, hashes dos APKs e limites estão em [BUILD_BASELINE.md](BUILD_BASELINE.md); comandos e evidências ficam em `docs/evidence/SESSION_2*`. Código Android, dependências, banco, recursos e workflows permanecem herdados. Não inclui funcionalidades de metadados da sessão 3 em diante.

- [x] Build debug gerou cinco APKs locais; assinaturas debug dos APKs universal e x86_64 verificadas.
- [x] Checklist de regressão preparado com dados sintéticos e evidência por caso.
- [x] SDK/Gradle, artefatos e chave debug mantidos fora do Git.
- [x] Autoria assistida pelo Codex explicitada; linhagem upstream preservada.
- [x] 52 testes unitários e Spotless passaram; resultados SQLDelight/lint registrados com suas limitações.
- [ ] Instalação, navegação e regressão manual em Android confirmadas.
- [ ] Backup/restauração e possível perda de `custom*` reproduzidos em execução.

Riscos: o AVD de software falhou com `0xC0000005`, e o host não possui hipervisor ativo/driver Android instalado. Lint retornou exit 0 com 3 erros herdados, 201 avisos e 2 hints; SQLDelight passou sem snapshots históricos. O pacote `.dev` e a identidade visual continuam herdados. Esses APKs debug não constituem uma release Yume. Instalação, regressão manual e migração/restore de dados continuam sem demonstração.

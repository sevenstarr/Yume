# Capturas reais e proveniência

Selecionadas e inspecionadas visualmente na sessão 12, em 22/10/2026. São cópias integrais, sem edição, de duas capturas da validação Android da sessão 9 (20/10/2026), Galaxy A05s/Android 15/API 35. [Registro da validação](../SESSION_9_ANDROID.md) e [evidência agregada](../evidence/SESSION_9_ANDROID.json).

O registro da rodada identifica APK ARM64 debug `0.20.0.1-8260`, SHA-256
`0863d71b91fcdf70077d3cd19bd526ba8e2984cb3ab4f79fa0a6ea7cab85f4c3`.
As capturas não têm metadado próprio de commit; essa é a referência da execução
histórica, sem alegar captura do SHA atual `91a378cee`.

| Arquivo | O que mostra | SHA-256 |
| --- | --- | --- |
| [session-09-paused.png](session-09-paused.png) | Configurações com lote pausado em 0/6, ações de retomar/cancelar | `708d58eee4575a38192a96015f3f94a9e1bde0220264b301cf3c26856ca078d4` |
| [session-09-completed.png](session-09-completed.png) | Configurações com lote concluído, 6/6 ignorados, zero erros | `2e466480581de0a43201b3f3610edede172f36b51f744a5280b135f479ef0f7b` |

Originais locais tinham os nomes `r2-paused.png` e `sync-running.png`; o segundo nome não descrevia o estado visível, que já era **concluído**. Hashes conferidos contra os originais; tamanho de ambas: 1080×2400. As cópias selecionadas não mostram títulos/capas de obras, biblioteca, contas, caminhos ou notificações pessoais. Apenas esses dois arquivos revisados foram incluídos; bancos, XMLs/logs e outras capturas brutas continuam fora do Git.

Limites: build anterior ao pacote/marca Yume atuais; interface em inglês, tema escuro, opt-ins ligados previamente pelo usuário. Não representa defaults de instalação limpa. O texto de dados móveis ainda contém AND-01, corrigido depois em `25d50a506`; veja a string atual em `i18n/src/commonMain/moko-resources/base/strings.xml`. O lote com skips não prova tráfego/provider bem-sucedido. Uma imagem estática não valida acessibilidade, animação, performance ou retomada após reboot.

Não são imagens geradas ou mockups. Capturas atuais de seleção manual e capas devem usar uma instalação sintética e material visual permitido, com versão/commit e condições registrados. A ausência dessas imagens atuais permanece uma limitação do portfólio, não uma evidência de fluxo aprovado.

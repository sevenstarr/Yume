# docs: integrate session 12 guides and validated fork changes

## Problema e resultado

O README e a arquitetura ainda misturavam propostas antigas com o incremento
atual. Esta PR consolida documentação pública e demonstração técnica e incorpora
as correções, qualidade e CI previamente revisados na PR #5, encerrada a pedido
do proprietário. Corrige também o bloqueio de `sdkmanager` fora do PATH no CI.

## Mudanças e origem

- Documentação da sessão 12: comparação upstream/Yume, guias públicos de build
  e demonstração, arquitetura/contratos/schema/ADRs e histórico preservado,
  contribuição, segurança, origem/sincronização, changelog e avisos.
- Duas capturas reais históricas de configurações/lote, revisadas e sem edição,
  com alt text, proveniência e hashes; sem biblioteca/contas/capas de obras.
- Commits incorporados e preservados: identidade Android Yume/updater desligado,
  locale e fonte local; testes/relatório de qualidade; CI debug sem secrets e
  candidato release manual com assinatura própria. Não atribui recursos herdados
  de biblioteca/player/fontes/downloads/tracking como novos.
- Correção necessária de CI: chamada explícita a Command-line Tools nos workflows
  build/release, validação do executável e exportação da raiz/PATH. Mantém runner,
  permissões mínimas, verificações obrigatórias e gates de assinatura/publicação.

Branch criada diretamente de `origin/yume-develop=25d50a506`, atualizada por
fetch/merge sem conflitos. O proprietário autorizou incorporar os commits até
`91a378cee`, enviar esta branch, substituir a PR #5 e realizar merge somente
após os checks obrigatórios verdes na cabeça final. Trabalho assistido pelo Codex.

## Validação

- Sessão 12: 249 testes JVM selecionados/14 suítes, zero falhas/erros/ignorados.
  Domain/data 170 executados na primeira tentativa e reutilizados na repetição;
  app 79 executados na repetição aprovada em 3m58s. Primeira tentativa teve falha
  de acesso local ao JAR/daemon; nenhum fonte alterado para contornar a falha.
- Nove testes Python e duas migrações sintéticas aprovados; 18 Markdown/230
  destinos locais conferidos. LICENSE/histórico/capturas preservados byte a byte.
- Correção SDK: actionlint e 14 blocos `bash -n` aprovados; quatro smoke cases
  cobrem raiz com espaços, fallback, executável ausente e falha propagada.
  Smoke usa instalador sintético; shellcheck/pyflakes não executados.
- Evidências anteriores da sessão 11: build debug, SQLDelight, Spotless e lint
  locais aprovados; 306 testes nos XMLs, parte reutilizada, zero erros de lint
  e 211 avisos. Cinco APKs debug com assinatura v2 e hashes verificados localmente.

Registros: [sessão 12](evidence/SESSION_12_COMMANDS.md),
[publicação/correção CI](evidence/SESSION_12_INTEGRATION.md),
[sessão 11](evidence/SESSION_11_COMMANDS.md), [qualidade](TEST_REPORT.md).
O run anterior da PR #5 falhou por `sdkmanager: command not found` (exit 127),
antes dos testes/build; o CI desta PR deve validar a correção na cabeça final.

## Aceite e limites

- Documentação/atribuição/contratos e comandos públicos preparados para revisão.
- Checks remotos obrigatórios e sincronização da base condicionam o merge.
- Capturas atuais de seleção/capas, reprodução independente em máquina limpa e
  validação Android completa permanecem pendentes: UI/TalkBack, performance,
  bateria/Doze, reprodução/download/restore completos e atualização de APK próprio.
- OAuth próprio, políticas/direitos de provedores/imagens, licenças nativas e
  assinatura/distribuição têm gates próprios. Updater/automação textual off.

Esta integração não promove a `yume-main`, não cria tag/release nem inicia a
sessão 13. Publicação de binário e assinatura do proprietário não são executadas.

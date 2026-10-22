# Rascunho local — CI debug e candidatos de release do Yume

Base: `yume-develop`; compare: `feature/session-11-ci-distribution`.
Envio e abertura de PR ainda não autorizados. A branch incorpora, por autorização
expressa e fast-forward, `33878ff77` e `1b848e794` como commits separados. A PR
futura também deverá revisar essas correções e o relatório da sessão 10.

O CI herdado gerava release com chave debug e o workflow de release ainda
referenciava o upstream. Agora PRs/pushes verificam formatação, testes, migrações
e lint e geram cinco APKs debug temporários, com assinatura verificada, SHA-256
e manifesto. Nenhum secret é fornecido ao CI; token é somente leitura, ações
permanecem fixadas em SHA e cache só é gravado em push de integração/principal.

Candidatos de release passam a exigir dispatch confirmado em `yume-main`,
ambiente de assinatura pronto e chave própria. O workflow verifica o certificado,
preserva mapping e gera artifact de sete dias, sem criar tag ou publicar Release.
A configuração de release não herda chave debug; benchmark mantém assinatura de
desenvolvimento para preservar o profiler. [Processo](BUILD_DISTRIBUTION.md).

## Verificação e aceite

- Nove testes Python dos gates/empacotador e duas migrações sintéticas aprovados.
- actionlint passou com o label Ubuntu 26.04 declarado localmente; 14 blocos
  de shell passaram em `bash -n`. Shellcheck/pyflakes não executados.
- Build conjunto aprovado em 13m52s: 306 testes nos XMLs, 86 executados e 220
  reutilizados; zero falhas/erros/ignorados. Spotless, SQLDelight e lint aprovados,
  zero erros de lint (211 avisos nos quatro módulos). Cinco assinaturas debug v2
  e cinco SHA-256 reais verificados. CRLF dos checksums corrigido após teste shell.
  Resultados completos em [comandos](evidence/SESSION_11_COMMANDS.md).
- CI remoto, upload/download de artifact, instalação real desse artifact e
  assinatura com chave do proprietário ainda pendentes. Não marcar esses itens
  como aprovados a partir do build local.

Antes de abrir PR, atualizar `origin/yume-develop`, incorporar mudanças se houver,
revalidar e obter autorização de push/PR. Não integrar com checks obrigatórios
pendentes/falhando. A configuração do ambiente/secrets e a promoção à principal
dependem de autorização própria. As lacunas Android, OAuth, provedores e
dependências nativas da sessão 10 permanecem bloqueios anteriores à distribuição.

## Critérios de aceite da sessão 11

- [x] CI de PR/push sem secrets, permissões mínimas e cache controlado.
- [x] Geração e empacotamento de APK debug com checksums/proveniência preparados.
- [x] Fluxo manual de candidato e processo de custódia/versão/instalação documentados.
- [ ] Pipeline remoto verde e artifact baixado/instalado.
- [ ] Configuração e teste do ambiente de assinatura pelo proprietário.
- [ ] Autorizações separadas de integração, promoção e eventual publicação.

Não é declaração de aceite integral, integração ou publicação.

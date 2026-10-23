# Relatório de qualidade — Sessão 10

22/10/2026. Branch `feature/session-10-quality`, base remota `25d50a506`, com
incorporação expressamente autorizada do commit local `33878ff77`.
O relatório da rodada anterior está preservado ao final deste documento.

## Resultado e evidências

- **JVM:** 306 testes, zero falhas, erros ou ignorados. Os 86 do app foram executados nesta rodada (três novos); 220 foram reutilizados pelo Gradle como `UP-TO-DATE`: domain 162, data 54 e source-local quatro.
- **Build/migração/formatação:** check conjunto aprovado em 11 minutos (66 tarefas executadas, 745 atualizadas), cinco APKs debug; assinatura v2 do ARM64 validada. Verificações SQLDelight e Spotless aprovadas; scripts sintéticos preservaram 11/18 tabelas e rollback.
- **Lint:** zero erros nos quatro módulos. Avisos: app 204, data zero, source-local três e widgets quatro. XMLs inspecionados; exit zero sozinho não foi usado como prova de lint limpo.
- **Ambiente:** tentativa restrita falhou em 4m49 por acesso negado ao JAR de presentation-widget; repetição com acesso aos caches passou. Logs e hashes no snapshot; nenhuma alteração de código para contornar permissões.

Novos testes: matriz de 13 URLs não confiáveis recusadas antes de chamada HTTP
e dois casos de limite de bytes UTF-8 no transporte AniList. Nenhum código de
produção foi alterado nesta sessão. O script de consulta OSV é reproduzível e
não atualiza dependências. [Comandos, escopo e riscos](evidence/SESSION_10_COMMANDS.md)
e [snapshot agregado](evidence/SESSION_10.json).

## Cobertura por requisito

Cobertura abaixo significa cenários exercitados. Percentuais de linhas/branches
não foram medidos; não foi encontrada configuração JaCoCo/Kover no build inspecionado.
Não há suíte `app/src/androidTest` disponível para os fluxos do MVP.

| Requisito | Evidência automatizada disponível | Limite e teste manual necessário |
| --- | --- | --- |
| Normalização/matcher | Normalizer, matcher, calibração e validação separada; ambiguidades/vetos e pins | Fixtures sintéticas; precisão no catálogo real não comprovada, automação textual desligada |
| Prioridade/fallback/cache | CoverPolicy, interceptor, fetcher e transporte; manual, original, offline, erro/decode, cancelamento e revisão tardia | Chain/fakes JVM não provam decoder real, recomposição, scroll ou widget Android |
| Cliente AniList | Provider/transporte/governor; payload, campos parciais, limites, erros, cancelamento, quota durável | Sem tráfego AniList real nesta sessão; sem estresse do serviço |
| Persistência/migração | SQLite JDBC real e scripts sintéticos 148→149→150; FKs, rollback e originais | Não é upgrade de instalação Android representativa |
| Sincronização | Fila durável, 2.000 itens em fatias, TTL, pins, pausa/cancelamento, retries e reabertura JDBC | Não mede bateria, memória de imagens, rede ou WorkManager/Doze em dispositivo |
| Backup | Wire antigo/novo, decisões opcionais, projeção `custom*` e conflitos de identidade no repositório | Restauração integral em Android/SAF, capas manuais binárias e recuperação destrutiva pendentes |
| Regressão herdada | Suítes existentes de episódios, favoritos/flags, intervalos, migração e releases; testes da fonte local/updater incorporados | Não substitui player, áudio, download completo/offline, extensões e atualização entre APKs |
| UI/acessibilidade | Evidência histórica parcial de biblioteca/detalhes/configurações na sessão 9, outra identidade Android | Sem nova execução UI; onboarding Yume, seleção/opt-out/fallback, TalkBack/foco/teclado pendentes |

## Segurança e compatibilidade

Consulta OSV realizada nesta sessão: **118 coordenadas/versões declaradas**, zero
pacotes com alertas retornados. **15 declarações omitidas**; análise exclui versões
transitivas resolvidas, BOMs, plugins e bibliotecas nativas. Ausência de resultado
não prova ausência de vulnerabilidades ou indexação da versão no OSV.

Varredura de quatro padrões de segredo em **1.960 arquivos textuais rastreados**,
zero correspondências. Inspeção estática de 14 permissões e quatro componentes
explicitamente exportados no manifesto fonte. Sem auditoria do histórico Git,
fuzzing de intents, pentest ou certificação de segurança.

A camada de metadados mantém endpoint HTTPS fixo, CDN restrito, isolamento de
cookies/headers/autenticadores, redirects bloqueados e limites de memória/tempo.
Busca limitada nos fontes de metadados e transporte/interceptor de capas não
identificou chamadas de log; logs herdados e de extensões não foram auditados.

**Riscos para distribuição:** configuração herdada aceita HTTP e CAs de usuário;
Safe Browsing está desativado na WebView. Callbacks/clientes OAuth continuam
herdados e exigem cadastro próprio. Componentes exportados e dependências
transitivas/nativas exigem revisão complementar. Política de rede não alterada
sem regressão das fontes. Detalhes e prioridades no registro de comandos.

## Testes manuais ainda necessários

| Cenário | Critério observável para aprovar |
| --- | --- |
| Instalação Yume/upgrade/coexistência | Concluir onboarding em pasta sintética, iniciar e atualizar APK próprio preservando dados e instalação upstream |
| Biblioteca/detalhes/revisão | Escolher candidato com confirmação, preservar pin após refresh, alternar original e opt-out com tela aberta |
| Imagens reais | HTTP 404, bytes corrompidos e offline com/sem cache terminam no fallback; capas manuais preservadas |
| Regressão de mídia e dados | Reproduzir com áudio/seek, concluir download e reproduzir offline; conferir favoritos, histórico e progresso antes/depois |
| Backup/limpeza/restore | Usar instalação e biblioteca sintéticas; comparar originais, customizações, episódios, progresso e associações após restauração; nunca usar biblioteca pessoal como fixture |
| Agendamento/energia | Observar diário natural, deep Doze e recuperação, reboot e rede intermitente; medir consumo prolongado |
| Performance | Biblioteca representativa e cenários frio/quente com métricas de frame, heap, chamadas e bytes; 2.000 linhas JDBC não equivalem a esse benchmark |
| Acessibilidade/compatibilidade | TalkBack, foco e fonte ampliada; API mínima e outra versão/fabricante; SAF e widgets |

Nenhum dispositivo foi operado nesta sessão. Os testes manuais acima permanecem
abertos, com roteiro e critérios verificáveis. O aceite documental da sessão
consiste neste relatório com resultados/limites; não representa aprovação geral
para usuários reais. Confirmação AniList adiada conforme orientação anterior.
Assinatura/distribuição continuam sujeitas a seus gates. Sem push, PR, tag ou
release; sessão 11 aguarda autorização separada.

---

# Relatório de qualidade — retomada das pendências

22/10/2026, America/Sao_Paulo. Execução assistida pelo Codex, autorizada pelo proprietário. Continuação das correções na branch `fix/non-device-pending-items`, base `25d50a506`. Este relatório não declara o aceite integral Android nem uma versão pronta para distribuição.

## Correções sob validação

- Calendário usa o locale observado pela composição e invalida os dias lembrados ao trocar o idioma; nenhum detector de lint suprimido.
- Identidade aprovada Yume, pacote próprio com sufixos de variantes preservados, ícones e splash próprios.
- Updater desabilitado em toda distribuição, com recusa de configuração que tenta habilitá-lo e guarda antes da consulta de releases, inclusive forçada.
- Fonte local compartilha a política de leitura/escrita: `details.json` canônico, `info.json` legado e streams fechados. Testes JVM usam `UniFile` mockado e streams de bytes; não comprovam SAF real em Android.
- Avisos de origem/licenças complementares incluídos no AboutLibraries, preservando as dependências herdadas. Não é um inventário completo das bibliotecas nativas nem uma certificação para distribuir binários.

## Evidências desta execução

| Verificação | Resultado |
| --- | --- |
| Fetch e base | Concluído; HEAD igual a `origin/yume-develop`, SHA `25d50a506` |
| Migração 148 → 149, script Python | Aprovada: 11 tabelas herdadas preservadas, FKs, default off e rollback |
| Migração 149 → 150, script Python | Aprovada: 18 tabelas existentes preservadas, FKs, idle/opt-ins e rollback |
| Testes JVM | 303 testes: domain 162, data 54, app 83, source-local 4; zero falhas, erros ou ignorados |
| Build debug | Cinco APKs gerados nesta execução; ARM64 com assinatura v2 verificada, label Yume e pacote `io.github.sevenstarr.yume.dev` |
| SQLDelight | Verificações debug/release aprovadas |
| Spotless/lint | Check conjunto aprovado em 32m 37s. App: zero erros, 204 avisos, zero `NonObservableLocale`; data: zero issues; source-local: três avisos; widgets: quatro avisos. Nenhum detector suprimido |
| Guarda de configuração do updater | `:app:generateDebugBuildConfig -Penable-updater=true` recusado como esperado em 17s, com mensagem específica; build normal aprovado |
| Varredura limitada de segredos | 1.958 arquivos textuais, zero correspondências às três regras de chave privada/token GitHub/chave AWS; histórico completo e entropia não avaliados |
| Instalação/coexistência inicial | Galaxy A05s/API 35: instalação nova aprovada; os dois pacotes antigos e Yume constam instalados. `am start -W` retornou Status ok. Após desbloqueio pelo proprietário, onboarding visível e avanço até seleção de armazenamento; inspeção completa interrompida após outro aplicativo assumir o primeiro plano |

APK ARM64 testado: versão `0.20.0.1-8270`, versionCode 153, SHA-256 `d087ead7d176dfb77d023c58b6b9b812121af572226d0da7256b8813cc58a52c`. Coexistência aqui comprova instalação paralela; login OAuth, transferência de dados e atualização entre versões próprias ainda não foram exercitados.

Logs completos e dados brutos permanecem no diretório ignorado `build/non-device-pending/`. Cobertura de linhas/branches não foi medida. Os resultados históricos das sessões 4–9 permanecem nos snapshots próprios e não substituem esta execução.

Resultados agregados e hashes dos cinco APKs em [NON_DEVICE_PENDING.json](evidence/NON_DEVICE_PENDING.json). Pasta de teste criada no aparelho: `Yume-QA-20261022`, ainda não escolhida como raiz de armazenamento. Não houve importação ou restauração de dados pessoais nesta rodada.

## Pendências de aceite

Diário natural, deep Doze e recuperação, consumo prolongado de bateria, performance de scroll/memória/rede em biblioteca representativa, TalkBack/foco/teclado, decoder/imagens/fallback real, reprodução e download completos/offline, restauração sintética completa e limpeza/restauração permanecem sem aceite nesta retomada. Reinício/fila já têm evidências anteriores, mas a identidade nova exige verificação própria de instalação e coexistência.

A tentativa de copiar o banco privado WorkManager para inspeção no host foi rejeitada pela revisão automática por exigir autorização específica para extração. Nenhuma cópia foi executada por esse comando e não houve contorno. A execução diária permanece sem nova evidência por esse método.

O proprietário informou que ainda não há clientes OAuth próprios; dependência registrada. Confirmação do AniList adiada conforme instrução de 22/10, sem presumir autorização externa recebida. Assinatura, distribuição, fontes correspondentes das dependências nativas e revisão dos demais componentes continuam gates próprios. Automação textual permanece off enquanto não houver validação suficiente em catálogo real.

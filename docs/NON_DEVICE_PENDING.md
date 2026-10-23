# Pendências sem aparelho — acompanhamento

21/10/2026, America/Sao_Paulo. Trabalho autorizado pelo proprietário: resolver as pendências que não usam celular, respeitando o protocolo. Esta rodada é uma correção/consolidação das sessões anteriores; não declara o aceite Android nem o encerramento da sessão 10.

Base: `25d50a506b36b29c4ae47b76bc354bec4ac6f6ed`, confirmada por fetch. Branch `fix/non-device-pending-items`. `docs/prompt.md`, não rastreado, foi preservado. O mapa externo foi consultado por subgrafos; é um snapshot dessa base e não será copiado para o Git.

## Escopo e critérios

| Item | Critério de resolução | Estado nesta rodada |
| --- | --- | --- |
| Lint herdado | Corrigir os três `NonObservableLocale`, executar lint completo e conferir o relatório; nenhum detector suprimido | Resolvido no host: zero erros, 204 avisos no app, zero `NonObservableLocale` |
| Identidade | Nome Yume, pacote `io.github.sevenstarr.yume`, símbolo ¥ minimalista preto/amarelo, recursos compilados | Implementada/compilada; APK instalado separadamente, onboarding visível; revisão visual completa pendente |
| Updater | Nenhuma variante consultar/instalar APK upstream; builds com updater solicitado devem falhar de forma explícita | Desabilitado na configuração, guarda e teste de chamada forçada aprovados; `BuildConfig` gerado false |
| Organização Git | `yume-main` remota sem promover funcionalidades experimentais; proteções em principal e integração | Renomeação de `yume-release` preservou `787b275e8230a748661be1f7c50ebbdd49ea9533`; ruleset ativo 25605405 |
| Proteção | PR, Build & Test App da integração GitHub Actions, base atualizada, conversas resolvidas, sem bypass/force-push/exclusão | Configurado e confirmado pela API; aprovação humana continua pelo protocolo |
| Relato de segurança | Canal privado existente e documentação correta | Private vulnerability reporting habilitado e confirmado pela API |
| AND-01 | Não listar como bug atual o subtítulo já corrigido | Indicação histórica atualizada; correção já integrada na base |
| Metadados locais | Verificar a divergência histórica entre escrita `info.json` e leitura `details.json` | Política unificada implementada; quatro testes JVM aprovados; SAF real pendente |
| Verificações locais | JVM, migrações, build debug, formatação, lint e inspeção estática dos artefatos | Aprovadas; 303 testes e cinco APKs; detalhes abaixo |

## Limites que não serão confundidos com correção de código

- Diário natural, deep Doze, bateria real, TalkBack, scroll/memória/rede em biblioteca representativa, decoder Android, download/reprodução completos/offline, instalação/coexistência e restauração real continuam exigindo Android utilizável. Testes JVM/fakes não aprovam esses critérios.
- A configuração textual automática permanece desligada. A evidência sintética existente não demonstra qualidade no catálogo real; habilitação exige revisão própria do gate P-01.
- Cadastros OAuth pertencentes ao fork, autorização do provedor e direitos das imagens dependem de terceiros/contas do proprietário. A documentação técnica não pode criar esses direitos ou confirmar uma autorização externa inexistente.
- Nenhuma tag, release, chave de assinatura ou promoção de `yume-develop` para `yume-main` integra esta rodada. A renomeação remota não promoveu código.

## Retomada de 22/10/2026

O proprietário autorizou começar a resolver as pendências listadas e adiar a confirmação do AniList. Retomada na mesma branch de correções, preservando as alterações locais anteriores. Fetch concluído; `HEAD` e `origin/yume-develop` continuam em `25d50a506`. O mapa externo foi consultado para `LocalSource`, `AppUpdateChecker` e calendário; não contém as mudanças locais desta rodada.

Galaxy A05s/API 35 conectado por ADB. Os dois pacotes antigos permanecem instalados; o pacote `io.github.sevenstarr.yume.dev` ainda não estava instalado na inspeção inicial. Não há acessibilidade habilitada. Isso permite iniciar testes em instalação separada, mas não comprova TalkBack, Doze, consumo de bateria ou restauração.

As verificações Python das migrações 148 e 149 passaram novamente, incluindo preservação das 11/18 tabelas existentes, FKs, defaults e rollback. Build, testes JVM, SQLDelight, Spotless e lint completos foram iniciados juntos; resultados serão registrados após seu término. Não há cobertura percentual medida.

A correção de restore `custom*` e seu teste de wire/projeção já existem desde a sessão 6. A pendência correspondente é o fluxo completo em Android. AND-01 foi integrado na PR #4 e não é um bug atual. A automação textual permanece desligada enquanto a validação de catálogo real não sustentar sua habilitação.

Confirmação do AniList adiada por instrução do proprietário; não constitui confirmação recebida nem bloqueia os checks técnicos desta retomada. Cadastros OAuth próprios, assinatura de release e condições de distribuição continuam dependências externas.

## Verificações e resultado final

Check conjunto aprovado em **32m 37s**, 815 tarefas: 719 executadas, 92 do cache e quatro atualizadas. **303 testes**, zero falhas/erros/ignorados; cinco APKs debug, assinatura ARM64 v2 aprovada. SQLDelight debug/release, scripts Python, Spotless e lint aprovados. App: zero erros e 204 avisos; data: zero issues; source-local: três avisos; widgets: quatro avisos. Sem supressão de detectores, sem cobertura percentual medida. [Snapshot](evidence/NON_DEVICE_PENDING.json) e [relatório](TEST_REPORT.md).

Instalação nova no Galaxy A05s/API 35 aprovada, com os dois Animiru anteriores ainda instalados. Primeira tela de onboarding visível; tema avançado até a escolha de armazenamento. Criada pasta exclusiva `Yume-QA-20261022`; seleção não concluída porque outro aplicativo assumiu o primeiro plano e as interações foram interrompidas. Nenhum aceite completo de UI, transferência de dados ou OAuth é inferido.

O proprietário confirmou que ainda não cadastrou clientes OAuth próprios. Confirmação AniList adiada conforme solicitado. A revisão automática rejeitou a tentativa de copiar o banco WorkManager ao host por exigir autorização específica de extração; nenhum contorno executado. Diário natural permanece pendente.

Fechamento local desta rodada não encerra o aceite Android, não aprova distribuição e não integra alterações no remoto.

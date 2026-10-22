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

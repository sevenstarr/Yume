# Evidências — Sessão 6

20/10/2026. Base `4531d813e680488497b384a83d82f01ce478b065`, branch `feat/session-6-metadata-persistence`, derivada do HEAD da sessão 5. Pedido do proprietário: executar sessão 6. Árvore inicial limpa. A pasta externa `Yume/` não é Git e não contém `prompt.md`; lidos o prompt mestre em `Downloads/prompt.md` e os documentos canônicos em `Animiru/docs/`. `git status`, `git log -5 --oneline`, `git show --stat HEAD`, código e aceites da sessão anterior foram conferidos. Nenhuma alteração pré-existente foi descartada.

## Escopo e reprodução

DDL aditivo 148/schema 149, sete tabelas separadas, repositório/Flow/CAS/leases/epoch, TTL e negativos/LRU, falhas tipadas/cooldown, barreira/eviction seletiva de Coil, backup opcional v1/tag 600, remapeamento/conflitos e correção do restore herdado `custom*`. Sem requests visuais externos ou consumidor do provider/matcher; sem worker/gatilho/lote novo. Automação textual continua off. Sem push, PR remoto, tag ou release. Código/documentação assistidos pelo Codex.

Ambiente do baseline: JDK 21.0.11, Gradle 9.7.1, SDK/caches locais e dois workers. Comando de reprodução (o script salva logs e tempos por tarefa):

```powershell
.\scripts\run-baseline.ps1 `
  -JavaHome 'C:\Program Files\Java\jdk-21.0.11' `
  -SdkRoot 'C:\Users\Administrator\Documents\Yume\.tooling\android-sdk' `
  -GradleUserHome 'C:\Users\Administrator\Documents\Yume\.tooling\gradle' `
  -Tasks 'testDebugUnitTest',':app:assembleDebug','verifySqlDelightMigration','spotlessCheck',':data:lintDebug' `
  -OutputDirectory 'build/session6/reproduction'
```

Teste SQLite adicional, com Python 3 e biblioteca padrão:

```powershell
python scripts/verify-metadata-migration.py
```

O relatório por tarefa/suíte está em [SESSION_6.json](SESSION_6.json). Logs completos, XMLs, relatórios Android e APKs permanecem locais/ignorados. O script registra HEAD da base, mas verifica as fontes do working tree; tarefas atualizadas podem ser reutilizadas pelo Gradle. Não foi medida cobertura de linhas/branches.

## Tentativas e achados

1. `schema-base`: `:data:generateDebugDatabaseSchema` passou. A geração já incorporava o DDL novo em preparação; foram removidas exclusivamente as tabelas novas para fixar a fixture 148. Preservados esquema, views, índices, trigger e defaults das 11 tabelas herdadas; VACUUM aplicado apenas à fixture sintética. Não é snapshot pessoal nem prova de todas as migrações históricas.
2. `compile`: acesso restrito falhou com `AccessDeniedException` ao JAR `gradle-stdlib-java-extensions-9.7.1.jar`. Repetição autorizada com acesso aos caches. Não houve rejeição da revisão automática de aprovação.
3. `compile-access`: compilação encontrou fontes/dependências em estados intermediários de implementação e dois ajustes de tipos no código novo; corrigidos. A verificação SQLDelight de migração passou. Essas tentativas de compilação não são classificadas como aprovadas.
4. `first-tests`: formatação recusou imports wildcard/linhas longas; teste encontrou imports indevidos produzidos pela preparação do código. Corrigidos sem suprimir regras.
5. `second-tests`: sete dos 16 testes SQL passaram; nove falharam porque as fixtures/camada nova usavam `ANILIST`, enquanto o contrato da sessão 5 exige `anilist`. Corrigidos o namespace e os IDs novos; contrato anterior preservado. Um erro de encoding na preparação da string herdada foi detectado no diff e revertido antes da validação final; a mudança de recursos final contém somente a string nova.
6. `validation`: 16 testes SQL e suíte agregada passaram. Na revisão foram acrescentadas falhas de busca sem vínculo, invalidação do estado buscando e testes correspondentes; a verificação final usa essas últimas fontes. Os testes de persistência foram repetidos após esses ajustes.
7. Revisão staged detectou normalização textual indevida na fixture `.db`; a regra binária em `.gitattributes` foi adicionada e os bytes staged comparados ao arquivo. `PRAGMA integrity_check` passou na cópia extraída do índice Git. Nenhum banco pessoal foi usado.

## O que foi exercitado

- SQLite real/JDBC: upgrade 148→149 com linha herdada, abertura/criação nova, fechar/reabrir, pin/cooldown/orçamento, leases/generation, CAS, respostas atrasadas após off/clear/remoção/mudança da origem, cascade e leitura em lote.
- Relógio determinístico: fresco até sete dias, retenção abaixo de 30 dias, negativo de 24 horas, fingerprint/versões da consulta, LRU de 200 páginas, erro/incompletude sem negativo e falha sem renovar verificação.
- Limpeza: pins/rejeições/originais preservados no cache clear; clear total desliga e mantém cooldown; barreira fake de imagem durante clear remove somente chave externa e impede publicação elegível antiga.
- Backup: fixtures de fio antigo/novo e leitor anterior dos campos relevantes, opções antigas, remapeamento de ID local, destino com decisão, duplicatas divergentes/indistinguíveis, provedor desconhecido, versão inválida e revalidação após importação. Projeção e escrita SQL dos seis campos personalizados exercitadas separadamente.
- Script SQLite: migração e rollback em cópia descartável com favoritos, capa original, customizações, categorias, tracking, episódios vistos/marcados e progresso/histórico; todas as linhas e definições das 11 tabelas herdadas comparadas. FK check e default off aprovados.

## Continuidade antes da sessão 7

Em 20/10/2026, o pedido de executar a sessão 7 encontrou esta implementação staged e com ajustes adicionais no working tree, ainda sem commit, snapshot final ou atualização de progresso. O proprietário autorizou explicitamente validar/finalizar a sessão 6 em commit separado antes da sessão 7. Conteúdo anterior preservado; nenhuma alteração funcional foi necessária nesta finalização.

`continuity-validation`: a tentativa restrita de `testDebugUnitTest` falhou em 159,5 s por `AccessDeniedException` no JAR de `presentation-widget`. A sequência foi interrompida antes de repetir o mesmo bloqueio no build. `python` no PATH era um alias WindowsApps inacessível; o script SQLite passou usando o Python do runtime local do Codex. Repetição com acesso aos caches em `build/session6/continuity-access`; resultados finais registrados em `SESSION_6.json` e no progresso. Essas tentativas não alteram os resultados históricos acima.

Na continuidade, os XMLs atuais confirmaram 19 testes SQL e quatro de backup, além dos 164 anteriores: 187 no total. `:data:lintDebug` terminou sem issues. `spotlessCheck` identificou apenas CRLF em `data/build.gradle.kts`, `MetadataRepositoryTest.kt` e strings XML; normalizados para LF. Quebrada uma linha longa de verificação da lease e aplicada a formatação de `data`, sem mudar comportamento. A repetição final aprovada está em `build/session6/final-format`. A validação SQLite foi repetida com o runtime Python local e passou.

## Limites preservados

Os testes JVM simulam reinício por fechar/reabrir SQLite e interleavings por coroutines determinísticas; não são teste de morte de processo Android ou stress multithread. A fixture de leitor anterior cobre os campos necessários para demonstrar skip do tag 600, sem garantir compatibilidade com todo fork/versão. A correção herdada foi confirmada por inspeção do HEAD anterior e testes de projeção/escrita; não foi executado APK antigo para um round-trip em dispositivo.

Sem instalação/emulação/regressão manual, transporte real AniList/CDN, performance de biblioteca grande, decode/fallback/headers Coil ou cache físico em Android. A ponte compila contra Coil e a barreira usa eviction fake nos testes; sua integração visual fica na sessão 7. Capas manuais não ganham backup de bytes. Quota/circuito do cliente da sessão 4 continuam em memória: o coordenador da sessão 9 ainda precisa integrar os controles duráveis. Lint integral do app não repetido, inconclusivo desde a sessão 4; lint de `data` é registrado separadamente. Nenhuma dependência ou detector de lint foi suprimido.

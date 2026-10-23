# Rascunho local — Sessão 10: qualidade, segurança e compatibilidade

Base proposta: `yume-develop`; compare: `feature/session-10-quality`.
Documento local; abertura de PR e envio ao remoto não autorizados nesta sessão.
A branch também contém o commit de correções `33878ff77`, incorporado com
aprovação explícita do proprietário; ele permanece separado no histórico.

O MVP precisava de um relatório que reunisse os resultados verificáveis e as
lacunas de qualidade. A sessão amplia a regressão de transporte com URLs hostis
e limites UTF-8, acrescenta consulta OSV reproduzível e registra uma matriz de
cobertura, riscos de segurança herdados e roteiro manual com critérios de aceite.
Não altera o runtime do aplicativo.

## Validação

- **JVM:** 306 testes, zero falhas, erros ou ignorados. Os 86 do app foram executados nesta rodada (três novos); 220 foram reutilizados pelo Gradle como `UP-TO-DATE`: domain 162, data 54 e source-local quatro.
- **Build/migração/formatação:** check conjunto aprovado em 11 minutos (66 tarefas executadas, 745 atualizadas), cinco APKs debug; assinatura v2 do ARM64 validada. Verificações SQLDelight e Spotless aprovadas; scripts sintéticos preservaram 11/18 tabelas e rollback.
- **Lint:** zero erros nos quatro módulos. Avisos: app 204, data zero, source-local três e widgets quatro. XMLs inspecionados; exit zero sozinho não foi usado como prova de lint limpo.
- **Ambiente:** tentativa restrita falhou em 4m49 por acesso negado ao JAR de presentation-widget; repetição com acesso aos caches passou. Logs e hashes no snapshot; nenhuma alteração de código para contornar permissões.

Consulta OSV: 118 coordenadas/versões, zero pacotes com alertas retornados;
15 declarações omitidas. Varredura limitada de quatro padrões de segredo em
1.960 arquivos textuais sem correspondências. Migrações sintéticas aprovadas.
Assinatura v2 do ARM64 debug validada. Escopo e limites completos em
[TEST_REPORT](TEST_REPORT.md) e [comandos](evidence/SESSION_10_COMMANDS.md).

## Riscos e aceite

Relatório de qualidade entregue, sem alegar cobertura percentual ou aprovação
integral Android. UI, acessibilidade, mídia completa/offline, restore Android,
Doze/diário/bateria e performance de biblioteca representativa continuam abertos.
Políticas herdadas de rede/WebView, clientes OAuth e dependências nativas exigem
revisão antes da distribuição. Confirmação AniList permanece adiada.

Antes de abrir PR: novo fetch, incorporar alterações de `origin/yume-develop`
se necessário e revalidar; solicitar autorização para push/PR. Não integrar com
checks obrigatórios falhando. Sessão 11 exige autorização separada.

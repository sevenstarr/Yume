# docs: consolidate public guides and project portfolio

## Problema e resultado

O README e os guias de origem/contribuição misturavam a fundação histórica com
o comportamento atual. A arquitetura ainda apresentava componentes implementados
como propostas futuras. Esta revisão permite entender o incremento Yume, preparar
o ambiente por instruções públicas e avaliar decisões/testes sem prompt privado.

## Mudanças

- README com comparação upstream/Yume, uso opt-in, instalação/build, limites e
  autoria assistida pelo Codex; licença e atribuições preservadas.
- Arquitetura consolidada com fluxo Mermaid, contratos, tabelas/schema,
  concorrência/backup e decisões; texto anterior preservado integralmente.
- Guias de desenvolvimento e demonstração com snapshot fixado, comandos,
  fontes de testes, resultados esperados e limites de evidência Android.
- Duas capturas reais históricas de configurações/lote, revisadas, sem edição,
  com alt text, contexto e hashes. Não representam a identidade Yume atual.
- Origem/sincronização, contribuição, segurança, changelog e avisos atualizados;
  documentos das sessões 6–9 identificam suas limitações históricas.

## Base, escopo e validação

Branch criada diretamente de `origin/yume-develop=25d50a506` após fetch.
Proprietário autorizou incorporar os quatro commits até `91a378cee` por
fast-forward; esses commits permanecem separados do commit documental da sessão.
PR futura deve usar base `yume-develop`, após novo fetch/merge e revalidação.
Nenhuma alteração funcional ou de workflow nesta sessão.

Resultados locais e comandos: [SESSION_12_COMMANDS](evidence/SESSION_12_COMMANDS.md).
Incluem links locais, revisão de imagens/hashes, licença/histórico preservados,
testes demonstráveis, scripts de migração e checks de diff. Diferenciam a falha
de acesso da primeira tentativa, repetição e resultados históricos reutilizados.

## Critérios de aceite e limites

- [x] Descrição/diferenças/estado/origem/autoria conferíveis no código/histórico.
- [x] Build e testes descritos com versões, snapshot e caminhos de saída públicos.
- [x] Contratos, persistência, decisões e cenários demonstráveis ligados às fontes.
- [x] Capturas reais com contexto e limitações, sem biblioteca ou arte de obras.
- [ ] Capturas atuais de seleção/capas e validação visual completa em Android.
- [ ] Reprodução integral em máquina limpa por leitor independente.

Na conferência remota, PR #5 permanece aberta. O
[run 39877855871](https://github.com/sevenstarr/Yume/actions/runs/39877855871)
falhou por `sdkmanager: command not found` (exit 127), antes de testes/build.
Corrigir e revalidar o CI é pendência anterior; não declarar pipeline verde ou
artifact instalável. Demais gates Android, OAuth/provedores, licenças nativas e
assinatura/distribuição permanecem. Não há tag/release, publicação desta sessão
ou início da sessão 13. Este arquivo é apenas um rascunho local, sem PR criada.

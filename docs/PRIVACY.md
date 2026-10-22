# Dados e privacidade no Yume experimental

21/10/2026. Descrição técnica do incremento atual, antes de qualquer release Yume. O projeto deriva do Animiru/Aniyomi; extensões e serviços de terceiros têm comportamentos e políticas próprios.

## Biblioteca e backups

Biblioteca, favoritos, progresso, histórico, decisões de metadados e preferências ficam no armazenamento do aplicativo. A troca para o pacote Yume cria outro armazenamento privado: não transfere automaticamente o banco, permissões de arquivos, downloads ou sessões do Animiru. Backup e restauração são ações explícitas; os arquivos exportados podem conter sua biblioteca e preferências. Guarde-os em local protegido e não os publique em issues.

O bloco opcional de metadados no backup contém decisões de vínculo; não exporta o cache AniList, fila ou imagens externas. Não trate isso como garantia de que um backup completo não contém outros dados pessoais herdados.

## Fontes, imagens e tracking

Pesquisar/carregar fontes, reproduzir e baixar episódios usa os serviços e extensões selecionados. Esses destinos podem receber IP e dados necessários à requisição. Logins de tracking e seus tokens são separados do enriquecimento público; o código novo de metadados não cria contas nem altera listas remotas. As integrações OAuth herdadas ainda precisam de cadastros próprios e validação antes de distribuição do fork.

## Enriquecimento AniList

O recurso começa desligado. Quando autorizado, a pesquisa pública envia título e filtros fornecidos, ou um ID remoto, a `graphql.anilist.co`. Pesquisas manuais usam a conexão atual. Gatilhos de inclusão e atualização diária são opt-ins separados; o lote respeita a preferência de rede móvel e restrições de bateria/carga documentadas.

As capas selecionadas são obtidas do host de CDN permitido (`s4.anilist.co`), por transporte separado dos headers, cookies e tokens das extensões. Há cache local de resultados/imagens e associação por ID local. Rolar uma lista não dispara pesquisa de metadados. Não há servidor próprio do Yume recebendo a biblioteca nessa funcionalidade; o AniList e os hosts acessados continuam podendo observar suas requisições conforme suas políticas.

É possível desligar o recurso, restaurar visualmente a capa original, remover um vínculo, limpar caches externos ou apagar dados enriquecidos nas configurações. Desligar não apaga biblioteca ou decisões; a limpeza explícita tem escopo próprio. Veja [revisão manual](metadata/manual-review.md) e [sincronização](metadata/background-sync.md).

## Diagnóstico e contato

Relatórios de falha, logs, screenshots e backups podem revelar títulos, caminhos e informações de ambiente. Revise e remova dados pessoais antes de compartilhar. Não envie tokens, senhas ou chaves de assinatura. Vulnerabilidades podem ser relatadas pelo [canal privado de segurança](https://github.com/sevenstarr/Yume/security/advisories/new), habilitado nesta rodada. Esse canal não estabelece um prazo de resposta ou certificação de segurança.

Este documento cobre o fluxo técnico inspecionado; não certifica práticas de toda extensão externa nem substitui os termos dos serviços. O aceite Android e os gates de distribuição permanecem abertos conforme [acompanhamento](NON_DEVICE_PENDING.md).

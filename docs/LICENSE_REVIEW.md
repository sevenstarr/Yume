# Licença e atribuições — levantamento da fundação

Referência: 19/10/2026, base `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`. Levantamento documental para o fork; não é parecer jurídico nem inventário completo das dependências resolvidas. Jurisdição de distribuição não foi informada. A análise parte da licença upstream, sem presumir autorização sobre marcas ou imagens.

## Código e documentos upstream

O arquivo `LICENSE` contém Apache License 2.0 e foi mantido byte a byte em relação à base. Foram lidos também README e CONTRIBUTING da referência. Não foi encontrado arquivo NOTICE, COPYING ou licença adicional na árvore versionada auditada; isso **não prova** ausência de obrigações em artefatos de dependências.

Resumo operacional das seções 4 e 6 da [licença oficial Apache-2.0](https://www.apache.org/licenses/LICENSE-2.0): entregar a licença, identificar arquivos modificados, conservar avisos pertinentes e reproduzir atribuições de um NOTICE quando existente; marcas não são licenciadas para apresentar o fork como produto do upstream. A orientação de forks no CONTRIBUTING herdado pede nome/ícone distintos, `applicationId` distinto e alteração ou desativação do verificador de atualização.

Medidas desta sessão: `LICENSE` preservado; avisos de modificação nos arquivos herdados editados; README com copyright original; snapshots dos documentos originais; NOTICE informativo novo; changelog Yume separado. Não foi inventado titular de copyright, vínculo de endosso, licença própria de capa ou autoria humana para o trabalho do Codex.

## Escopo das atribuições

| Material | Evidência | Tratamento e limite |
| --- | --- | --- |
| Código/documentação raiz | LICENSE Apache-2.0, README e histórico | Preservar licença e avisos; registrar mudanças. |
| Créditos de adaptações Animiru | CONTRIBUTING e marcadores `AM` | Snapshot integral em `docs/upstream/CONTRIBUTING.md`; marcadores no código intactos. |
| Logo, launcher e marcas herdadas | Arquivos de recursos e README | Preservados como origem durante a preparação; substituir por arte própria aprovada antes de distribuição Yume. A licença de código não resolve automaticamente direitos de marca. |
| Dependências JVM/Android e nativas | Catálogos, `app/build.gradle.kts`, mpv e ffmpeg-kit entre as referências | Versões resolvidas e licenças transitivas ainda não verificadas; não atribuir Apache-2.0 a todo o APK. |
| Metadados e imagens AniList | Auditoria da sessão 0 | Nenhum binário de capa incorporado. Termos de API e direitos de imagem continuam pendências de distribuição, independentes da licença do código. |

## Pendência concreta de empacotamento

`app/build.gradle.kts`, bloco `packaging.resources.excludes`, exclui `LICENSE.txt`, `META-INF/LICENSE` e `META-INF/NOTICE`, entre outros. O projeto usa AboutLibraries e possui tela de licenças, mas **não foi gerado nem inspecionado** um APK ou inventário de licenças nesta sessão. Não é possível afirmar que os avisos exigidos chegam ao usuário.

Antes de distribuir: resolver dependências da variante real, inventariar licenças/avisos (incluindo bibliotecas nativas), inspecionar o APK, verificar como a tela AboutLibraries e a distribuição apresentam os textos exigidos e corrigir ausências sem remover exclusões de forma indiscriminada. Inventário/SBOM, compatibilidade e deveres específicos de cada componente ficam abertos; nenhuma varredura completa foi declarada.

O NOTICE adicionado é informativo e não substitui esses textos. Revisão do proprietário e, para incertezas materiais de direitos/distribuição, orientação jurídica qualificada são necessárias antes de publicar binários. Nenhum termo foi aceito em nome do proprietário e nenhum contato com terceiros foi feito.

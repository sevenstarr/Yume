# Identidade e coexistência — proposta da sessão 1

**Nome público aprovado: Yume**, em resposta do proprietário em 19/10/2026. A criação do fork público na conta **sevenstarr** foi autorizada e executada posteriormente no mesmo dia. Identidade visual e identificador Android ainda não aprovados. Esta sessão aplicou o nome aos documentos e templates; nenhuma identidade de runtime foi alterada.

## Matriz de mudanças futuras

| Ponto real | Estado herdado | Proposta e dependência |
| --- | --- | --- |
| `app/build.gradle.kts` | `applicationId = "xyz.Quickdev.Animiru.mi"`; namespace `eu.kanade.tachiyomi` | Pacote exclusivo do fork; candidato `io.github.sevenstarr.yume`, ainda sujeito à aprovação do proprietário. Conta sevenstarr confirmada para o fork. Manter namespace/pacotes internos para limitar conflitos upstream. |
| Variantes no mesmo arquivo | Sufixos `.dev`, `.foss`, `.preview`, `.benchmark` | Preservar separação por variante sobre o novo ID; verificar manifestos mesclados, testes e coexistência. |
| `i18n/src/commonMain/moko-resources/base/strings.xml` | `app_name` = Animiru | Alterar para Yume junto com a identidade visual aprovada. Verificar recursos gerados e nomes por variante. |
| `app/src/main/res/mipmap*/ic_launcher*`, `drawable*` e ícones de splash | Arte herdada | Criar ícone original aprovado, adaptativo e monocromático; não recolorir marca upstream e declarar autoria própria. Revisar também `.github/assets/logo.png` e referências de branding. Nenhuma arte nova nesta sessão. |
| `gradle/build-logic/src/main/kotlin/mihon/gradle/BuildConfig.kt` | `enableUpdater` pode ser ligado por `dist=ci/github/foss`; LOCAL desliga por padrão | Proposta: desabilitar updater para todas as variantes do fork até existir endpoint/release próprios. Não basta depender do padrão LOCAL. Baseline da sessão 2 usa `-Penable-updater=false`. |
| `app/src/main/java/eu/kanade/tachiyomi/data/updater/AppUpdateChecker.kt` | `GITHUB_REPO` aponta ao Animiru/preview; `RELEASE_URL` derivada desses dados | Rever checker e links de release/About em conjunto após confirmar o remoto. Não apontar Yume para APKs Animiru. |
| `app/src/main/AndroidManifest.xml` | Callback `animiru://` para seis hosts de autenticação | Proposta de esquema exclusivo `yume://`, condicionada à configuração e validação de clientes OAuth próprios. Não fazer substituição cega. |
| APIs dos trackers em `app/src/main/java/eu/kanade/tachiyomi/data/track/` | Redirects explícitos `animiru://` em Bangumi, Shikimori e Simkl; outros fluxos também dependem de cadastro externo | Inventariar cada cliente/redirect/host e validar ida e volta; IDs públicos de clientes herdados não são tokens privados nem autorização para usar o cadastro do upstream. Não pedir tokens pessoais para o enriquecimento público AniList. |
| Manifesto: links `aniyomi://add-repo` e `aniyomi://extension-store` | Entrada compartilhada para instalação de repositórios de extensões | Preservar comportamento no baseline. Avaliar futuro alias `yume://` junto com compatibilidade de links existentes, sem mudar a API das extensões nem remover links automaticamente. |
| Manifesto: FileProvider/Shizuku | Authorities usam `${applicationId}.provider` e `${applicationId}.shizuku` | Derivam do ID novo, mas precisam de inspeção do manifesto mesclado e teste real; não prometem isolamento de todos os componentes por si só. |
| About, Discord RPC, user agent e URLs auxiliares | Referências de produto Animiru no código | Rever `AboutScreen.kt`, `DiscordRPCModels.kt`, `BangumiInterceptor.kt` e busca global de nomes/URLs. Conservar referências de atribuição como origem. |
| Workflows de release, assinatura e nomes dos APKs | Guardas e artefatos upstream | Definir para Yume na sessão 11 com autorização; não retirar guardas nem reutilizar chave do Animiru. |

## Instalação, migração e recuperação

Mudar `applicationId` cria outro aplicativo, com armazenamento privado separado; não é atualização no lugar do Animiru. Certificado diferente também impede tratar um APK como atualização do mesmo pacote. O plano é permitir coexistência e migração explícita por backup, sem desinstalar o original.

Antes da distribuição: criar backup pelo app original, preservar cópia externa e testar com dados sintéticos a restauração no fork e a continuidade de favoritos, categorias, histórico, episódios e progresso. Preferências que contêm nome de pacote, permissões SAF, URIs de arquivos, diretórios de downloads e logins podem precisar ser configurados novamente. Não copiar o banco privado nem prometer transferência automática de downloads/tokens.

A auditoria identificou um possível defeito herdado no restore de campos personalizados, ainda sem reprodução. Não usar backup real do proprietário como fixture. A futura camada de metadados deve ter dados opcionais separados e restauração compatível; não há migração nova nesta sessão.

Assinatura do Yume deve ser gerenciada pelo proprietário fora do Git. Não gerar chave de release com titular inventado, não trocar chave silenciosamente e não substituir a instalação original como teste. Verificação de coexistência e atualização entre versões próprias permanece pendente de APK/dispositivo.

## Decisões necessárias

- Conta GitHub confirmada: sevenstarr. Ainda decidir se o candidato de pacote acima deve ser adotado.
- Aprovar conceito de ícone/paleta antes de alterar assets/nome no aplicativo.
- Aprovar a proposta de updater desativado e organizar clientes OAuth/redirects antes do primeiro APK distribuível.
- Definir canal privado de segurança/conduta e responsável por assinatura antes de abertura pública/distribuição.

São decisões pendentes, não pedidos para fornecer segredos. A próxima implementação de identidade deve ser pequena, isolada e comparável ao baseline. Não é autorização para iniciar outra sessão.

# Compilar e testar o Yume

Guia público da sessão 12, 22/10/2026. Não depende do prompt privado, de caminhos do mantenedor nem de segredos. O código funcional documentado está no commit `91a378cee729b45457a89c691747b49a7d3b7a16`, publicado na branch da sessão 11; a revisão documental da sessão 12 ainda é local. `yume-develop=25d50a506` ainda não contém todas as correções descritas aqui na conferência inicial.

## Ferramentas

**Limitação de CI conhecida:** na [execução 39877855871](https://github.com/sevenstarr/Yume/actions/runs/39877855871) da PR #5, o runner não tinha `sdkmanager` no PATH (exit 127). Testes/build não chegaram a executar. O guia abaixo exige instalar Command-line Tools e configurar o PATH explicitamente; a preparação equivalente do workflow remoto ainda precisa de correção e validação.

| Ferramenta | Versão / motivo |
| --- | --- |
| Git | Clone com histórico completo: o versionName usa contagem de commits |
| JDK | **21**, como em [.java-version](../.github/.java-version); o alvo Java 17 do catálogo não é o JDK de execução |
| Gradle | **9.7.1**, baixar pelo wrapper; não exige instalação global |
| Android SDK | Command-line Tools, Platform Tools, plataforma **android-37.2**, Build Tools **36.0.0 e 37.0.0** |
| NDK | **29.0.14206865**, conforme [catálogo](../gradle/mihon.versions.toml) |
| Python | **3.11+**, biblioteca padrão para scripts/testes auxiliares |
| Android de teste | API **26+**; aparelho/emulador é necessário para UI, SAF, player e WorkManager reais |

Instale JDK/Android SDK pelos distribuidores oficiais. Configure `JAVA_HOME` para o JDK 21 e `ANDROID_HOME` para seu SDK; acrescente `JAVA_HOME/bin`, `ANDROID_HOME/platform-tools` e `ANDROID_HOME/cmdline-tools/latest/bin` ao PATH. Android Studio pode gerenciar o SDK, mas não substitui o wrapper.

Com `sdkmanager` no PATH, após revisar/aceitar as licenças do SDK:

```sh
sdkmanager --licenses
sdkmanager "platform-tools" "platforms;android-37.2" "build-tools;36.0.0" "build-tools;37.0.0" "ndk;29.0.14206865"
```

A primeira resolução requer internet e espaço para SDK/NDK/caches. O Gradle reserva heap de 4 GiB; o sistema e as outras ferramentas exigem memória adicional. Não há promessa de funcionamento offline em checkout limpo.

## Obter um snapshot identificável

```sh
git clone https://github.com/sevenstarr/Yume.git Yume
cd Yume
git switch --detach 91a378cee729b45457a89c691747b49a7d3b7a16
git status --short --branch
java -version
```

Esse checkout serve para avaliar o código das sessões 10–11 sem supor que a PR #5 já foi integrada. Para contribuir, crie branch de `origin/yume-develop` conforme [CONTRIBUTING](../CONTRIBUTING.md); incorporar trabalho não integrado exige autorização. A base upstream original pode ser reproduzida pelo [guia de origem](UPSTREAM.md).

## Build e verificações

Na raiz do repositório, Windows/PowerShell:

```powershell
./gradlew.bat --version
./gradlew.bat :app:assembleDebug -Pdist=ci --console=plain --max-workers=2
./gradlew.bat spotlessCheck testDebugUnitTest verifySqlDelightMigration :app:lintDebug :data:lintDebug :source-local:lintDebug :presentation-widget:lintDebug -Pdist=ci --console=plain --max-workers=2
python -m unittest discover -s scripts/tests -v
python scripts/verify-metadata-migration.py
python scripts/verify-metadata-sync-migration.py
python scripts/check-ci-reports.py
```

Em Linux/macOS, substitua `./gradlew.bat` por `./gradlew` e use `python3` se esse for o nome do Python 3 instalado. Os comandos Gradle são os mesmos do [workflow](../.github/workflows/build.yml). `-Pdist=ci` inclui a configuração de CI, inclusive Cast; sem essa propriedade a distribuição local tem configuração diferente. Updater permanece desligado em ambas.

Não use `-Penable-updater=true`: o projeto recusa esse valor. Debug usa chave de desenvolvimento automática; não exige OAuth, conta AniList nem keystore do proprietário. Release tem processo distinto e não herda chave debug: [assinatura e distribuição](BUILD_DISTRIBUTION.md).

`scripts/run-baseline.ps1` é uma alternativa para logs por tarefa no PowerShell 7; exige `-JavaHome`, `-SdkRoot` e `-GradleUserHome` fornecidos pelo usuário. Ele usa distribuição local por padrão, portanto não substitui o comando `dist=ci` acima.

## Saídas e avaliação

| Saída | Local |
| --- | --- |
| APKs debug, universal e por ABI | `app/build/outputs/apk/debug/` |
| Metadados de pacote, versão e ABI | `app/build/outputs/apk/debug/output-metadata.json` |
| Relatórios JUnit | `<módulo>/build/test-results/testDebugUnitTest/` |
| Relatórios HTML de testes | `<módulo>/build/reports/tests/testDebugUnitTest/index.html` |
| Lint XML/HTML | `<módulo>/build/reports/lint-results-debug.*` |

Sucesso Gradle deve terminar com exit 0. Tarefas `UP-TO-DATE`/`FROM-CACHE` podem reutilizar resultados; registre isso. `check-ci-reports.py` valida presença e conteúdo de relatórios, sem reexecutar testes. Lint permite avisos remanescentes, mas erros bloqueiam. O [relatório de qualidade](TEST_REPORT.md) informa as evidências anteriores e os limites Android; uma compilação nova não herda automaticamente o aceite manual anterior.

Para instalar, confira SHA-256 e certificado com `apksigner` conforme [BUILD_DISTRIBUTION](BUILD_DISTRIBUTION.md). Use aparelho/emulador e biblioteca sintéticos, sem substituir instalações pessoais. O pacote `.dev` não atualiza o Animiru. O teste manual de seleção pode enviar título/ID ao AniList após ativação explícita; os [casos automatizados](DEMONSTRATION.md) usam fakes/mocks e não precisam desse tráfego.

## Falhas de ambiente

- Confira JDK com `java -version` e `gradlew --version`; não confunda o JDK do Android Studio com o do terminal.
- Se faltar plataforma/NDK/licença, confira as versões do catálogo e o SDK apontado por `ANDROID_HOME` ou `local.properties` local ignorado. Não versione caminhos pessoais.
- Falha de rede/dependência não deve ser contornada removendo verificações ou mudando versões sem diagnóstico. Preserve log sanitizado, tarefa e SHA.
- Se uma tarefa falhar, registre o exit code e o primeiro erro relevante. Relatório de uma execução anterior não comprova que a nova execução passou.

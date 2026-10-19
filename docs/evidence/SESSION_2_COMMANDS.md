# Sessão 2 — comandos e evidências

Data: 19/10/2026, fuso do proprietário `America/Sao_Paulo`. Branch local `chore/session-2-baseline`, criada a partir de `4d67e3a0e0df2b7bf964370ee1e47b2bdfa34d8e`. Base funcional: `0b9bb0a347aa7bcee226d8782d98d9abafc34b32`. Registro preparado com assistência do Codex; resultados provenientes das execuções indicadas, sem atribuir os recursos herdados ao fork.

## Inspeção e preparação

- Lidos o prompt mestre em `C:\Users\Administrator\Downloads\prompt.md`, progresso das sessões anteriores, build scripts, catálogos, workflows, configuração de testes e modelo/restorer de backup.
- `git status --short --branch`, `git log -5 --oneline` e `git diff <base> HEAD --name-only`: fundação inicial limpa; diferenças anteriores somente documentais/templates/exclusões, sem código funcional.
- `git switch -c chore/session-2-baseline`: executado, sem alterar remotos.
- `.github/.java-version`: 21. `Get-Command java` resolveu Java 25 no PATH; execução direta de `C:\Program Files\Java\jdk-21.0.11\bin\java.exe -version` confirmou 21.0.11+9-LTS-211. Seleção por variável de processo, sem configuração global.
- `rg --files app/src/test domain/src/test` e contagem de `@Test`: 52 métodos em seis arquivos. O número executado foi verificado posteriormente nos XML, sem tomar o inventário estático como aprovação.

Diretório de ferramentas/logs: `C:\Users\Administrator\Documents\Yume\.tooling` (fora do repositório `Animiru`).

```powershell
$env:JAVA_HOME='C:\Program Files\Java\jdk-21.0.11'
$env:GRADLE_USER_HOME='C:\Users\Administrator\Documents\Yume\.tooling\gradle'
.\gradlew.bat --version --console=plain
```

Primeira tentativa sem acesso de rede: exit 1, 8,799 s; `Permission denied: getsockopt` ao buscar a distribuição. Repetição com rede autorizada: exit 0, 20,063 s; Gradle 9.7.1 executando em JDK 21. O wrapper foi preservado.

Download oficial com `Invoke-WebRequest` de `https://dl.google.com/android/repository/commandlinetools-win-15859902_latest.zip`: 9,936 s. `Get-FileHash -Algorithm SHA256` conferido com a [página Android](https://developer.android.com/studio#command-tools): `90ae805d20434428bffcb699c290860f19bb5f66a67e6b330067e3de801fb04a`. Extração local e organização em `.tooling/android-sdk/cmdline-tools/latest`; versão 22.0 confirmada no `source.properties`.

```powershell
sdkmanager.bat --sdk_root=C:\Users\Administrator\Documents\Yume\.tooling\android-sdk --list
sdkmanager.bat --sdk_root=C:\Users\Administrator\Documents\Yume\.tooling\android-sdk --licenses
sdkmanager.bat --sdk_root=C:\Users\Administrator\Documents\Yume\.tooling\android-sdk 'platform-tools' 'platforms;android-37.2' 'build-tools;37.0.0' 'emulator' 'system-images;android-36;default;x86_64'
```

Executados pela ferramenta em `cmdline-tools/latest/bin`, com JDK 21 e `ANDROID_USER_HOME` local. Catálogo: exit 0, 7,730 s. Antes do aceite de licenças, o proprietário respondeu explicitamente **“Sim, aceito os termos e autorizo a instalação local”**. `--licenses`: exit 0, 3,105 s. Instalação: exit 0, 232,374 s. O SDK Manager emitiu aviso de depreciação em favor do Android CLI; os comandos funcionaram. O build também solicitou Build Tools 36.0.0, encontrados depois no SDK instalado.

## Build, testes e verificações

Com JDK 21, cache Gradle local e `ANDROID_HOME` apontando ao SDK preparado:

```powershell
.\gradlew.bat :app:assembleDebug --console=plain --stacktrace
```

Exit 0, 1.556,010 s externos, `BUILD SUCCESSFUL in 25m 54s`; 348 tarefas acionáveis, 288 executadas e 60 de cache. Cinco APKs gerados. Avisos de traduções, APIs obsoletas/experimentais e stripping foram preservados; não houve correção funcional.

O script versionado foi exercitado primeiro com `-Tasks @('--version')`: exit 0, 3,960 s, gravação de SHA/tempo/log em JSON confirmada. As verificações seguintes usaram o próprio script:

```powershell
.\scripts\run-baseline.ps1 `
  -JavaHome 'C:\Program Files\Java\jdk-21.0.11' `
  -SdkRoot 'C:\Users\Administrator\Documents\Yume\.tooling\android-sdk' `
  -GradleUserHome 'C:\Users\Administrator\Documents\Yume\.tooling\gradle' `
  -Tasks @('testDebugUnitTest','spotlessCheck','verifySqlDelightMigration',':app:lintDebug') `
  -OutputDirectory 'C:\Users\Administrator\Documents\Yume\.tooling\logs\checks'
```

Cada tarefa recebeu `--console=plain --stacktrace --max-workers=2`. Resultados completos, inclusive lint, constam no [snapshot selecionado](SESSION_2.json). Testes: exit 0, 219,373 s; seis suítes XML de app/domain, 52 testes, zero falhas/erros/ignorados. Spotless: exit 0, 91,225 s. SQLDelight: exit 0, 12,005 s, tarefas debug/release executadas; nenhum snapshot `*.db` encontrado para provar migração histórica. Consulte o limite e a referência oficial em [BUILD_BASELINE.md](../BUILD_BASELINE.md).

Os logs brutos ficam em `.tooling/logs`; trechos selecionados e resultados XML reduzidos são versionados como evidência. Não foram versionados XML completos com propriedades de processo, credenciais, backups, APKs ou keystores.

## APKs e dispositivo

- `Get-FileHash` calculou SHA-256 dos cinco APKs; nomes, bytes e hashes estão no JSON de evidências.
- `apksigner.bat verify --verbose --print-certs` nos APKs universal e x86_64: exit 0 em ambos, assinatura v2 válida, certificado Android Debug.
- `aapt2.exe dump badging app-x86_64-debug.apk`: pacote `xyz.Quickdev.Animiru.mi.dev`, versionCode 153, versionName `0.20.0.1-8244`, mínimo 26 e target 36.
- Chave debug padrão gerada pelo AGP em `%USERPROFILE%\.android\debug.keystore`; nenhuma leitura de conteúdo privado ou inclusão no Git. Apenas o certificado público foi inspecionado no APK.
- `emulator.exe -accel-check`: exit 6, driver do hipervisor Android ausente.
- `avdmanager.bat list device -c`, `create avd` com Pixel 6/API 36 e `list avd`: AVD próprio `yume_session2_api36` confirmado. Aviso `devices.xml` ausente registrado; listagem e arquivos confirmaram criação.
- `adb version`: 37.0.1. A execução restrita inicialmente falhou ao resolver diretório Android; com ambiente local e execução autorizada funcionou. `adb devices -l` inicial não mostrou dispositivo físico.
- Boot do AVD com porta 5580, sem janela/snapshot/áudio, software SwiftShader e aceleração desligada. A segunda tentativa também desabilitou Vulkan e métricas; ADB mostrou `offline`, sem confirmação de `sys.boot_completed`. Interrompida explicitamente após 147,523 s para liberar RAM, exit -1. Não foi classificada como crash espontâneo.
- Após terminar as verificações, `gradlew --stop` encerrou um daemon do cache desta sessão. Nova tentativa de boot com `-show-kernel`, sem concorrência de build: exit espontâneo -1073741819 (`0xC0000005`) em 2,885 s. Instalação do APK não chegou a ocorrer. Não se concluiu uma causa raiz apenas pelo código de saída.

Nenhuma instalação sobre app ou dados pessoais, alteração de driver/hipervisor, publicação, tag, push ou PR remoto. O checklist funcional continua separado das verificações automatizadas.

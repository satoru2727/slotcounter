# Release signing

GitHub Releases の APK はプロジェクトのリリース鍵で署名しています。

- Certificate SHA-256: `df362f60b9c7eff17bc4e9e5e167b1db989de62c3bcfece835e7a6b50ecaa784`
- Keystore: `~/.slotcounter-release/release.jks` (600, このリポジトリには含めない)
- Password: `~/.slotcounter-release/password` (600)

## 重要

この keystore を失うと、インストール済みのアプリに更新を配れなくなる（Android の署名検証で弾かれる）。
`release.jks` と `password` は必ず別の場所（パスワードマネージャ等）にバックアップする。

## 署名手順

```bash
export JAVA_HOME=$HOME/.local/share/mise/installs/java/temurin-21.0.13+9.0.LTS
./gradlew :app:assembleRelease
PW=$(cat ~/.slotcounter-release/password)
$HOME/android-sdk/build-tools/36.0.0/apksigner sign \
  --ks ~/.slotcounter-release/release.jks \
  --ks-key-alias slotcounter \
  --ks-pass pass:"$PW" --key-pass pass:"$PW" \
  --out dist/slotcounter-<version>.apk \
  app/build/outputs/apk/release/app-release-unsigned.apk
```

## F-Droid との関係

F-Droid は独自の鍵で署名する。そのため GitHub 版と F-Droid 版は署名が異なり、
切り替えるときは一度アンインストールが必要（アプリ内データは消える）。
これを避けたい場合は、F-Droid の再現可能ビルド（`Binaries` + `AllowedAPKSigningKeys`）に
このリリース鍵を登録する。

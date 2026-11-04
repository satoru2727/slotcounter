# 子役カウンター (Slot Counter)

パチスロの子役カウントに特化した、完全オフラインの Android アプリです。
音量キーでカウントでき、画面を消したままでも使えます。

## 機能

- **音量キーでカウント** — 音量▲/▼に好きな子役を割り当てて、画面を見ずにカウント
- **ダブルクリックで取り消し** — 同じキーを素早く2回押すと直前のカウントを戻す
- **画面オフ対応** — 前景サービス + MediaSession で、画面を消しても音量キーを取得
- **触覚フィードバック** — カウントは短く1回、取り消しは2回振動
- **確率の自動計算** — ゲーム数を入れると子役ごとに `1/6.4` のような確率を表示
- **カウンター編集** — 追加・名前変更・色分け・並べ替え・キー割当
- **プリセット** — ジャグラー系 / ハナハナ系 / AT機汎用 / カスタム
- **履歴** — 実戦ごとにカウントと確率を保存

## 使い方

1. 機種名を入れてカウンター構成を選び、「カウント開始」
2. 音量▲/▼でカウント（初期設定は ▲=ブドウ、▼=チェリー）
3. データランプのゲーム数を入力すると確率が出る
4. 「実戦終了」で履歴に保存

画面を消してもカウントできます。通知の「取り消し」からも戻せます。

## 画面オフの仕組み

Android は通常、バックグラウンドのアプリに音量キーを渡しません。このアプリは
`CountingService`（前景サービス、`mediaPlayback`）が `MediaSession` に
`VolumeProvider` を接続し、画面オフの間だけセッションを有効にしています。
さらに、音量キーのルーティングを確実にするため、画面オフ中は無音の
`AudioTrack` をループ再生します（設定でオフにできます）。

画面が点いているときは MediaSession を無効にし、Activity の `onKeyDown` で
受けます。つまり、画面オフ中だけがサービス担当です。

### 既知の制約

- 音楽アプリが再生中のときは、音量キーがそちらに渡ることがあります。
- 端末メーカーの強い電池最適化は前景サービスを止めることがあります。
  設定 → アプリ → 電池 → 「制限なし」にしてください。
- Android 13 以降は通知許可を推奨します（拒否してもカウント自体は動きます）。
- **実機での画面オフ動作は未検証です。** エミュレータでは音量キーを再現できないため、
  実機で「画面ロック → 音量キー → 解除してカウント増加」を確認してください。

## ビルド

```bash
export JAVA_HOME=$HOME/.local/share/mise/installs/java/temurin-21.0.13+9.0.LTS
./gradlew :app:assembleDebug
./gradlew :app:testDebugUnitTest :app:lintDebug
./gradlew :app:assembleRelease        # F-Droid が署名するため unsigned のまま
```

- JDK 21 / compileSdk 36 / minSdk 29
- `local.properties` に `sdk.dir=...` が必要
- 依存は AndroidX / Room / DataStore のみ（独自ブロブなし）

## F-Droid

- applicationId: `io.github.satoru2727.slotcounter`
- ライセンス: GPL-3.0
- INTERNET 権限なし、広告・解析なし、`allowBackup=false`
- `fastlane/metadata/android/{en-US,ja-JP}` を含む
- release ビルドに `signingConfig` を置かない（F-Droid の署名と衝突させない）

## プライバシー

カウント・履歴・設定は端末内の Room データベースと DataStore にだけ保存されます。
外部送信は一切ありません。アンインストールすると消えます（v1 ではエクスポート未実装）。

## ライセンス

GPL-3.0. `LICENSE` を参照。

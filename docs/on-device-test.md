# 実機テスト（画面オフ）

エミュレータでは音量キーを再現できないため、画面オフ動作は実機で確認する。

## 準備

```bash
adb install -r slotcounter-0.1.0-test.apk
adb logcat -s CountingService:V
```

必要なら通知を許可し、端末の電池最適化から除外する
（設定 → アプリ → 子役カウンター → 電池 → 制限なし）。

## チェックリスト

1. 機種名を入れて「カウント開始」（プリセットはジャグラー系）
2. 画面オン・アプリ表示中に音量▲ → ブドウが +1、短い振動
3. 音量▼ → チェリーが +1
4. 音量▲を素早く2回 → ブドウが -1、2回振動
5. 端末をロック（画面オフ）→ logcat に `screen off: media session active`
6. 画面オフのまま音量▲を3回 → 画面を点けてアプリに戻り、ブドウが +3
7. 画面オフのまま音量▼を2回 → チェリーが -1（ダブルクリック取り消し）
8. 通知の「取り消し」→ 直前のカウントが戻る
9. 音量キーで端末のメディア音量が変わっていないこと
10. 画面オン → logcat に `screen on: media session inactive`、音量キーが通常動作に戻る

## うまくいかないとき

- `volume key direction=...` が logcat に出ない
  → 音量キーが MediaSession に届いていない。画面オフ時に他アプリが音楽再生していないか確認。
- サービスが落ちている
  → 電池最適化を解除。`adb shell dumpsys activity services io.github.satoru2727.slotcounter`
- 通知が出ない
  → POST_NOTIFICATIONS を許可（拒否してもカウント自体は動く）

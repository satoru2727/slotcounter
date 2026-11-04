package io.github.satoru2727.slotcounter.core

/**
 * Counter layouts offered when a session starts. These are only names — no
 * machine odds are bundled, so nothing here can be wrong for a particular
 * machine's spec.
 */
data class CounterPreset(
    val id: String,
    val label: String,
    val counterNames: List<String>,
)

object Presets {
    val all: List<CounterPreset> = listOf(
        CounterPreset(
            id = "juggler",
            label = "ジャグラー系",
            counterNames = listOf("ブドウ", "チェリー", "ピエロ", "ベル", "リプレイ", "ボーナス"),
        ),
        CounterPreset(
            id = "hanahana",
            label = "ハナハナ系",
            counterNames = listOf("ブドウ", "チェリー", "スイカ", "ベル", "リプレイ", "ボーナス"),
        ),
        CounterPreset(
            id = "at",
            label = "AT機汎用",
            counterNames = listOf("チェリー", "スイカ", "チャンス目", "ベル", "リプレイ", "ボーナス"),
        ),
        CounterPreset(
            id = "custom",
            label = "カスタム",
            counterNames = listOf("ブドウ", "チェリー"),
        ),
    )

    val default: CounterPreset = all.first()

    fun byId(id: String): CounterPreset = all.firstOrNull { it.id == id } ?: default
}

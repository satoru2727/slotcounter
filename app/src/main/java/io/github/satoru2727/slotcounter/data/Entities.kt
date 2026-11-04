package io.github.satoru2727.slotcounter.data

import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(tableName = "sessions")
data class SessionEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val machineName: String,
    val startedAt: Long,
    /** Null while the session is still running. */
    val endedAt: Long? = null,
    val gameCount: Int = 0,
)

@Entity(
    tableName = "counters",
    foreignKeys = [
        ForeignKey(
            entity = SessionEntity::class,
            parentColumns = ["id"],
            childColumns = ["sessionId"],
            onDelete = ForeignKey.CASCADE,
        ),
    ],
    indices = [Index("sessionId")],
)
data class CounterEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val sessionId: Long,
    val name: String,
    val count: Int = 0,
    /** One of [io.github.satoru2727.slotcounter.core.KeyBinding]. */
    val keyBinding: Int = 0,
    val colorIndex: Int = 0,
    val sortOrder: Int = 0,
)

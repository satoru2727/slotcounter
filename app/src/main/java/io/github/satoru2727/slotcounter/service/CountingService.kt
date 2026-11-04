package io.github.satoru2727.slotcounter.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.AudioFormat
import android.media.AudioTrack
import android.media.VolumeProvider
import android.media.session.MediaSession
import android.media.session.PlaybackState
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import io.github.satoru2727.slotcounter.MainActivity
import io.github.satoru2727.slotcounter.R
import io.github.satoru2727.slotcounter.SlotCounterApplication
import io.github.satoru2727.slotcounter.data.CounterEntity
import io.github.satoru2727.slotcounter.data.SessionEntity
import io.github.satoru2727.slotcounter.ui.AppContainer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch

/**
 * Keeps the count alive while the screen is off.
 *
 * Android does not deliver volume keys to a normal background app. A foreground
 * service that owns an active MediaSession with a remote VolumeProvider is the
 * supported way to keep receiving them: the system routes volume-key presses to
 * [VolumeProvider.onAdjustVolume] instead of changing the ringer. The session is
 * only active while the screen is off, so volume keys behave normally the rest
 * of the time; the activity handles them directly while it is visible.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CountingService : Service() {

    private val container: AppContainer by lazy {
        (application as SlotCounterApplication).container
    }
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    private var mediaSession: MediaSession? = null
    private var silentTrack: AudioTrack? = null
    private var receiverRegistered = false
    private var screenOff = false

    private var session: SessionEntity? = null
    private var counters: List<CounterEntity> = emptyList()

    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            when (intent?.action) {
                Intent.ACTION_SCREEN_OFF -> {
                    screenOff = true
                    Log.i(TAG, "screen off: media session active")
                    setSessionActive(true)
                    startSilence()
                }
                Intent.ACTION_SCREEN_ON -> {
                    screenOff = false
                    Log.i(TAG, "screen on: media session inactive")
                    stopSilence()
                    setSessionActive(false)
                }
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        createChannel()
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            buildNotification(),
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK,
        )
        setupMediaSession()

        ContextCompat.registerReceiver(
            this,
            screenReceiver,
            IntentFilter().apply {
                addAction(Intent.ACTION_SCREEN_OFF)
                addAction(Intent.ACTION_SCREEN_ON)
            },
            ContextCompat.RECEIVER_EXPORTED,
        )
        receiverRegistered = true

        screenOff = !(getSystemService(PowerManager::class.java)?.isInteractive ?: true)
        Log.i(TAG, "service created, screenOff=$screenOff")
        setSessionActive(screenOff)
        if (screenOff) startSilence()

        observeSession()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_UNDO -> container.engine.undo()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        if (receiverRegistered) {
            runCatching { unregisterReceiver(screenReceiver) }
            receiverRegistered = false
        }
        stopSilence()
        mediaSession?.apply {
            isActive = false
            release()
        }
        mediaSession = null
        super.onDestroy()
    }

    // Session / notification -------------------------------------------------

    private fun observeSession() {
        serviceScope.launch {
            container.repository.activeSession
                .flatMapLatest { active ->
                    if (active == null) {
                        flowOf<Pair<SessionEntity?, List<CounterEntity>>>(null to emptyList())
                    } else {
                        container.repository.counters(active.id).map { list -> active to list }
                    }
                }
                .collect { (active, list) ->
                    if (active == null) {
                        stopSelf()
                        return@collect
                    }
                    session = active
                    counters = list
                    updateNotification()
                }
        }
    }

    private fun createChannel() {
        val manager = getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = getString(R.string.notification_channel_desc)
            setShowBadge(false)
            lockscreenVisibility = Notification.VISIBILITY_PUBLIC
        }
        manager.createNotificationChannel(channel)
    }

    private fun buildNotification(): Notification {
        val openApp = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val undo = PendingIntent.getService(
            this,
            1,
            Intent(this, CountingService::class.java).setAction(ACTION_UNDO),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )

        val machine = session?.machineName
        val title = if (machine.isNullOrBlank()) {
            getString(R.string.app_name)
        } else {
            getString(R.string.notification_title, machine)
        }
        val text = notificationText()

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(text)
            .setStyle(NotificationCompat.BigTextStyle().bigText(text))
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setShowWhen(false)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .setContentIntent(openApp)
            .addAction(0, getString(R.string.notification_undo), undo)
            .build()
    }

    private fun notificationText(): String {
        val parts = counters
            .sortedByDescending { it.count }
            .take(3)
            .map { getString(R.string.notification_counter, it.name, it.count) }
            .toMutableList()
        session?.let { parts += getString(R.string.notification_games, it.gameCount) }
        return if (parts.isEmpty()) getString(R.string.notification_empty) else parts.joinToString(" ・ ")
    }

    private fun updateNotification() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, android.Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return
        }
        NotificationManagerCompat.from(this).notify(NOTIFICATION_ID, buildNotification())
    }

    // Volume keys ------------------------------------------------------------

    private fun setupMediaSession() {
        val volumeProvider = object : VolumeProvider(VOLUME_CONTROL_RELATIVE, 100, 50) {
            override fun onAdjustVolume(direction: Int) {
                if (direction == 0) return
                Log.d(TAG, "volume key direction=$direction")
                // Keep the reported level in the middle so neither end of the
                // range can swallow a press.
                setCurrentVolume(50)
                container.engine.onVolumeKey(direction)
            }
        }
        mediaSession = MediaSession(this, "SlotCounter").apply {
            setPlaybackState(
                PlaybackState.Builder()
                    .setActions(PlaybackState.ACTION_PLAY_PAUSE)
                    .setState(PlaybackState.STATE_PLAYING, PlaybackState.PLAYBACK_POSITION_UNKNOWN, 1f)
                    .build(),
            )
            setPlaybackToRemote(volumeProvider)
        }
    }

    private fun setSessionActive(active: Boolean) {
        mediaSession?.isActive = active
    }

    /**
     * A silent, looping AudioTrack makes this app the current media session for
     * volume routing on OEM builds that otherwise keep the keys on the ringer.
     * It is only alive while the screen is off and costs no audible output.
     */
    private fun startSilence() {
        if (!container.silentAudioEnabled || silentTrack != null) return
        runCatching {
            val sampleRate = 8000
            val samples = ShortArray(sampleRate)
            val track = AudioTrack.Builder()
                .setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_MEDIA)
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .build(),
                )
                .setAudioFormat(
                    AudioFormat.Builder()
                        .setEncoding(AudioFormat.ENCODING_PCM_16BIT)
                        .setSampleRate(sampleRate)
                        .setChannelMask(AudioFormat.CHANNEL_OUT_MONO)
                        .build(),
                )
                .setTransferMode(AudioTrack.MODE_STATIC)
                .setBufferSizeInBytes(samples.size * 2)
                .build()
            track.write(samples, 0, samples.size)
            track.setLoopPoints(0, samples.size, -1)
            track.setVolume(0f)
            track.play()
            silentTrack = track
        }
    }

    private fun stopSilence() {
        val track = silentTrack ?: return
        silentTrack = null
        runCatching {
            track.pause()
            track.flush()
            track.release()
        }
    }

    companion object {
        private const val TAG = "CountingService"
        private const val CHANNEL_ID = "counting"
        private const val NOTIFICATION_ID = 1001
        private const val ACTION_UNDO = "io.github.satoru2727.slotcounter.action.UNDO"

        fun start(context: Context) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, CountingService::class.java),
            )
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, CountingService::class.java))
        }
    }
}

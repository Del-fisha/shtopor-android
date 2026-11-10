package com.company.shtopor.vzvod.ui.home

import android.content.Context
import android.graphics.ImageDecoder
import android.graphics.drawable.Animatable2
import android.graphics.drawable.AnimatedImageDrawable
import android.graphics.drawable.Drawable
import android.widget.ImageView
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import com.company.shtopor.vzvod.domain.PetrovichPlayback
import com.company.shtopor.vzvod.domain.PetrovichStageLogic

@Composable
fun PetrovichGifBackground(
    playback: PetrovichPlayback,
    onLoopCycleEnded: () -> Unit,
    onChangeEnded: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val onLoopUpdated by rememberUpdatedState(onLoopCycleEnded)
    val onChangeUpdated by rememberUpdatedState(onChangeEnded)
    val controller = remember { PetrovichGifController(context) }

    AndroidView(
        modifier = modifier,
        factory = { ctx ->
            ImageView(ctx).apply {
                scaleType = ImageView.ScaleType.CENTER_CROP
                controller.attach(this)
            }
        },
        update = { view ->
            controller.attach(view)
        }
    )

    LaunchedEffect(playback) {
        controller.applyPlayback(
            playback = playback,
            onLoopCycleEnded = { onLoopUpdated() },
            onChangeEnded = { onChangeUpdated() }
        )
    }
}

private class PetrovichGifController(
    private val context: Context
) {
    private var imageView: ImageView? = null
    private var drawable: AnimatedImageDrawable? = null
    private var callback: Animatable2.AnimationCallback? = null
    private var loadedAsset: String? = null
    private var awaitingLoopEnd: Boolean = false
    private var playingChange: Boolean = false

    fun attach(view: ImageView) {
        imageView = view
        drawable?.let { view.setImageDrawable(it) }
    }

    fun applyPlayback(
        playback: PetrovichPlayback,
        onLoopCycleEnded: () -> Unit,
        onChangeEnded: () -> Unit
    ) {
        val asset = PetrovichStageLogic.assetPath(playback)
        when (playback) {
            is PetrovichPlayback.Loop -> {
                awaitingLoopEnd = false
                playingChange = false
                ensureLooping(asset)
            }
            is PetrovichPlayback.FinishCurrentThenChange -> {
                playingChange = false
                if (loadedAsset == asset && drawable != null) {
                    awaitingLoopEnd = true
                    drawable?.repeatCount = 0
                    rebindCallback(
                        onEnd = {
                            if (awaitingLoopEnd) {
                                awaitingLoopEnd = false
                                onLoopCycleEnded()
                            }
                        }
                    )
                } else {
                    // Нечего доигрывать — сразу к change.
                    awaitingLoopEnd = false
                    onLoopCycleEnded()
                }
            }
            is PetrovichPlayback.PlayChangeOnce -> {
                awaitingLoopEnd = false
                playingChange = true
                loadOnce(asset) {
                    if (playingChange) {
                        playingChange = false
                        onChangeEnded()
                    }
                }
            }
        }
    }

    private fun ensureLooping(asset: String) {
        if (loadedAsset == asset && drawable != null && !playingChange) {
            drawable?.repeatCount = AnimatedImageDrawable.REPEAT_INFINITE
            if (drawable?.isRunning != true) {
                drawable?.start()
            }
            rebindCallback(onEnd = {})
            return
        }
        load(asset, repeatInfinite = true, onEnd = {})
    }

    private fun loadOnce(asset: String, onEnd: () -> Unit) {
        load(asset, repeatInfinite = false, onEnd = onEnd)
    }

    private fun load(asset: String, repeatInfinite: Boolean, onEnd: () -> Unit) {
        clearDrawable()
        try {
            val source = ImageDecoder.createSource(context.assets, asset)
            val decoded = ImageDecoder.decodeDrawable(source)
            if (decoded !is AnimatedImageDrawable) {
                imageView?.setImageDrawable(decoded)
                loadedAsset = asset
                return
            }
            drawable = decoded
            loadedAsset = asset
            decoded.repeatCount =
                if (repeatInfinite) AnimatedImageDrawable.REPEAT_INFINITE else 0
            rebindCallback(onEnd)
            imageView?.setImageDrawable(decoded)
            decoded.start()
        } catch (_: Exception) {
            loadedAsset = null
        }
    }

    private fun rebindCallback(onEnd: () -> Unit) {
        val d = drawable ?: return
        callback?.let { d.unregisterAnimationCallback(it) }
        val cb = object : Animatable2.AnimationCallback() {
            override fun onAnimationEnd(drawable: Drawable) {
                onEnd()
            }
        }
        callback = cb
        d.registerAnimationCallback(cb)
    }

    private fun clearDrawable() {
        callback?.let { cb -> drawable?.unregisterAnimationCallback(cb) }
        callback = null
        drawable?.stop()
        drawable = null
        imageView?.setImageDrawable(null)
        loadedAsset = null
    }
}

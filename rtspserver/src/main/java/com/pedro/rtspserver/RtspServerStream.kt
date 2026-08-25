package com.pedro.rtspserver

import android.content.Context
import android.media.MediaCodec
import android.os.Build
import androidx.annotation.RequiresApi
import com.pedro.common.AudioCodec
import com.pedro.common.ConnectChecker
import com.pedro.common.VideoCodec
import com.pedro.encoder.input.sources.audio.AudioSource
import com.pedro.encoder.input.sources.audio.MicrophoneSource
import com.pedro.encoder.input.sources.video.Camera2Source
import com.pedro.encoder.input.sources.video.VideoSource
import com.pedro.library.base.StreamBase
import com.pedro.rtspserver.server.RtspServer
import com.pedro.rtspserver.util.RtspServerStreamClient
import java.nio.ByteBuffer

/**
 * Created by pedro on 13/02/19.
 */
@RequiresApi(api = Build.VERSION_CODES.LOLLIPOP)
class RtspServerStream(
  context: Context, port: Int, connectChecker: ConnectChecker,
  videoSource: VideoSource, audioSource: AudioSource,
): StreamBase(context, videoSource, audioSource) {

  private val rtspServer = RtspServer(connectChecker, port)

  constructor(context: Context, port: Int, connectChecker: ConnectChecker):
      this(context, port, connectChecker, Camera2Source(context), MicrophoneSource())

  fun startStream() {
    super.startStream("")
  }

  override fun onAudioInfoImp(sampleRate: Int, isStereo: Boolean) {
    rtspServer.setAudioInfo(sampleRate, isStereo)
  }

  override fun startStreamImp(endPoint: String) {
    rtspServer.startServer()
  }

  override fun stopStreamImp() {
    rtspServer.stopServer()
  }

  override fun getAudioDataImp(audioBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtspServer.sendAudio(audioBuffer, info)
  }

  override fun onVideoInfoImp(sps: ByteBuffer, pps: ByteBuffer?, vps: ByteBuffer?) {
    val newSps = sps.duplicate()
    val newPps = pps?.duplicate()
    val newVps = vps?.duplicate()
    // Declared alongside the codec data, which is the moment the SDP becomes
    // answerable and the encoder's configured rate is known.
    rtspServer.setFps(getVideoFps())
    rtspServer.setVideoInfo(newSps, newPps, newVps)
  }

  override fun getVideoDataImp(videoBuffer: ByteBuffer, info: MediaCodec.BufferInfo) {
    rtspServer.sendVideo(videoBuffer, info)
  }

  /**
   * Video frames the sender discarded rather than send.
   *
   * The send queue is offered frames with a non-blocking trySend, so when the
   * socket cannot drain fast enough the frame is dropped instead of stalling the
   * camera — the right trade for a live stream, and an invisible one. Counting is
   * already done per client; this is the total, so a caller can say how many
   * frames left the encoder and never reached the wire.
   */
  val droppedVideoFrames: Long get() = rtspServer.droppedVideoFrames

  /**
   * Re-declare the frame rate, for clients connecting from here on.
   *
   * The rate declared at [onVideoInfoImp] is the one the encoder was CONFIGURED
   * with, which on a high-speed mode is a request rather than an outcome: a camera
   * asked for 240 can deliver 130. A receiver told 240 believes it and times the
   * stream wrongly, which is worse than the inference it would have made from
   * packet arrival had it been told nothing. Callers that measure the real rate
   * should pass it here so the SDP describes the stream instead of the intent.
   */
  fun declareFps(fps: Int) {
    rtspServer.setFps(fps)
  }

  /**
   * Declare the coded frames' display rotation (degrees clockwise) in the SDP.
   *
   * A capture the camera feeds to the encoder directly is SENSOR-oriented —
   * the pixels are landscape whatever the operator holds — and only the sender
   * knows how to stand them up. 0 says nothing, which is right for the GL path
   * whose pixels are already upright.
   */
  fun declareRotation(rotation: Int) {
    rtspServer.setRotation(rotation)
  }

  override fun getStreamClient(): RtspServerStreamClient = RtspServerStreamClient(rtspServer)

  override fun setVideoCodecImp(codec: VideoCodec) {
    rtspServer.setVideoCodec(codec)
  }

  override fun setAudioCodecImp(codec: AudioCodec) {
    rtspServer.setAudioCodec(codec)
  }
}
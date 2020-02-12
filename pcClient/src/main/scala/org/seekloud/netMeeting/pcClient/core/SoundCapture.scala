package org.seekloud.netMeeting.pcClient.core

import java.nio.{ByteBuffer, ByteOrder, ShortBuffer}
import java.util.concurrent.{LinkedBlockingDeque, ScheduledFuture, ScheduledThreadPoolExecutor, TimeUnit}

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import javax.sound.sampled.TargetDataLine
import org.bytedeco.javacv.FFmpegFrameRecorder
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.pcClient.core.EncodeActor


object SoundCapture {


  private val log = LoggerFactory.getLogger(this.getClass)
  var debug: Boolean = true

  def debug(msg: String): Unit = {
    if (debug) log.debug(msg)
  }

  sealed trait Command

  final case object StartSample extends Command

  final case object Sample extends Command

  final case object StopSample extends Command

  //  final case class SoundStartEncode(encoder: FFmpegFrameRecorder1) extends Command
  final case class SoundStartEncode(encoder: ActorRef[EncodeActor.EncodeCmd]) extends Command


  final case object StopEncode extends Command


  def create(line: TargetDataLine,
             frameRate: Int,
             sampleRate: Float,
             channels: Int,
             sampleSize: Int,
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"SoundCapture is staring...")
      val audioBufferSize = sampleRate * channels
      val audioBytes = new Array[Byte](audioBufferSize.toInt)
      ctx.self ! StartSample
      working(line, frameRate, sampleRate, channels, sampleSize, audioBytes)
    }


  private def working(line: TargetDataLine,
                      frameRate: Int,
                      sampleRate: Float,
                      channels: Int,
                      sampleSize: Int,
                      audioBytes: Array[Byte],
                      encoder: Option[ActorRef[EncodeActor.EncodeCmd]] = None,
                      audioExecutor: Option[ScheduledThreadPoolExecutor] = None,
                      audioLoop: Option[ScheduledFuture[_]] = None,
                      encodeFlag: Boolean = false): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case StartSample =>
          log.info(s"Media microphone started.")
          val audioExecutor = new ScheduledThreadPoolExecutor(1)
          val audioLoop =
            audioExecutor.scheduleAtFixedRate(
              () => {
                ctx.self ! Sample
              },
              0,
              ((1000 / frameRate) * 1000).toLong,
              TimeUnit.MICROSECONDS)
          working(line, frameRate, sampleRate, channels, sampleSize, audioBytes, encoder, Some(audioExecutor), Some(audioLoop), encodeFlag)

        case Sample =>
          try {
            val nBytesRead = line.read(audioBytes, 0, line.available)
            val nSamplesRead = if (sampleSize == 16) nBytesRead / 2 else nBytesRead
            val samples = new Array[Short](nSamplesRead)
            sampleSize match {
              case 8 =>
                val shortBuff = ShortBuffer.wrap(audioBytes.map(_.toShort))
                debug(s"8-bit sample order: ${shortBuff.order()}")
                shortBuff.get(samples)
              case 16 =>
                ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer.get(samples)
              case _ => //invalid
            }

            val sp = ShortBuffer.wrap(samples, 0, nSamplesRead)
            if (encodeFlag){
              try{
                //                encoder.foreach(_.recordSamples(sampleRate.toInt, channels, sp))
                encoder.foreach(_ ! EncodeActor.SendSample(sp))
              }catch{
                case ex:Exception=>
                  log.error(s"encode audio frame error: $ex")
              }

            }
          } catch {
            case ex: Exception =>

              log.warn(s"sample sound error: $ex")
          }
          working(line, frameRate, sampleRate, channels, sampleSize, audioBytes, encoder, audioExecutor, audioLoop, encodeFlag)

        case StopSample =>
          log.info(s"Media microphone stopped.")
          audioLoop.foreach(_.cancel(false))
          audioExecutor.foreach(_.shutdown())
          line.stop()
          line.flush()
          line.close()
          Behaviors.stopped

        case msg: SoundStartEncode =>
          working(line, frameRate, sampleRate, channels, sampleSize, audioBytes, Some(msg.encoder), audioExecutor, audioLoop, encodeFlag=true)

        case StopEncode =>
          working(line, frameRate, sampleRate, channels, sampleSize, audioBytes, None, audioExecutor, audioLoop, encodeFlag=false)

        case x =>
          log.warn(s"unknown msg in working: $x")
          Behaviors.unhandled
      }
    }


}

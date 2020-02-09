package org.seekloud.netMeeting.pcClient.core

import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, TargetDataLine}
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame, FrameGrabber, OpenCVFrameGrabber}
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.Boot.executor
import org.seekloud.netMeeting.pcClient.core.RmManager.RmCommand
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * @user: wanruolong
  * @date: 2020/2/7 18:54
  *
  */
object CaptureManager {
  val log = LoggerFactory.getLogger(this.getClass)

  object ImageType extends Enumeration{
    val Camera, Desktop = Value
  }

  case class MediaSettings(
                            imageWidth: Int,
                            imageHeight: Int,
                            frameRate: Int,
                            outputBitrate: Int,
                            needImage: Boolean,
                            sampleRate: Float,
                            sampleSizeInBits: Int,
                            channels: Int,
                            needSound: Boolean,
                            audioCodec: Int,
                            videoCodec: Int,
                            camDeviceIndex: Int,
                            audioDeviceIndex: Int
                          )

  case class EncodeConfig(
                           var imgWidth: Int = 640,
                           var imgHeight: Int = 360,
                           var frameRate: Int = 30,
                           var sampleRate: Float = 44100.0f,
                           var sampleSizeInBit: Int = 16,
                           var channels: Int = 2,
                           var audioCodec: Int = avcodec.AV_CODEC_ID_AAC,
                           var videoCodec: Int = avcodec.AV_CODEC_ID_H264,
                           var audioBitRate: Int = 192000,
                           var videoBitRate: Int = 2000000
                         )

  case class ImageMode(
                        var needCamera: Boolean = true,
                        var needDesktop: Boolean = false,
                        var x: Int = 0,
                        var y: Int = 0,
                        var rate: Int = 50
                      )

  sealed trait CaptureCommand

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends CaptureCommand

  final case class StartCaptureImage(mediaType: ImageType.Value) extends CaptureCommand

  final case object StartCaptureSound extends CaptureCommand

  final case class CameraStartSuccess(grabber: FrameGrabber, imageType: ImageType.Value) extends CaptureCommand

  final case class SoundStartSuccess(line: TargetDataLine) extends CaptureCommand

  sealed trait DrawCommand

  final case class DrawImage(image: Image) extends DrawCommand

  final case class ReSet(reset: () => Unit, offOrOn: Boolean) extends DrawCommand

  final case object StopDraw extends DrawCommand




  def create(
              rmManager: ActorRef[RmCommand],
              gc: GraphicsContext
            ): Behavior[CaptureCommand] = {
    Behaviors.setup[CaptureCommand]{ ctx =>
      log.info("ImageCapture is starting")
      implicit val stashBuffer: StashBuffer[CaptureCommand] = StashBuffer[CaptureCommand](Int.MaxValue)
      Behaviors.withTimers[CaptureCommand]{ implicit timer =>
        ctx.self ! StartCaptureImage(ImageType.Camera)
        ctx.self ! StartCaptureSound
        val imageMode = ImageMode()
        val encodeConfig = EncodeConfig()
        val drawActor = ctx.spawn(drawer(gc), "drawActor")
        idle(gc, imageMode, encodeConfig, drawActor)
      }
    }
  }

  def idle(
          gc: GraphicsContext,
          imageMode: ImageMode,
          encodeConfig: EncodeConfig,
          drawActor: ActorRef[DrawCommand],
          imageCapture: Option[ActorRef[ImageCapture.Command]] = None,
          soundCapture: Option[ActorRef[SoundCapture.Command]] = None
          )(
    implicit stashBuffer: StashBuffer[CaptureCommand],
    timer: TimerScheduler[CaptureCommand]
  ): Behavior[CaptureCommand] = {
    Behaviors.receive[CaptureCommand]{ (ctx, msg) =>
      msg match {
        case StartCaptureSound =>
          val audioFormat = new AudioFormat(encodeConfig.sampleRate, encodeConfig.sampleSizeInBit, encodeConfig.channels, true, false)
          //        val minfoSet: Array[Mixer.Info] = AudioSystem.getMixerInfo
          //        val mixer: Mixer = AudioSystem.getMixer(minfoSet(mediaSettings.audioDeviceIndex))
          val dataLineInfo = new DataLine.Info(classOf[TargetDataLine], audioFormat)

          Future {
            val line = AudioSystem.getLine(dataLineInfo).asInstanceOf[TargetDataLine]
            line.open(audioFormat)
            line.start()
            line
          }.onComplete {
            case Success(line) => ctx.self ! SoundStartSuccess(line)
            case Failure(ex) =>
              log.error("sound start failed")
              log.error(s"$ex")
          }
          Behaviors.same

        case msg: StartCaptureImage =>
          val imageGrabber = msg.mediaType match {
            case ImageType.Camera => new OpenCVFrameGrabber(0)
            case _ =>
              val grabber = new FFmpegFrameGrabber("desktop")
              grabber.setFormat("gdigrab")
              grabber
          }
          imageGrabber.setImageWidth(encodeConfig.imgWidth)
          imageGrabber.setImageHeight(encodeConfig.imgHeight)
          Future {
            log.debug(s"imageGrabber ${msg.mediaType} is starting...")
            imageGrabber.start()
            //          log.debug(s"cameraGrabber-${0} started.")
            imageGrabber
          }.onComplete {
            case Success(grabber) => ctx.self ! CameraStartSuccess(grabber, msg.mediaType)
            case Failure(ex) =>
              log.error("camera start failed")
              log.error(s"$ex")
          }
          Behaviors.same

        case msg: CameraStartSuccess =>
          val childName = s"${msg.imageType}Capture"
          val imageCapture = getImageCapture(ctx, msg.grabber, msg.imageType, Some(drawActor), childName)
          idle(gc, imageMode, encodeConfig, drawActor, Some(imageCapture), soundCapture)

        case msg: SoundStartSuccess =>
          val soundCapture = getSoundCapture(ctx, msg.line, encodeConfig.frameRate, encodeConfig.sampleRate, encodeConfig.channels, encodeConfig.sampleSizeInBit)
          idle(gc, imageMode, encodeConfig, drawActor, imageCapture, Some(soundCapture))

      }
    }
  }

  private def drawer(
                      gc: GraphicsContext,
                      needImage: Boolean = true,
                    ): Behavior[DrawCommand] =
    Behaviors.receive[DrawCommand] { (ctx, msg) =>
      msg match {
        case msg: DrawImage =>
          val sWidth = gc.getCanvas.getWidth
          val sHeight = gc.getCanvas.getHeight
          if (needImage) {
            Boot.addToPlatform {
              gc.drawImage(msg.image, 0.0, 0.0, sWidth, sHeight)
            }
          }
          Behaviors.same

        case msg: ReSet =>
          log.info("drawer reset")
          Boot.addToPlatform(msg.reset())
          drawer(gc, !msg.offOrOn)

        case StopDraw =>
          log.info(s"Capture Drawer stopped.")
          Behaviors.stopped

        case x =>
          log.warn(s"unknown msg in drawer: $x")
          Behaviors.unhandled
      }
    }

  def getImageCapture(
                       ctx: ActorContext[CaptureCommand],
                       grabber: FrameGrabber,
                       imageType: ImageType.Value,
                       drawActor: Option[ActorRef[DrawCommand]],
                       childName: String,
                       frameQueue: Option[LinkedBlockingDeque[Frame]] = None,
                       frameRate: Int = 30
                     ) = {
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(ImageCapture.create(grabber, imageType,  frameRate, drawActor, needDraw = true, frameQueue), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[ImageCapture.Command]
  }

  def getSoundCapture(ctx: ActorContext[CaptureCommand],
                      line: TargetDataLine,
                      frameRate: Int,
                      sampleRate: Float,
                      channels: Int,
                      sampleSize: Int,
                     ) = {
    val childName = s"sound-${System.currentTimeMillis()}"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(SoundCapture.create(line, frameRate, sampleRate, channels, sampleSize), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[SoundCapture.Command]
  }
}

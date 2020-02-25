package org.seekloud.netMeeting.pcClient.core

import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import javafx.scene.image.Image
import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, TargetDataLine}
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv._
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.Boot.executor
import org.seekloud.netMeeting.pcClient.core.RmManager.RmCommand
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success}
import scala.collection.mutable

/**
  * @user: wanruolong
  * @date: 2020/2/7 18:54
  *
  */
object CaptureManager {
  val log = LoggerFactory.getLogger(this.getClass)

  object MediaType extends Enumeration{
    val Camera, Desktop, Server = Value
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
                           var frameRate: Double = 30,
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

  final case object Start extends CaptureCommand

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends CaptureCommand

  final case class StartGrabberImage(mediaType: MediaType.Value) extends CaptureCommand

  final case object StartCaptureSound extends CaptureCommand

  final case object StartGrab extends CaptureCommand

  final case class ImageCaptureStartSuccess(frameRate: Double) extends CaptureCommand

  final case class SoundStartSuccess(line: TargetDataLine) extends CaptureCommand

  final case object StartEncode extends CaptureCommand

  final case object StartEncodeSuccess extends CaptureCommand

  final case object StartStreamProcessSuccess extends CaptureCommand

  final case class ChangeEncodeFlag(imageFlag: Option[Boolean] = None, soundFlag: Option[Boolean] = None) extends CaptureCommand

//  final case class Ready4GrabStream(url: String) extends CaptureCommand

  final case object Close extends CaptureCommand

  final case object Close1 extends CaptureCommand

  final case object Terminate extends CaptureCommand

  case object STOP_KEY

  sealed trait DrawCommand

  final case class DrawImage(image: Image) extends DrawCommand

  final case class ReSet(reset: () => Unit, offOrOn: Boolean) extends DrawCommand

  final case object StopDraw extends DrawCommand


  def create(
              rmManager: ActorRef[RmCommand],
              pushUrl: String,
              pullUrl: String,
              gc4Self: GraphicsContext,
              gc4Pull: GraphicsContext
            ): Behavior[CaptureCommand] = {
    Behaviors.setup[CaptureCommand]{ ctx =>
      log.info("ImageCapture is starting")
      implicit val stashBuffer: StashBuffer[CaptureCommand] = StashBuffer[CaptureCommand](Int.MaxValue)
      Behaviors.withTimers[CaptureCommand]{ implicit timer =>
        val imageMode = ImageMode()
        val encodeConfig = EncodeConfig()
        val grabberMap = new mutable.HashMap[MediaType.Value, ActorRef[ImageCapture.Command]]()
        idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag = false,  imageMode, encodeConfig, None, grabberMap)
      }
    }
  }

  def idle(
            rmManager: ActorRef[RmCommand],
            pushUrl: String,
            pullUrl: String,
            gc4Self: GraphicsContext,
            gc4Pull: GraphicsContext,
            encodeFlag: Boolean = false,
            imageMode: ImageMode,
            encodeConfig: EncodeConfig,
            drawActorOpt: Option[ActorRef[DrawCommand]],
            grabberMap: mutable.HashMap[MediaType.Value, ActorRef[ImageCapture.Command]],
            soundCaptureOpt: Option[ActorRef[SoundCapture.Command]] = None,
            recorderActorOpt: Option[ActorRef[EncodeActor.Command]] = None,
            streamProcessOpt: Option[ActorRef[StreamProcess.Command]] = None,
          )(
            implicit stashBuffer: StashBuffer[CaptureCommand],
            timer: TimerScheduler[CaptureCommand]
          ): Behavior[CaptureCommand] =
    Behaviors.receive[CaptureCommand]{ (ctx, msg) =>
      msg match {
        case Start =>
          ctx.self ! StartCaptureSound
          val drawActor = ctx.spawn(drawer(gc4Self), "drawActor")
          val childName = s"${MediaType.Camera}_capture"
          val imageCapture = getImageCapture(ctx, encodeConfig, MediaType.Camera, Some(drawActor), childName)
          grabberMap.put(MediaType.Camera, imageCapture)
          idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag,  imageMode, encodeConfig, Some(drawActor),
            grabberMap, soundCaptureOpt, recorderActorOpt, streamProcessOpt)

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

        case msg: SoundStartSuccess =>
          val soundCapture = getSoundCapture(ctx, msg.line, encodeConfig.frameRate, encodeConfig.sampleRate, encodeConfig.channels, encodeConfig.sampleSizeInBit)
          idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag, imageMode, encodeConfig, drawActorOpt, grabberMap, Some(soundCapture), recorderActorOpt, streamProcessOpt)

        case ImageCaptureStartSuccess(frameRate) =>
          encodeConfig.frameRate = frameRate
          val encodeActor = getEncoderActor(ctx, pushUrl, encodeConfig)
          idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag, imageMode, encodeConfig, drawActorOpt, grabberMap, soundCaptureOpt, Some(encodeActor), streamProcessOpt)

          //提前启动，不是响应StartEncode消息, 但两者都接收到以后才进行推流
        case StartEncodeSuccess =>
          encodeFlag match {
            case true =>
              grabberMap.foreach(_._2 ! ImageCapture.StartEncode(recorderActorOpt.get))
              soundCaptureOpt.foreach(_ ! SoundCapture.SoundStartEncode(recorderActorOpt.get))
              val streamProcess = getStreamProcess(ctx, pullUrl, encodeConfig, gc4Pull)
              idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag, imageMode, encodeConfig, drawActorOpt, grabberMap, soundCaptureOpt, recorderActorOpt, Some(streamProcess))

            case _ =>
              idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag = true, imageMode, encodeConfig, drawActorOpt, grabberMap, soundCaptureOpt, recorderActorOpt, streamProcessOpt)
          }

        case StartStreamProcessSuccess =>
          log.debug(s"got msg StartStreamProcessSuccess.")
//          grabberMap.foreach(_._2 ! ImageCapture.ChangeState(needDraw = Some(false)))
          Behaviors.same

        case msg: ChangeEncodeFlag =>
          recorderActorOpt.foreach(_ ! EncodeActor.ChangeFlag(msg.imageFlag, msg.soundFlag))
          Behaviors.same

        //接收到ws消息后
        case StartEncode =>
          log.debug(s"got msg $msg")
          encodeFlag match {
            case true =>
              grabberMap.foreach(_._2 ! ImageCapture.StartEncode(recorderActorOpt.get))
              soundCaptureOpt.foreach(_ ! SoundCapture.SoundStartEncode(recorderActorOpt.get))
              val streamProcess = getStreamProcess(ctx, pullUrl, encodeConfig, gc4Pull)
              idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag, imageMode, encodeConfig, drawActorOpt, grabberMap, soundCaptureOpt, recorderActorOpt, Some(streamProcess))

            case _ =>
              idle(rmManager, pushUrl, pullUrl, gc4Self, gc4Pull, encodeFlag = true, imageMode, encodeConfig, drawActorOpt, grabberMap, soundCaptureOpt, recorderActorOpt, streamProcessOpt)
          }

        case Close =>
          log.debug(s"stream process is not null: ${streamProcessOpt.isDefined}")
          streamProcessOpt.foreach(_ ! StreamProcess.Close)
//          timer.startSingleTimer("wait for close", Close1, 2.seconds)
          soundCaptureOpt.foreach(_ ! SoundCapture.StopSample)
          grabberMap.foreach(_._2 ! ImageCapture.Close)
          drawActorOpt.foreach(_ ! StopDraw)
          recorderActorOpt.foreach(_ ! EncodeActor.Close)
          timer.startSingleTimer("capture_manager_terminate", Terminate, 4.seconds)
          Behaviors.same

        case Close1 =>
          soundCaptureOpt.foreach(_ ! SoundCapture.StopSample)
          grabberMap.foreach(_._2 ! ImageCapture.Close)
          drawActorOpt.foreach(_ ! StopDraw)
          recorderActorOpt.foreach(_ ! EncodeActor.Close)
          timer.startSingleTimer("capture_manager_terminate", Terminate, 2.seconds)
          Behaviors.same

        case Terminate =>
          log.debug(s"capture manager terminated.")
          Behaviors.stopped

        /*case msg: ChildDead[DrawCommand] =>
          log.info(s"$msg")
          Behaviors.same
*/
        case msg: ChildDead[ImageCapture.Command] =>
          log.info(s"$msg")
          Behaviors.same

        case msg: ChildDead[SoundCapture.Command] =>
          log.info(s"$msg")
          Behaviors.same

        case msg: ChildDead[StreamProcess.Command] =>
          log.info(s"$msg")
          Behaviors.same

        case x =>
          log.info(s"rev unknown msg $x")
          Behaviors.unhandled
      }
    }


  private def drawer(
                      gc: GraphicsContext,
                      needImage: Boolean = true,
                    ): Behavior[DrawCommand] =
    Behaviors.receive[DrawCommand] { (ctx, msg) =>
      msg match {
        case msg: DrawImage =>
//          log.debug(s"got msg $msg")
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
                       encodeConfig: EncodeConfig,
                       mediaType: MediaType.Value,
                       drawActor: Option[ActorRef[DrawCommand]],
                       childName: String,
                       needDraw: Boolean = true
                     ) = {
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(ImageCapture.create(ctx.self, encodeConfig, mediaType, drawActor, needDraw), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[ImageCapture.Command]
  }

  def getSoundCapture(ctx: ActorContext[CaptureCommand],
                      line: TargetDataLine,
                      frameRate: Double,
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

  def getEncoderActor(ctx: ActorContext[CaptureCommand],
                      url: String,
                      encodeConfig: EncodeConfig,
                     ): ActorRef[EncodeActor.Command] = {
    val childName = s"encoderActor"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(EncodeActor.create(ctx.self, url, encodeConfig), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[EncodeActor.Command]
  }

  def getStreamProcess(
                        ctx: ActorContext[CaptureCommand],
                        url: String,
                        encodeConfig: EncodeConfig,
                        gc: GraphicsContext,
                        needDraw: Boolean = true
                      ): ActorRef[StreamProcess.Command] = {
    val childName = s"streamProcessActor"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(StreamProcess.create(ctx.self, url, encodeConfig, gc, needDraw), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[StreamProcess.Command]

  }
}

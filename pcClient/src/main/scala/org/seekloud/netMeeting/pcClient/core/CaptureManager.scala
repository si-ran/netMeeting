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

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends CaptureCommand

  final case class StartGrabberImage(mediaType: MediaType.Value) extends CaptureCommand

  final case object StartCaptureSound extends CaptureCommand

  final case object StartGrab extends CaptureCommand

//  final case class GrabberStartSuccess(grabber: FrameGrabber, imageType: MediaType.Value) extends CaptureCommand

  final case class ImageCaptureStartSuccess(frameRate: Double) extends CaptureCommand

  final case class SoundStartSuccess(line: TargetDataLine) extends CaptureCommand

  final case object StartEncode extends CaptureCommand

//  final case class StartEncodeSuccess(recorder: FFmpegFrameRecorder1) extends CaptureCommand

  final case object StartEncodeSuccess extends CaptureCommand

  final case object StartStreamProcessSuccess extends CaptureCommand

  final case class Ready4GrabStream(url: String) extends CaptureCommand

  final case object Close extends CaptureCommand

  final case object Terminate extends CaptureCommand

  case object STOP_KEY

  sealed trait DrawCommand

  final case class DrawImage(image: Image) extends DrawCommand

  final case class ReSet(reset: () => Unit, offOrOn: Boolean) extends DrawCommand

  final case object StopDraw extends DrawCommand


  def create(
              rmManager: ActorRef[RmCommand],
              url: String,
              gc: GraphicsContext
            ): Behavior[CaptureCommand] = {
    Behaviors.setup[CaptureCommand]{ ctx =>
      log.info("ImageCapture is starting")
      implicit val stashBuffer: StashBuffer[CaptureCommand] = StashBuffer[CaptureCommand](Int.MaxValue)
      Behaviors.withTimers[CaptureCommand]{ implicit timer =>
//        ctx.self ! StartGrabberImage(MediaType.Camera)
        ctx.self ! StartCaptureSound
        val imageMode = ImageMode()
        val encodeConfig = EncodeConfig()
        val drawActor = ctx.spawn(drawer(gc), "drawActor")
        val childName = s"${MediaType.Camera}_capture"
        val imageCapture = getImageCapture(ctx, encodeConfig, MediaType.Camera, Some(drawActor), childName)
        val grabberMap = new mutable.HashMap[MediaType.Value, ActorRef[ImageCapture.Command]]()
        grabberMap.put(MediaType.Camera, imageCapture)
        idle(url, gc, imageMode, encodeConfig, drawActor, grabberMap)
      }
    }
  }

  def idle(
            url2Server: String,
            gc: GraphicsContext,
            imageMode: ImageMode,
            encodeConfig: EncodeConfig,
            drawActor: ActorRef[DrawCommand],
            grabberMap: mutable.HashMap[MediaType.Value, ActorRef[ImageCapture.Command]],
            soundCaptureOpt: Option[ActorRef[SoundCapture.Command]] = None,
            recorderActorOpt: Option[ActorRef[EncodeActor.Command]] = None,
            streamProcessOpt: Option[ActorRef[StreamProcess.Command]] = None,
            urlFromServer: Option[String] = None
          )(
            implicit stashBuffer: StashBuffer[CaptureCommand],
            timer: TimerScheduler[CaptureCommand]
          ): Behavior[CaptureCommand] =
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

/*        case msg: StartGrabberImage =>
          val imageGrabber = msg.mediaType match {
            case MediaType.Camera => new OpenCVFrameGrabber(0)
            case MediaType.Server =>
              log.debug(s"got msg $msg")
              new FFmpegFrameGrabber(urlFromServer.get)
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
            case Success(grabber) => ctx.self ! GrabberStartSuccess(grabber, msg.mediaType)
            case Failure(ex) =>
              log.error("camera start failed")
              log.error(s"$ex")
          }
          Behaviors.same

        case msg: GrabberStartSuccess =>
          val streamProcess =
            msg.imageType match {
              case MediaType.Server =>
                //todo gc切换过程中可以使用一块布遮住
                grabberMap.filter(_._1 != MediaType.Server).foreach(_._2 ! ImageCapture.ChangeState(needDraw = Some(false)))
                val streamProcess = getStreamProcess(ctx, msg.grabber, encodeConfig, gc)
                streamProcess

              case mediaType =>
                ctx.self ! StartEncode
                val childName = s"${msg.imageType}_grabber"
                val imageCapture = getImageCapture(ctx, msg.grabber, mediaType, Some(drawActor), childName)
                grabberMap.put(msg.imageType, imageCapture)
                null
            }
          val streamOpt = if(null == streamProcess) streamProcessOpt else Some(streamProcess)
          idle(url2Server, gc, imageMode, encodeConfig, drawActor, grabberMap, soundCaptureOpt, recorderActorOpt, streamOpt, urlFromServer)*/

        case msg: SoundStartSuccess =>
          val soundCapture = getSoundCapture(ctx, msg.line, encodeConfig.frameRate, encodeConfig.sampleRate, encodeConfig.channels, encodeConfig.sampleSizeInBit)
          idle(url2Server, gc, imageMode, encodeConfig, drawActor, grabberMap, Some(soundCapture), recorderActorOpt, streamProcessOpt, urlFromServer)

        case ImageCaptureStartSuccess(frameRate) =>
          val encodeActor = getEncoderActor(ctx, url2Server, encodeConfig)
          encodeConfig.frameRate = frameRate
          idle(url2Server, gc, imageMode, encodeConfig, drawActor, grabberMap, soundCaptureOpt, Some(encodeActor), streamProcessOpt, urlFromServer)

        case StartEncodeSuccess =>
          val streamProcess = getStreamProcess(ctx, urlFromServer.get, encodeConfig, gc)
          grabberMap.foreach(_._2 ! ImageCapture.StartEncode(recorderActorOpt.get))
          soundCaptureOpt.foreach(_ ! SoundCapture.SoundStartEncode(recorderActorOpt.get))
          idle(url2Server, gc, imageMode, encodeConfig, drawActor, grabberMap, soundCaptureOpt, recorderActorOpt, Some(streamProcess), urlFromServer)

        case StartStreamProcessSuccess =>
          log.debug(s"got msg StartStreamProcessSuccess.")
          grabberMap.foreach(_._2 ! ImageCapture.ChangeState(needDraw = Some(false)))
          Behaviors.same

/*
        case StartEncode =>
          log.debug(s"push stream to $url2Server")
          val recorder = new FFmpegFrameRecorder1(url2Server, encodeConfig.imgWidth, encodeConfig.imgHeight)
          recorder.setVideoOption("tune", "zerolatency")
          recorder.setVideoOption("preset", "ultrafast")
          recorder.setVideoOption("crf", "23")
          recorder.setFormat("flv")
          recorder.setInterleaved(true)
          recorder.setGopSize(60)
          recorder.setMaxBFrames(0)

          recorder.setVideoBitrate(encodeConfig.videoBitRate)
          recorder.setVideoCodec(encodeConfig.videoCodec)
          recorder.setFrameRate(encodeConfig.frameRate)
          /*audio*/
          recorder.setAudioOption("crf", "0")
          recorder.setAudioQuality(0)
          recorder.setAudioBitrate(192000)
          recorder.setSampleRate(44100)
          recorder.setAudioChannels(encodeConfig.channels)
          recorder.setAudioCodec(encodeConfig.audioCodec)
          Future {
            log.debug(s" recorder is starting...")
            recorder.startUnsafe()
            recorder
          }.onComplete {
            case Success(recorder) => ctx.self ! StartEncodeSuccess(recorder)
            case Failure(ex) =>
              log.error("recorder start failed")
              log.error(s"$ex")
          }
          Behaviors.same

        case msg: StartEncodeSuccess =>
          //推流成功后开始拉流
          ctx.self ! StartGrabberImage(MediaType.Server)
          val recorderActor = getEncoderActor(ctx, encodeConfig, msg.recorder)
          grabberMap.filter(_._1 != MediaType.Server).foreach(_._2 ! ImageCapture.StartEncode(recorderActor))
          soundCaptureOpt.foreach(_ ! SoundCapture.SoundStartEncode(recorderActor))
          idle(url2Server, gc, imageMode, encodeConfig, drawActor, grabberMap, soundCaptureOpt, Some(recorderActor), streamProcessOpt, urlFromServer)
*/

        //接收发来的url
        case msg: Ready4GrabStream =>
          log.debug(s"got msg $msg")
          idle(url2Server, gc, imageMode, encodeConfig, drawActor, grabberMap, soundCaptureOpt, recorderActorOpt, streamProcessOpt, Some(msg.url))

        case Close =>
          soundCaptureOpt.foreach(_ ! SoundCapture.StopSample)
          grabberMap.foreach(_._2 ! ImageCapture.Close)
          drawActor ! StopDraw
          recorderActorOpt.foreach(_ ! EncodeActor.Close)
          streamProcessOpt.foreach(_ ! StreamProcess.Close)
          timer.startSingleTimer(STOP_KEY, Terminate, 1.seconds)
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
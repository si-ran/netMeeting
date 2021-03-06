package org.seekloud.netMeeting.pcClient.core

import java.nio.ShortBuffer

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import javafx.scene.image.Image
import org.bytedeco.javacv._
import org.seekloud.netMeeting.pcClient.core.CaptureManager.EncodeConfig
import org.seekloud.netMeeting.pcClient.Boot.executor
import org.seekloud.netMeeting.pcClient.utils.{FrameUtils}
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


/**
  * @user: wanruolong
  * @date: 2019/12/3 21:31
  *
  */
object EncodeActor {

  val log = LoggerFactory.getLogger(this.getClass)

  var lastTs: Long = 0

//  val file = new File("pcClient/src/main/resources/img/camera.png")
//  val bufferedImage = ImageIO.read(file)
//  val converter = new Java2DFrameConverter
  val image = new Image("/img/background.jpg")

  val frame = FrameUtils.convert(image)

  case class EncodeFlag(
                       var imageFlag: Boolean = true,
                       var soundFlag: Boolean = true
                       )

  sealed trait Command

  final case object StartEncode extends Command

  final case class StartEncodeSuccess(recorder: FFmpegFrameRecorder1) extends Command

  final case class SendFrame(frame:Frame) extends Command

  final case class SendSample(samples: ShortBuffer) extends Command

  final case class ChangeFlag(imageFlag: Option[Boolean] = None, soundFlag: Option[Boolean] = None) extends Command

  final case object Close extends Command

  final case object Restart extends Command

  final case object Terminate extends Command

  final case object TERMINATE_KEY

  object ENCODE_START_KEY

  private[this] def switchBehavior(
                                    ctx: ActorContext[Command],
                                    behaviorName: String,
                                    behavior: Behavior[Command]
                                  )(implicit stashBuffer: StashBuffer[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(
              parent: ActorRef[CaptureManager.CaptureCommand],
              url: String,
              encodeConfig: EncodeConfig,
            ): Behavior[Command] =
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command]{ implicit timer =>
        ctx.self ! StartEncode
        val encodeFlag = EncodeFlag()
        init(parent, url, encodeConfig, encodeFlag)
      }
    }

  def init(
            parent: ActorRef[CaptureManager.CaptureCommand],
            url: String,
            encodeConfig: EncodeConfig,
            encodeFlag: EncodeFlag
          )(
            implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case StartEncode =>
          log.debug(s"push stream to $url")
          val recorder = new FFmpegFrameRecorder1(url, encodeConfig.imgWidth, encodeConfig.imgHeight, encodeConfig.channels)
          recorder.setVideoOption("tune", "zerolatency")
          recorder.setVideoOption("preset", "ultrafast")
          recorder.setVideoOption("crf", "23")
          recorder.setFormat("flv")
//          recorder.setInterleaved(true)
//          recorder.setGopSize(60)
//          recorder.setMaxBFrames(0)

//          recorder.setVideoBitrate(encodeConfig.videoBitRate)
          recorder.setVideoCodec(encodeConfig.videoCodec)
          recorder.setFrameRate(encodeConfig.frameRate)
          /*audio*/
          recorder.setAudioOption("crf", "0")
          recorder.setAudioQuality(0)
//          recorder.setAudioBitrate(192000)
          recorder.setSampleRate(44100)
          recorder.setAudioChannels(encodeConfig.channels)
          recorder.setAudioCodec(encodeConfig.audioCodec)
          try{
            log.debug(s" recorder is starting...")
            recorder.start()
            ctx.self ! StartEncodeSuccess(recorder)
          }catch {
            case e: Exception =>
              log.info("recorder start failed.")
          }
/*          Future {
            log.debug(s" recorder is starting...")
            recorder.start()
            recorder
          }.onComplete {
            case Success(record) => ctx.self ! StartEncodeSuccess(record)
            case Failure(ex) =>
              log.error("recorder start failed")
              log.error(s"$ex")
          }*/
          Behaviors.same

        case msg: StartEncodeSuccess =>
          log.debug(s"recorder start success.")
          parent ! CaptureManager.StartEncodeSuccess
          switchBehavior(ctx, "work", work(parent, url, msg.recorder, encodeConfig, encodeFlag))

        case msg: ChangeFlag =>
          if(msg.imageFlag.isDefined) encodeFlag.imageFlag = msg.imageFlag.get
          if(msg.soundFlag.isDefined) encodeFlag.soundFlag = msg.soundFlag.get
          Behaviors.same

        case Close =>
          log.warn(s"close in init")
          Behaviors.stopped

        case x =>
          log.warn(s"rec unknown msg in init: $x")
          Behaviors.unhandled
      }
    }

  def work(
            parent: ActorRef[CaptureManager.CaptureCommand],
            url: String,
            encoder: FFmpegFrameRecorder1,
            encodeConfig: EncodeConfig,
            encodeFlag: EncodeFlag
          )(
            implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case msg: SendFrame =>
//          val videoTs = System.nanoTime() - ts

            try{
              //              encoder.setTimestamp(startTime * ((1000/encoderConfig.frameRate)*1000).toLong)

//              if(videoTs/1000>encoder.getTimestamp){
//                println(s"timeInterv：${System.nanoTime()/1000 - msg.ts/1000}")
//                println(s"${videoTs/1000} -> ${encoder.getTimestamp} = ${videoTs/1000-encoder.getTimestamp}=====:number${encoder.getFrameNumber}")
//                encoder.setTimestamp(videoTs/1000)
//              }
              if(msg.frame.image != null){
                if(encodeFlag.imageFlag) {
                  encoder.record(msg.frame)
                } else{
                  encoder.record(frame.clone())
                }
              }

            }catch{
              case ex:Exception=>
                if(ex.getMessage.startsWith("av_interleaved_write_frame() error")){
                  ctx.self ! Restart
                }
                log.error(s"encode image frame error: $ex")
            }
          Behaviors.same

        case msg:SendSample =>
            try{
              val cur = System.currentTimeMillis()
//              println(s"time_interval: ${cur-lastTs}")
              lastTs = cur
              if(encodeFlag.soundFlag) {
                encoder.recordSamples(encodeConfig.sampleRate.toInt, encodeConfig.channels, msg.samples)
              } else{
                val capacity = msg.samples.capacity()
                val samples = ShortBuffer.allocate(capacity)
                encoder.recordSamples(encodeConfig.sampleRate.toInt, encodeConfig.channels, samples)
              }
            }catch{
              case ex:Exception=>
                log.error(s"encode audio frame error: $ex")
                ctx.self ! Close
            }
          Behaviors.same

        case msg: ChangeFlag =>
          if(msg.imageFlag.isDefined) encodeFlag.imageFlag = msg.imageFlag.get
          if(msg.soundFlag.isDefined) encodeFlag.soundFlag = msg.soundFlag.get
          Behaviors.same

        case Close =>
          try {
            encoder.releaseUnsafe()
            log.info(s"release encode resources.")
          } catch {
            case ex: Exception =>
              log.error(s"release encode error: $ex")
          }
//          timer.startSingleTimer(TERMINATE_KEY, Terminate, 10.millis)
          Behaviors.stopped

        case Restart =>
          log.debug("restart encoder")
          try {
            encoder.releaseUnsafe()
            ctx.self ! StartEncode
            log.info(s"release encode resources.")
          } catch {
            case ex: Exception =>
              log.error(s"release encode error: $ex")
          }
          switchBehavior(ctx, "init", init(parent, url, encodeConfig, encodeFlag))

        case Terminate =>
          Behaviors.stopped

        case x =>
          log.info(s"rec unknown msg in work: $x")
          Behaviors.unhandled
      }
    }

}

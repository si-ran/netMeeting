package org.seekloud.netMeeting.pcClient.core

import java.io.{File, OutputStream}
import java.nio.ShortBuffer

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.bytedeco.javacv.{FFmpegFrameRecorder, FFmpegFrameRecorder1, Frame}
import org.seekloud.netMeeting.pcClient.core.CaptureManager.EncodeConfig
import org.slf4j.LoggerFactory

/**
  * @user: wanruolong
  * @date: 2019/12/3 21:31
  *
  */
object EncodeActor {

  val log = LoggerFactory.getLogger(this.getClass)

  var lastTs: Long = 0

  sealed trait EncodeCmd

  final case class SendFrame(frame:Frame) extends EncodeCmd

  final case class SendSample(samples: ShortBuffer) extends EncodeCmd

  final case object StopEncode extends EncodeCmd

  object ENCODE_START_KEY


  def create(parent: ActorRef[CaptureManager.CaptureCommand],
             encoder: FFmpegFrameRecorder1,
             encodeConfig: EncodeConfig,
             rtmpServer: Option[String] = None,
             file: Option[File] = None,
             outputStream: Option[OutputStream] = None): Behavior[EncodeCmd] =
    Behaviors.setup[EncodeCmd]{ ctx =>
      Behaviors.withTimers[EncodeCmd]{implicit timer =>
        work(parent, encoder, encodeConfig, rtmpServer, file, outputStream)
      }
    }

  def work(parent: ActorRef[CaptureManager.CaptureCommand],
           encoder: FFmpegFrameRecorder1,
           encoderConfig: EncodeConfig,
           rtmpServer: Option[String] = None,
           file: Option[File] = None,
           outputStream: Option[OutputStream] = None,
           startTime: Long = 0
           )(
          implicit timer: TimerScheduler[EncodeCmd]
  ): Behavior[EncodeCmd] = {
    Behaviors.receive[EncodeCmd]{ (ctx, msg) =>
      msg match {
        case msg: SendFrame =>
          val ts = if(startTime == 0) System.nanoTime() else startTime
          val videoTs = System.nanoTime() - ts

            try{
              //              encoder.setTimestamp(startTime * ((1000/encoderConfig.frameRate)*1000).toLong)

//              if(videoTs/1000>encoder.getTimestamp){
//                println(s"timeInterv：${System.nanoTime()/1000 - msg.ts/1000}")
//                println(s"${videoTs/1000} -> ${encoder.getTimestamp} = ${videoTs/1000-encoder.getTimestamp}=====:number${encoder.getFrameNumber}")
//                encoder.setTimestamp(videoTs/1000)
//              }

              encoder.record(msg.frame)

            }catch{
              case ex:Exception=>
                log.error(s"encode image frame error: $ex")
                if(ex.getMessage.startsWith("av_interleaved_write_frame() error")){
//                  parent! CaptureActor.OnEncodeException
                  ctx.self ! StopEncode
                }
            }

          work(parent, encoder, encoderConfig, rtmpServer, file, outputStream, ts)

        case msg:SendSample =>
            try{
              val cur = System.currentTimeMillis()
//              println(s"time_interval: ${cur-lastTs}")
              lastTs = cur
              encoder.recordSamples(encoderConfig.sampleRate.toInt, encoderConfig.channels, msg.samples)
            }catch{
              case ex:Exception=>
                log.error(s"encode audio frame error: $ex")
                }
          Behaviors.same

        case StopEncode =>
          try {
            encoder.releaseUnsafe()
            log.info(s"release encode resources.")
          } catch {
            case ex: Exception =>
              log.error(s"release encode error: $ex")
          }
          Behaviors.stopped

        case x =>
          log.info(s"rec unknown msg: $x")
          Behaviors.unhandled
      }
    }
  }


}

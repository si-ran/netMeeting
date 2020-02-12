package org.seekloud.netMeeting.pcClient.core

import java.io.{File, OutputStream}
import java.nio.ShortBuffer

import akka.actor.typed.scaladsl.{Behaviors, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.bytedeco.javacv.{FFmpegFrameRecorder, FFmpegFrameRecorder1, Frame}
import org.seekloud.netMeeting.pcClient.core.CaptureManager.EncodeConfig
import org.slf4j.LoggerFactory

import scala.concurrent.duration._


/**
  * @user: wanruolong
  * @date: 2019/12/3 21:31
  *
  */
object EncodeActor {

  val log = LoggerFactory.getLogger(this.getClass)

  var lastTs: Long = 0

  sealed trait Command

  final case class SendFrame(frame:Frame) extends Command

  final case class SendSample(samples: ShortBuffer) extends Command

  final case object Close extends Command

  final case object Terminate extends Command

  final case object TERMINATE_KEY

  object ENCODE_START_KEY



  def create(
              parent: ActorRef[CaptureManager.CaptureCommand],
              encoder: FFmpegFrameRecorder1,
              encodeConfig: EncodeConfig,
              rtmpServer: Option[String] = None,
              file: Option[File] = None,
              outputStream: Option[OutputStream] = None
            ): Behavior[Command] =
    Behaviors.setup[Command]{ ctx =>
      Behaviors.withTimers[Command]{ implicit timer =>
        work(parent, encoder, encodeConfig, rtmpServer, file, outputStream)
      }
    }

  def work(
            parent: ActorRef[CaptureManager.CaptureCommand],
            encoder: FFmpegFrameRecorder1,
            encoderConfig: EncodeConfig,
            rtmpServer: Option[String] = None,
            file: Option[File] = None,
            outputStream: Option[OutputStream] = None,
            startTime: Long = 0
          )(
            implicit timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command]{ (ctx, msg) =>
      msg match {
        case msg: SendFrame =>
          val ts = if(startTime == 0) System.nanoTime() else startTime
//          val videoTs = System.nanoTime() - ts

            try{
              //              encoder.setTimestamp(startTime * ((1000/encoderConfig.frameRate)*1000).toLong)

//              if(videoTs/1000>encoder.getTimestamp){
//                println(s"timeInterv：${System.nanoTime()/1000 - msg.ts/1000}")
//                println(s"${videoTs/1000} -> ${encoder.getTimestamp} = ${videoTs/1000-encoder.getTimestamp}=====:number${encoder.getFrameNumber}")
//                encoder.setTimestamp(videoTs/1000)
//              }
              if(msg.frame.image != null)
                encoder.record(msg.frame)

            }catch{
              case ex:Exception=>
                log.error(s"encode image frame error: $ex")
                if(ex.getMessage.startsWith("av_interleaved_write_frame() error")){
//                  parent! CaptureActor.OnEncodeException
                  ctx.self ! Close
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
                ctx.self ! Close
            }
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

        case x =>
          log.info(s"rec unknown msg: $x")
          Behaviors.unhandled
      }
    }

}

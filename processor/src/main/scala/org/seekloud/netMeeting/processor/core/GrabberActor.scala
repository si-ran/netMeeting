package org.seekloud.netMeeting.processor.core

import java.io.InputStream

import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.bytedeco.javacv.FFmpegFrameGrabber
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.util.{Success, Try}


/**
  * User: cq
  * Date: 2020/1/16
  */
object GrabberActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class StartGrabber(roomId: Long) extends Command

  case object StopGrabber extends Command

  case object CloseGrabber extends Command

  case object GrabFrameFirst extends Command

  case object GrabFrame extends Command

  case class Recorder(rec: ActorRef[RecorderActor.Command]) extends Command

  case object GrabLost extends Command

  case object TimerKey4Close

  def create(roomId: Long, liveId: String, url: String, recorderRef: ActorRef[RecorderActor.Command]): Behavior[Command]= {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"grabberActor start----")
          init(roomId, liveId, url, recorderRef)
      }
    }
  }

  def init(roomId: Long, liveId: String, url: String,
           recorderRef:ActorRef[RecorderActor.Command]
          )(implicit timer: TimerScheduler[Command],
            stashBuffer: StashBuffer[Command]):Behavior[Command] = {
    log.info(s"$liveId grabber turn to init")
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case t: Recorder =>
          log.info(s"${ctx.self} receive a msg $t")
          log.info(url)
          val grabber = new FFmpegFrameGrabber(url)
          Try {
            grabber.start()
          } match {
            case Success(value) =>
              log.info("start success grab")
            case e: Exception =>
              log.info(s"exception occured in creant grabber")
          }
          log.info(s"$liveId grabber start successfully")
          ctx.self ! GrabFrameFirst
          work(roomId, liveId, grabber, t.rec, url)

        case StopGrabber =>
          log.info(s"grabber $liveId stopped when init")
          Behaviors.stopped

        case x=>
          log.info(s"${ctx.self} got an unknown msg:$x")
          Behaviors.same
      }
    }
  }

  def work( roomId: Long,
            liveId: String,
            grabber: FFmpegFrameGrabber,
            recorder: ActorRef[RecorderActor.Command],
            url: String
          )(implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case GrabLost =>
          val frame = grabber.grab()
          if(frame != null){
            if(frame.image != null){
              recorder ! RecorderActor.NewFrame(liveId, frame.clone())
              ctx.self ! GrabFrame
            }else{
              ctx.self ! GrabLost
            }
          }
          Behaviors.same

        case t:Recorder =>
          Behaviors.same

        case GrabFrameFirst =>
          log.info(s"${ctx.self} receive a msg:${msg}")
          val frame = grabber.grab()
          val channel = grabber.getAudioChannels
          val sampleRate = grabber.getSampleRate
          val height = grabber.getImageHeight
          val width = grabber.getImageWidth
//          recorder ! RecorderActor.UpdateRecorder(channel, sampleRate, grabber.getFrameRate, width, height, liveId)

          if(frame != null){
            if(frame.image != null){
              recorder ! RecorderActor.NewFrame(liveId, frame.clone())
              ctx.self ! GrabFrame
            }else{
              ctx.self ! GrabLost
            }
          } else {
            log.info(s"$liveId --- frame is null")
            ctx.self ! StopGrabber
          }
          Behaviors.same

        case GrabFrame =>
          val frame = grabber.grab()
          if(frame != null) {
            recorder ! RecorderActor.NewFrame(liveId, frame.clone())
            ctx.self ! GrabFrame
          }else{
            log.info(s"$liveId --- frame is null")
            ctx.self ! StopGrabber
          }
          Behaviors.same

        case StopGrabber =>
          timer.startSingleTimer(TimerKey4Close, CloseGrabber, 400.milli)
          Behaviors.same


        case CloseGrabber =>
          try {
            log.info(s"${ctx.self} stop ----")
            grabber.release()
            grabber.close()
          }catch {
            case e:Exception =>
              log.error(s"${ctx.self} close error:$e")
          }
          Behaviors.stopped

        case x =>
          log.info(s"${ctx.self} rev an unknown msg: $x")
          Behaviors.same

      }
    }
  }

}

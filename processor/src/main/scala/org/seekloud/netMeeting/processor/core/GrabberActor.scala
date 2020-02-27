package org.seekloud.netMeeting.processor.core

import java.io.InputStream
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import org.bytedeco.javacv.{FFmpegFrameGrabber, Frame}
import org.seekloud.netMeeting.processor.Boot.executor
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}


object GrabberActor {

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  final case class GrabberStartSuccess(grabber: FFmpegFrameGrabber) extends Command

  final case object InitGrab extends Command

  final case object GrabFrame extends Command

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends Command

  final case object Close extends Command

  final case object Restart extends Command

  final case object Terminate extends Command

  private[this] def switchBehavior(
                                    ctx: ActorContext[Command],
                                    behaviorName: String,
                                    behavior: Behavior[Command]
                                  )(implicit stashBuffer: StashBuffer[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(
              parent: ActorRef[RoomActor.Command],
              needQueue: Boolean,
              roomId: Long,
              liveId: String,
              url: String,
              recorderRef: ActorRef[RecorderActor.Command],
              queue: LinkedBlockingDeque[Frame]
            ): Behavior[Command]= {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"grabberActor start----")
          ctx.self ! InitGrab
          init(parent, needQueue, roomId, liveId, url, recorderRef, queue)
      }
    }
  }

  def init(
            parent: ActorRef[RoomActor.Command],
            needQueue: Boolean,
            roomId: Long,
            liveId: String,
            url: String,
            recorderRef:ActorRef[RecorderActor.Command],
            queue: LinkedBlockingDeque[Frame]
          )(
            implicit timer: TimerScheduler[Command],
            stashBuffer: StashBuffer[Command]
          ):Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match{
        case InitGrab =>
          val grabber = new FFmpegFrameGrabber(url)
//          grabber.setImageWidth(encodeConfig.imgWidth)
//          grabber.setImageHeight(encodeConfig.imgHeight)
          grabber.setOption("rw_timeout", "2000000")
          Future {
            log.debug(s"stream grabber is starting...")
            grabber.start()
            //          log.debug(s"cameraGrabber-${0} started.")
            grabber
          }.onComplete {
            case Success(grab) =>
              log.debug(s"grab start success")
              ctx.self ! GrabberStartSuccess(grab)
            case Failure(ex) =>
              log.error("camera start failed")
              log.error(s"$ex")
          }
          Behaviors.same

        case msg: GrabberStartSuccess =>
//          ctx.self ! GrabFrame
          switchBehavior(ctx, "work", work(parent, needQueue, roomId, liveId, msg.grabber, recorderRef, queue, url))

        case x =>
          log.info(s"grab got unknown msg in init $x")
          Behaviors.unhandled
      }
    }
  }

  def work(
            parent: ActorRef[RoomActor.Command],
            needQueue: Boolean,
            roomId: Long,
            liveId: String,
            grabber: FFmpegFrameGrabber,
            recorder: ActorRef[RecorderActor.Command],
            queue: LinkedBlockingDeque[Frame],
            url: String
          )(implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]): Behavior[Command] = {
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case GrabFrame =>
          try {
            val frame = grabber.grab()
            if (null != frame) {
              if (null != frame.image) {
                if(needQueue) {
                  queue.clear()
                  queue.offer(frame.clone())
                }else {
                    //todo 发送给recorder
                }
              }
              if(null != frame.samples) {
                //todo 发送给recorder
              }
              ctx.self ! GrabFrame
            } else {
              ctx.self ! Close
            }
          } catch {
            case e: Exception =>
              ctx.self ! Close
              log.info(s"net grab error ${e.getMessage}")
          }
          Behaviors.same

        case Close =>
          try{
            grabber.stop()
          }catch {
            case e: Exception =>
              log.info(s"grab close failed ${e.getMessage}")
          }
          Behaviors.stopped

        case x =>
          log.info(s"grab got unknown msg in work $x")
          Behaviors.unhandled
      }
    }
  }

}

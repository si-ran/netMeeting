package org.seekloud.netMeeting.roomManager.core

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.netMeeting.protocol.ptcl.ChatEvent._
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
  * User: XuSiRan
  * Date: 2018/12/26
  * Time: 12:24
  */
object RoomActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class TimeOut(msg: String) extends Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private final case object BehaviorChangeKey

  final case class RAHostCreate(userFrontActor: ActorRef[WsMsg]) extends Command

  final case class RAUserJoin(userId:Long, userFrontActor: ActorRef[WsMsg]) extends Command

  private[this] def switchBehavior(
    ctx: ActorContext[Command],
    behaviorName: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]): Behavior[Command] = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  private def busy(id: Long)(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          switchBehavior(ctx, "init", init(id))

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  def init(id: Long): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"roomActor($id) is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        Behaviors.receiveMessage[Command] {
          case RAHostCreate(hostFrontActor) =>
            switchBehavior(ctx, "idle", idle(id, AppSettings.RtmpUrlHeader, id, hostFrontActor, mutable.HashMap.empty[Long, ActorRef[WsMsg]], mutable.HashMap(id -> s"$id"), s"9$id"))

          case unknownMsg =>
            log.info(s"init unknown msg : $unknownMsg")
            Behaviors.same
        }
      }
    }

  private def idle(
    roomId: Long,
    urlHeader: String,
    hostId: Long,
    hostFrontActor: ActorRef[WsMsg],
    userMap: mutable.HashMap[Long, ActorRef[WsMsg]],
    videoMap: mutable.HashMap[Long, String],
    mixUrl: String
  )(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.setup[Command]{ ctx =>
      dispatchTo(hostFrontActor, VideoUpdate(roomId, hostId, urlHeader, mixUrl,  List(VideoData(hostId, videoMap.get(hostId)))))
      Behaviors.receive[Command]{(ctx, msg) =>
        msg match {
          case RAUserJoin(userId, userFrontActor) =>
            userMap.put(userId, userFrontActor)
            videoMap.put(userId, s"$userId")
            dispatchTo(hostFrontActor, VideoUpdate(
              roomId,
              hostId,
              urlHeader,
              mixUrl,
              videoMap.map{ data => VideoData(data._1, Some(data._2))}.toList
            ))
            dispatchAllTo(userMap.values, VideoUpdate(
              roomId,
              hostId,
              urlHeader,
              mixUrl,
              videoMap.map{ data => VideoData(data._1, Some(data._2))}.toList
            ))
            Behaviors.same

          case unknownMsg =>
            log.info(s"room:$roomId idle unknown msg : $unknownMsg")
            Behaviors.same
        }
      }
    }

  }

  private def dispatchTo(subscriber: ActorRef[WsMsg], msg: MeetingBackendEvent)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscriber ! Wrap(msg.fillMiddleBuffer(sendBuffer).result())
  }
  private def dispatchAllTo(subscribers: Iterable[ActorRef[WsMsg]], msg: MeetingBackendEvent)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscribers.foreach{ subscriber =>
      subscriber ! Wrap(msg.fillMiddleBuffer(sendBuffer).result())
    }
  }

}


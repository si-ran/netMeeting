package org.seekloud.netMeeting.roomManager.core

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.AskPattern._
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.byteobject.ByteObject._
import org.seekloud.netMeeting.protocol.ptcl.ChatEvent._
import org.seekloud.netMeeting.roomManager.Boot._
import org.seekloud.netMeeting.roomManager.core.RoomManager.{RMCreateRoom, RMJoinRoom}
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.Future
import scala.util.{Failure, Success}

/**
  * User: XuSiRan
  * Date: 2018/12/26
  * Time: 12:24
  */
object UserActor {
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

  final case object TextGet extends Command

  final case class TextFailure(e: Throwable) extends Command

  final case class WsMessage(msg: MeetingClientEvent) extends Command

  final case class UserJoin(frontActor: ActorRef[WsMsg]) extends Command

  final case class UserDisconnect(frontActor: ActorRef[WsMsg]) extends Command

  final case class RoomCreateRsp(roomId: Long, errCode: Int) extends Command

  final case class RoomJoinRsp(roomId: Long, errCode: Int) extends Command

  def flow(selfActor: ActorRef[Command]): Flow[WsMessage, WsMsg, NotUsed] ={
    val in: Sink[WsMessage, NotUsed] = Flow[WsMessage].to(ActorSink.actorRef[Command](selfActor, TextGet, TextFailure))
    val out: Source[WsMsg, Unit] = ActorSource.actorRef[WsMsg](
      completionMatcher = {
        case WsComplete ⇒
          log.info("complete")
      },
      failureMatcher = {
        case WsFailure ⇒
          val a = new Throwable("fail")
          a
      },
      256,
      OverflowStrategy.dropHead).mapMaterializedValue(actor => selfActor ! UserActor.UserJoin(actor))
    Flow.fromSinkAndSource(in, out)
  }

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

  private def busy(id: Long, frontActor: ActorRef[WsMsg])(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          log.info(s"change to $name")
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case RoomCreateRsp(roomId, errCode) =>
          if(errCode == 0){
            switchBehavior(ctx, "live", live(id, roomId, frontActor))
          }
          else{
            switchBehavior(ctx, "wait", wait(id, frontActor))
          }

        case RoomJoinRsp(roomId, errCode) =>
          if(errCode == 0){
            switchBehavior(ctx, "live", live(id, roomId, frontActor))
          }
          else{
            switchBehavior(ctx, "wait", wait(id, frontActor))
          }

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
      log.info(s"userActor($id) is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        Behaviors.receiveMessage[Command] {
          case UserJoin(frontActor) =>
            ctx.watchWith(frontActor, UserDisconnect(frontActor))
            switchBehavior(ctx, "idle", wait(id, frontActor))

          case unknownMsg =>
            log.info(s"init unknown msg : $unknownMsg")
            Behaviors.same
        }
      }
    }

  private def wait(
    userId: Long,
    frontActor: ActorRef[WsMsg]
  )(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case WsMessage(frontEvent) =>
          frontEvent match {
            case Ping =>
              log.debug("ping")
              Behaviors.same

            case RoomCreate =>
              roomManager ! RMCreateRoom(userId, frontActor, ctx.self)
              switchBehavior(ctx, "busy", busy(userId, frontActor))

            case RoomJoin(roomId) =>
              roomManager ! RMJoinRoom(roomId, userId, frontActor, ctx.self)
              switchBehavior(ctx, "busy", busy(userId, frontActor))

            case e =>
              log.info(s"wait ws unknown msg $e")
              dispatchTo(frontActor, WsCommon(msg = "等待状态，不接受此ws消息"))
              Behaviors.same
          }

        case UserDisconnect(actor) => //frontActor中断
          ctx.unwatch(actor)
          Behaviors.stopped

        case TextGet => //前端连接中断
          frontActor ! WsComplete
          ctx.unwatch(frontActor)
          Behaviors.stopped

        case unknownMsg =>
          log.info(s"init unknown msg : $unknownMsg")
          Behaviors.same
      }
    }
  }

  private def live(
    userId: Long,
    roomId: Long,
    frontActor: ActorRef[WsMsg],
  )(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case WsMessage(frontEvent) =>
          frontEvent match {
            case Ping =>
              Behaviors.same

            case e =>
              log.info(s"live ws unknown msg $e")
              dispatchTo(frontActor, WsCommon(msg = "直播状态，不接受此ws消息"))
              Behaviors.same
          }

        case UserDisconnect(actor) => //frontActor中断
          println("dis")
          ctx.unwatch(actor)
          Behaviors.stopped

        case TextGet => //前端连接中断
          println("get dis")
          frontActor ! WsComplete
          ctx.unwatch(frontActor)
          Behaviors.stopped

        case unknownMsg =>
          log.info(s"live unknown msg : $unknownMsg")
          Behaviors.same
      }
    }
  }

  private def dispatchTo(subscriber: ActorRef[WsMsg], msg: MeetingBackendEvent)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscriber ! Wrap(msg.fillMiddleBuffer(sendBuffer).result())
  }

}


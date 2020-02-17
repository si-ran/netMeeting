package org.seekloud.netMeeting.roomManager.core

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
  * User: XuSiRan
  * Date: 2018/12/26
  * Time: 12:24
  */
object UserManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class TimeOut(msg: String) extends Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  final case class GameJoin(
    id: Long,
    replyTo: ActorRef[Flow[Message,Message,Any]]
  ) extends Command

  private final case class ChildDead(
    id: Long,
    actor: ActorRef[UserActor.Command]
  ) extends Command

  private final case object BehaviorChangeKey

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

  private def busy()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          switchBehavior(ctx, "init", init())

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  def init(): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"userManager is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        switchBehavior(ctx, "idle", idle(mutable.HashMap.empty[Long, ActorRef[UserActor.Command]]))
      }
    }

  private def idle(
    userMap: mutable.HashMap[Long, ActorRef[UserActor.Command]]
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case GameJoin(id, replyTo) =>
          val userActor = getUserActor(ctx, id)
          userMap.put(id, userActor)
          replyTo ! socketOk(userActor)
          Behaviors.same

        case ChildDead(id, actor) =>
          log.debug(s"user:$id is dead")
          userMap.remove(id)
          ctx.unwatch(actor)
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }

  def socketOk(selfActor: ActorRef[UserActor.Command]): Flow[Message, Message, NotUsed] ={
    import org.seekloud.byteobject.ByteObject._
    import org.seekloud.byteobject.MiddleBufferInJvm
    import scala.language.implicitConversions


    Flow[Message].map{
      case BinaryMessage.Strict(bm) =>
        val buffer = new MiddleBufferInJvm(bm.asByteBuffer)
        bytesDecode[WsMsgClient](buffer) match {
          case Right(req) =>
            UserActor.WsMessage(req)
          case Left(e) =>
            log.info(s"BinaryMessage error: $e")
            UserActor.WsMessage(EmptyFrontEvent)
        }
      case TextMessage.Strict(tm) =>
        log.info(s"$tm , ws get TextMessage and no deal")
        tm match{
          case _ =>
            UserActor.WsMessage(EmptyFrontEvent)
        }
      case _ =>
        log.info(s"errorMessage")
        UserActor.WsMessage(EmptyFrontEvent)
    }.via(UserActor.flow(selfActor))
      .map{
        case Wrap(ws) =>
          BinaryMessage.Strict(ByteString(ws))

        case unknown =>
          TextMessage(s"$unknown")
    }

  }


  private def getUserActor(ctx: ActorContext[Command], id: Long): ActorRef[UserActor.Command] = {
    val childName = s"UserActor-$id"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(UserActor.init(id), childName)
      ctx.watchWith(actor, ChildDead(id, actor))
      actor
    }.unsafeUpcast[UserActor.Command]
  }

}


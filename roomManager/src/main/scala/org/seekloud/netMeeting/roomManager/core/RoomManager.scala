package org.seekloud.netMeeting.roomManager.core

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage}
import akka.stream.scaladsl.Flow
import akka.util.ByteString
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._
import org.seekloud.netMeeting.roomManager.core.UserActor.{RoomCreateRsp, RoomJoinRsp}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.duration.FiniteDuration

/**
  * User: XuSiRan
  * Date: 2018/12/26
  * Time: 12:24
  */
object RoomManager {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class TimeOut(msg: String) extends Command

  case class RMCreateRoom(url: String, roomId: Long,  host: Long, hostFrontActor: ActorRef[WsMsgManager], replyTo: ActorRef[UserActor.Command]) extends Command

  case class RMJoinRoom(roomId: Long, userId: Long, userFrontActor: ActorRef[WsMsgManager], replyTo: ActorRef[UserActor.Command]) extends Command

  case class RMClientSpeakReq(roomId: Long, userId: Long) extends Command

  case class RMClientSpeakRsp(roomId: Long, userId: Long, acceptance: Boolean) extends Command

  case class RMMediaControlReq(roomId: Long, userId: Long, needAudio: Boolean, needVideo: Boolean) extends Command

  case class RMKickOutReq(roomId: Long, userId: Long) extends Command

  case class RMGiveHost(roomId: Long, userId: Long) extends Command

  case class RMUserExit(userId: Long, roomId: Long) extends Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private final case class ChildDead(
    id: Long,
    actor: ActorRef[RoomActor.Command]
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
      log.info(s"roomManager is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        switchBehavior(ctx, "idle", idle(mutable.HashMap.empty[Long, ActorRef[RoomActor.Command]]))
      }
    }

  private def idle(
    roomMap: mutable.HashMap[Long, ActorRef[RoomActor.Command]]
  )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case RMCreateRoom(url, roomId, hostId, hostFrontActor, replyTo) =>
          val roomActor = getRoomActor(ctx, roomId)
          roomMap.put(roomId, roomActor)
          roomActor ! RoomActor.RAHostCreate(url, hostId, hostFrontActor)
          replyTo ! RoomCreateRsp(roomId, 0)
          Behaviors.same

        case RMJoinRoom(roomId, userId, userFrontActor, replyTo) =>
          val roomActor = getOptionRoomActor(ctx, roomId)
          if(roomActor.nonEmpty){
            roomActor.foreach( _ ! RoomActor.RAUserJoin(userId, userFrontActor))
            replyTo ! RoomJoinRsp(roomId, 0)
          }
          else{
            log.debug(s"join error, no room: $roomId")
            replyTo ! RoomJoinRsp(roomId, 10001)
          }
          Behaviors.same

        case RMUserExit(uId, rId) =>
          val roomActor = getOptionRoomActor(ctx, rId)
          if(roomActor.nonEmpty){
            roomActor.foreach( _ ! RoomActor.RAUserExit(uId))
          }
          else{
            log.debug(s"user exit room error, no room: $rId")
          }
          Behaviors.same

        case ChildDead(id, actor) =>
          log.debug(s"user:$id is dead")
          roomMap.remove(id)
          ctx.unwatch(actor)
          Behaviors.same

        //接受userActor的ws消息
        case msg: RMClientSpeakReq =>
          val roomActor = getOptionRoomActor(ctx, msg.roomId)
          if(roomActor.nonEmpty){
            roomActor.foreach( _ ! RoomActor.RAClientSpeakReq(msg.userId))
          }
          else{
            log.debug(s"RMClientSpeakReq error, no room: ${msg.roomId}")
          }
          Behaviors.same

        case msg: RMClientSpeakRsp =>
          val roomActor = getOptionRoomActor(ctx, msg.roomId)
          if(roomActor.nonEmpty){
            roomActor.foreach( _ ! RoomActor.RAClientSpeakRsp(msg.userId, msg.acceptance))
          }
          else{
            log.debug(s"RMClientSpeakRsp error, no room: ${msg.roomId}")
          }
          Behaviors.same

        case msg: RMMediaControlReq =>
          val roomActor = getOptionRoomActor(ctx, msg.roomId)
          if(roomActor.nonEmpty){
            roomActor.foreach( _ ! RoomActor.RAMediaControlReq(msg.roomId, msg.userId, msg.needAudio, msg.needVideo))
          }
          else{
            log.debug(s"RMMediaControlReq error, no room: ${msg.roomId}")
          }
          Behaviors.same

        case msg: RMKickOutReq =>
          val roomActor = getOptionRoomActor(ctx, msg.roomId)
          if(roomActor.nonEmpty){
            roomActor.foreach( _ ! RoomActor.RAKickOutReq(msg.roomId, msg.userId))
          }
          else{
            log.debug(s"RMKickOutReq error, no room: ${msg.roomId}")
          }
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }

  private def getRoomActor(ctx: ActorContext[Command], roomId: Long): ActorRef[RoomActor.Command] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).getOrElse {
      val actor = ctx.spawn(RoomActor.init(roomId), childName)
      ctx.watchWith(actor, ChildDead(roomId, actor))
      actor
    }.unsafeUpcast[RoomActor.Command]
  }

  private def getOptionRoomActor(ctx: ActorContext[Command], roomId: Long): Option[ActorRef[RoomActor.Command]] = {
    val childName = s"RoomActor-$roomId"
    ctx.child(childName).map(_.unsafeUpcast[RoomActor.Command])
  }

}


package org.seekloud.netMeeting.roomManager.core

import akka.NotUsed
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import akka.actor.typed.{ActorRef, Behavior}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Flow, Sink, Source}
import akka.stream.typed.scaladsl.{ActorSink, ActorSource}
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.netMeeting.roomManager.Boot.executor
import org.seekloud.netMeeting.protocol.ptcl.CommonInfo.RoomInfo
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.utils.{ProcessorClient, TestMixSingle, VideoRecorder}
import org.slf4j.LoggerFactory

import scala.collection.mutable
import scala.concurrent.Future
import scala.concurrent.duration.FiniteDuration
import scala.util.{Failure, Success}

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

  final case object StopActor extends Command

  final case class RAHostCreate(url: String, hostId: Long, userFrontActor: ActorRef[WsMsgManager]) extends Command

  final case class RAUserJoin(userId:Long, userFrontActor: ActorRef[WsMsgManager]) extends Command

  final case class RAClientSpeakReq(userId: Long) extends Command

  final case class RAClientSpeakRsp(userId: Long, acceptance: Boolean) extends Command

  final case class RAMediaControlReq(roomId: Long, userId: Long, needAudio: Boolean, needVideo: Boolean) extends Command

  final case class RAKickOutReq(roomId: Long, userId: Long) extends Command

  final case class RAUserExit(userId: Long) extends Command

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

        case StopActor =>
          Behaviors.stopped

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  def init(roomId: Long): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"roomActor($roomId) is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      implicit val sendBuffer: MiddleBufferInJvm = new MiddleBufferInJvm(81920)
      Behaviors.withTimers[Command] { implicit timer =>
        Behaviors.receiveMessage[Command] {
          case RAHostCreate(url, hostId, hostFrontActor) =>
            val recorder = new VideoRecorder(roomId, s"rtmp://47.92.170.2:42069/live/${roomId}_x")
            val mixSingle = new TestMixSingle()
            dispatchTo(hostFrontActor, EstablishMeetingRsp())
            switchBehavior(ctx,"idle", idle(RoomInfo(roomId, List(hostId), hostId), hostFrontActor, mutable.HashMap.empty[Long, ActorRef[WsMsgManager]], url, recorder, mixSingle))

          case unknownMsg =>
            log.info(s"init unknown msg : $unknownMsg")
            Behaviors.same
        }
      }
    }

  private def idle(
    roomInfo: RoomInfo,
    hostFrontActor: ActorRef[WsMsgManager],
    userMap: mutable.HashMap[Long, ActorRef[WsMsgManager]], //无host
    mixUrl: String,
    recorder: VideoRecorder,
    mixSingle: TestMixSingle
  )(
    implicit stashBuffer: StashBuffer[Command],
    sendBuffer: MiddleBufferInJvm,
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.setup[Command]{ ctx =>
      Behaviors.receive[Command]{(ctx, msg) =>
        msg match {
          case RAUserJoin(userId, userFrontActor) =>
            userMap.put(userId, userFrontActor)
            val newRoomInfo = RoomInfo(roomInfo.roomId, roomInfo.hostId :: userMap.keys.toList, roomInfo.hostId)
//            Future{
//              mixSingle.recordStart(roomInfo.hostId :: userMap.keys.toList, roomInfo.roomId)
//            }
//            Future{
//              recorder.recordStart()
//            }.onComplete{
//              case Success(value) =>
//                log.info(s"room ${roomInfo.roomId} record ok")
//              case Failure(exception) =>
//                log.debug(s"$exception")
//            }
//            dispatchTo(hostFrontActor, JoinRsp(
//              newRoomInfo,
//              acceptance = true
//            ))
//            dispatchAllTo(userMap.values, JoinRsp(
//              newRoomInfo,
//              acceptance = true
//            ))
            ProcessorClient.newConnect(roomInfo.roomId, roomInfo.hostId :: userMap.keys.toList).map{
              case Right(value) =>
                if(value.errCode == 0){
                  Future{
                    recorder.recordStart()
                  }.onComplete{
                    case Success(value) =>
                      log.info(s"room ${roomInfo.roomId} record ok")
                    case Failure(exception) =>
                      log.debug(s"$exception")
                  }
                  dispatchTo(hostFrontActor, JoinRsp(
                    newRoomInfo,
                    acceptance = true
                  ))
                  dispatchAllTo(userMap.values, JoinRsp(
                    newRoomInfo,
                    acceptance = true
                  ))
                }
                else{
                  log.debug(s"processor error: errCode ${value.errCode}, msg ${value.msg}")
                }
              case Left(error) =>
                log.debug(s"processor error: $error")
                dispatchTo(hostFrontActor, JoinRsp(
                  newRoomInfo,
                  acceptance = false,
                  errCode = 20001,
                  msg = s"processor错误：$error"
                ))
                dispatchAllTo(userMap.values, JoinRsp(
                  newRoomInfo,
                  acceptance = false,
                  errCode = 20001,
                  msg = s"processor错误：$error"
                ))
            }
            idle(newRoomInfo, hostFrontActor, userMap, mixUrl, recorder, mixSingle)

          case RAClientSpeakReq(uId) =>
            dispatchTo(hostFrontActor, SpeakReq(roomInfo.roomId, uId))
            Behaviors.same

          case msg: RAClientSpeakRsp =>
            userMap.get(msg.userId).foreach{ actor =>
              dispatchTo(actor, SpeakRsp(roomInfo.roomId, msg.userId, msg.acceptance))
            }
            Behaviors.same

          case msg: RAMediaControlReq =>
            userMap.get(msg.userId).foreach{ actor =>
              dispatchTo(actor, MediaControlReq(msg.roomId, msg.userId, msg.needAudio, msg.needVideo))
              log.debug(s"RAMediaControlReq success room ${msg.roomId} and user ${msg.userId}")
            }
            Behaviors.same

          case msg: RAKickOutReq =>
            userMap.get(msg.userId).foreach{ actor =>
              dispatchTo(actor, KickOutReq(msg.roomId, msg.userId))
            }
            ctx.self ! RAUserExit(msg.userId)
            Behaviors.same

          case RAUserExit(uId) =>
            userMap.remove(uId)
            val userList = roomInfo.userId.filterNot(_ == uId)
            if(userList.isEmpty){
              recorder.recordStop()
              ProcessorClient.closeConnection(roomInfo.roomId)
//              mixSingle.recordStop()
              log.info(s"roomId: ${roomInfo.roomId} is empty, dead")
              Behaviors.stopped
            }
            else if(userList.length > 1){
              ProcessorClient.newConnect(roomInfo.roomId, userList)
//              mixSingle.recordStop()
//              Future{
//                while(true){
//                  if(mixSingle.grabbers.isEmpty){
//                    mixSingle.recordStart(userList, roomInfo.roomId)
//                  }
//                }
//              }
              idle(RoomInfo(roomInfo.roomId, userList, roomInfo.hostId), hostFrontActor, userMap, mixUrl, recorder, mixSingle)
            }
            else{
              idle(RoomInfo(roomInfo.roomId, userList, roomInfo.hostId), hostFrontActor, userMap, mixUrl, recorder, mixSingle)
            }

          case unknownMsg =>
            log.info(s"room:${roomInfo.roomId} idle unknown msg : $unknownMsg")
            Behaviors.same
        }
      }
    }

  }

  private def dispatchTo(subscriber: ActorRef[WsMsgManager], msg: WsMsgRm)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscriber ! Wrap(msg.fillMiddleBuffer(sendBuffer).result())
  }
  private def dispatchAllTo(subscribers: Iterable[ActorRef[WsMsgManager]], msg: WsMsgRm)(implicit sendBuffer: MiddleBufferInJvm) = {
    subscribers.foreach{ subscriber =>
      subscriber ! Wrap(msg.fillMiddleBuffer(sendBuffer).result())
    }
  }

}


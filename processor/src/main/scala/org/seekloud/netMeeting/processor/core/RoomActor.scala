package org.seekloud.netMeeting.processor.core

import java.io.{File, InputStream, OutputStream}
import java.nio.channels.Channels
import java.nio.channels.Pipe.SourceChannel
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacv.Frame
import org.seekloud.netMeeting.processor.stream.PipeStream
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.processor.common.AppSettings._
import org.seekloud.netMeeting.processor.Boot._

import scala.concurrent.duration._
import scala.collection.mutable

/**
  * User: cq
  * Date: 2020/1/16
  */
object RoomActor {
  private  val log = LoggerFactory.getLogger(this.getClass)

  case class RecorderFlag(var flag: Boolean = false)

  sealed trait Command

  case class NewRoom(roomId: Long, userIdList:List[String], pushLiveCode: String, layout: Int) extends Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case class Recorder(roomId: Long, recorderRef: ActorRef[RecorderActor.Command]) extends Command

  case class CloseRoom(roomId: Long) extends Command

  case class ChildDead4Grabber(userId: String, childName: String, value: ActorRef[GrabberActor.Command]) extends Command// fixme liveID

  case class ChildDead4Recorder(roomId: Long, childName: String, value: ActorRef[RecorderActor.Command]) extends Command

  case class ClosePipe(liveId: String) extends Command

  final case object StartRecorderSuccess extends Command

  final case class StartGrabberSuccess(userId: String) extends Command

  case object Timer4Stop

  case object Stop extends Command

  case class Timer4PipeClose(liveId: String)


  def create(roomId: Long, userIdList:List[String], pushLiveCode: String,  layout: Int): Behavior[Command]= {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"roomActor-$roomId start----")
          val recorderFlag = RecorderFlag()
          work(roomId, mutable.HashMap[String, ActorRef[GrabberActor.Command]](),
            None, List[String](), recorderFlag, mutable.HashMap[String, Boolean](),
            mutable.HashMap[String, LinkedBlockingDeque[Frame]]())
      }
    }
  }

  def work(
            roomId: Long,
            grabberMap: mutable.HashMap[String, ActorRef[GrabberActor.Command]],
            recorderOpt: Option[ActorRef[RecorderActor.Command]] = None,
            userIdList: List[String],
            recorderFlag: RecorderFlag,
            grabReadyMap: mutable.HashMap[String, Boolean],
            queueMap: mutable.HashMap[String, LinkedBlockingDeque[Frame]]
          )(implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {

        case msg:NewRoom =>
          log.info(s"${ctx.self} receive a msg $msg")
          val pushLiveUrl = s"rtmp://$srsServerUrl/live/${msg.roomId}_xyz"
          log.info(s"pushurl:$pushLiveUrl")
          val recorderActor = getRecorderActor(ctx, msg.userIdList, msg.roomId, pushLiveUrl, msg.pushLiveCode, msg.layout)
          val headUserId = msg.userIdList.head
          log.info(s"headUserId: $headUserId")
          val headUrl = s"rtmp://$srsServerUrl/live/$headUserId"
          val grabberActor1 = getGrabberActor(ctx, headUserId, false, headUrl, recorderActor, null)
          grabberMap.put(headUserId, grabberActor1)
          (1 until msg.userIdList.length).foreach{i =>
            val userId = msg.userIdList(i)
            val url = s"rtmp://$srsServerUrl/live/$userId"
            val queue = new LinkedBlockingDeque[Frame](100)
            val grabberActor = getGrabberActor(ctx, userId, true, url, recorderActor, queue)
            grabberMap.put(userId, grabberActor)
            queueMap.put(userId, queue)
          }
          recorderActor ! RecorderActor.UpdateQueue(queueMap)
          msg.userIdList.foreach{userId =>

          }
          work(roomId, grabberMap, Some(recorderActor), msg.userIdList, recorderFlag, grabReadyMap, queueMap)

        case StartRecorderSuccess =>
          log.info(s"got msg StartRecordSuccess")
          recorderFlag.flag = true
          grabberMap.foreach{item =>
            val readyOpt = grabReadyMap.get(item._1)
            if(readyOpt.isDefined && readyOpt.get){
              item._2 ! GrabberActor.GrabFrame
            }
          }
          Behaviors.same

        case StartGrabberSuccess(userId) =>
          log.info(s"got msg startGrabberSuccess:${userId}")
          grabReadyMap.put(userId, true)
          if(recorderFlag.flag) {
            val grabberOpt = grabberMap.get(userId)
            if(grabberOpt.isDefined) {
              grabberOpt.get ! GrabberActor.GrabFrame
            }
          }
          Behaviors.same


/*        case UpdateRoomInfo(roomId, layout) =>
          if(recorderMap.get(roomId).nonEmpty) {
            recorderMap.get(roomId).foreach(_ ! RecorderActor.UpdateRoomInfo(roomId,layout ))
          } else {
            log.info(s"${roomId} recorder not exist")
          }
          Behaviors.same*/

/*        case msg:Recorder =>
          log.info(s"${ctx.self} receive a msg $msg")
          val grabberActor = grabberMap.get(msg.roomId)
          if(grabberActor.isDefined){
            grabberActor.get.foreach(_ ! GrabberActor.Recorder(msg.recorderRef))
          } else {
            log.info(s"${msg.roomId} grabbers not exist")
          }
          Behaviors.same*/


        case Stop =>
          log.info(s"${ctx.self} stopped ------")
          Behaviors.stopped

        case ChildDead4Grabber(userId, childName, value) =>
          log.info(s"${childName} is dead ")
          grabberMap.remove(userId)
          Behaviors.same

        case ChildDead4Recorder(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          Behaviors.same
      }

    }
  }

  def getGrabberActor(
                       ctx: ActorContext[Command],
                       liveId: String,
                       needQueue: Boolean,
                       url:String,
                       recorderRef: ActorRef[RecorderActor.Command],
                       queue: LinkedBlockingDeque[Frame]
                     ) = {
    val childName = s"grabberActor_$liveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(GrabberActor.create(ctx.self, needQueue, liveId, url, recorderRef, queue), childName)
      ctx.watchWith(actor,ChildDead4Grabber(liveId, childName, actor))
      actor
    }.unsafeUpcast[GrabberActor.Command]
  }

  def getRecorderActor(ctx: ActorContext[Command], userIdList:List[String], roomId:Long, pushLiveUrl:String,  pushLiveCode: String,layout: Int) = {
    log.info(s"getRecorderActor started.")
    val childName = s"recorderActor_$roomId"
    val que = mutable.HashMap[String, LinkedBlockingDeque[Frame]]()
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(RecorderActor.create(ctx.self, roomId, userIdList, pushLiveUrl, layout, null), childName)
      ctx.watchWith(actor,ChildDead4Recorder(roomId, childName, actor))
      actor
    }.unsafeUpcast[RecorderActor.Command]
  }

}

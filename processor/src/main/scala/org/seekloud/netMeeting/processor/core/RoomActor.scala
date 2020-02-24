package org.seekloud.netMeeting.processor.core

import java.io.{File, InputStream, OutputStream}
import java.nio.channels.Channels
import java.nio.channels.Pipe.SourceChannel

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
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

  sealed trait Command

  case class NewRoom(roomId: Long, userIdList:List[String], pushLiveCode: String, layout: Int) extends Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case class Recorder(roomId: Long, userId:String, recorderRef: ActorRef[RecorderActor.Command]) extends Command

  case class CloseRoom(roomId: Long) extends Command

  case class ChildDead4Grabber(roomId: Long, childName: String, value: ActorRef[GrabberActor.Command]) extends Command// fixme liveID

  case class ChildDead4Recorder(roomId: Long, childName: String, value: ActorRef[RecorderActor.Command]) extends Command

  case class ClosePipe(liveId: String) extends Command

  case object Timer4Stop

  case object Stop extends Command

  case class Timer4PipeClose(liveId: String)


  def create(roomId: Long, userIdList:List[String], pushLiveCode: String,  layout: Int): Behavior[Command]= {
    Behaviors.setup[Command]{ ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"roomActor start----")
          work(mutable.Map[Long, List[ActorRef[GrabberActor.Command]]](), mutable.Map[Long,List[ActorRef[RecorderActor.Command]]](), mutable.Map[Long, List[String]]())
      }
    }
  }

  def work(
            grabberMap: mutable.Map[Long, List[ActorRef[GrabberActor.Command]]],
            recorderMap: mutable.Map[Long,List[ActorRef[RecorderActor.Command]]],
            roomLiveMap: mutable.Map[Long, List[String]]
          )(implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]):Behavior[Command] = {
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {

        case msg:NewRoom =>
          log.info(s"${ctx.self} receive a msg $msg")
          val recorderActorMap = mutable.Map[String,ActorRef[RecorderActor.Command]]()
          msg.userIdList.map{
            id =>
              val recorderActor = getRecorderActor(ctx, msg.userIdList.filter(_!=id), msg.roomId, id, msg.pushLiveCode, msg.layout)
              recorderActorMap.put(id,recorderActor)
          }
          val grabberActorList = msg.userIdList.map{
            id =>
              val url = s"rtmp://$srsServerUrl/live/$id"
              getGrabberActor(ctx, msg.roomId, id, url, msg.userIdList)
          }


//          if (isDebug) {
//            val file = new File(debugPath + msg.roomId)
//            if (!file.exists()) {
//              file.mkdir()
//            }
//          }


          grabberMap.put(msg.roomId, grabberActorList)
          recorderMap.put(msg.roomId, recorderActorMap.values.toList)
          roomLiveMap.put(msg.roomId,List())
          Behaviors.same

        case UpdateRoomInfo(roomId, layout) =>
          if(recorderMap.get(roomId).nonEmpty) {
            recorderMap.get(roomId).get.foreach(_ ! RecorderActor.UpdateRoomInfo(roomId,layout ))
          } else {
            log.info(s"${roomId} recorder not exist")
          }
          Behaviors.same

        case msg:Recorder =>
          log.info(s"${ctx.self} receive a msg $msg")
          val grabberActor = grabberMap.get(msg.roomId)
          if(grabberActor.isDefined){
            grabberActor.get.foreach(_ ! GrabberActor.Recorder(msg.userId, msg.recorderRef))
          } else {
            log.info(s"${msg.roomId} grabbers not exist")
          }
          Behaviors.same

//        case CloseRoom(roomId) =>
//          log.info(s"${ctx.self} receive a msg $msg")
//          if(grabberMap.get(roomId).nonEmpty){
//            grabberMap.get(roomId).foreach{g => g.foreach(_ ! GrabberActor.StopGrabber)}
//            grabberMap.remove(roomId)
//          } else {
//            log.info(s"${roomId}  grabbers not exist when closeRoom")
//          }
//          if(recorderMap.get(roomId).nonEmpty) {
//            recorderMap.get(roomId).foreach(_ ! RecorderActor.StopRecorder)
//            recorderMap.remove(roomId)
//          } else{
//            log.info(s"${roomId}  recorder not exist when closeRoom")
//
//          }
//          if(roomLiveMap.get(roomId).nonEmpty){
//            streamPullActor ! StreamPullActor.RoomClose(roomLiveMap(roomId))
//            roomLiveMap.get(roomId).foreach{live =>
//              live.foreach{l =>
//                pullPipeMap.get(l).foreach( a => a ! StreamPullPipe.ClosePipe)
//                timer.startSingleTimer(Timer4PipeClose(l), ClosePipe(l),1000.milli)
//              }
//            }
//            roomLiveMap.remove(roomId)
//          } else {
//            log.info(s"${roomId}  pipe not exist when closeRoom")
//          }
//          timer.startSingleTimer(Timer4Stop, Stop, 1500.milli)
//          Behaviors.same


        case Stop =>
          log.info(s"${ctx.self} stopped ------")
          Behaviors.stopped

        case ChildDead4Grabber(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          grabberMap.remove(roomId)
          Behaviors.same

        case ChildDead4Recorder(roomId, childName, value) =>
          log.info(s"${childName} is dead ")
          recorderMap.remove(roomId)
          Behaviors.same

      }

    }
  }

  def getGrabberActor(ctx: ActorContext[Command], roomId: Long, liveId: String, url:String, userIdList:List[String]) = {
    val childName = s"grabberActor_$liveId"
    ctx.child(childName).getOrElse{
      val actor = ctx.spawn(GrabberActor.create(roomId, liveId, url:String, userIdList, mutable.Map[String,ActorRef[RecorderActor.Command]]()), childName)
      ctx.watchWith(actor,ChildDead4Grabber(roomId, childName, actor))
      actor
    }.unsafeUpcast[GrabberActor.Command]
  }

  def getRecorderActor(ctx: ActorContext[Command], userIdList:List[String], roomId:Long,  userId:String,  pushLiveCode: String,layout: Int) = {
    val childName = s"recorderActor_${roomId}_$userId"
    ctx.child(childName).getOrElse{
      val pushLiveUrl = s"rtmp://$srsServerUrl/live/${roomId}_$userId"
      log.info(s"pushLiveUrl:$pushLiveUrl")
      val actor = ctx.spawn(RecorderActor.create(roomId,userIdList,pushLiveUrl, userId, layout), childName)
      ctx.watchWith(actor,ChildDead4Recorder(roomId, childName, actor))
      actor
    }.unsafeUpcast[RecorderActor.Command]
  }

//  def getPullPipe(ctx: ActorContext[Command], roomId: Long, liveId: String, out: OutputStream) = {
//    val childName = s"pullPipeActor_$liveId"
//    ctx.child(childName).getOrElse{
//      val actor = ctx.spawn(StreamPullPipe.create(roomId: Long, liveId: String, out), childName)
//      ctx.watchWith(actor, ChildDead4PullPipe(liveId, childName, actor))
//      actor
//    }.unsafeUpcast[StreamPullPipe.Command]
//  }
//
//  def getPushPipe(ctx: ActorContext[Command], roomId: Long, pushLiveId: String, pushLiveCode: String, source: SourceChannel) = {
//    val childName = s"pushPipeActor_$pushLiveId"
//    ctx.child(childName).getOrElse{
//      val actor = ctx.spawn(StreamPushPipe.create(roomId, pushLiveId, pushLiveCode, source,0l), childName)
//      ctx.watchWith(actor, ChildDead4PushPipe(pushLiveId, childName, actor) )
//      actor
//    }.unsafeUpcast[StreamPushPipe.Command]
//  }
}

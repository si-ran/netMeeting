//package org.seekloud.netMeeting.processor.core
//
//import java.io.{File, FileOutputStream, OutputStream}
//
//import akka.actor.typed.Behavior
//import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
//import org.seekloud.netMeeting.processor.Boot
//import org.seekloud.netMeeting.processor.Boot.Command
//import org.slf4j.LoggerFactory
//import Boot.{roomManager, streamPullActor, streamPushActor}
//import org.seekloud.netMeeting.processor.core.StreamPullActor.NewLive
//import org.seekloud.netMeeting.processor.common.AppSettings._
//
//import scala.concurrent.duration._
//
///**
//  * User: cq
//  * Date: 2020/1/16
//  */
//object StreamPullPipe {
//
//  sealed trait Command
//
//  case class NewBuffer(data: Array[Byte]) extends Command
//
//  case object ClosePipe extends Command
//
//  case object Timer4Stop
//
//  case object Stop extends Command
//
//  val log = LoggerFactory.getLogger(this.getClass)
//
//  def create(roomId: Long, liveId: String, out: OutputStream): Behavior[Command] = {
//    Behaviors.setup[Command] { ctx =>
//      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
//      Behaviors.withTimers[Command] {
//        implicit timer =>
//          streamPullActor ! NewLive(liveId, roomId, ctx.self)
//          val output = if (isDebug) {
//            val file = new File(s"$debugPath$roomId/${liveId}_in.ts")
//            Some(new FileOutputStream(file))
//          } else None
//          work(roomId, liveId, out, output)
//      }
//    }
//  }
//
//  def work(roomId: Long, liveId: String, out: OutputStream, fileOut:Option[FileOutputStream])(implicit timer: TimerScheduler[Command],
//                                                                                              stashBuffer: StashBuffer[Command]): Behavior[Command] = {
//    Behaviors.receive[Command] { (ctx, msg) =>
//      msg match {
//        case NewBuffer(data) =>
////          if (Boot.showStreamLog) {
////            log.info(s"NewBuffer $liveId ${data.length}")
////          }
//          fileOut.foreach(_.write(data))
//          out.write(data)
//          Behaviors.same
//
//        case ClosePipe =>
//          timer.startSingleTimer(Timer4Stop, Stop, 50.milli)
//          Behaviors.same
//
//        case Stop =>
//          log.info(s"$liveId pullPipe stopped ----")
//          fileOut.foreach(_.close())
//          out.close()
//          Behaviors.stopped
//
//        case x =>
//          log.info(s"recv unknown msg: $x")
//          Behaviors.same
//      }
//    }
//  }
//
//}

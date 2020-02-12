package org.seekloud.netMeeting.processor.core

import java.net.InetSocketAddress
import java.nio.channels.DatagramChannel

import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.seekloud.netMeeting.processor.Boot.Command
import org.slf4j.LoggerFactory

import scala.collection.mutable

/**
  * User: cq
  * Date: 2020/1/16
  */
object StreamPushActor {
  case object Timer4Heart

  case object Timer4InitSocket

  case object Time4ReConnect

  val buf = new Array[Byte](1316)

  private var count = 0

  val log = LoggerFactory.getLogger(this.getClass)


  case class NewLive(liveId:String, liveCode:String) extends Command

  //  case class Packet4Dispatcher(dataArray:Array[Byte]) extends Command

  case class Packet4Rtp(data:Array[Byte]) extends Command

  //fixme 此处是否还有作用
  case class ReAuth(liveId:String, liveCode:String) extends Command

  case class ReAuthTimer(liveId:String)

  case class PushData(liveId: String,data: Array[Byte]) extends Command

  //  case object InitSocket extends Command

  case object ReConnectSocket extends Command

  case class Timer4AuthAgain(liveId:String)

  private val liveIdCodeMap = mutable.Map[String, String]()
  private val authCountMap = mutable.Map[String, Int]()

  //  var output:OutputStream = null

  def create(): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
//          log.info(s"StreamPushActor start----")
//          val pushStreamChannel = DatagramChannel.open()
//          pushStreamChannel.socket().setReuseAddress(true)
//          val pushStreamDst = new InetSocketAddress(rtpToHost, 61041)
//          val host = "0.0.0.0"
//          val port = getRandomPort()
//          val client = new PushStreamClient(host,port,pushStreamDst,ctx.self,rtpServerDst)
//          client.authStart()
//          work(client)
          work()

      }
    }
  }

  def getRandomPort() = {
    val channel = DatagramChannel.open()
    val port = channel.socket().getLocalPort
    channel.close()
    port
  }

  def work()(implicit timer: TimerScheduler[Command],
                                    stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match{
        case x =>
          log.info("unknown message")
          Behaviors.same
        case PushData(liveId, data)=>
          log.info(s"push data:$data")
          Behaviors.same
//
//        case t:NewLive =>
//          client.auth(t.liveId, t.liveCode)
//          timer.startSingleTimer(Timer4AuthAgain(t.liveId), ReAuth(t.liveId, t.liveCode), 10.seconds)
//          log.info(s"${t.liveId} start push auth ----")
//          authCountMap.put(t.liveId, 0)
//          liveIdCodeMap.put(t.liveId, t.liveCode)
//          Behaviors.same
//
//        case ReAuth(liveId, liveCode) =>
//          client.auth(liveId, liveCode)
//          Behaviors.same
//
//        case t:AuthRsp =>
//          if(t.ifSuccess){
//            log.info(s"${t.liveId} auth successfully ---")
//            liveIdCodeMap.remove(t.liveId)
//            authCountMap.remove(t.liveId)
//          }else{
//            log.info(s"${t.liveId} auth fails -----")
//            val authTime = authCountMap.getOrElse(t.liveId, 1)
//            if(liveIdCodeMap.get(t.liveId).isDefined && authTime < 5) {
//              timer.startSingleTimer(Timer4AuthAgain(t.liveId), NewLive(t.liveId, liveIdCodeMap(t.liveId)), 5.seconds)
//              authCountMap.put(t.liveId, authTime + 1)
//            }
//          }
//          Behaviors.same
//
//        case t:PushStreamError =>
//          log.info(t.msg)
//          Behaviors.same
      }
    }
  }

}

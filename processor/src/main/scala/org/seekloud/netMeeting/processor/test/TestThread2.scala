package org.seekloud.netMeeting.processor.test

import java.net.InetSocketAddress
import java.nio.ByteBuffer
import java.nio.channels.{Channels, DatagramChannel}
import java.nio.channels.Pipe.{SinkChannel, SourceChannel}
import java.util.concurrent.atomic.AtomicInteger
import java.util.concurrent.{ExecutorService, Executors}

import org.seekloud.netMeeting.processor.Boot.executor
import org.seekloud.netMeeting.processor.protocol.SharedProtocol.{NewConnect, NewConnectRsp, SuccessRsp}
import org.seekloud.netMeeting.processor.stream.PipeStream
import org.seekloud.netMeeting.processor.utils.HttpUtil
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * User: cq
  * Date: 2020/2/12
  */
object TestThread2 extends HttpUtil {
  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  val processorBaseUrl = "http://127.0.0.1:30388/netMeeting/processor"
  private val log = LoggerFactory.getLogger(this.getClass)

  def newConnect(roomId:Long,userIdList:List[String],liveCode:String,layout:Int):Future[Either[String,NewConnectRsp]] = {
    val url = processorBaseUrl + "/newConnect"
    val jsonString = NewConnect(roomId,userIdList,liveCode, layout).asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000,needLogRsp = false).map{
      case Right(v) =>
        decode[NewConnectRsp](v) match {
          case Right(data) =>
            log.info("get data")
            Right(data)
          case Left(e) =>
            log.error(s"connectRoom error:$e")
            Left("error")
        }
      case Left(error) =>
        log.error(s"connectRoom postJsonRequestSend error:$error")
        Left("Error")
    }
  }
  def main(args: Array[String]): Unit = {
    val threadPool:ExecutorService = Executors.newFixedThreadPool(60)

  }
}

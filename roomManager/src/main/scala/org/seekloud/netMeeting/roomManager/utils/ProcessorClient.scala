package org.seekloud.netMeeting.roomManager.utils

import org.seekloud.netMeeting.roomManager.Boot.{executor, scheduler, system, timeout}
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.protocol.CommonInfoProtocol.{CommonRsp, TestRsp}
import org.slf4j.LoggerFactory

import scala.concurrent.Future

/**
  * User: si-ran
  * Date: 2020/2/14
  * Time: 12:06
  */
object ProcessorClient extends HttpUtil {
  import io.circe.generic.auto._
  import io.circe.syntax._
  import io.circe.parser.decode

  private val log = LoggerFactory.getLogger(this.getClass)

//  val processorBaseUrl = s"http://${AppSettings.processorIp}:${AppSettings.processorPort}/netMeeting/user"
val processorBaseUrl = s"http://127.0.0.1:42061/netMeeting/user"

  def newConnect(roomId:Long, userList: List[Long]):Future[Either[String,String]] = {
    val url = processorBaseUrl + "/test"
//    val jsonString = newConnectInfo(roomId, liveId4host, liveId4client, liveId4push, liveCode4push, layout).asJson.noSpaces
    postJsonRequestSend("test",url,List(),"",timeOut = 60 * 1000).map{
      case Right(v) =>
        decode[String](v) match{
          case Right(value) =>
            log.info(s"newConnect success $v")
            Right(value)
          case Left(e) =>
            log.error(s"newConnect decode error : $e")
            Left("Error")
        }
      case Left(error) =>
        log.error(s"newConnect postJsonRequestSend error : $error")
        Left("Error")
    }

  }

  def main(args: Array[String]): Unit = {
    newConnect(10001, List(10001, 10002))
  }
}

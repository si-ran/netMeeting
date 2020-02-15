package org.seekloud.netMeeting.processor.http
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives.{as, complete, entity, path}
import io.circe.Error
import io.circe.generic.auto._
import org.seekloud.netMeeting.processor.core.RoomManager
import org.seekloud.netMeeting.processor.protocol.SharedProtocol._
import org.seekloud.netMeeting.processor.utils.ServiceUtils
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.processor.Boot.roomManager
import org.seekloud.netMeeting.processor.protocol.CommonErrorCode._
import scala.concurrent.Future

/**
  * User: cq
  * Date: 2020/1/17
  */
trait ProcessorService extends ServiceUtils{
  private val log = LoggerFactory.getLogger(this.getClass)

  private def newConnect = (path("newConnect") & post) {
    entity(as[Either[Error, NewConnect]]) {
      case Right(req) =>
        log.info(s"post method $NewConnect")
        roomManager ! RoomManager.NewConnection(req.roomId, req.userIdList, req.pushLiveCode,req.layout)
        complete(NewConnectRsp())
      case Left(e) =>
        complete(parseJsonError)
    }
  }

  val processorRoute:Route = pathPrefix("processor") {
    newConnect
  }
}

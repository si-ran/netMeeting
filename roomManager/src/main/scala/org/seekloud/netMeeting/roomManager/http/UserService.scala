package org.seekloud.netMeeting.roomManager.http

/**
  * Created by LTy on 19/5/24
  */

import akka.http.scaladsl.server.Directives.path
import io.circe.generic.auto._
import akka.actor.Scheduler
import akka.actor.typed.scaladsl.AskPattern._
import akka.http.scaladsl.model.ws.Message
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.scaladsl.Flow
import org.slf4j.LoggerFactory
import io.circe._
import io.circe.syntax._
import org.seekloud.netMeeting.roomManager.Boot.{executor, scheduler, timeout, userManager}
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.core.UserManager
import org.seekloud.netMeeting.roomManager.utils.{SecureUtil, ServiceUtils}

import scala.concurrent.Future
import java.io.File

import org.seekloud.netMeeting.roomManager.core.UserManager
import org.seekloud.netMeeting.roomManager.utils.ServiceUtils

trait UserService extends ServiceUtils with SessionBase {
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private val websocketJoin: Route = path("websocketJoin"){
    parameter(
      'id.as[Long]
    ) { id =>
      val flowFuture:Future[Flow[Message,Message,Any]] = userManager ? (UserManager.GameJoin(id, _))
      dealFutureResult(
        flowFuture.map(t => handleWebSocketMessages(t))
      )
    }
  }

  val userRoute: Route = pathPrefix("user") {
    websocketJoin
  }


}

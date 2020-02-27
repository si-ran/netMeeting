package org.seekloud.netMeeting.roomManager.http

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
import org.seekloud.netMeeting.protocol.ptcl.ClientProtocol._
import org.seekloud.netMeeting.protocol.ptcl.{ClientProtocol, CommonRsp}
import org.seekloud.netMeeting.roomManager.Boot.{emailActor, executor, scheduler, timeout, userManager}
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.core.{EmailActor, UserManager}
import org.seekloud.netMeeting.roomManager.models.dao.WebDAO
import org.seekloud.netMeeting.roomManager.protocol.CommonInfoProtocol.UserInfo
import org.seekloud.netMeeting.roomManager.utils.{ProcessorClient, SecureUtil, ServiceUtils}

import scala.concurrent.Future
/**
  * User: si-ran
  * Date: 2020/2/14
  * Time: 16:26
  */
trait ClientService extends ServiceUtils with SessionBase {
  private val log = LoggerFactory.getLogger(this.getClass)

  private val signIn: Route = (path("signIn") & post){
    entity(as[Either[Error, SignInReq]]) {
      case Right(value) =>
        dealFutureResult(
          WebDAO.getUserInfoByAccount(value.account).map{
            userInfo =>
              if(userInfo.isEmpty){
                complete(SignInRsp(None, 10001, "用户不存在"))
              }
              else{
                val info = userInfo.get
                if(info.password == value.password){
                  complete(SignInRsp(Some(ClientProtocol.userInfo(info.uid, info.userName, info.email, info.headImg))))
                }
                else{
                  complete(SignInRsp(None, 10002, "密码错误"))
                }
              }
          }
        )
      case Left(error) =>
        log.debug("parse error")
        complete(SignInRsp(None, 10000, "json parse error"))
    }
  }

  private val emailSend: Route = (path("emailSend") & post){
    entity(as[Either[Error, SendEmailReq]]) {
      case Right(value) =>
        val sendFuture: Future[SendEmailRsp] = emailActor ? (EmailActor.SendEmail(value.email, value.fromName, value.toName, value.time, value.roomId, _))
        dealFutureResult(
          sendFuture.map{response => complete(response)}
        )
      case Left(error) =>
        log.debug("parse error")
        complete(SignInRsp(None, 10000, "json parse error"))
    }
  }

  val clientRoute: Route = pathPrefix("client") {
    signIn ~ emailSend
  }

}

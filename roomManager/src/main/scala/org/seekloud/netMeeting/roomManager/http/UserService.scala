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
import org.seekloud.netMeeting.protocol.ptcl.WebProtocol._
import org.seekloud.netMeeting.protocol.ptcl.CommonRsp
import org.seekloud.netMeeting.roomManager.Boot.{executor, scheduler, timeout, userManager}
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.core.UserManager
import org.seekloud.netMeeting.roomManager.http.SessionBase
import org.seekloud.netMeeting.roomManager.models.dao.WebDAO
import org.seekloud.netMeeting.roomManager.protocol.CommonInfoProtocol.UserInfo
import org.seekloud.netMeeting.roomManager.utils.{ProcessorClient, SecureUtil, ServiceUtils}

import scala.concurrent.Future
import scala.util.{Failure, Success}

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

  private val signUp: Route = (path("signUp") & post){
    entity(as[Either[Error, SignUpReq]]) {
      case Right(value) =>
        dealFutureResult(
          WebDAO.getUserInfoByAccount(value.account).flatMap{
            case Some(_) =>
              Future(complete(SignUpRsp(20001, "用户已存在")))
            case None =>
              WebDAO.addUserInfo(
                UserInfo(
                  user_name = value.account,
                  account = value.account,
                  password = value.password,
                  create_time = System.currentTimeMillis(),
                  rtmp_url = "",
                  email = value.email
                )
              ).map{ _ =>
                complete(SignUpRsp())
              }
          }
        )
      case Left(error) =>
        log.debug("decode error")
        complete("error")
    }
  }

  private val signIn: Route = (path("signIn") & post){
    entity(as[Either[Error, SignInReq]]) {
      case Right(value) =>
        dealFutureResult(
          WebDAO.getUserInfoByAccount(value.account).map{
            case Some(user) =>
              if(value.password != user.password){
                complete(SignInRsp(-1, 20002, "密码错误"))
              }
              else{
                val userSession = SessionBase.UserSession(SessionBase.UserInfo(user.uid.toString)).toUserSessionMap
                addSession(userSession) {
                  complete(SignInRsp(user.uid))
                }
              }
            case None =>
              complete(SignInRsp(-1, 20001, "用户不存在"))
          }
        )
      case Left(error) =>
        log.debug("decode error")
        complete("error")
    }
  }

  private val userInfo: Route = (path("userInfo") & get){
    userAuth{ user =>
      dealFutureResult(
        WebDAO.getUserInfoById(user.videoUserInfo.userId.toLong).map{
          case Some(info) =>
            val userInfo = UserSimpleInfo(info.uid, info.userName, info.email, info.headImg)
            complete(UserInfoRsp(Some(userInfo)))
          case None =>
            complete(UserInfoRsp(None, 20001, "用户不存在"))
        }
      )
    }
  }

  val userRoute: Route = pathPrefix("user") {
    websocketJoin ~ signUp ~ signIn ~ userInfo
  }


}

/*
 * Copyright 2018 seekloud (https://github.com/seekloud)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.seekloud.netMeeting.roomManager.http

import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server
import akka.http.scaladsl.server.{Directive0, Directive1, ValidationRejection}
import akka.http.scaladsl.server.directives.BasicDirectives
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.utils.{CirceSupport, ServiceUtils, SessionSupport}
import org.seekloud.netMeeting.roomManager.protocol.CommonErrorCode._
import com.sun.xml.internal.ws.encoding.soap.DeserializationException
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.utils.{ServiceUtils, SessionSupport}
import org.slf4j.LoggerFactory


/**
  * User: Taoz
  * Date: 12/4/2016
  * Time: 7:57 PM
  */

object SessionBase {
  private val logger = LoggerFactory.getLogger(this.getClass)

  private val sessionTimeout = 24 * 60 * 60 * 1000
  val SessionTypeKey = "STKey"


  object AdminSessionKey {
    val SESSION_TYPE = "adminSession"
    val adminId = "aid"
    val loginTime = "aTime"
  }

  case class AdminInfo (
    adminId: String,
  )
  case class AdminSession(
    videoUserInfo: AdminInfo,
  ){
    def toAdminSessionMap = {
      Map(
        SessionTypeKey -> AdminSessionKey.SESSION_TYPE,
        AdminSessionKey.adminId -> videoUserInfo.adminId,
        AdminSessionKey.loginTime -> System.currentTimeMillis().toString
      )
    }
  }


  implicit class SessionTransformer(sessionMap: Map[String, String]) {

    def toVideoSession: Option[AdminSession] = {
      logger.debug(s"toAdminSession: change map to session, ${sessionMap.mkString(",")}")
      try{
        if(sessionMap.get(SessionTypeKey).exists(_.equals(AdminSessionKey.SESSION_TYPE))){
          if(System.currentTimeMillis() - sessionMap(AdminSessionKey.loginTime).toLong > sessionTimeout){
            None
          }
          else{
            Some(AdminSession(
              AdminInfo(
                sessionMap(AdminSessionKey.adminId),
              )
            ))
          }
        }else{
          logger.debug("no session type in the session")
          None
        }
      } catch {
        case e: Exception =>
          e.printStackTrace()
          logger.warn(s"toAdminSession: ${e.getMessage}")
          None
      }
    }
  }

}

trait SessionBase extends SessionSupport with ServiceUtils{

  import SessionBase._
  import io.circe.generic.auto._
  
  override val sessionEncoder = SessionSupport.PlaySessionEncoder
  override val sessionConfig = AppSettings.sessionConfig

//  def noSessionError(message:String = "no session") = ErrorRsp(1000102,s"$message")

  protected def setVideoSession(adminSession: AdminSession): Directive0 = setSession(adminSession.toAdminSessionMap)

  def adminAuth(f: AdminSession => server.Route) =
    optionalAdminSession {
      case Some(session) =>
        f(session)
      case None =>
        //redirect("/bubble/", StatusCodes.SeeOther)
        complete(noSessionError())
    }


  protected val optionalAdminSession: Directive1[Option[AdminSession]] = optionalSession.flatMap{
    case Right(sessionMap) => BasicDirectives.provide(sessionMap.toVideoSession)
    case Left(error) =>
      logger.debug(error)
      BasicDirectives.provide(None)
  }

}

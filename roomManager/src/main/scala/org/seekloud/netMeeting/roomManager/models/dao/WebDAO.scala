package org.seekloud.netMeeting.roomManager.models.dao

//import slick.jdbc.H2Profile.api._
import slick.jdbc.PostgresProfile.api._
import org.seekloud.netMeeting.roomManager.utils.DBUtil._
import org.seekloud.netMeeting.roomManager.models.SlickTables._
import org.seekloud.netMeeting.roomManager.Boot.executor
import org.seekloud.netMeeting.roomManager.protocol.CommonInfoProtocol._

import scala.collection.mutable
import scala.concurrent.Future
/**
  * User: si-ran
  * Date: 2019/11/9
  * Time: 20:50
  */
object WebDAO {

  def addUserInfo(userInfo: UserInfo) = {
    db.run(tUserInfo += rUserInfo(-1, userInfo.account, userInfo.account, userInfo.password, "", "", userInfo.create_time, userInfo.rtmp_url, userInfo.email))
  }

  def getUserInfoByAccount(account: String) ={
    db.run(tUserInfo.filter(_.account === account).result.headOption)
  }

  def getUserInfoById(id: Long) ={
    db.run(tUserInfo.filter(_.uid === id).result.headOption)
  }

  def updateHeadImg(userId: Long, ImgUrl: String) ={
    db.run(tUserInfo.filter(_.uid === userId).map(_.headImg).update(ImgUrl))
  }

}

package org.seekloud.netMeeting.roomManager.protocol

/**
  * User: si-ran
  * Date: 2019/11/2
  * Time: 21:17
  */
object CommonInfoProtocol {

  trait CommonRsp {
    val errorCode: Int
    val msg: String
  }

  case class TestRsp(
    errorCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  //------
  case class UserInfo(
    user_name: String,
    account: String,
    password: String,
    create_time: Long,
    rtmp_url: String,
    email: String,
  )

}

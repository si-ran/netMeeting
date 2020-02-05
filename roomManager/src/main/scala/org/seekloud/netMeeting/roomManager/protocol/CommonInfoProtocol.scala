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

  case class matchInfo(

  )

}

package org.seekloud.netMeeting.protocol

/**
  * User: Taoz
  * Date: 5/30/2017
  * Time: 10:37 AM
  */
/**
  *
  * Created by liuziwei on 2017/5/5.
  *
  */


package object ptcl {

  trait CommonRsp {
    val errCode: Int
    val msg: String
  }

  final case class ErrorRsp(
    errCode: Int,
    msg: String
  ) extends CommonRsp

  final case class SuccessRsp(
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  trait Success extends CommonRsp {
    implicit val errCode = 0
    implicit val msg = "ok"
  }



}

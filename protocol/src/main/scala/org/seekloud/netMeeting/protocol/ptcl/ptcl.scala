package org.seekloud.netMeeting.protocol

/**
  * User: Arrow
  * Date: 2019/7/15
  * Time: 16:36
  */
package object ptcl {

  trait Request

  trait Response {
    val errCode: Int
    val msg: String
  }

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

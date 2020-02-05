package org.seekloud.netMeeting.protocol.ptcl

/**
  * User: easego
  * Date: 2018/12/19
  * Time: 17:37
  */

object FileProtocol {

  case class SaveFileRsp(
    fileName: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class GetVideoReq(
    number: Int
  )

  case class GetVideoRsp(
    data: List[String],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class GetVideoNumberRsp(
    srcNumber: Int,
    tstNumber: Int,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class DeleteVideoReq(
    fileName: String,
    videoRrc: Int
  )

  case class DeleteVideoRsp(
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp
}


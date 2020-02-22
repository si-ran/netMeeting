package org.seekloud.netMeeting.protocol.ptcl

/**
  * User: si-ran
  * Date: 2020/2/21
  * Time: 16:39
  */
object ProcessorProtocol {

  case class NewConnectReq(
    roomId: Long,
    userIdList: List[String],
    pushLiveCode: String = "",
    layout: Int = 1
  )

  case class NewConnectRsp(
    errCode: Int = 0,
    msg: String = "ok"
  )

}

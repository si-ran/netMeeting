package org.seekloud.netMeeting.protocol.ptcl

/**
  * User: si-ran
  * Date: 2020/2/14
  * Time: 16:28
  */
object ClientProtocol {

  //url: netMeeting/client/signIn   post
  case class SignInReq(
    account: String,
    password: String
  )

  case class userInfo(
    userId: Long
  )

  case class SignInRsp(
    data: Option[userInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

}

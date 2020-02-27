package org.seekloud.netMeeting.protocol.ptcl

/**
  * User: si-ran
  * Date: 2020/2/14
  * Time: 13:59
  */
object WebProtocol {

  case class SignUpReq(
    email: String,
    account: String,
    password: String
  )

  case class SignUpRsp(
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class SaveHeadImgRsp(
    fileNameUrl: String,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class SignInReq(
    account: String,
    password: String
  )

  case class SignInRsp(
    userId: Long,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

  case class UserSimpleInfo(
    userId: Long,
    nickname: String,
    email: String,
    headImg: String
  )

  case class UserInfoRsp(
    data: Option[UserSimpleInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends Response

}

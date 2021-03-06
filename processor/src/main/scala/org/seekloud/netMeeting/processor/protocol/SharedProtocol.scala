package org.seekloud.netMeeting.processor.protocol

/**
  * User: cq
  * Date: 2020/2/9
  */
object SharedProtocol {

  sealed trait CommonRsp{
    val errCode:Int
    val msg:String
  }

  case class CloseConnect(
                         roomId: Long
                         )

  case class NewConnect(
                         roomId: Long,
                         userIdList:List[String],
                         pushLiveCode:String="",
                         layout: Int = 1
                       )

  case class NewConnectRsp(
                            errCode: Int = 0,
                            msg:String = "ok"
                          ) extends CommonRsp

  final case class SuccessRsp(
                               errCode: Int = 0,
                               msg: String = "ok"
                             )
}

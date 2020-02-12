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

  case class NewConnect(
                         roomId: Long,
                         host: String,
                         client: String,
                         pushLiveId:String,
                         pushLiveCode:String,
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

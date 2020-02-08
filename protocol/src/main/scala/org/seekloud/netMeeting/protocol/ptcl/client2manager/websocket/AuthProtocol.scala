package org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket

import org.seekloud.netMeeting.protocol.ptcl.CommonInfo.RoomInfo

/**
  * @user: wanruolong
  * @date: 2020/1/15 20:03
  *
  */


object AuthProtocol {

  sealed trait WsMsgFront

  sealed trait WsMsgManager

  sealed trait WsMsgClient extends WsMsgFront

  sealed trait WsMsgRm extends WsMsgManager

  //用户退出
  case object CompleteMsgClient extends WsMsgFront

  case class FailMsgClient(ex: Exception) extends WsMsgFront

  case object CompleteMsgRm extends WsMsgManager

  case class FailMsgRm(ex: Exception) extends WsMsgManager

  case class Wrap(ws: Array[Byte]) extends WsMsgRm


  case class TextMsg(msg: String) extends WsMsgRm

  case object DecodeError extends WsMsgRm

  /**
    *
    * 主持人
    *
    **/

  /*client发送*/
  sealed trait WsMsgHost extends WsMsgClient


  /*roomManager发送*/
  sealed trait WsMsgRm2Host extends WsMsgRm

  /*心跳包*/

  case object PingPackage extends WsMsgClient with WsMsgRm

  case class HeatBeat(ts: Long) extends WsMsgRm

  case object AccountSealed extends WsMsgRm// 被封号

  case object NoUser extends WsMsgRm

  case object NoAuthor extends WsMsgRm


  //由于没有鉴权，故可以直接推流到srs，推流后给后台发送创建房间的请求
  case class EstablishMeeting(
                               url: String,
                               roomId: Long,
                               userId: Long
                             ) extends WsMsgHost

  //踢出会议室
  case class KickOut(
                      roomId: Long,
                      userId: Long
                    ) extends WsMsgHost

  case class GiveHost2(
                        roomId: Long,
                        userId: Long
                      ) extends WsMsgHost

  //指定sb说话
  case class GiveMicrophone2(
                              roomId: Long,
                              userId: Long
                            ) extends WsMsgHost

  //邀请用户加入会议
  case class InviteReq(
                       roomId: Long,
                       userId: Long
                     ) extends WsMsgHost

  //控制用户的声音或画面
  case class MediaControl(
                           roomId: Long,
                           userId: Long,
                           needAudio: Boolean = true,
                           needVideo: Boolean = true
                         ) extends WsMsgHost with WsMsgRm2Audience

  case class SpeakRsp4Host(
                     roomId: Long,
                     userId: Long,  //申请人id
                     acceptance: Boolean
                     ) extends WsMsgHost


  case class UpdateRoomInfo(
                             roomInfo: RoomInfo,
                             errCode: Int = 0,
                             msg: String = "ok"
                           ) extends WsMsgRm2Host

//  case class InviteRsp(
//                      roomInfo: RoomInfo,
//
//                      ) extends WsMsgRm2Host

  //向主持人发送申请说话的请求
  case class SpeakReq4Host(
                     roomId: Long,
                     userId: Long
                     ) extends WsMsgRm2Host

  /**
    *
    * 观众端
    *
    **/


  /*client发送*/
  sealed trait WsMsgAudience extends WsMsgClient

  /*room manager发送*/
  sealed trait WsMsgRm2Audience extends WsMsgRm


  //申请加入会议
  case class JoinReq(
                      userId: Long,
                      roomId: Long,
                    ) extends WsMsgAudience


  case class JoinRsp(
                      roomInfo: RoomInfo,
                      errCode: Int = 0,
                      acceptance: Boolean,
                      msg: String = "ok"
                    ) extends WsMsgRm2Audience

  case class SpeakReq(
                     userId: Long,
                     roomId: Long
                     ) extends WsMsgAudience

  case class SpeakRsp(
                     userId: Long,
                     roomId: Long,
                     acceptance: Boolean,
                     errCode: Int = 0,
                     msg: String = "ok"
                     ) extends WsMsgRm2Audience
}


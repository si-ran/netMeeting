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

  //前端主动用户退出
  case object EmptyFrontEvent extends WsMsgFront

  case object CompleteMsgClient extends WsMsgFront

  case class FailMsgClient(ex: Exception) extends WsMsgFront

  case object CompleteMsgRm extends WsMsgManager

  case class FailMsgRm(ex: Exception) extends WsMsgManager

  //ws结构信息
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

  /**
    *
    * client
    *
    **/
  /*client发送*/
  sealed trait WsMsgAudience extends WsMsgClient

  /*room manager发送*/
  sealed trait WsMsgRm2Audience extends WsMsgRm

  case object Disconnect extends WsMsgHost with WsMsgAudience


  //由于没有鉴权，故可以直接推流到srs，推流后给后台发送创建房间的请求
  case class EstablishMeetingReq(
                               url: String,
                               roomId: Long,
                               userId: Long
                             ) extends WsMsgHost

  case class EstablishMeetingRsp(
                                  errorCode: Int = 0,
                                  msg: String = "ok"
                                ) extends WsMsgRm2Host

  //申请加入会议
  case class JoinReq(
                      userId: Long,
                      roomId: Long,
                    ) extends WsMsgRm2Host with WsMsgAudience

  case class JoinRsp(
                      roomInfo: RoomInfo,
                      errCode: Int = 0,
                      acceptance: Boolean,
                      msg: String = "ok"
                    ) extends WsMsgHost with WsMsgRm2Audience


  //踢出会议室
  case class KickOut(
                      roomId: Long,
                      userId: Long
                    ) extends WsMsgHost with WsMsgRm2Audience

  //TODO
  case class GiveHost2(
                        roomId: Long,
                        userId: Long
                      ) extends WsMsgHost with WsMsgRm2Audience

  //TODO 指定sb说话
  case class GiveMicrophone2(
                              roomId: Long,
                              userId: Long
                            ) extends WsMsgHost with WsMsgRm2Audience

  //TODO 邀请用户加入会议
  case class InviteReq(
                       roomId: Long,
                       userId: Long
                     ) extends WsMsgHost with WsMsgRm2Audience

  //TODO 控制用户的声音或画面
  case class MediaControl(
                           roomId: Long,
                           userId: Long,
                           needAudio: Boolean = true,
                           needVideo: Boolean = true
                         ) extends WsMsgHost with WsMsgRm2Audience

  //TODO
  case class SpeakRsp4Host(
                     roomId: Long,
                     userId: Long,  //申请人id
                     acceptance: Boolean
                     ) extends WsMsgHost


  //TODO
  case class UpdateRoomInfo(
                             roomInfo: RoomInfo,
                             errCode: Int = 0,
                             msg: String = "ok"
                           ) extends WsMsgRm2Host

  /**
    * 申请说话
    * */
  case class SpeakReq(
                     roomId: Long,
                     userId: Long
                     ) extends WsMsgRm2Host with WsMsgAudience

  case class SpeakRsp(
                       roomId: Long,
                       userId: Long,  //申请人id
                       acceptance: Boolean,
                       errCode: Int = 0,
                       msg: String = "ok"
                     ) extends WsMsgHost with WsMsgRm2Audience

  /*开始录像*/
  case class UserRecordReq(
    mode: Int
  ) extends WsMsgRm2Host with WsMsgAudience

  case class UserRecordStopReq(
    mode: Int
  ) extends WsMsgRm2Host with WsMsgAudience
}


package org.seekloud.netMeeting.protocol.ptcl

/**
  * User: XuSiRan
  * Date: 2019/2/13
  * Time: 17:08
  */
object ChatEvent {

//  trait MeetingEvent
//
//  sealed trait MeetingClientEvent extends MeetingEvent //前端发送数据
//
//  sealed trait MeetingBackendEvent extends MeetingEvent //后台发送数据
//
//  /*-----Frontend or client-----*/
//  final case object EmptyFrontEvent extends MeetingClientEvent
//
//  final case object Ping extends MeetingClientEvent
//
//  final case object RoomCreate extends MeetingClientEvent
//
//  final case class RoomJoin(roomId: Long) extends MeetingClientEvent
//
//  final case object RoomExit extends MeetingClientEvent
//
//  final case class TestFrontEvent(text: String) extends MeetingClientEvent
//
//  /*-----Backend-----*/
//  final case class ChatMessage2Front(fromId: Long, msg: String) extends MeetingBackendEvent
//
//  final case class VideoData(
//    userId: Long,                   //用户ID
//    videoUrl: Option[String]        //推流地址
//  )
//
//  // (1)房间更新推拉流地址
//  final case class VideoUpdate(
//    roomId: Long,
//    hostId: Long,
//    UrlHeader: String,
//    mixUrl: String,
//    infos: List[VideoData],
//  ) extends MeetingBackendEvent
//
//  final case class WsCommon(errCode: Int = 0, msg: String = "ok") extends MeetingBackendEvent
//
//  /*-----WsMsg-----*/
//  sealed trait WsMsg extends MeetingBackendEvent
//
//  case class Wrap(ws: Array[Byte]) extends WsMsg
//
//  case object WsComplete extends WsMsg
//
//  case object WsFailure extends WsMsg

}

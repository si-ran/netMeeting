package org.seekloud.netMeeting.protocol.ptcl

/**
  * @user: wanruolong
  * @date: 2020/1/15 20:11
  *
  */
object CommonInfo {
  case class RoomInfo(
                       roomId: Long,
                       userId: List[Long],
                       hostId: Long
                     )
}


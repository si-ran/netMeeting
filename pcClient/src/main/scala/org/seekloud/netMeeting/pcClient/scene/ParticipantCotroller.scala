package org.seekloud.netMeeting.pcClient.scene

import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol.{HeatBeat, WsMsgRm}

/**
  * @user: wanruolong
  * @date: 2020/2/6 20:42
  *
  */
class ParticipantCotroller {

  def wsMessageHandler(data: WsMsgRm) = {
    data match {
      case msg: HeatBeat =>
    }

  }

}

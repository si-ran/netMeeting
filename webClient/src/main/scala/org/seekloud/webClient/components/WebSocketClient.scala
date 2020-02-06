package org.seekloud.webClient.components

import org.seekloud.webClient.common.Routes
import org.seekloud.netMeeting.protocol.ptcl.ChatEvent._
import org.scalajs.dom
import org.scalajs.dom.raw._
import org.seekloud.byteobject.MiddleBufferInJs
import org.seekloud.byteobject.ByteObject._

import scala.scalajs.js.typedarray.ArrayBuffer


class WebSocketClient(
                       connectSuccessCallback: Event => Unit,
                       connectErrorCallback:Event => Unit,
                       messageHandler: MeetingBackendEvent => Unit,
                       closeCallback:Event => Unit
                     ) {

  import io.circe.generic.auto._
  import io.circe.syntax._

  private var wsSetup = false

  private var websocketStreamOpt : Option[WebSocket] = None

  def getWsState = wsSetup

  def getWebSocketUri(id: String): String = {
//    println(dom.document.location.host)
//    println(dom.document.location.hostname)
    val wsProtocol = if (dom.document.location.protocol == "https:") "wss" else "ws"
    s"$wsProtocol://${dom.document.location.host}${Routes.User.wsJoinUrl(id)}"
  }

  private val sendBuffer:MiddleBufferInJs = new MiddleBufferInJs(2048)

  def sendByteMsg(msg: MeetingClientEvent): Unit = {
    import org.seekloud.byteobject.ByteObject._
    websocketStreamOpt.foreach{s =>
      s.send(msg.fillMiddleBuffer(sendBuffer).result())
    }
  }

  def sendTextMsg(msg: String): Unit ={
    websocketStreamOpt.foreach{ s =>
      s.send(msg)
    }
  }


  def setup(id: String):Unit = {
    if(wsSetup){
      println(s"websocket已经启动")
    }else{
      val websocketStream = new WebSocket(getWebSocketUri(id))
      websocketStreamOpt = Some(websocketStream)

      websocketStream.onopen = { (event: Event) =>
        wsSetup = true
        dom.window.setInterval(()=> sendByteMsg(Ping), 3000)
        connectSuccessCallback(event)
      }

      websocketStream.onerror = { (event: Event) =>
        wsSetup = false
        websocketStreamOpt = None
        connectErrorCallback(event)
      }

      websocketStream.onmessage = { (event: MessageEvent) =>
        event.data match {
          case blobMsg: Blob =>
            val fr = new FileReader()
            fr.readAsArrayBuffer(blobMsg)
            fr.onloadend = { _: Event =>
              val buf = fr.result.asInstanceOf[ArrayBuffer]
              val middleDataInJs = new MiddleBufferInJs(buf)
              val data = bytesDecode[MeetingBackendEvent](middleDataInJs) match {
                case Right(msg) => msg
              }
              messageHandler(data)
            }
          case unknown => println(s"room_recv unknow msg:${unknown}")
        }
      }

      websocketStream.onclose = { (event: Event) =>
        wsSetup = false
        websocketStreamOpt = None
        closeCallback(event)
      }
    }
  }


}

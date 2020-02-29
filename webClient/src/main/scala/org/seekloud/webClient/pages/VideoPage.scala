package org.seekloud.webClient.pages

import java.util.Date

import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom.raw.Event
import org.seekloud.netMeeting.protocol.ptcl.WebProtocol._
import org.seekloud.webClient.common.{Page, Routes}
import org.seekloud.webClient.components.{PopWindow, WebSocketClient}
import org.seekloud.webClient.utils.{Http, JsFunc, TimeTool}
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * User: si-ran
  * Date: 2020/2/27
  * Time: 19:35
  */
class VideoPage(fileName: String) extends Page{


  private def init(): Unit ={

  }

  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div class="video-page" style="
    position: relative;
    height: 720px;
    background-color: #000;">
      <video style="width: 100%; height: 100%" controls="true">
        <source src={Routes.File.getRecord + s"/$fileName"} type="video/mp4"></source>
      </video>
    </div>
  }

}

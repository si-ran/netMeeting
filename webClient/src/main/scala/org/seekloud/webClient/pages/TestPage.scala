package org.seekloud.webClient.pages

import java.util.Date

import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import org.scalajs.dom
import org.scalajs.dom.raw.Event

import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom.raw.{Event, MessageEvent}
import org.seekloud.webClient.common.{Page, Routes}
import org.seekloud.webClient.components.WebSocketClient
import org.seekloud.webClient.utils.{Http, JsFunc, TimeTool}
import org.seekloud.netMeeting.protocol.ptcl.ChatEvent._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User: si-ran
  * Date: 2019/10/11
  * Time: 10:35
  */
class TestPage(id: String) extends Page{

  private val wsClient = new WebSocketClient(wsConnectSuccess,wsConnectError,wsMessageHandler,wsConnectClose)
  private val textList = Var(List.empty[String])

  private def init(): Unit ={
    wsClient.setup(id)
  }

  private def wsConnectSuccess(e:Event) = {
    println(s"连接服务器成功")
    e
  }
  private def wsConnectError(e:Event) = {
    JsFunc.alert("网络连接错误，请重新刷新")
    e
  }
  private def wsConnectClose(e:Event) = {
    JsFunc.alert("网络连接失败，请重新刷新")
    e
  }

  private def wsMessageHandler(e: MeetingBackendEvent) = {
    e match {
      case event =>
        println(event)
      case ChatMessage2Front(fromId, msg) =>
        println(msg)
        textList.update(t => s"$fromId: $msg" :: t)
      case unknown =>
        println(s"recv unknown msg:$unknown")
    }
    e
  }


  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div class="all-top video-match-page">
      <div class="button-list">
        <button onclick={(e: Event)=>wsClient.sendByteMsg(RoomCreate)}>创建</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(RoomJoin(10001))}>加入10001</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(RoomJoin(10002))}>加入10002</button>
      </div>
      <div class="text-list">
        {textList.map{ list => list.map{ li =>
        <div>{li}</div>
      }}}
      </div>
    </div>
  }

}

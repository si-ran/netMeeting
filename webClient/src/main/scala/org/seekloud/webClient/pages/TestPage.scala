package org.seekloud.webClient.pages

import java.util.Date

import org.scalajs.dom
import org.scalajs.dom.html.{Div, Input}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import org.scalajs.dom

import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom.raw.Event
import org.seekloud.webClient.common.{Page, Routes}
import org.seekloud.webClient.components.WebSocketClient
import org.seekloud.webClient.utils.{Http, JsFunc, TimeTool}
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._

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

  private def wsMessageHandler(e: WsMsgManager) = {
    e match {
      case event =>
        println(event)
      case unknown =>
        println(s"recv unknown msg:$unknown")
    }
    e
  }


  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div class="all-top video-match-page">
      <div class="button-list">
        <button onclick={(e: Event)=>wsClient.sendByteMsg(EstablishMeetingReq("url", id.toLong, id.toLong))}>创建房间{id}</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(JoinReq(id.toLong, 10011))}>加入10011</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(JoinReq(id.toLong, 10012))}>加入10012</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(UserRecordReq(1))}>开始录制</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(UserRecordStopReq(1))}>停止录制</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(MediaControlReq(10011, 10012))}>房间10011控制10012</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(SpeakReq(10011, id.toLong))}>申请说话</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(SpeakRsp(10011, 10012, true))}>10012同意说话</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(KickOutReq(10011, 10012))}>10011踢出10012</button>
        <button onclick={(e: Event)=>wsClient.sendByteMsg(Disconnect)}>断线</button>
      </div>
      <div class="text-list">
        {textList.map{ list => list.map{ li =>
        <div>{li}</div>
      }}}
      </div>
    </div>
  }

}

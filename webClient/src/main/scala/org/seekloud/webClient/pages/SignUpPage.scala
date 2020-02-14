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
import org.seekloud.webClient.components.WebSocketClient
import org.seekloud.webClient.utils.{Http, JsFunc, TimeTool}
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._

import scala.concurrent.ExecutionContext.Implicits.global
/**
  * User: si-ran
  * Date: 2020/2/14
  * Time: 14:02
  */
object SignUpPage extends Page {

  private def init(): Unit ={

  }

  private def signUp(): Unit = {
    val account = dom.document.getElementById("account").asInstanceOf[Input].value
    val password = dom.document.getElementById("password").asInstanceOf[Input].value
    val data = SignUpReq(account, password).asJson.noSpaces
    Http.postJsonAndParse[SignUpRsp](Routes.User.signUp, data).map{
      case Right(value) =>
        println(s"signup success $value")
      case Left(error) =>
        println(s"signup error $error")
    }
  }

  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div class="signUp-page">
      <div class="sign-up-top">用户注册</div>
      <div class="sign-up-contain">
        <input id="account" placeholder="输入用户名"></input>
        <input id="password" placeholder="输入密码"></input>
        <input id="password-twice" placeholder="确认密码"></input>
      </div>
      <div class="sign-up-confirm">
        <div class="button" onclick={()=>signUp()}>注册</div>
      </div>
    </div>
  }

}

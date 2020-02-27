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
  * Date: 2020/2/14
  * Time: 14:02
  */
object SignUpPage extends Page {

  val mode = Var(0) // 0 -> 登录; 1 -> 注册

  private def init(): Unit ={

  }

  private def signUp(): Unit = {
    val email = dom.document.getElementById("email").asInstanceOf[Input].value
    val account = dom.document.getElementById("account").asInstanceOf[Input].value
    val password = dom.document.getElementById("password").asInstanceOf[Input].value
    val passwordConfirm = dom.document.getElementById("password-twice").asInstanceOf[Input].value
    if(password.equals(passwordConfirm)){
      val data = SignUpReq(email, account, password).asJson.noSpaces
      Http.postJsonAndParse[SignUpRsp](Routes.User.signUp, data).map{
        case Right(value) =>
          if(value.errCode == 0){
            PopWindow.commonPop(s"注册成功")
          }
          else if(value.errCode == 20001){
            PopWindow.commonPop(s"用户已存在")
          }
          else{}
        case Left(error) =>
          PopWindow.commonPop(s"注册失败： $error")
      }
    }
    else{
      PopWindow.commonPop("两次密码不同")
    }
  }

  private def signIn(): Unit = {
    val account = dom.document.getElementById("account-in").asInstanceOf[Input].value
    val password = dom.document.getElementById("password-in").asInstanceOf[Input].value
    val data = SignInReq(account, password).asJson.noSpaces
    Http.postJsonAndParse[SignInRsp](Routes.User.signIn, data).map{
      case Right(value) =>
        if(value.errCode == 0){
          dom.window.location.hash = s"/personal"
        }
        else if(value.errCode == 10001){
          PopWindow.commonPop(s"用户不存在")
        }
        else if(value.errCode == 10002){
          PopWindow.commonPop(s"密码不正确")
        }
        else{}
      case Left(error) =>
        PopWindow.commonPop(s"注册失败： $error")
    }
  }

  private val signUpElem = {
    <div class="signUp-page">
      <div class="sign-up-top">用户注册</div>
      <div class="sign-up-contain">
        <input id="email" placeholder="输入邮箱"></input>
        <input id="account" placeholder="输入用户名"></input>
        <input type="password" id="password" placeholder="输入密码"></input>
        <input type="password" id="password-twice" placeholder="确认密码"></input>
      </div>
      <div class="sign-up-confirm">
        <div class="button" onclick={()=>signUp()}>注册</div>
      </div>
    </div>
  }

  private val signInElem = {
    <div class="signUp-page">
      <div class="sign-up-top">用户登录</div>
      <div class="sign-up-contain">
        <input id="account-in" placeholder="输入用户名"></input>
        <input type="password" id="password-in" placeholder="输入密码"></input>
      </div>
      <div class="sign-up-confirm">
        <div class="button" onclick={()=>signIn()}>登录</div>
      </div>
    </div>
  }

  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div class="sign-page">
      <div class="sign-header">
        <div class="li" onclick={() => mode := 0}>注册</div>
        <div class="li" onclick={() => mode := 1}>登录</div>
      </div>
      {mode.map {
      case 0 => signUpElem
      case 1 => signInElem
      case _ => signUpElem
    }}
    </div>
  }

}

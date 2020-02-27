package org.seekloud.webClient.pages

import java.util.Date

import org.scalajs.dom
import org.scalajs.dom.html.{Div, Image, Input}

import scala.scalajs.js
import scala.scalajs.js.UndefOr
import scala.xml.Elem
import io.circe.generic.auto._
import io.circe.syntax._
import mhtml.{Rx, Var, emptyHTML}
import org.scalajs.dom.raw.{Event, FileList, FileReader, FormData, HTMLElement}
import org.seekloud.netMeeting.protocol.ptcl.WebProtocol._
import org.seekloud.webClient.common.{Page, Routes}
import org.seekloud.webClient.components._
import org.seekloud.webClient.utils.{Http, JsFunc, TimeTool}
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._

import scala.concurrent.ExecutionContext.Implicits.global

/**
  * User: si-ran
  * Date: 2020/2/26
  * Time: 15:46
  */
class PersonalPage() extends Page {

  case class WebInfo(
    nickname: String = "-",
    headImg: String = "/netMeeting/static/img/user.png",
    email: String = "-"
  )

  val userInfo = Var(WebInfo())

  private def init(): Unit ={
    obtainUserInfo()
  }

  private def obtainUserInfo(): Unit = {
    Http.getAndParse[UserInfoRsp](Routes.User.userInfo).map{
      case Right(value) =>
        if(value.errCode == 0){
          val info = value.data.get
          userInfo := WebInfo(info.nickname, info.headImg, info.email)
        }
        else if(value.errCode == 20001){
          PopWindow.commonPop(s"用户不存在")
        }
        else{}
      case Left(error) =>
        dom.window.location.hash = "/"
        PopWindow.commonPop(s"error： $error")
    }
  }

  private def saveHeadImg(fileUrl: String, files: FileList): Unit ={
    val file = dom.document.getElementById("headImg-file").asInstanceOf[Input].files(0)
    if(file.size > 20971520){
      PopWindow.commonPop("文件传输最大为20M")
    }
    else{
      val form = new FormData()
      form.append("fileUpload", file)
      Http.postFormAndParse[SaveHeadImgRsp](Routes.File.saveHeadImg, form).map{
        case Right(rsp) =>
          if(rsp.errCode == 0){
            dom.document.getElementById("headImg").asInstanceOf[Image].setAttribute("src", rsp.fileNameUrl)
            PopWindow.commonPop(s"上传图像成功")
          }
          else{
            PopWindow.commonPop(s"上传图像出错:${rsp.msg}")
          }
        case Left(e) =>
          PopWindow.commonPop(s"上传图像出错:$e")
      }
    }
  }

  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div style="background-color: #f6f6f6">
      {new HeaderBarTop().render}
      <div class="personal-page">
        {userInfo.map{ info =>
        <div class="personal-info">
          <div class="info-left">
            <input style="display: none" type="file" id="headImg-file" onchange={(e: Event)=>saveHeadImg(e.target.asInstanceOf[Input].value, e.target.asInstanceOf[Input].files)}></input>
            <img id="headImg" class="head-img" src={info.headImg} onclick={()=>dom.document.getElementById("headImg-file").asInstanceOf[HTMLElement].click()}></img>
            <div class="name">{info.nickname}</div>
          </div>
          <div class="info-right">
            <div class="button detail">个人详细资料</div>
          </div>
        </div>
      }}
        <div class="personal-other">
          <div class="message">
            <div class="msg-header">
              <div class="li active">录像</div>
              <div class="li">消息</div>
            </div>
            <div class="msg-content">-----</div>
          </div>
          <div class="friends">
            <div class="fri-header">好友</div>
            <div class="fri-content">-----</div>
          </div>
        </div>
      </div>
    </div>
  }

}

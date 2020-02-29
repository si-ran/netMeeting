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
  val videoInfos = Var(List.empty[String])

  private def init(): Unit ={
    obtainUserInfo()
    obtainVideoList()
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

  private def obtainVideoList(): Unit = {
    Http.getAndParse[GetVideoRsp](Routes.File.getVideo).map{
      case Right(value) =>
        if(value.errCode == 0){
          videoInfos := value.data
        }
        else if(value.errCode == 20001){
          PopWindow.commonPop(s"录像获取失败:${value.msg}")
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

  private val editElem = {
    <div class="edit-page">
      <div class="edit-contain">
        <input id="new-nick" placeholder="新昵称"></input>
        <input id="new-email" placeholder="新邮箱"></input>
      </div>
      <div class="edit-confirm">
        <div class="button" onclick={()=> println("还没写好")}>提交</div>
      </div>
    </div>
  }

  private val videoListElem = {
    <div class="video-list">
    {videoInfos.map{ infos =>
      infos.map{ info =>
        <div class="video-card">
          <img class="video-img" src="/netMeeting/static/img/video.png"></img>
          <div class="video-info">
            <div class="video-room">房间: {info}</div>
            <div class="video-time">时间: {TimeTool.dateFormatDefault(new Date().getTime)}</div>
          </div>
          <!--div class="video-btn" onclick={()=>dom.window.location.href = s"/netMeeting/file/getRecord/$info"}>观看</div-->
          <div class="video-btn" onclick={()=>dom.window.location.hash = s"/video/$info"}>观看</div>
        </div>
      }
    }}
    </div>
  }

  override def render: Elem ={
    dom.window.setTimeout(()=>init(), 0)
    <div style="background-color: #f6f6f6">
      {new HeaderBarTop().render}
      <div class="personal-page">
        <div class="personal-color"></div>
        {userInfo.map{ info =>
        <div class="personal-info">
          <div class="info-left">
            <input style="display: none" type="file" id="headImg-file" onchange={(e: Event)=>saveHeadImg(e.target.asInstanceOf[Input].value, e.target.asInstanceOf[Input].files)}></input>
            <img id="headImg" class="head-img" src={info.headImg} onclick={()=>dom.document.getElementById("headImg-file").asInstanceOf[HTMLElement].click()}></img>
            <div class="name">
              <div class="name-nick">{info.nickname}</div>
              <div class="name-email">{info.email}</div>
            </div>
          </div>
          <div class="info-right">
            <div class="button detail" onclick={()=>PopWindow.elemPop("编辑资料", editElem)}>修改个人资料</div>
          </div>
        </div>
      }}
        <div class="personal-other">
          <div class="message">
            <div class="msg-header">
              <div class="li active">录像</div>
              <div class="li">消息</div>
            </div>
            <div class="msg-content">{videoListElem}</div>
          </div>
          <div class="friends">
            <div class="fri-header">好友</div>
            <div class="fri-content">
              <div class="fri-li">
                <img class="fri-head" src="/netMeeting/static/img/user.png"></img>
                <div class="fri-email">xusiran@bupt.edu.cn</div>
              </div>
              <div class="fri-li">
                <img class="fri-head" src="/netMeeting/static/img/user.png"></img>
                <div class="fri-email">xusiran@bupt.edu.cn</div>
              </div>
            </div>
          </div>
        </div>
      </div>
    </div>
  }

}

package org.seekloud.webClient.components

import mhtml.{Var, _}
import org.scalajs.dom
import org.scalajs.dom.Event

import scala.xml.Elem


/**
  * create by si-ran
  * 2019/7/19  12:17 AM
  *
  * 一般的弹窗结构
  */
object PopWindow {

  val showPop = Var(emptyHTML)

  //阻止事件冒泡防止弹窗消失
  def stopCancel(e: Event, id: String): Unit = {
    //stopPropagation防止事件冒泡
    e.stopPropagation()
    dom.document.getElementById(id).setAttribute("disabled", "")
  }

  def commonPop(text: String): Unit ={
    showPop := {
      <div class="pop-background" onclick={(e: Event)=> showPop := emptyHTML}>
        <div class="pop-main" onclick={(e: Event)=>stopCancel(e,"pop-common")}>
          <div class="pop-header"></div>
          <div class="pop-content">
            <div class="pop-text">{text}</div>
          </div>
          <div class="pop-confirm">
            <div class="pop-button" onclick={(e: Event)=> showPop := emptyHTML}>确认</div>
          </div>
        </div>
      </div>
    }
  }

  //包含元素的弹窗
  def elemPop(title: String, elem: Elem): Unit ={
    showPop := {
      <div class="pop-background" onclick={(e: Event)=> showPop := emptyHTML}>
        <div class="pop-main" onclick={(e: Event)=>stopCancel(e,"pop-common")}>
          <div class="pop-title">{title}</div>
          <div class="pop-content">
            {elem}
          </div>
        </div>
      </div>
    }
  }

  //底部弹出弹窗
  def bottomPop(text: String): Unit ={
    showPop := {
      <div class="bottom-background" onclick={(e: Event)=> showPop := emptyHTML}>
        <div class="bottom-main" onclick={(e: Event)=>stopCancel(e,"pop-common")}>
          <div class="bottom-header"></div>
          <div class="bottom-content">
            <div class="bottom-text">{text}</div>
          </div>
          <div class="bottom-confirm">
            <div class="bottom-button" onclick={(e: Event)=> showPop := emptyHTML}>确认</div>
          </div>
        </div>
      </div>
    }
  }


}

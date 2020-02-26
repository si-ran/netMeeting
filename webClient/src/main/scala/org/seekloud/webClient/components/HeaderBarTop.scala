package org.seekloud.webClient.components

import org.scalajs.dom

import scala.xml.Elem

/**
  * Created by hongruying on 2018/12/10
  * Modified by si-ran on 2019/10/11
  *
  * 基于原来的headerBar修改为Statistic使用的headerBar
  */
class HeaderBarTop(){

  def changePage(hash: String, scrollY: Int): Unit ={
    dom.window.location.hash = hash
    dom.window.scrollTo(0, scrollY)
  }

  def render: Elem =
    <div class ="header-top-container">
      <div class ="header">
        <div class ="header-right">
          <div class="header-title">netMeeting</div>
        </div>
        <div class ="header-left"></div>
      </div>
    </div>



}

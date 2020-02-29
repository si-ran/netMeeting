package org.seekloud.webClient.pages

import mhtml.{Cancelable, Rx, Var, mount}
import org.scalajs.dom
import org.scalajs.dom.Event
import org.seekloud.webClient.common.PageSwitcher
import org.seekloud.webClient.components.PopWindow

import scala.xml.Elem

/**
  * Created by hongruying on 2018/4/8
  */
object MainPage extends PageSwitcher {

  override def switchPageByHash(): Unit = {
    val tokens = {
      val t = getCurrentHash.split("/").toList
      if (t.nonEmpty) {
        t.tail
      } else Nil
    }

    println(s"currentHash=$tokens")
    switchToPage(tokens)
  }

  //currentPage, 包含mainDiv和导航栏
  private val currentPage: Rx[Elem] = currentPageHash.map {
    case "video" :: fileName :: Nil =>
      <div>
        {new VideoPage(fileName).render}
      </div>

    case "personal" :: Nil =>
      <div>
        {new PersonalPage().render}
      </div>

    case "user" :: id :: Nil =>
      <div>
        {new TestPage(id).render}
      </div>

    case "signUp" :: Nil =>
      <div>
        {SignUpPage.render}
      </div>

    case Nil =>
      <div>
        {SignUpPage.render}
      </div>

    case _ => <div>TODO</div>

  }

  def show(): Cancelable = {
    switchPageByHash()
    val page =
      <div>
        {PopWindow.showPop}
        {currentPage}
      </div>
    mount(dom.document.body, page)
  }

}

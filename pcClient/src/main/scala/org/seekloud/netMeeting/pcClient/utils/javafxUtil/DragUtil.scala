package org.seekloud.netMeeting.pcClient.utils.javafxUtil

import javafx.event.EventHandler
import javafx.scene.Node
import javafx.scene.input.MouseEvent
import javafx.stage.Stage

/**
  * @user: wanruolong
  * @date: 2020/2/27 10:53
  *
  */
object DragUtil {

  def addDragHandler(stage: Stage, node: Node) = {
    new DragUtil(stage).enableDrag(node)
  }

}

class DragUtil(stage: Stage) extends EventHandler[MouseEvent]{

  private var xOffset: Double = 0
  private var yOffset: Double = 0

  override def handle(event: MouseEvent): Unit = {
    event.consume()
    if (event.getEventType eq MouseEvent.MOUSE_PRESSED) {
      xOffset = event.getSceneX
      yOffset = event.getSceneY
    }
    else if (event.getEventType eq MouseEvent.MOUSE_DRAGGED) {
//      println(s"screenX: ${event.getScreenX}")
//      println(s"xOffset: ${xOffset}")
      stage.setX(event.getScreenX - xOffset)
      if (event.getScreenY - yOffset < 0) stage.setY(0)
      else stage.setY(event.getScreenY - yOffset)
    }

  }

  def enableDrag(node: Node) = {
    node.setOnMousePressed(this)
    node.setOnMouseDragged(this)
  }

}

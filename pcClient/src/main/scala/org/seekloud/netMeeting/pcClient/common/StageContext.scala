package org.seekloud.netMeeting.pcClient.common

import javafx.scene.Scene
import javafx.scene.control.Alert.AlertType
import javafx.scene.control.{Alert, ButtonType}
import javafx.stage.Stage

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 11:26
  */

object StageContext{
  def closeClick(stage: Stage): Boolean = {

    val alert = new Alert(AlertType.CONFIRMATION)
    val x = if (stage.getWidth > 500) (stage.getX + (stage.getWidth - 500) / 2) else stage.getX
    alert.setX(x)
    alert.setY(stage.getY + stage.getHeight / 3)
    alert.setTitle("退出")
    alert.setHeaderText("")
    alert.setContentText("确定要退出吗")
    val result = alert.showAndWait()
    if (result.get() == ButtonType.OK) true else false
  }
}

class StageContext(stage: Stage) {

  def getStage: Stage = stage

  def getStageWidth: Double = stage.getWidth

  def getStageHeight: Double = stage.getHeight

  def isFullScreen: Boolean = stage.isFullScreen


  def switchScene(scene: Scene, title: String = "netMeeting", resize: Boolean = false, fullScreen: Boolean = false, isSetOffX: Boolean = false): Unit = {
    //    stage.centerOnScreen()
    stage.setScene(scene)
    stage.sizeToScene()
    stage.setResizable(resize)
    stage.setTitle(title)
    stage.setFullScreen(fullScreen)
    if (isSetOffX) {
      stage.setX(0)
      stage.setY(0)
    }
    stage.show()
  }

}

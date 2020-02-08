package org.seekloud.netMeeting.pcClient.scene

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.Insets
import javafx.scene.input.MouseEvent
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.common.StageContext
import org.seekloud.netMeeting.pcClient.scene.HomeScene.HomeSceneListener
import org.slf4j.LoggerFactory

/**
  * @user: wanruolong
  * @date: 2020/2/6 11:12
  *
  */
class HomeController(context: StageContext,
                     homeScene: HomeScene) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  homeScene.setListener(new HomeSceneListener {

  })

  def showScene() = {
    Boot.addToPlatform(
      context.switchScene(homeScene.getScene, title = "netMeeting", true)
    )
  }



}

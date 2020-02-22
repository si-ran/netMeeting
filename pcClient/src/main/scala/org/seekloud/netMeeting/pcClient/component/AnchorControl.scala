package org.seekloud.netMeeting.pcClient.component

import javafx.geometry.Pos
import javafx.scene.control.{ToggleButton, Tooltip}
import javafx.scene.layout.{AnchorPane, HBox, VBox}

/**
  * @user: wanruolong
  * @date: 2020/2/19 9:37
  *
  */
class AnchorControl(x: Double, y: Double) {
  val host = new ToggleButton("")
  host.getStyleClass.add("host")
  host.setDisable(true)
//  Tooltip.install(host, new Tooltip("host"))

  val camera = new ToggleButton("")
  camera.getStyleClass.add("camera")
  //    camera.setDisable(true)
//  Tooltip.install(camera, new Tooltip("禁用摄像头"))

  val microphone = new ToggleButton("")
  microphone.getStyleClass.add("microphone")
  //    microphone.setDisable(true)
//  Tooltip.install(camera, new Tooltip("禁用摄像头"))

  val hBox = new HBox(host, camera, microphone)
  hBox.setSpacing(10)
  hBox.setStyle("-fx-background-color: #66808080")
  hBox.setAlignment(Pos.CENTER)

  val vBox = new VBox(hBox)


  val anchorPane = new AnchorPane()
//  anchorPane.setDisable(false)
  anchorPane.getChildren.add(vBox)
  anchorPane.setLayoutX(x)
  anchorPane.setLayoutY(y)
  anchorPane.setVisible(false)


  def getAnchorPane() = this.anchorPane
}

package org.seekloud.netMeeting.pcClient.component

import javafx.application.Application
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{AnchorPane, VBox}
import javafx.scene.{Group, Scene}
import javafx.stage.{Stage, StageStyle}
import org.seekloud.netMeeting.pcClient.utils.javafxUtil.DragUtil

/**
  * @user: wanruolong
  * @date: 2020/2/27 9:47
  *
  */
class Title extends Application{
/*  val title = new AnchorPane()
  title.getStylesheets().add(
    this.getClass.getClassLoader.getResource("css/style.css").toExternalForm
  )
  title.setId("title")
  val close = new Label()
  close.setId("winClose")
  close.setPrefWidth(33)
  close.setPrefHeight(26)
  val icon = new ImageView("/img/camera.png")
  title.getChildren.addAll(icon, close)
  AnchorPane.setRightAnchor(close, 0.0)
  AnchorPane.setLeftAnchor(icon, 0.0)*/


  override def start(primaryStage: Stage): Unit = {
    val group = new Group()
    val scene = new Scene(group)
    scene.getStylesheets().add(this.getClass.getClassLoader.getResource("css/style.css").toExternalForm)
    primaryStage.initStyle(StageStyle.TRANSPARENT)

    val title = new AnchorPane()
    title.setId("title")
    val close = new Label()
    close.setId("winClose")
    close.setPrefWidth(33)
    close.setPrefHeight(26)
    AnchorPane.setRightAnchor(close, 0.0)
    val canvas = new Canvas(640, 360)
    val vBox = new VBox(title, canvas)
    val icon = new Image("/img/icon.png")
    val titleIcon = new Label()
    titleIcon.setId("icon")
    titleIcon.setPrefWidth(33)
    titleIcon.setPrefHeight(26)
    AnchorPane.setLeftAnchor(titleIcon, 0.0)
    title.getChildren.addAll(titleIcon, close)
    primaryStage.getIcons.add(icon)
    group.getChildren.add(vBox)

    DragUtil.addDragHandler(primaryStage, title)

/*    title.setOnDragDetected((event: MouseEvent) => {
      val
    })*/



    primaryStage.setScene(scene)
    primaryStage.show()
    primaryStage.setOnCloseRequest{_ =>
      println("closing...")
      System.exit(0)
    }
  }
}

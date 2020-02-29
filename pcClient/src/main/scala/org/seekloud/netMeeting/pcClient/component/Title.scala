package org.seekloud.netMeeting.pcClient.component

import javafx.application.Application
import javafx.geometry.Insets
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{AnchorPane, HBox, StackPane, VBox}
import javafx.scene.paint.Color
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
    title.setPrefHeight(50)
//    val closePane = new AnchorPane()
    val close = new Label()
    close.setId("winClose")
    close.setPrefWidth(33)
    close.setPrefHeight(26)
//    closePane.getChildren.add(close)
//    closePane.setLayoutX(title.getWidth-43)
//    closePane.setLayoutY(12)

    title.setPadding(new Insets(10))
    val t = new Label("netMeeting")
    t.setTextFill(Color.WHITE)
    AnchorPane.setRightAnchor(close, 10.0)
    AnchorPane.setBottomAnchor(close, 10.0)
    val canvas = new Canvas(640, 360)
    val canvas4Self = new Canvas(400, 280)
    val hBox= new HBox(canvas4Self, canvas)
    val vBox = new VBox(title, hBox)
    val icon = new Image("/img/icon.png")
    val titleIcon = new Label()
//    val hBox11 = new HBox(titleIcon, t)
//    hBox11.setSpacing(5)
//    val hTitle = new HBox(hBox11, close)
//    hTitle.setSpacing(800)


    titleIcon.setId("icon")
    titleIcon.setPrefWidth(40)
    titleIcon.setPrefHeight(30)
    val hTBox = new HBox(titleIcon, t)
    hTBox.setSpacing(5)
    AnchorPane.setLeftAnchor(hTBox, 10.0)
    AnchorPane.setBottomAnchor(hTBox, 10.0)
    title.getChildren.addAll(hTBox, close)
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

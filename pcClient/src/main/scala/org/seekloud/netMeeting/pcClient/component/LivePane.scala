package org.seekloud.netMeeting.pcClient.component

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.layout.{GridPane, HBox, VBox}
import javafx.stage.Stage

/**
  * @user: wanruolong
  * @date: 2020/2/19 9:35
  *
  */
class LivePane extends Application{

  override def start(primaryStage: Stage): Unit = {
    val canvas4Self = new Canvas(400, 225)
    val image = new Image("/img/camera.png")
    val image1 = new Image("/img/camera.png")
    canvas4Self.getGraphicsContext2D.drawImage(image, 0, 0, canvas4Self.getWidth, canvas4Self.getHeight)
    val canvas = new Canvas(640, 360)
    canvas.getGraphicsContext2D.drawImage(image1, 0, 0, canvas.getWidth, canvas.getHeight)
    val group = new Group
    val scene = new Scene(group)
    scene.getStylesheets.add(
      this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
    )
    def getGc4Live = canvas.getGraphicsContext2D
    val anchorControl1 = new AnchorControl(90+400, 144)
    anchorControl1.host.setDisable(true)
    val anchorControl2 = new AnchorControl(410+400,144)
    val anchorControl3 = new AnchorControl(90+400,324)
    val anchorControl4 = new AnchorControl(410+400,324)
    val vBox = new VBox(canvas4Self)
    vBox.setAlignment(Pos.CENTER)
//    val gridPane = new GridPane
    val hBox = new HBox(vBox, canvas)
//    gridPane.add(canvas4Self, 0,0)
//    gridPane.add(canvas, 1,0)
    group.getChildren.addAll(hBox, anchorControl1.getAnchorPane(), anchorControl2.getAnchorPane())

    primaryStage.setScene(scene)
    primaryStage.show()
  }



}

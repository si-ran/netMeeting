package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.{GridPane, Pane}
import javafx.stage.{Modality, Stage}
import org.seekloud.netMeeting.pcClient.scene.HomeScene.icon

/**
  * @user: wanruolong
  * @date: 2020/2/6 15:25
  *
  */
class LivingState extends Application{
  val stage = new Stage()

  override def start(primaryStage: Stage): Unit = {
    val label = new Label("living")
    val file = new File("E:\\file\\1.jpg").toURI.toString
    val image = new Image(file)
    val canvas = new Canvas( 640, 360)
    canvas.getGraphicsContext2D.drawImage(icon,140, 0, 360, 360)
//    val pane = new Pane(canvas)
    val gridPane = new GridPane()
    gridPane.add(canvas, 0, 0)
    gridPane.setAlignment(Pos.CENTER)
    val scene = new Scene(gridPane, 640, 360)

    primaryStage.setScene(scene)
    primaryStage.setTitle("netMeeting")
    primaryStage.getIcons.add(icon)
    primaryStage.initModality(Modality.APPLICATION_MODAL)
    primaryStage.show()
    primaryStage.setOnCloseRequest{event =>
      //todo stop living
    }
  }

  def showStage(): Unit ={
    start(stage)
  }

}

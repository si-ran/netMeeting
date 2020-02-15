package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.Scene
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.layout.{GridPane, Pane}
import javafx.stage.{Modality, Stage}

/**
  * @user: wanruolong
  * @date: 2020/2/6 15:25
  *
  */

object LivingStage{
  trait LivingStageListener{
    def stop()

  }
}

class LivingStage extends Application{
  import LivingStage._
  private val stage = new Stage()

  private var listener: LivingStageListener = _

  private val canvas = new Canvas(640, 360)

  def setListener(listener: LivingStageListener) = {
    this.listener = listener
  }

  def getGC(): GraphicsContext = {
    this.canvas.getGraphicsContext2D
  }

  override def start(primaryStage: Stage): Unit = {
    val label = new Label("living")
    val file = new File("/img/camera.png").toURI.toString
    val icon = new Image("/img/camera.png")
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
      listener.stop()
    }
  }

  def showStage(): Unit ={
    start(stage)
  }

}

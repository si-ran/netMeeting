package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{GridPane, HBox, Pane, VBox}
import javafx.stage.{Modality, Stage}
import org.seekloud.netMeeting.pcClient.component.AnchorControl

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

  private val group = new Group()

  var isHost: Boolean = false

  private val scene = new Scene(group)
  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  private var listener: LivingStageListener = _

  private val canvas4Self = new Canvas(400, 225)

  val canvas4Pull = new Canvas(640, 360)

  val anchorControl1 = new AnchorControl(90+400, 144)
  val anchorControl2 = new AnchorControl(410+400,144)
  val anchorControl3 = new AnchorControl(90+400,324)
  val anchorControl4 = new AnchorControl(410+400,324)

  val anchorPane1 = anchorControl1.getAnchorPane()
  val anchorPane2 = anchorControl2.getAnchorPane()
  val anchorPane3 = anchorControl3.getAnchorPane()
  val anchorPane4 = anchorControl4.getAnchorPane()

//  val anchorPaneList = List[AnchorControl]

  scene.addEventFilter(MouseEvent.MOUSE_MOVED, (event: MouseEvent) => {
    if(event.getX < (canvas4Pull.getWidth/2 + 400) && event.getX > 400 && event.getY < canvas4Pull.getHeight/2){
      if(group.getChildren.size() > 1){
        group.getChildren.remove(1, group.getChildren.size())
      }
      group.getChildren.add(anchorPane1)
    } else if(event.getX >= (canvas4Pull.getWidth/2 + 400) && event.getY < canvas4Pull.getHeight/2) {
      if(group.getChildren.size() > 1){
        group.getChildren.remove(1, group.getChildren.size())
      }
      group.getChildren.add(anchorPane2)
    } else if(event.getX < (canvas4Pull.getWidth/2 + 400) && event.getX > 400 && event.getY >= canvas4Pull.getHeight/2) {
      if(group.getChildren.size() > 1){
        group.getChildren.remove(1, group.getChildren.size())
      }
      group.getChildren.add(anchorPane3)
    } else if(event.getX >= (canvas4Pull.getWidth/2 + 400) && event.getY >= canvas4Pull.getHeight/2) {
      if(group.getChildren.size() > 1){
        group.getChildren.remove(1, group.getChildren.size())
      }
      group.getChildren.add(anchorPane4)
    } else {
      if(group.getChildren.size() > 1){
        group.getChildren.remove(1, group.getChildren.size())
      }
    }
  })

  scene.addEventFilter(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
    if(group.getChildren.size() > 1) {
      group.getChildren.remove(1, group.getChildren.size())
    }
  })


  def setListener(listener: LivingStageListener) = {
    this.listener = listener
  }

  def getGc4Self() = this.canvas4Self.getGraphicsContext2D

  def getGc4Pull() = this.canvas4Pull.getGraphicsContext2D

  override def start(primaryStage: Stage): Unit = {
    val label = new Label("living")
    val icon = new Image("/img/camera.png")
    val icon1 = new Image("/img/camera.png")
    canvas4Self.getGraphicsContext2D.drawImage(icon,(400-225)/2, 0, 225, 225)
    canvas4Pull.getGraphicsContext2D.drawImage(icon1,140, 0, 360, 360)
    //    val pane = new Pane(canvas)
    val canvasVBox = new VBox(canvas4Self)
    canvasVBox.setAlignment(Pos.CENTER)
    val hBox = new HBox(canvasVBox, canvas4Pull)
    group.getChildren.addAll(hBox)

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

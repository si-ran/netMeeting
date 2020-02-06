package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.{Insets, Pos}
import javafx.scene.canvas.Canvas
import javafx.scene.control.{Button, Label}
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.text.TextAlignment
import javafx.scene.{Cursor, Scene}
import org.seekloud.netMeeting.pcClient.scene.CreatorStage.MeetingType

/**
  * @user: wanruolong
  * @date: 2020/2/6 11:02
  *
  */

object HomeScene{
  val file = new File("E:\\file\\camera.png").toURI.toString
  val icon = new Image(file)

  trait HomeSceneListener{

  }

}

class HomeScene() {
  import HomeScene._

  private var listener: HomeSceneListener = _

  private var creatorStage: CreatorStage = _

  val button = new Button("second")
//  val creatorScene = new CreatorScene()
//  creatorScene.stage.setOnCloseRequest(_ => {
//    button.setDisable(false)
//  })
//  button.setOnAction(_ =>{
//    creatorScene.showStage()
//    button.setDisable(true)
//  })

  val label1 = new Label("新会议")
  val startMeeting = new Canvas(88, 88)
  startMeeting.getGraphicsContext2D.drawImage(icon, 0, 0, startMeeting.getWidth, startMeeting.getHeight)
  val vBox1 = new VBox(startMeeting, label1)
  vBox1.setAlignment(Pos.CENTER)
  vBox1.setPadding(new Insets(10, 0,0,0))

  val label2 = new Label("加入会议")
  label2.setTextAlignment(TextAlignment.CENTER)
  val joinMeeting = new Canvas(88, 88)
  joinMeeting.getGraphicsContext2D.drawImage(icon, 0, 0, startMeeting.getWidth, startMeeting.getHeight)
  val vBox2 = new VBox(joinMeeting, label2)
  vBox2.setAlignment(Pos.CENTER)
  vBox2.setPadding(new Insets(10, 0,0,0))

  val hBox1 = new HBox(vBox1, vBox2)
  hBox1.setSpacing(40)

  val gridPane = new GridPane
  gridPane.add(hBox1, 0, 0)
  val anchorPane = new AnchorPane(gridPane)

  val pane = new Pane(anchorPane)
  val scene = new Scene(pane, 380, 720)

  startMeeting.setCursor(Cursor.HAND)
  joinMeeting.setCursor(Cursor.HAND)

  anchorPane.setLayoutX((scene.getWidth-216)/2)
  anchorPane.setLayoutY((scene.getHeight-118)/2)

  def setListener(listener: HomeSceneListener): Unit = {
    this.listener = listener
  }


  def getScene() = {
    this.scene
  }

  def setCreatorStage(creatorStage: CreatorStage) = {
    this.creatorStage = creatorStage
  }

  startMeeting.addEventFilter(MouseEvent.MOUSE_PRESSED, (event: MouseEvent) => {
    //todo goto new meeting
      println("goto new meeting")
      setCreatorStage(new CreatorStage(MeetingType.CREATE))
      creatorStage.showStage()
  })

  startMeeting.addEventFilter(MouseEvent.MOUSE_ENTERED, (event: MouseEvent) => {
    vBox1.setPadding(new Insets(0))
  })

  startMeeting.addEventFilter(MouseEvent.MOUSE_EXITED, (event: MouseEvent) => {
    vBox1.setPadding(new Insets(10, 0,0,0))
  })

  joinMeeting.addEventFilter(MouseEvent.MOUSE_PRESSED, (event: MouseEvent) => {
    println("join meeting")
    setCreatorStage(new CreatorStage(MeetingType.JOIN))
    creatorStage.showStage()
  })

  joinMeeting.addEventFilter(MouseEvent.MOUSE_ENTERED, (event: MouseEvent) => {
    vBox2.setPadding(new Insets(0))
  })

  joinMeeting.addEventFilter(MouseEvent.MOUSE_EXITED, (event: MouseEvent) => {
    vBox2.setPadding(new Insets(10, 0,0,0))
  })

  scene.heightProperty().addListener(new ChangeListener[Number] {
    override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
      anchorPane.setLayoutY((newValue.intValue()-gridPane.getHeight)/2)
      //        println(s"gridPane.getHeight: ${gridPane.getHeight}")
    }
  })
  scene.widthProperty().addListener(new ChangeListener[Number] {
    override def changed(observable: ObservableValue[_ <: Number], oldValue: Number, newValue: Number): Unit = {
      anchorPane.setLayoutX((newValue.intValue()-gridPane.getWidth)/2)
      //        println(s"gridPane.getWidth： ${gridPane.getWidth}")
    }
  })
}

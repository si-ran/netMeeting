package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.event.Event
import javafx.geometry.{Insets, Pos}
import javafx.scene.Scene
import javafx.scene.control.{Button, Label, TextField}
import javafx.scene.image.Image
import javafx.scene.layout._
import javafx.scene.paint.Color
import javafx.stage.{Modality, Stage}

/**
  * @user: wanruolong
  * @date: 2020/2/6 11:06
  *
  */
object CreatorStage{
  object MeetingType extends Enumeration {
    val CREATE, JOIN = Value
  }

  trait CreatorStageListener {
    def createNewMeeting()
  }
}

import CreatorStage._

class CreatorStage() extends Application{
  val stage = new Stage()
  val file = new File("E:\\file\\camera.png").toURI.toString
  val icon = new Image(file)

  var listener: CreatorStageListener = _

  var meetingType: MeetingType.Value = _

  def this(meetingType: MeetingType.Value) = {
    this()
    this.meetingType = meetingType
  }

  def setMeetingType(meetingType: MeetingType.Value) = {
    this.meetingType = meetingType
  }

  def getMeetingType(): MeetingType.Value = this.meetingType

  def setListener(listener: CreatorStageListener) = {
    this.listener = listener
  }

  override def start(primaryStage: Stage): Unit = {
    val roomLabel = new Label("roomId:")
    val roomId = new TextField()
    roomId.setText("12345")
    val roomBox = new HBox(roomLabel, roomId)
    roomBox.setSpacing(10)

    val userLabel = new Label(("userId:"))
    val userId = new TextField()
    userId.setText("12345")
    val userBox = new HBox(userLabel, userId)
    userBox.setSpacing(10)

    val urlLabel = new Label(("url:"))
    val url = new TextField()
    url.setText("12345")
    val urlBox = new HBox(urlLabel, url)
    urlBox.setSpacing(10)

    val confirmButton = new Button("确定")
//    confirmButton.setStyle("-fx-background-color: A8D1E4")
    val cancelButton = new Button("取消")
    val buttonBox = new HBox(confirmButton, cancelButton)
    buttonBox.setSpacing(40)
    buttonBox.setAlignment(Pos.CENTER)

    val vBox = new VBox(roomBox, userBox, urlBox, buttonBox)
    vBox.setSpacing(16)

//    val anchorPane = new AnchorPane(vBox)
    val gripdPane = new GridPane()
    gripdPane.add(vBox, 0, 0)
    gripdPane.setHgap(0)
    gripdPane.setVgap(0)
//    pane.setGridLinesVisible(true)
    gripdPane.setAlignment(Pos.CENTER)
    gripdPane.setPadding(new Insets(20))

    val scene = new Scene(gripdPane)
//    pane.setPadding()
//    anchorPane.setLayoutX()

    confirmButton.setOnAction { _ =>
      listener.createNewMeeting()
      primaryStage.close()
    }

    cancelButton.setOnAction{_ =>
      stage.close()
    }

    primaryStage.setResizable(false)
    primaryStage.setTitle("netMeeting")
    primaryStage.getIcons.add(icon)
    primaryStage.setScene(scene)
    primaryStage.initModality(Modality.APPLICATION_MODAL)
    primaryStage.show()
  }

  def showStage() = {
    start(stage)
  }
}

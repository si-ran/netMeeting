package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.beans.value.{ChangeListener, ObservableValue}
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

  case class InputInfo(
                        val roomId: Long,
                        val userId: Long,
                        val url: String
                      )

  trait CreatorStageListener {
    def createNewMeeting(meetingType: MeetingType.Value)
  }
}

import CreatorStage._

class CreatorStage(meetingType: MeetingType.Value) extends Application{
  val stage = new Stage()
  val icon = new Image("/img/camera.png")

  final var userId: Long = _

  def this(meetingType: MeetingType.Value, userId: Long) = {
    this(meetingType: MeetingType.Value)
    this.userId = userId
  }

  var listener: CreatorStageListener = _

  val roomIdText = new TextField()
  val userIdText = new TextField()
  val urlText = new TextField()

  userIdText.setEditable(false)
  if(meetingType == MeetingType.CREATE) {
    roomIdText.setEditable(false)
    urlText.setEditable(false)
  }

  roomIdText.textProperty().addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      if(!newValue.matches("\\d*")){
        roomIdText.setText(newValue.replaceAll("[^\\d]", ""))
      }
      if(newValue.length > 10){
        roomIdText.setText(oldValue)
      }
    }
  })

  userIdText.textProperty().addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      if(!newValue.matches("\\d*")){
        userIdText.setText(newValue.replaceAll("[^\\d]", ""))
      }
      if(newValue.length > 10){
        userIdText.setText(oldValue)
      }
    }
  })

  def setRoomId(roomId: Long) = {
    roomIdText.setText("" + roomId)
  }

  def setUrl(url: String) = {
    urlText.setText(url)
  }

  def setListener(listener: CreatorStageListener) = {
    this.listener = listener
  }

  def getInput(): InputInfo = {
    if(roomIdText.getText == "" || userIdText.getText == "" || urlText.getText == "")
      throw new Exception("info is not complete")
    InputInfo(roomIdText.getText().toLong, userIdText.getText().toLong, urlText.getText())
  }

  override def start(primaryStage: Stage): Unit = {
    val roomLabel = new Label("roomId:")
    val roomBox = new HBox(roomLabel, roomIdText)
    roomBox.setSpacing(10)

    val userLabel = new Label(("userId:"))
    userIdText.setText(s"$userId")
    val userBox = new HBox(userLabel, userIdText)
    userBox.setSpacing(10)

    val urlLabel = new Label(("url:"))
    val urlBox = new HBox(urlLabel, urlText)
    urlBox.setSpacing(10)

    val confirmButton = new Button("确定")
    confirmButton.setDefaultButton(true)
    val cancelButton = new Button("取消")
    cancelButton.setCancelButton(true)
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
      listener.createNewMeeting(this.meetingType)
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

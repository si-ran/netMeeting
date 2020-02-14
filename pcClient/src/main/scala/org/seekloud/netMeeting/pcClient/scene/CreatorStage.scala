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
  val file = new File("E:\\file\\camera.png").toURI.toString
  val icon = new Image(file)

  var listener: CreatorStageListener = _

  val roomId = new TextField()
  val userId = new TextField()
  val url = new TextField()

  roomId.textProperty().addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      if(!newValue.matches("\\d*")){
        roomId.setText(newValue.replaceAll("[^\\d]", ""))
      }
      if(newValue.length > 10){
        roomId.setText(oldValue)
      }
    }
  })

  userId.textProperty().addListener(new ChangeListener[String] {
    override def changed(observable: ObservableValue[_ <: String], oldValue: String, newValue: String): Unit = {
      if(!newValue.matches("\\d*")){
        userId.setText(newValue.replaceAll("[^\\d]", ""))
      }
      if(newValue.length > 10){
        userId.setText(oldValue)
      }
    }
  })

  def setListener(listener: CreatorStageListener) = {
    this.listener = listener
  }

  def getInput(): InputInfo = {
    if(roomId.getText == "" || userId.getText == "" || url.getText == "")
      throw new Exception("info is not complete")
    InputInfo(roomId.getText().toLong, userId.getText().toLong, url.getText())
  }

  override def start(primaryStage: Stage): Unit = {
    val roomLabel = new Label("roomId:")
    roomId.setText("12345")
    val roomBox = new HBox(roomLabel, roomId)
    roomBox.setSpacing(10)

    val userLabel = new Label(("userId:"))
    userId.setText("12345")
    val userBox = new HBox(userLabel, userId)
    userBox.setSpacing(10)

    val urlLabel = new Label(("url:"))
    url.setText("rtmp://10.1.29.247:42069/live/test1")
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

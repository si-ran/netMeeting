package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.event.EventHandler
import javafx.geometry.Pos
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.{Canvas, GraphicsContext}
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.stage.{Modality, Stage}
import org.seekloud.netMeeting.pcClient.component.AnchorControl
import org.seekloud.netMeeting.protocol.ptcl.CommonInfo.RoomInfo
import org.slf4j.LoggerFactory

/**
  * @user: wanruolong
  * @date: 2020/2/6 15:25
  *
  */

object LivingStage{
  val log = LoggerFactory.getLogger(this.getClass)

  trait LivingStageListener{
    def stop()

    def giveHost2(userId: Long)

    def mediaControl(userId: Long, needImage: Boolean, needSound: Boolean)

    def giveMicrophone2(userId: Long)

    def invite(userId: Long, roomId: Long)
  }
}

class LivingStage(userId: Long) extends Application{
  import LivingStage._
  private val stage = new Stage()

  private val group = new Group()

  private[this] var _isHost: Boolean = _

  private var roomInfo: RoomInfo = _

  private val scene = new Scene(group)
  scene.getStylesheets.add(
    this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
  )

  private var listener: LivingStageListener = _

  private val canvas4Self = new Canvas(400, 225)

  val canvas4Pull = new Canvas(640, 360)

  val anchorControl4Self = new AnchorControl(130,257)
  val anchorControl1 = new AnchorControl(90+400, 144)
  val anchorControl2 = new AnchorControl(410+400,144)
  val anchorControl3 = new AnchorControl(90+400,324)
  val anchorControl4 = new AnchorControl(410+400,324)

  val anchorPane4Self = anchorControl4Self.getAnchorPane()
  anchorPane4Self.setVisible(true)
  val anchorPane1 = anchorControl1.getAnchorPane()
  val anchorPane2 = anchorControl2.getAnchorPane()
  val anchorPane3 = anchorControl3.getAnchorPane()
  val anchorPane4 = anchorControl4.getAnchorPane()

  val anchorControlList = List[AnchorControl](anchorControl1, anchorControl2, anchorControl3, anchorControl4)
  val anchorPaneList = List[AnchorPane](anchorPane1, anchorPane2, anchorPane3, anchorPane4)

  anchorControl4Self.microphone.setOnAction(_ => {
    println(s"microphone clicked")
  })

  scene.addEventFilter(MouseEvent.MOUSE_MOVED, (event: MouseEvent) => {
    group.getChildren.remove(1, group.getChildren.size())
    if(event.getX <= 400 && event.getY <= (360-(360-225)/2) && event.getY >= (360-(360-225)/2)-225) {
      group.getChildren.add(anchorPane4Self)
    } else if(event.getX < (canvas4Pull.getWidth/2 + 400) && event.getX > 400 && event.getY < canvas4Pull.getHeight/2){
      group.getChildren.add(anchorPane1)
    } else if(event.getX >= (canvas4Pull.getWidth/2 + 400) && event.getY < canvas4Pull.getHeight/2) {
      group.getChildren.add(anchorPane2)
    } else if(event.getX < (canvas4Pull.getWidth/2 + 400) && event.getX > 400 && event.getY >= canvas4Pull.getHeight/2) {
      group.getChildren.add(anchorPane3)
    } else if(event.getX >= (canvas4Pull.getWidth/2 + 400) && event.getY >= canvas4Pull.getHeight/2) {
      group.getChildren.add(anchorPane4)
    }
  })

  scene.addEventFilter(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
    if(group.getChildren.size() > 1) {
      group.getChildren.remove(1, group.getChildren.size())
    }
  })


  def isHost: Boolean = _isHost

  def setHost(value: Boolean): Unit = {
    _isHost = value
  }



  def updateRoomInfo(roomInfo: RoomInfo) = {

    anchorPaneList.foreach(_.setVisible(false))

    val isHost = if(userId == roomInfo.hostId) true else false
    setHost(isHost)
    this.roomInfo = roomInfo
    anchorControl4Self.host.setSelected(isHost)
    val indexOfHost = roomInfo.userId.indexWhere(_ == roomInfo.hostId)
    if(isHost) {
      (0 until roomInfo.userId.length-1).foreach{i =>
        if(i < indexOfHost) {
          anchorPaneList(i).setVisible(true)
        } else if(i > indexOfHost) {
          anchorPaneList(i-1).setVisible(true)
        }
      }
//      anchorPane1.setVisible(true)
//      anchorPane2.setVisible(true)
//      anchorPane3.setVisible(true)
//      anchorPane4.setVisible(true)
    } else {
      if(indexOfHost == -1) {
        log.warn(s"hostId is not in userList")
      } else {
//        val indexOfSelf = roomInfo.userId.indexWhere(_ == userId)
//        val index = if(indexOfSelf > indexOfHost)  indexOfHost-1 else indexOfHost
//        anchorPaneList(index).setVisible(true)
//        anchorControlList(index).host.setSelected(true)
      }
    }
  }

  def setListener(listener: LivingStageListener) = {
    this.listener = listener
  }

  def getGc4Self() = this.canvas4Self.getGraphicsContext2D

  def getGc4Pull() = this.canvas4Pull.getGraphicsContext2D

  override def start(primaryStage: Stage): Unit = {
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
    primaryStage.setResizable(false)
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

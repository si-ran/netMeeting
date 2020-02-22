package org.seekloud.netMeeting.pcClient.scene

import java.io.File

import javafx.application.Application
import javafx.beans.value.{ChangeListener, ObservableValue}
import javafx.geometry.{Insets, Pos}
import javafx.scene.canvas.Canvas
import javafx.scene.control.{Button, Label}
import javafx.scene.image.{Image, ImageView}
import javafx.scene.input.MouseEvent
import javafx.scene.layout._
import javafx.scene.text.TextAlignment
import javafx.scene.{Cursor, Group, Scene}
import javafx.stage.{Stage, StageStyle}
import org.seekloud.netMeeting.pcClient.Boot.addToPlatform
import org.seekloud.netMeeting.pcClient.scene.CreatorStage.MeetingType
import org.seekloud.netMeeting.pcClient.common.StageContext._
import org.slf4j.LoggerFactory

/**
  * @user: wanruolong
  * @date: 2020/2/6 11:02
  *
  */

object HomeStage{
  val log = LoggerFactory.getLogger(this.getClass)
  trait HomeStageListener{
    def createNewIssue(meetingType: MeetingType.Value)

    def close()
  }

}

class HomeStage(userId: Long) extends Application{
  import HomeStage._

  val stage = new Stage()
  val icon = new Image("/img/camera.png")

  def getUserId = userId

  private var listener: HomeStageListener = _

//  stage.initStyle(StageStyle.UTILITY)

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

//  val pane = new BorderPane(anchorPane)
  val group = new Group()
//  val imageView = new ImageView(new Image("/img/bg.png"))
//  group.getChildren.addAll(imageView, anchorPane)
  group.getChildren.add(anchorPane)

  val scene = new Scene(group, 380, 720)
/*  val bg = new BackgroundImage(new Image("/img/bg.png", scene.getWidth, scene.getHeight, false, true, true), BackgroundRepeat.NO_REPEAT, BackgroundRepeat.NO_REPEAT, BackgroundPosition.DEFAULT, BackgroundSize.DEFAULT)
  pane.setBackground(new Background(bg))*/

  startMeeting.setCursor(Cursor.HAND)
  joinMeeting.setCursor(Cursor.HAND)

  anchorPane.setLayoutX((scene.getWidth-216)/2)
  anchorPane.setLayoutY((scene.getHeight-118)/2)

  def setListener(listener: HomeStageListener): Unit = {
    this.listener = listener
  }

  override def start(primaryStage: Stage): Unit = {
    primaryStage.setScene(this.scene)
    primaryStage.setMinHeight(720)
    primaryStage.setMinWidth(380)
    primaryStage.getIcons().add(icon)
    primaryStage.show()

    primaryStage.setOnCloseRequest(event => {
      //      rmManager ! StopSelf
      log.debug("OnCloseRequest...")
      event.consume()
      if(closeClick(primaryStage)){
        this.listener.close()
        addToPlatform{
          log.info("application closing...")
          Thread.sleep(1000)
          System.exit(0)
        }
      }
    })
  }

  def showStage() = {
    start(stage)
  }

  def close() = {
    stage.close()
  }

  startMeeting.addEventFilter(MouseEvent.MOUSE_PRESSED, (event: MouseEvent) => {
    this.listener.createNewIssue(MeetingType.CREATE)
  })

  startMeeting.addEventFilter(MouseEvent.MOUSE_ENTERED, (event: MouseEvent) => {
    vBox1.setPadding(new Insets(0))
  })

  startMeeting.addEventFilter(MouseEvent.MOUSE_EXITED, (event: MouseEvent) => {
    vBox1.setPadding(new Insets(10, 0,0,0))
  })

  joinMeeting.addEventFilter(MouseEvent.MOUSE_PRESSED, (event: MouseEvent) => {
    println("join meeting")
    this.listener.createNewIssue(MeetingType.JOIN)
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

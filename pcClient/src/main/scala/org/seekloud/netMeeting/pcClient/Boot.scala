package org.seekloud.netMeeting.pcClient

/**
  * @user: wanruolong
  * @date: 2020/2/6 11:47
  *
  */

import java.io.File

import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.typed.scaladsl.adapter._
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.stream.ActorMaterializer
import akka.util.Timeout
import javafx.application.Platform
import javafx.scene.image.Image
import javafx.stage.Stage
import org.seekloud.netMeeting.pcClient.core.RmManager
import org.seekloud.netMeeting.pcClient.scene.{LoginScene, PageController}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
//import scala.language.postfixOps

object Boot {

  import org.seekloud.netMeeting.pcClient.common.AppSettings._
  private[this] val log = LoggerFactory.getLogger(this.getClass)

  implicit val system: ActorSystem = ActorSystem("netMeeting", config)
  implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds)

  //  val netImageProcessor: ActorRef[NetImageProcessor.Command] = system.spawn(NetImageProcessor.create(), "netImageProcessor")

  def addToPlatform(fun: => Unit): Unit = {
    Platform.runLater(() => fun)
  }

}

class Boot extends javafx.application.Application {
  import Boot._

  override def start(primaryStage: Stage): Unit = {
    val icon = new Image("/img/camera.png")
    val rmManager = system.spawn(RmManager.create(), "rmManager")

    val loginScene = new LoginScene(primaryStage)
    val pageController = new PageController(loginScene, rmManager)
    rmManager ! RmManager.GetPageItem(Some(pageController))

    val scene = loginScene.getScene
    primaryStage.setAlwaysOnTop(true)
    primaryStage.setResizable(false)
    primaryStage.setScene(scene)
    primaryStage.getIcons().add(icon)
    primaryStage.show()
    primaryStage.setOnCloseRequest{event =>
      println("closing...")
      System.exit(0)
    }
  }
}


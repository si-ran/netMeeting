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
import javafx.scene.text.Font
import javafx.stage.Stage
import org.seekloud.netMeeting.pcClient.common.StageContext
import org.seekloud.netMeeting.pcClient.core.RmManager
import org.seekloud.netMeeting.pcClient.scene.{PageController, HomeScene}
import org.slf4j.LoggerFactory

import scala.concurrent.duration._
import scala.language.postfixOps

object Boot {

  import org.seekloud.netMeeting.pcClient.common.AppSettings._

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

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  override def start(primaryStage: Stage): Unit = {

    val file = new File("E:\\file\\camera.png").toURI.toString
    val icon = new Image(file)



    primaryStage.setMinHeight(720)
    primaryStage.setMinWidth(380)

    val rmManager = system.spawn(RmManager.create(), "rmManager")
    val homeScene = new HomeScene()
    val context = new StageContext(primaryStage)
    val pageController = new PageController(context, homeScene, rmManager)
    pageController.showHomeScene()
    rmManager ! RmManager.GetPageItem(Some(pageController))

    /*val emojionemozilla = Font.loadFont(getClass.getResourceAsStream("/img/seguiemj.ttf"), 12) //表情浏览器？
//    DeviceUtil.init

    val context = new StageContext(primaryStage)

    val rmManager = system.spawn(RmManager.create(context), "rmManager")

    val loginController = new LoginController(context, rmManager)
    val editController = new EditController(context,rmManager,primaryStage)

    val homeScene = new HomeScene()
    val homeSceneController = new HomeController(context, homeScene, loginController, editController, rmManager)
    rmManager ! RmManager.GetHomeItems(homeScene, homeSceneController)
    homeSceneController.showScene()

    addToPlatform {
      homeSceneController.loginByTemp()
    }
*/


    primaryStage.getIcons().add(icon)

    primaryStage.setOnCloseRequest(event => {
      //      rmManager ! StopSelf
      log.info("OnCloseRequest...")
      event.consume()
      context.closeClick()
    })

  }

}


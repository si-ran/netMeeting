
package org.seekloud.netMeeting.roomManager

import java.nio.ShortBuffer

import akka.actor.{ActorSystem, Scheduler}
import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.event.{Logging, LoggingAdapter}
import akka.http.scaladsl.Http
import akka.stream.ActorMaterializer
import akka.util.Timeout
import akka.actor.typed.scaladsl.adapter._
import akka.dispatch.MessageDispatcher
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.core.{UserManager, RoomManager}
import org.seekloud.netMeeting.roomManager.http.HttpService

import scala.util.{Failure, Success}
import scala.language.postfixOps
/**
  * User: Taoz
  * Date: 8/26/2016
  * Time: 10:25 PM
  */
object Boot extends HttpService {

  import concurrent.duration._
  import AppSettings._


  override implicit val system: ActorSystem = ActorSystem("netMeeting", config)
  // the executor should not be the default dispatcher.
  override implicit val executor: MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  override implicit val materializer: ActorMaterializer = ActorMaterializer()

  override implicit val scheduler: Scheduler = system.scheduler

  override implicit val timeout: Timeout = Timeout(20 seconds) // for actor asks

  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  val userManager: ActorRef[UserManager.Command] = system.spawn(UserManager.init(), "userManager")

  val roomManager: ActorRef[RoomManager.Command] = system.spawn(RoomManager.init(), "roomManager")

  val log: LoggingAdapter = Logging(system, getClass)

	def main(args: Array[String]) {

    log.info("Starting.")

    val httpBinding = Http().bindAndHandle(routes, AppSettings.httpInterface, AppSettings.httpPort)

    httpBinding.onComplete {
      case Success(b) ⇒
        val localAddress = b.localAddress
        println(s"Server is listening on http://${localAddress.getHostName}:${localAddress.getPort}")
      case Failure(e) ⇒
        println(s"httpBinding failed with ${e.getMessage}")
        system.terminate()
        System.exit(-1)
    }

  }

}
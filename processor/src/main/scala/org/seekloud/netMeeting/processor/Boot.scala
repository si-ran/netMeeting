package org.seekloud.netMeeting.processor

import akka.actor.typed.{ActorRef, DispatcherSelector}
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout
import org.seekloud.netMeeting.processor.core.{RoomManager}
import akka.actor.typed._
import akka.http.scaladsl.Http
import scaladsl.adapter._

import scala.language.postfixOps
import org.seekloud.netMeeting.processor.http.HttpService

/**
  * User: cq
  * Date: 2020/1/16
  */
object Boot extends HttpService {
  import org.seekloud.netMeeting.processor.common.AppSettings._

  import concurrent.duration._

  implicit val system:ActorSystem = ActorSystem("processor",config)
  implicit val executor:MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds) // for actor asks
  trait Command

  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  val log: LoggingAdapter = Logging(system, getClass)

  val roomManager:ActorRef[RoomManager.Command] = system.spawn(RoomManager.create(),"roomManager")

//  val streamPushActor:ActorRef[Command]=system.spawn(StreamPushActor.create(),"streamPushActor")
//
//  val streamPullActor:ActorRef[Command] = system.spawn(StreamPullActor.create(), "streamPullActor")

  def main(args: Array[String]): Unit = {

    Http().bindAndHandle(routes, httpInterface, httpPort)
    log.info(s"Listen to the $httpInterface:$httpPort")
    log.info("Done")
  }
}

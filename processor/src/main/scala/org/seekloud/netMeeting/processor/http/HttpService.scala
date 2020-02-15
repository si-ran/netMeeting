package org.seekloud.netMeeting.processor.http

import akka.actor.{ActorSystem, Scheduler}
import akka.http.scaladsl.server.Directives._
import akka.stream.Materializer
import akka.util.Timeout

import scala.concurrent.ExecutionContextExecutor

/**
  * User: cq
  * Date: 2020/1/17
  */
trait HttpService extends
  ResourceService with ProcessorService {
  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val timeout: Timeout

  implicit val scheduler: Scheduler

  val routes =
    pathPrefix("netMeeting") {
      resourceRoutes ~ processorRoute
    }
}

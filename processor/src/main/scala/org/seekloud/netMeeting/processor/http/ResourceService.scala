package org.seekloud.netMeeting.processor.http

import akka.actor.{ActorSystem, Scheduler}
import akka.event.LoggingAdapter
import akka.http.scaladsl.model.headers.CacheDirectives.{`max-age`,public}
import akka.http.scaladsl.model.headers.`Cache-Control`
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.Route
import akka.stream.Materializer
import org.seekloud.netMeeting.processor.common.AppSettings

import scala.concurrent.ExecutionContextExecutor

/**
  * User: cq
  * Date: 2020/1/17
  */
trait ResourceService{
  implicit val system: ActorSystem

  implicit val executor: ExecutionContextExecutor

  implicit val materializer: Materializer

  implicit val scheduler: Scheduler

  val log: LoggingAdapter


  private val resources = {
    pathPrefix("css") {
      extractUnmatchedPath { path =>
        getFromResourceDirectory("css")
      }
    } ~
      pathPrefix("js") {
        extractUnmatchedPath { path =>
          getFromResourceDirectory("js")
        }
      } ~
      pathPrefix("sjsout") {
        extractUnmatchedPath { path =>
          getFromResourceDirectory("sjsout")
        }
      } ~
      pathPrefix("img") {
        getFromResourceDirectory("img")
      } ~
      pathPrefix("dash") {
        println("lalala")
        getFromResourceDirectory(s"/home/medusa/dash/10000")
      } ~
      pathPrefix("test") {
        getFromDirectory("D:\\workstation\\sbt\\vigour\\logs\\test")
      } ~
      path("jsFile" / Segment / AppSettings.projectVersion) { name =>
        val jsFileName = name + ".js"
        if (jsFileName == "frontend-fastopt.js") {
          getFromResource(s"sjsout/$jsFileName")
        } else {
          getFromResource(s"js/$jsFileName")
        }
      }
  }

  //cache code copied from zhaorui.
  private val cacheSeconds = 24 * 60 * 60

  val resourceRoutes: Route = (pathPrefix("static") & get) {
    mapResponseHeaders { headers => `Cache-Control`(`public`, `max-age`(cacheSeconds)) +: headers } {
      encodeResponse(resources)
    }
  } ~ pathPrefix("html") {
    extractUnmatchedPath { path =>
      getFromResourceDirectory("html")
    }
  }
}

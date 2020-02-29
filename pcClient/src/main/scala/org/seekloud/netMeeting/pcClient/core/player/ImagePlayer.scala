package org.seekloud.netMeeting.pcClient.core.player

import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import org.bytedeco.javacv.Frame
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.core.CaptureManager.EncodeConfig
import org.seekloud.netMeeting.pcClient.core.StreamProcess
import org.seekloud.netMeeting.pcClient.utils.ImageConverter
import org.seekloud.netMeeting.pcClient.core.StreamProcess.timestamp
import org.seekloud.netMeeting.pcClient.core.StreamProcess.timerIntervalBase
import org.slf4j.LoggerFactory

import scala.concurrent.duration._


/**
  * @user: wanruolong
  * @date: 2020/2/23 21:24
  *
  */
object ImagePlayer {
  val log = LoggerFactory.getLogger(this.getClass)

  private var speed = 1.0f

  case class LastTs(
                   var lastTs: Long = 0L
                   )

  sealed trait Command

  final case object Start extends Command

  final case object Play extends Command

  final case object Close extends Command

  final case object IMAGE_KEY

  def create(
              parent: ActorRef[StreamProcess.Command],
              frameRate: Double,
              converter: ImageConverter,
              gc: GraphicsContext,
              encodeConfig: EncodeConfig,
              imageQueue: LinkedBlockingDeque[Frame],
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
    log.debug(s"image player is staring...")
    implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
    Behaviors.withTimers[Command] { implicit timer =>
      val lastTs = LastTs()
      ctx.self ! Start
      init(parent, frameRate, lastTs, gc, encodeConfig, imageQueue, converter)
    }
  }

  def init(
            parent: ActorRef[StreamProcess.Command],
            frameRate: Double,
            lastTs: LastTs,
            gc: GraphicsContext,
            encodeConfig: EncodeConfig,
            imageQueue: LinkedBlockingDeque[Frame],
            converter: ImageConverter,
          )(
            implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case Start =>
          ctx.self ! Play
          Behaviors.same

        case Play =>
//          log.debug("got msg play")
          val frame = imageQueue.poll()
          if(null != frame && null != frame.image){
//            println(speed)
//            println(s"sound: $timestamp, image: ${frame.timestamp}   --${timestamp-frame.timestamp}")
            speed =
              if (frame.timestamp - timestamp > 80000) 1.5f
              else if (frame.timestamp - timestamp > 40000) 1.3f
              else if (timestamp - frame.timestamp > 100000) 0.1f
              else if (timestamp - frame.timestamp > 80000) 0.4f
              else if (timestamp - frame.timestamp > 40000) 0.6f
              else 1.0f
            Boot.addToPlatform{
              gc.drawImage(converter.convert(frame), 0.0, 0.0, encodeConfig.imgWidth, encodeConfig.imgHeight)
            }
            lastTs.lastTs = if(lastTs.lastTs == 0) (frame.timestamp - 1000000/frameRate).toLong else lastTs.lastTs
            var timeInterval = ((frame.timestamp - lastTs.lastTs) * speed/1000).toLong
            timeInterval = if(timeInterval < 0) (1000/frameRate).toLong else timeInterval
            lastTs.lastTs = frame.timestamp
//            println(s"$speed, $timeInterval")
            timer.startSingleTimer(IMAGE_KEY, Play, timeInterval.millis)
          }
          else ctx.self ! Play
          Behaviors.same

        case Close =>
          log.debug("image player closed.")
          Behaviors.stopped
      }
    }
}

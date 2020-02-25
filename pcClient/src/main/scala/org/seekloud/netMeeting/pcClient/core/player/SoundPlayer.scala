package org.seekloud.netMeeting.pcClient.core.player

import java.nio.{ByteBuffer, ShortBuffer}
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import javax.sound.sampled.SourceDataLine
import org.seekloud.netMeeting.pcClient.core.StreamProcess.timestamp
import org.bytedeco.javacv.Frame
import org.seekloud.netMeeting.pcClient.core.StreamProcess
import org.slf4j.LoggerFactory

import scala.concurrent.duration._


/**
  * @user: wanruolong
  * @date: 2020/2/23 21:19
  *
  */
object SoundPlayer {
  val log = LoggerFactory.getLogger(this.getClass)

  private val size = 4096

  private val dataBuf = ByteBuffer.allocate(size)

  sealed trait Command

  final case object Start extends Command

  final case object Play extends Command

  final case object Close extends Command

  final case object SOUND_KEY

  def create(
              parent: ActorRef[StreamProcess.Command],
              sdl: SourceDataLine,
              soundQueue: LinkedBlockingDeque[Frame]
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
    log.debug(s"sound player is staring...")
    implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
    Behaviors.withTimers[Command] { implicit timer =>
      ctx.self ! Start
      val sdlCapacity = sdl.available()
      init(parent, sdl, sdlCapacity, soundQueue)
    }
  }

  def init(
            parent: ActorRef[StreamProcess.Command],
            sdl: SourceDataLine,
            sdlCapacity: Int,
            soundQueue: LinkedBlockingDeque[Frame]
          )(
            implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case Start =>
          timer.startPeriodicTimer(SOUND_KEY, Play, 15.millis)
          Behaviors.same

        case Play =>
          val available = sdl.available()
          if(available > size) {
            val frame = soundQueue.poll()
            if(null != frame && null != frame.samples){
              timestamp = frame.timestamp - (sdlCapacity-available)/4096*23000
              //              println(s"frame.timestamp：${frame.timestamp}, timestamp$timestamp availables$availables, available$available")
              val shortBuffer = frame.samples(0).asInstanceOf[ShortBuffer]
              dataBuf.asShortBuffer().put(shortBuffer)
              sdl.write(dataBuf.array, 0, dataBuf.remaining())
              dataBuf.clear()
            }
          }
          Behaviors.same

        case Close =>
          log.debug(s"sound player closed.")
//          sdl.flush()
          sdl.stop()
          Behaviors.stopped
      }
    }

}

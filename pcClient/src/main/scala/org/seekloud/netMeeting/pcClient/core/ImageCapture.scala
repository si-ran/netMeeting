package org.seekloud.netMeeting.pcClient.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacv._
import org.seekloud.netMeeting.pcClient.core.CaptureManager.MediaType
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.pcClient.utils.ImageConverter

import scala.util.{Failure, Success, Try}
import scala.concurrent.duration._


object ImageCapture {
  private val log = LoggerFactory.getLogger(this.getClass)

   case class CaptureSetting(
                             var state: Boolean = false,//控制camera是否工作
                             var encodeFlag: Boolean = false,//控制是否编码
                             var needDraw: Boolean = false//判断是否需要画出来
                           )

  sealed trait Command

  final case object StartGrab extends Command

  final case object GrabFrame extends Command

  final case object SuspendCamera extends Command

  final case object Close extends Command

  final case class StartEncode(encoder: ActorRef[EncodeActor.Command]) extends Command

  final case class ChangeState(state: Option[Boolean] = None, encodeFlag: Option[Boolean] = None,
                               needDraw: Option[Boolean] = None,
                               drawActor: Option[ActorRef[CaptureManager.DrawCommand]] = None) extends Command

  final case object StopEncode extends Command

  final case object Terminate extends Command

  final case object TERMINATE_KEY


  def create(grabber: FrameGrabber,
             mediaType: MediaType.Value,
             frameRate: Int,
             drawActor: Option[ActorRef[CaptureManager.DrawCommand]] = None,
             needDraw: Boolean = true,
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        log.debug(s"ImageCapture is staring...")
        val imageConverter = new ImageConverter
        ctx.self ! StartGrab
        val captureSetting = CaptureSetting()
        captureSetting.needDraw = needDraw
        working(grabber, frameRate, imageConverter, captureSetting, drawActor, mediaType=mediaType)
      }
    }

  private def working(grabber: FrameGrabber,
                      frameRate: Int,
                      imageConverter: ImageConverter,
                      captureSetting: CaptureSetting,
                      drawActor: Option[ActorRef[CaptureManager.DrawCommand]] = None,
                      recorder: Option[ActorRef[EncodeActor.Command]] = None,
                      mediaType: MediaType.Value,
                     )(
                       implicit stashBuffer: StashBuffer[Command],
                       timer: TimerScheduler[Command]
                     ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case StartGrab =>
          log.debug(s"ImageCapture started.")
          ctx.self ! GrabFrame
          captureSetting.state = true
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, recorder, mediaType)

        case SuspendCamera =>
          log.info(s"Media image suspend.")
          captureSetting.state = false
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, recorder, mediaType)

        case GrabFrame =>
          if(captureSetting.state) {
            Try(grabber.grab()) match {
              case Success(frame) =>
                if (frame != null) {
                  if (frame.image != null) {
                    if(captureSetting.encodeFlag){
                      recorder.foreach(_ ! EncodeActor.SendFrame(frame))
                    }
                    if(captureSetting.needDraw){
                      val image = imageConverter.convert(frame)
                      drawActor.foreach(_ ! CaptureManager.DrawImage(image))
                    }
                  }
                  ctx.self ! GrabFrame
                }

              case Failure(ex) =>
                log.error(s"grab error: $ex")
            }
          }
          Behaviors.same

        case Close =>
          log.info(s"image capture stopped.")
          try {
            grabber.release()
          } catch {
            case ex: Exception =>
              log.warn(s"release image grabber failed: $ex")
          }
          timer.startSingleTimer(TERMINATE_KEY, Terminate, 100.millis)
          Behaviors.same

        case msg: ChangeState =>
          if(msg.state.isDefined) captureSetting.state = msg.state.get
          if(msg.encodeFlag.isDefined) captureSetting.encodeFlag = msg.encodeFlag.get
          if(msg.needDraw.isDefined) captureSetting.needDraw = msg.needDraw.get
          val drawsActor = if(msg.drawActor.isDefined) msg.drawActor else drawActor
          working(grabber, frameRate, imageConverter, captureSetting, drawsActor, recorder, mediaType)

        case msg: StartEncode =>
          captureSetting.encodeFlag = true
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, Some(msg.encoder), mediaType)

        case StopEncode =>
          //          recorder.foreach(_.releaseUnsafe())
          captureSetting.encodeFlag = false
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, None, mediaType)

        case x =>
          log.warn(s"unknown msg in working: $x")
          Behaviors.unhandled
      }
    }
}

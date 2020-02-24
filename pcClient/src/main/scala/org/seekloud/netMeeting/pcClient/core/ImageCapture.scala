package org.seekloud.netMeeting.pcClient.core

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.javacv._
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.pcClient.Boot.executor
import org.seekloud.netMeeting.pcClient.core.CaptureManager.{EncodeConfig, MediaType}
import org.seekloud.netMeeting.pcClient.utils.ImageConverter

import scala.concurrent.Future
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

  final case class StartGrabberImage(mediaType: MediaType.Value) extends Command

  final case class GrabberStartSuccess(grabber: FrameGrabber, imageType: MediaType.Value) extends Command

  final case object GrabFrame extends Command

  final case object SuspendCamera extends Command

  final case object Close extends Command

  final case class StartEncode(encoder: ActorRef[EncodeActor.Command]) extends Command

  final case class ChangeState(state: Option[Boolean] = None, encodeFlag: Option[Boolean] = None,
                               needDraw: Option[Boolean] = None) extends Command

  final case object StopEncode extends Command

  final case object Terminate extends Command

  final case object TERMINATE_KEY

  final case object RELEASE_GRABBER_KEY

  private[this] def switchBehavior(
                                    ctx: ActorContext[Command],
                                    behaviorName: String,
                                    behavior: Behavior[Command]
                                  )(implicit stashBuffer: StashBuffer[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(
              parent: ActorRef[CaptureManager.CaptureCommand],
              encodeConfig: EncodeConfig,
              mediaType: MediaType.Value,
              drawActor: Option[ActorRef[CaptureManager.DrawCommand]] = None,
              needDraw: Boolean = true,
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        log.debug(s"$mediaType Capture is staring...")
        val imageConverter = new ImageConverter
        ctx.self ! StartGrabberImage(mediaType)
        val captureSetting = CaptureSetting()
        captureSetting.needDraw = needDraw
        init(parent, mediaType, encodeConfig, imageConverter, captureSetting, drawActor)
      }
    }

  private def init(
                    parent: ActorRef[CaptureManager.CaptureCommand],
                    mediaType: MediaType.Value,
                    encodeConfig: EncodeConfig,
                    imageConverter: ImageConverter,
                    captureSetting: CaptureSetting,
                    drawActorOpt: Option[ActorRef[CaptureManager.DrawCommand]] = None,
                    grabber: Option[FrameGrabber] = None
                  )(
                    implicit stashBuffer: StashBuffer[Command],
                    timer: TimerScheduler[Command]
                  ):Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case msg: StartGrabberImage =>
          val imageGrabber = msg.mediaType match {
            case MediaType.Camera => new OpenCVFrameGrabber(0)
            case _ =>
              val grabber = new FFmpegFrameGrabber("desktop")
              grabber.setFormat("gdigrab")
              grabber
          }
          imageGrabber.setImageWidth(encodeConfig.imgWidth)
          imageGrabber.setImageHeight(encodeConfig.imgHeight)
          Future {
            log.debug(s"imageGrabber ${msg.mediaType} is starting...")
            imageGrabber.start()
            //          log.debug(s"cameraGrabber-${0} started.")
            imageGrabber
          }.onComplete {
            case Success(grabber) => ctx.self ! GrabberStartSuccess(grabber, msg.mediaType)
            case Failure(ex) =>
              log.error("camera start failed")
              log.error(s"$ex")
          }
          init(parent, mediaType, encodeConfig, imageConverter, captureSetting, drawActorOpt, Some(imageGrabber))

        case msg: GrabberStartSuccess =>
          log.debug(s"$mediaType grabber start success. frameRate: ${msg.grabber.getFrameRate}")
          val frameRate = msg.grabber.getFrameRate
          parent ! CaptureManager.ImageCaptureStartSuccess(frameRate)
          ctx.self ! GrabFrame
          captureSetting.state = true
          switchBehavior(ctx, "work", work(parent, mediaType, msg.grabber, encodeConfig, imageConverter, captureSetting, drawActorOpt))

        case Close =>
          log.warn(s"close in init.")
          if(grabber.isEmpty){
            timer.startSingleTimer(RELEASE_GRABBER_KEY, Close, 10.millis)
            Behaviors.same
          }
          else{
            try{
              grabber.foreach(_.close())
            } catch {
              case e: Exception =>
                log.warn(s"grabber stopped failed.")
            }
            Behaviors.stopped
          }


        case x =>
          log.warn(s"rec unknown msg in init: $x")
          Behaviors.unhandled
      }
    }

  private def work(
                    parent: ActorRef[CaptureManager.CaptureCommand],
                    mediaType: MediaType.Value,
                    grabber: FrameGrabber,
                    encodeConfig: EncodeConfig,
                    imageConverter: ImageConverter,
                    captureSetting: CaptureSetting,
                    drawActorOpt: Option[ActorRef[CaptureManager.DrawCommand]] = None,
                    recorderOpt: Option[ActorRef[EncodeActor.Command]] = None,
                  )(
                    implicit stashBuffer: StashBuffer[Command],
                    timer: TimerScheduler[Command]
                  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SuspendCamera =>
          log.info(s"Media image suspend.")
          captureSetting.state = false
          Behaviors.same

        case GrabFrame =>
          if(captureSetting.state) {
            Try(grabber.grab()) match {
              case Success(frame) =>
                if (frame != null) {
                  if (frame.image != null) {
                    if(captureSetting.encodeFlag){
                      recorderOpt.foreach(_ ! EncodeActor.SendFrame(frame))
                    }
                    if(captureSetting.needDraw){
                      val image = imageConverter.convert(frame)
                      drawActorOpt.foreach(_ ! CaptureManager.DrawImage(image))
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
          timer.startSingleTimer("image_capture", Terminate, 10.millis)
          Behaviors.same

        case Terminate =>
          Behaviors.stopped

        case msg: ChangeState =>
          if(msg.state.isDefined) captureSetting.state = msg.state.get
          if(msg.encodeFlag.isDefined) captureSetting.encodeFlag = msg.encodeFlag.get
          if(msg.needDraw.isDefined) captureSetting.needDraw = msg.needDraw.get
          Behaviors.same

        case msg: StartEncode =>
          captureSetting.encodeFlag = true
          work(parent, mediaType, grabber, encodeConfig, imageConverter, captureSetting, drawActorOpt, Some(msg.encoder))

        case StopEncode =>
          //          recorder.foreach(_.releaseUnsafe())
          captureSetting.encodeFlag = false
          Behaviors.same

        case x =>
          log.warn(s"unknown msg in working: $x")
          Behaviors.unhandled
      }
    }
}

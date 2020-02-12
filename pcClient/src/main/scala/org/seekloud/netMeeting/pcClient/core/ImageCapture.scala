package org.seekloud.netMeeting.pcClient.core

import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.Behaviors
import org.bytedeco.javacv._
import org.seekloud.netMeeting.pcClient.core.CaptureManager.MediaType
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.pcClient.core.EncodeActor
import org.seekloud.netMeeting.pcClient.utils.ImageConverter

import scala.util.{Failure, Success, Try}

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

  final case object StopCamera extends Command

  final case class StartEncode(encoder: ActorRef[EncodeActor.EncodeCmd]) extends Command

  final case class ChangeState(state: Option[Boolean] = None, encodeFlag: Option[Boolean] = None,
                               needDraw: Option[Boolean] = None, frameQueue: Option[LinkedBlockingDeque[Frame]] = None,
                               drawActor: Option[ActorRef[CaptureManager.DrawCommand]] = None) extends Command

  final case object StopEncode extends Command

  //playing sound
  sealed trait SoundCommand

  case object StartPlay extends SoundCommand

  def create(grabber: FrameGrabber,
             imageType: MediaType.Value,
             frameRate: Int,
             drawActor: Option[ActorRef[CaptureManager.DrawCommand]] = None,
             needDraw: Boolean = true,
             frameQueue: Option[LinkedBlockingDeque[Frame]] = None
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.debug(s"ImageCapture is staring...")
      val imageConverter = new ImageConverter
      ctx.self ! StartGrab
      val captureSetting = CaptureSetting()
      captureSetting.needDraw = needDraw
      working(grabber, frameRate, imageConverter, captureSetting, drawActor, mediaType=imageType, frameQueue=frameQueue)
    }

  private def working(grabber: FrameGrabber,
                      frameRate: Int,
                      imageConverter: ImageConverter,
                      captureSetting: CaptureSetting,
                      drawActor: Option[ActorRef[CaptureManager.DrawCommand]] = None,
                      recorder: Option[ActorRef[EncodeActor.EncodeCmd]] = None,
                      mediaType: MediaType.Value,
                      frameQueue: Option[LinkedBlockingDeque[Frame]] = None
                     ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case StartGrab =>
          log.info(s"Media image started.")
          ctx.self ! GrabFrame
          captureSetting.state = true
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, recorder, mediaType, frameQueue)

        case SuspendCamera =>
          log.info(s"Media image suspend.")
          captureSetting.state = false
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, recorder, mediaType, frameQueue)

        case GrabFrame =>
          if(captureSetting.state) {
//            if(mediaType == MediaType.Server)
//              log.debug(s"grab frame")
            Try(grabber.grab()) match {
              case Success(frame) =>
                if (frame != null) {
                  if (frame.image != null) {
                    if(captureSetting.encodeFlag){
                      try{
                        recorder.foreach(_ ! EncodeActor.SendFrame(frame))

                      }catch{
                        case ex:Exception=>
                          log.error(s"encode image frame error: $ex")
                          if(ex.getMessage.startsWith("av_interleaved_write_frame() error")){
                            //                  parent! CaptureActor.OnEncodeException
                            //                            ctx.self ! StopEncode
                          }
                      }

                    }
                    if(captureSetting.needDraw){
                      val image = imageConverter.convert(frame)
                      drawActor.foreach(_ ! CaptureManager.DrawImage(image))
                    }
                    else{
                      frameQueue.foreach(_.clear())
                      frameQueue.foreach(_.offer(frame))
                    }
                  }
                  if(frame.samples != null) {
                    //todo playing sound
                  }
                  ctx.self ! GrabFrame
                }

              case Failure(ex) =>
                log.error(s"grab error: $ex")
            }
          }
          Behaviors.same

        case StopCamera =>
          log.info(s"Media camera stopped.")
          try {
            grabber.release()
          } catch {
            case ex: Exception =>
              log.warn(s"release camera resources failed: $ex")
          }
          Behaviors.stopped

        case msg: ChangeState =>
          if(msg.state.isDefined) captureSetting.state = msg.state.get
          if(msg.encodeFlag.isDefined) captureSetting.encodeFlag = msg.encodeFlag.get
          if(msg.needDraw.isDefined) captureSetting.needDraw = msg.needDraw.get
          val frameQueues = if(msg.frameQueue.isDefined) msg.frameQueue else frameQueue
          val drawsActor = if(msg.drawActor.isDefined) msg.drawActor else drawActor
          working(grabber, frameRate, imageConverter, captureSetting, drawsActor, recorder, mediaType, frameQueues)

        case msg: StartEncode =>
          captureSetting.encodeFlag = true
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, Some(msg.encoder), mediaType, frameQueue)

        case StopEncode =>
          //          recorder.foreach(_.releaseUnsafe())
          captureSetting.encodeFlag = false
          working(grabber, frameRate, imageConverter, captureSetting, drawActor, None, mediaType, frameQueue)

        case x =>
          log.warn(s"unknown msg in working: $x")
          Behaviors.unhandled
      }
    }

  def soundPlayer(): Behavior[SoundCommand] = {
    Behaviors.receive[SoundCommand] {(ctx, msg) =>
      msg match {
        case StartPlay =>
          Behaviors.same
      }
    }
  }



}

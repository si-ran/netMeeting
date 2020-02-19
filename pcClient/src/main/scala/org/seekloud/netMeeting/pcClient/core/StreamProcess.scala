package org.seekloud.netMeeting.pcClient.core

import java.nio.{ByteBuffer, ShortBuffer}
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import javax.sound.sampled.{AudioFormat, AudioSystem, SourceDataLine}
import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameGrabber, Frame, FrameGrabber}
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.core.CaptureManager.{EncodeConfig, MediaType}
import org.seekloud.netMeeting.pcClient.utils.ImageConverter
import org.seekloud.netMeeting.pcClient.Boot.executor
import org.seekloud.netMeeting.pcClient.core.ImageCapture.CaptureSetting
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.util.{Failure, Success, Try}

/**
  * @user: wanruolong
  * @date: 2020/2/12 10:17
  *
  */
object StreamProcess {
  private val log = LoggerFactory.getLogger(this.getClass)

  @volatile var timestamp: Long = 0
  @volatile var timerIntervalBase = 0L
  var firstFlag = false
  var imageFirstFlag = true
  var imageFirstTs = 0L
  var soundFirstFlag = true
  var soundFirstTs = 0L

  val imageQueue = new java.util.concurrent.LinkedBlockingDeque[Frame](500)
  val soundQueue = new java.util.concurrent.LinkedBlockingDeque[Frame](500)

  sealed trait Command

  final case object InitGrabber extends Command

  final case class GrabberStartSuccess(grabber: FFmpegFrameGrabber) extends Command

  final case object StartSdl extends Command

  final case object StartGrab extends Command

  final case object GrabFrame extends Command

  final case class StartSdlSuccess(sdl: SourceDataLine) extends Command

  final case object Close extends Command

  final case object Restart extends Command

  final case object Terminate extends Command

  final case object TERMINATE_KEY

  final case object GRAB_FRAME_KEY


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
              url: String,
              encodeConfig: EncodeConfig,
              gc: GraphicsContext,
              needDraw: Boolean = true
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.debug(s"stream processor is staring...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        val converter = new ImageConverter
        ctx.self ! StartSdl
        init(parent, url, converter, needDraw, gc, encodeConfig)
      }
    }

  def init(
            parent: ActorRef[CaptureManager.CaptureCommand],
            url: String,
            converter: ImageConverter,
            needDraw: Boolean,
            gc: GraphicsContext,
            encodeConfig: EncodeConfig,
            sdl: Option[SourceDataLine] = None
          )(
            implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case InitGrabber =>
          val grabber = new FFmpegFrameGrabber(url)
          grabber.setImageWidth(encodeConfig.imgWidth)
          grabber.setImageHeight(encodeConfig.imgHeight)
          Future {
            log.debug(s"stream grabber is starting...")
            grabber.start()
            //          log.debug(s"cameraGrabber-${0} started.")
            grabber
          }.onComplete {
            case Success(grab) =>
              log.debug(s"grab start success")
              ctx.self ! GrabberStartSuccess(grab)
            case Failure(ex) =>
              log.error("camera start failed")
              log.error(s"$ex")
          }
          Behaviors.same

        case msg: GrabberStartSuccess =>
          log.debug(s"got msg $msg")
          ctx.self ! StartGrab
          switchBehavior(ctx, "work", work(parent, url, msg.grabber, converter, needDraw, gc, encodeConfig, sdl))

        case StartSdl =>
          val BIG_ENDIAN = true
          val audioFormat = new AudioFormat(encodeConfig.sampleRate, encodeConfig.sampleSizeInBit, encodeConfig.channels, true, BIG_ENDIAN)
          val sdl = AudioSystem.getSourceDataLine(audioFormat)
          Future{
            sdl.open(audioFormat)
            sdl.start()
            sdl
          }.onComplete{
            case Success(sdl) => ctx.self ! StartSdlSuccess(sdl)
            case Failure(exception) =>
              log.error(s"start sdl failed, $exception")
          }
          Behaviors.same

        case msg: StartSdlSuccess =>
          ctx.self ! InitGrabber
          log.debug(s"got msg $msg")
          init(parent, url, converter, needDraw, gc, encodeConfig, Some(msg.sdl))

        case Close =>
          log.warn(s"close in init.")
          Behaviors.stopped

        case x =>
          log.warn(s"rec unknown msg in init: $x")
          Behaviors.unhandled
      }
    }

  def work(
            parent: ActorRef[CaptureManager.CaptureCommand],
            url: String,
            grabber: FFmpegFrameGrabber,
            converter: ImageConverter,
            needDraw: Boolean,
            gc: GraphicsContext,
            encodeConfig: EncodeConfig,
            sdlOpt: Option[SourceDataLine] = None,
            soundThread: Option[Thread] = None,
            imageThread: Option[Thread] = None
          )(
            implicit stashBuffer: StashBuffer[Command],
            timer: TimerScheduler[Command]
          ): Behavior[Command] =
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case StartGrab =>
          ctx.self ! GrabFrame
          val soundThread = getSoundThread(sdlOpt.get)
          soundThread.start()
          val imageThread = getImageThread(encodeConfig.frameRate, converter, gc, encodeConfig)
          imageThread.start()
          parent ! CaptureManager.StartStreamProcessSuccess
          work(parent, url, grabber, converter, needDraw, gc, encodeConfig, sdlOpt, Some(soundThread), Some(imageThread))


        case GrabFrame =>
          Try(grabber.grab()) match {
            case Success(frame) =>
              if (null != frame) {
                if (null != frame.image) {
//                  println(s"image: ${frame.timestamp}")
                  imageFirstTs = if(imageFirstTs == 0) frame.timestamp else imageFirstTs
                  if(imageFirstTs != 0 && soundFirstTs != 0){
                    timerIntervalBase = imageFirstTs - soundFirstTs
                  }
                  imageQueue.offer(frame.clone())
                }
                if(null != frame.samples) {
//                  println(s"sound ${frame.timestamp}")
                  soundFirstTs = if(imageFirstTs == 0) frame.timestamp else soundFirstTs
                  soundQueue.offer(frame.clone())
                }
                if(imageQueue.size() > 50 && soundQueue.size() > 50){
                  timer.startSingleTimer(GRAB_FRAME_KEY, GrabFrame, (1000/encodeConfig.frameRate).millis)
                }
                else{
                  ctx.self ! GrabFrame
                }
              }

            case Failure(ex) =>
              ctx.self ! Restart
              log.error(s"grab error: $ex")
              log.info(s"stop grab stream")
          }
          Behaviors.same

        case Restart =>
          log.debug("pull stream restart.")
          try {
            grabber.release()
          } catch {
            case ex: Exception =>
              log.warn(s"release stream resources failed: $ex")
          }
          ctx.self ! InitGrabber
          switchBehavior(ctx, "init", init(parent, url, converter, needDraw, gc, encodeConfig))

        case Close =>
          try {
            grabber.release()
            if(sdlOpt.nonEmpty && sdlOpt.get.isOpen){
              sdlOpt.foreach(_.close())
              log.debug(s"sdl closed.")
              log.debug("stream grabber closed")
            }
          } catch {
            case ex: Exception =>
              log.warn(s"release stream resources failed: $ex")
          }

          soundThread.foreach(_.interrupt())
          imageThread.foreach(_.interrupt())
          timer.startSingleTimer(TERMINATE_KEY, Terminate, 10.millis)
          Behaviors.same

        case Terminate =>
          log.info(s"stream processor stopped.")
          Behaviors.stopped

        case x =>
          log.warn(s"rec unknown msg in work: $x")
          Behaviors.unhandled

      }
    }

  def getSoundThread(sdl: SourceDataLine): Thread = new Thread(){
    override def run(): Unit = {
      log.debug(s"get sound thread.")
      val sdlCapacity = sdl.available()
      val size = 4096
      val dataBuf = ByteBuffer.allocate(size)
      //      tempBuf.position(1)
      try{
        while(true){
          val available = sdl.available()
          if(available >= size){
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
          Thread.sleep(15)
        }
      } catch {
        case e: InterruptedException =>
          log.info(s"sound player interrupted.")
      }

    }
  }

  def getImageThread(
                      frameRate: Double,
                      converter: ImageConverter,
                      gc: GraphicsContext,
                      encodeConfig: EncodeConfig
                    ): Thread = new Thread(){
    override def run(): Unit = {
      log.debug(s"image thread started.")
      var lastTs = 0L
//      val timeIntervalBase = 1000/frameRate
      var speed = 1f
      try{
        while (true){
          val frame = imageQueue.poll()
          if(null != frame && null != frame.image){
//            println(s"timerIntervalBase:$timerIntervalBase")
//            println(s"frame.timestamp - timeIntervalBase: ${frame.timestamp} - $timestamp = ${frame.timestamp-timestamp}")
            if(frame.timestamp - timestamp > 40000 + timerIntervalBase)
              speed = 1.1f
            if(frame.timestamp - timestamp > 80000 + timerIntervalBase)
              speed = 1.3f
            if(timestamp - frame.timestamp > 40000 + timerIntervalBase)
              speed = 0.8f
            if(timestamp - frame.timestamp > 80000 + timerIntervalBase)
              speed = 0.6f
            Boot.addToPlatform {
              gc.drawImage(converter.convert(frame), 0.0, 0.0, encodeConfig.imgWidth, encodeConfig.imgHeight)
            }
            lastTs = if(lastTs == 0) (frame.timestamp - 1000000/frameRate).toLong else lastTs
            var timeInterval = ((frame.timestamp - lastTs) * speed/1000).toLong
            timeInterval = if(timeInterval < 0) (1000/frameRate).toLong else timeInterval
            lastTs = frame.timestamp
//            println(s"sleep: $timeInterval")
            //        println(timeInterval)
            Thread.sleep(timeInterval)
          }
        }
      } catch {
        case e: InterruptedException =>
          log.info(s"image player interrupted.")
      }

    }
  }


}

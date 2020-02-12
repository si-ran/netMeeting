package org.seekloud.netMeeting.pcClient.core

import java.nio.{ByteBuffer, ShortBuffer}
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import javafx.scene.canvas.GraphicsContext
import javax.sound.sampled.{AudioFormat, AudioSystem, SourceDataLine}
import org.bytedeco.javacv.{CanvasFrame, Frame, FrameGrabber}
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
  val imageQueue = new java.util.concurrent.LinkedBlockingDeque[Frame](500)
  val soundQueue = new java.util.concurrent.LinkedBlockingDeque[Frame](500)

  sealed trait Command

  final case object StartGrab extends Command

  final case object GrabFrame extends Command

  final case class StartSdlSuccess(sdl: SourceDataLine) extends Command

  final case object Close extends Command

  final case object Terminate extends Command

  final case object TERMINATE_KEY

  final case object GRAB_FRAME_KEY


  def create(grabber: FrameGrabber,
             encodeConfig: EncodeConfig,
             gc: GraphicsContext,
             needDraw: Boolean = true
            ): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.debug(s"ImageCapture is staring...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
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
        val converter = new ImageConverter
        ctx.self ! StartGrab
        working(grabber, converter, needDraw, gc, encodeConfig)
      }
    }

  def working(
               grabber: FrameGrabber,
               converter: ImageConverter,
               needDraw: Boolean,
               gc: GraphicsContext,
               encodeConfig: EncodeConfig,
               sdl: Option[SourceDataLine] = None,
               soundThread: Option[Thread] = None,
               imageThread: Option[Thread] = None
             )(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] = {
    Behaviors.receive[Command] {(ctx, msg) =>
      msg match {
        case StartGrab =>
          ctx.self ! GrabFrame
          Behaviors.same

        case GrabFrame =>
          Try(grabber.grab()) match {
            case Success(frame) =>
              if (null != frame) {
                if (null != frame.image) {
                  imageQueue.offer(frame.clone())
                }
                if(null != frame.samples) {
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
              log.error(s"grab error: $ex")
              log.info(s"stop grab stream")
          }
          Behaviors.same

        case msg: StartSdlSuccess =>
          val soundThread = getSoundThread(msg.sdl)
          soundThread.start()
          val imageThread = getImageThread(encodeConfig.frameRate.toDouble, converter, gc, encodeConfig)
          imageThread.start()
          working(grabber, converter, needDraw, gc, encodeConfig, Some(msg.sdl), Some(soundThread), Some(imageThread))

        case Close =>
          try {
            grabber.release()
            if(sdl.nonEmpty && sdl.get.isOpen){
              sdl.foreach(_.close())
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
          working(null,null, needDraw, null, null)

        case Terminate =>
          log.info(s"stream processor stopped.")
          Behaviors.stopped
      }
    }
  }

  def getSoundThread(sdl: SourceDataLine): Thread = new Thread(){
    override def run(): Unit = {
      val sdlCapacity = sdl.available()
      val size = 4096
      val dataBuf = ByteBuffer.allocate(size)
      val tempBuf = ByteBuffer.allocate(size)
      tempBuf.flip()
      //      tempBuf.position(1)
      while(true){
        val available = sdl.available()
        if(available >= size){
          val shortBuffer = if(tempBuf.remaining()>0){
            tempBuf.asShortBuffer()
          }else{
            val frame = soundQueue.poll()
            if(null != frame && null != frame.samples){
              timestamp = frame.timestamp - (sdlCapacity-available)/4096*23000
              //                println(s"frame.timestamp：${frame.timestamp}, timestamp$timestamp availables$availables, available$available")
              frame.samples(0).asInstanceOf[ShortBuffer]
            }
            else null
          }
          if(null != shortBuffer){
            dataBuf.asShortBuffer().put(shortBuffer)
            sdl.write(dataBuf.array, 0, dataBuf.remaining())
          }
        }
        Thread.sleep(15)
      }
    }
  }

  def getImageThread(
                      frameRate: Double,
                      converter: ImageConverter,
                      gc: GraphicsContext,
                      encodeConfig: EncodeConfig
                    ): Thread = new Thread(){
//    val canvasFrame = new CanvasFrame("file")
    override def run(): Unit = {
      val timeIntervalBase = 1000/frameRate
      var speed = 1f
      while (true){
        val frame = imageQueue.poll()
        if(null != frame && null != frame.image){
          //          println(s"frame.timestamp - timeIntervalBase: ${frame.timestamp}   $timestamp")
          if(frame.timestamp - timestamp > 40000)
            speed = 1.1f
          if(frame.timestamp - timestamp > 80000)
            speed = 1.3f
          if(timestamp - frame.timestamp > 40000)
            speed = 0.8f
          if(timestamp - frame.timestamp > 80000)
            speed = 0.6f
          Boot.addToPlatform {
            gc.drawImage(converter.convert(frame), 0.0, 0.0, encodeConfig.imgWidth, encodeConfig.imgHeight)
          }
//          canvasFrame.showImage(frame)
        }
        val timeInterval = (timeIntervalBase * speed).toLong
        //        println(timeInterval)
        Thread.sleep(timeInterval)
      }
    }
  }


}

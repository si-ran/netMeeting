package org.seekloud.netMeeting.processor.core

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, FileOutputStream, OutputStream}
import java.nio.ShortBuffer
import java.util.concurrent.LinkedBlockingDeque

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import javax.imageio.ImageIO
import org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGBA
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.opencv.global.{opencv_imgproc => OpenCVProc}
import org.bytedeco.opencv.global.{opencv_core => OpenCVCore}
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv._
import org.bytedeco.opencv.opencv_core.{Mat, Rect, Scalar, Size}
import org.seekloud.netMeeting.processor.Boot.executor
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.processor.common.AppSettings._

import scala.concurrent.duration._
import org.seekloud.netMeeting.processor.Boot.roomManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future
import scala.util.{Failure, Success}


/**
  * User: cq
  * Date: 2020/1/16
  */
object RecorderActor {
  var audioChannels = 2 //todo 待议
  val sampleFormat = 1 //todo 待议
  var frameRate = 30
  val layout_x = 2

  val width = 640
  val height = 360

  val bottomSize = new Size(width, height)
  val topSize = new Size(width/2, height/2)
  val toMat = new ToMat()
  val resizeMat = new Mat()
  val canvas = new Mat(bottomSize, OpenCVCore.CV_8UC3, new Scalar(0, 0, 0, 0))
  val converter = new OpenCVFrameConverter.ToIplImage()
  val position = List[(Int, Int)]((0, 0), (width/2, 0), (0, height/2), (width/2, height/2))

  def imgCombination( frameList: List[Frame]) = {
    (0 until frameList.length).foreach{ i =>
      val layMat = toMat.convert(frameList(i))
      OpenCVProc.resize(layMat, resizeMat, topSize)
      val layMask = new Mat(topSize, OpenCVCore.CV_8UC1, new Scalar(1d))
      val layRoi = canvas(new Rect(position(i)._1, position(i)._2, topSize.width(), topSize.height()))
      resizeMat.copyTo(layRoi, layMask)
    }
    val convertFrame = converter.convert(canvas)
    convertFrame
  }

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case object InitFilter extends Command

  case class StartFilterSuccess(ffFilter: FFmpegFrameFilter) extends Command

  case class StartRecorderSuccess(recorder: FFmpegFrameRecorder, ffFilter: FFmpegFrameFilter) extends Command

  case class RecorderImage(liveId: String, frame: Frame) extends Command

  case class RecorderSound(liveId: String, frame: Frame) extends Command

  case object Close extends Command




  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case class UpdateUserList(userList:List[String]) extends Command

  case object RestartRecord extends Command

  case object StopRecorder extends Command

  case object CloseRecorder extends Command

  case class NewFrame(userId: String, frame: Frame) extends Command


  case object TimerKey4Close

/*  sealed trait VideoCommand

  case class TimeOut(msg: String) extends Command

  case class Image4Mix(liveId:String, frame: Frame) extends VideoCommand

  case class ImageDraw(liveId:String,frame:Frame) extends VideoCommand

  case class Sound(frame: Frame) extends VideoCommand

  case object Image4Test extends VideoCommand

  case class UpdateFrameQueue(userIdList:List[String]) extends VideoCommand

  case class SetLayout(layout: Int) extends VideoCommand

  case class NewRecord4Ts(recorder4ts: FFmpegFrameRecorder) extends VideoCommand

  case object Close extends VideoCommand*/

  case class Ts4User(var time: Long = 0)

  case class Ts4LastImage(var time: Long = -1)

  case class Ts4LastSample(var time: Long = 0)

  private val emptyAudio = ShortBuffer.allocate(1024 * 2)
  private val emptyAudio4one = ShortBuffer.allocate(1152)

  private[this] def switchBehavior(
                                    ctx: ActorContext[Command],
                                    behaviorName: String,
                                    behavior: Behavior[Command]
                                  )(implicit stashBuffer: StashBuffer[Command]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(
              parent: ActorRef[RoomActor.Command],
              roomId: Long,
              userIdList:List[String],
              pushLiveUrl:String,
              layout: Int,
              queMap: mutable.HashMap[String, LinkedBlockingDeque[Frame]]
            ): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.debug(s"recorderActor start----")
//          log.info(s"${ctx.self} userIdList:${userIdList}")
//          avutil.av_log_set_level(-8)
          ctx.self ! InitFilter

          init(parent, roomId,  userIdList, layout, pushLiveUrl, queMap)
      }
    }
  }

  def init(
            parent: ActorRef[RoomActor.Command],
            roomId: Long, userIdList:List[String], layout: Int,
            pushLiveUrl:String,
            queMap: mutable.HashMap[String, LinkedBlockingDeque[Frame]]
          )(
            implicit timer: TimerScheduler[Command],
            stashBuffer: StashBuffer[Command]
          ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case InitFilter =>
          var str =""
          (0 until userIdList.length).reverse.foreach{i =>
            str+=s"[$i:a]"
          }
          str = s"$str amix=inputs=${userIdList.length}:duration=longest:dropout_transition=3 [a]"
          println(s"str: $str")
          val ffFilterN = new FFmpegFrameFilter(str, audioChannels)
          ffFilterN.setAudioChannels(audioChannels)
          ffFilterN.setSampleFormat(sampleFormat)
          ffFilterN.setAudioInputs(userIdList.size)
          Future{
            ffFilterN.start()
            ffFilterN
          }.onComplete{
            case Success(ffFilter) =>
              ctx.self ! StartFilterSuccess(ffFilter)
            case Failure(ex) =>
              log.info(s"filter start failed ${ex.getMessage}")
          }
//          roomManager ! RoomManager.RecorderRef(roomId, ctx.self)
          Behaviors.same

        case msg: StartFilterSuccess =>
          val recorder = new FFmpegFrameRecorder(pushLiveUrl, 640, 480, audioChannels)
          recorder.setVideoOption("tune", "zerolatency")
          recorder.setVideoOption("preset", "ultrafast")
          recorder.setVideoOption("crf", "23")
          recorder.setFrameRate(frameRate)
          recorder.setVideoBitrate(bitRate)
          recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
          recorder.setMaxBFrames(0)

          recorder.setAudioQuality(0)
          recorder.setAudioOption("crf", "0")
          recorder.setFormat("flv")
          recorder.setSampleRate(44100)
          recorder.setAudioChannels(2)
          recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
          Future{
            recorder.start()
            recorder
          }.onComplete{
            case Success(recorder) =>
              ctx.self ! StartRecorderSuccess(recorder, msg.ffFilter)
            case Failure(ex) =>
              log.info(s"recorder start failed ${ex.getMessage}")
          }
          Behaviors.same

        case msg: StartRecorderSuccess =>
          switchBehavior(ctx, "work", work(parent, roomId, userIdList, queMap,  layout,msg.recorder, msg.ffFilter))


/*        case NewFrame(userId, frame) =>
          val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
          val frameMapQueue = scala.collection.mutable.Map[String,LinkedBlockingDeque[Frame]]()
          userIdList.foreach{
            id => {
              frameMapQueue.put(id, new LinkedBlockingDeque[Frame]())
            }
          }
          val drawer = ctx.spawn(draw(canvas, canvas.getGraphics, Ts4LastImage(), frameMapQueue, recorder4ts,
           new Java2DFrameConverter, layout, "defaultImg.jpg", roomId, (640, 360), userIdList), s"drawer_${roomId}_$userId")
          ctx.self ! NewFrame(userId, frame)
          work(roomId,userIdList,layout,recorder4ts,ffFilter, drawer,ts4User,tsDiffer,canvasSize)*/

        case Close =>
          log.info("recorder close in init")
          Behaviors.stopped

      }
    }
  }

  def work(
            parent: ActorRef[RoomActor.Command],
            roomId: Long,
            userIdList:List[String],
            queMap: mutable.HashMap[String, LinkedBlockingDeque[Frame]],
            layout: Int,
            recorder: FFmpegFrameRecorder,
            ffFilter: FFmpegFrameFilter,
          )(
            implicit timer: TimerScheduler[Command],
            stashBuffer: StashBuffer[Command]
          ): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case msg: RecorderImage =>
          var frameList = List[Frame](msg.frame)
          val index = userIdList.indexWhere(_ == msg.liveId)
          (0 until userIdList.length).foreach{i =>
            if(i != index) {
              val queOpt = queMap.get(userIdList(i))
              if(queOpt.isDefined) {
                val frame = queOpt.get.peek()
                if(frame != null && frame.image != null) {
                  frameList = frame :: frameList
                }
              }

            }
          }
          val frame = imgCombination(frameList)
          recorder.record(frame)
          Behaviors.same

        case msg: RecorderSound =>
          if(msg.frame != null && msg.frame.samples != null) {
            try {
              val index = userIdList.indexWhere(_ == msg.liveId)
              ffFilter.pushSamples(index, 2, sampleRate, ffFilter.getSampleFormat, msg.frame.samples: _*)
              val frame = ffFilter.pullSamples()
              if(frame != null && frame.samples != null) {
                recorder.record(frame)
              }
            } catch {
              case e: Exception =>
                log.info(s"recorder samples error ${e.getMessage}")
            }
          }
          Behaviors.same

        case Close =>
          try {
            ffFilter.close()
            recorder.close()
          } catch {
            case e: Exception =>
              log.info(s"close error ${e.getMessage}")
          }
          Behaviors.stopped

        case x =>
          log.info(s"rec unknown msg in work $x")
          Behaviors.same


  /*      case UpdateUserList(userList4updata:List[String]) =>
          var str =""
          (0 until userList4updata.length).reverse.foreach{i =>
            str+=s"[$i:a]"
          }
          log.info(s"$str amix=inputs=${userList4updata.length}:duration=longest:dropout_transition=3 [a]")
          val ffFilterN = new FFmpegFrameFilter(s"$str amix=inputs=${userList4updata.length}:duration=longest:dropout_transition=3 [a]", audioChannels)
          ffFilterN.setAudioChannels(audioChannels)
          ffFilterN.setSampleFormat(sampleFormat)
          ffFilterN.setAudioInputs(userList4updata.size)
          ffFilterN.start()
          work(roomId,userList4updata,layout,recorder,ffFilterN,drawer,ts4User,tsDiffer,canvasSize)
*/
       /* case msg: UpdateRoomInfo =>
          log.info(s"$roomId got msg: $msg in work.")
          if (msg.layout != layout) {
            drawer ! SetLayout(msg.layout)
          }
          ctx.self ! RestartRecord
          work(roomId,  userIdList, msg.layout, recorder, ffFilter, drawer, ts4User, tsDiffer, canvasSize)
*/
       /* case m@RestartRecord =>
          log.info(s"couple state get $m")
          Behaviors.same
*/
/*        case CloseRecorder =>
          try {
            ffFilter.close()
            drawer ! Close
          } catch {
            case e: Exception =>
              log.error(s"$roomId recorder close error ---")
          }
          Behaviors.stopped*/


/*        case StopRecorder =>
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 1.seconds)
          Behaviors.same*/


      }
    }
  }

/*  def draw(canvas: BufferedImage, graph: Graphics, lastTime: Ts4LastImage, frameMapQueue: mutable.Map[String,LinkedBlockingDeque[Frame]],
           recorder4ts: FFmpegFrameRecorder,convert:Java2DFrameConverter,
           layout: Int = 0, bgImg: String, roomId: Long, canvasSize: (Int, Int),
           userIdList:List[String]): Behavior[VideoCommand] = {
    Behaviors.setup[VideoCommand] { ctx =>
      Behaviors.receiveMessage[VideoCommand] {
        case Image4Test =>
          graph.drawString("Hello",100,100)
          val frame = convert.convert(canvas)
          recorder4ts.record(frame)
          Behaviors.same

        case t:Image4Mix =>
          frameMapQueue.get(t.liveId).foreach{queue =>
            println(s"${t.liveId} offer frame")
            queue.clear()
            queue.offer(t.frame)
          }
          Behaviors.same

        case f:ImageDraw =>
//          val time = f.frame.timestamp
          val size = frameMapQueue.size
          val layout_y = (size+1)/layout_x
          val width = canvasSize._1/layout_x
          val height = canvasSize._2/layout_y
          frameMapQueue.get(f.liveId).foreach{queue =>
            println(s"${f.liveId} offer frame")
            queue.clear()
            queue.offer(f.frame)
          }
          for(i <- 0 until userIdList.length){
            val frame = frameMapQueue.get(userIdList(i)).get.peek()
            var img:BufferedImage=null
            if(frame != null && frame.image != null){
              img = convert.convert(frame)
            }else {
              log.warn(s"$i frame from deq is null")
            }
            graph.drawImage(img, i%layout_x*width, i/layout_x*height, width,height,null)
            graph.drawString(s"User ${i+1}",i%layout_x*width+50,i/layout_x*height+50)
          }
          val frame = convert.convert(canvas)
//          log.info(s"${ctx.self} frame=$frame, userIdList=${userIdList}")
          recorder4ts.record(frame)
//          log.info("recorded")
          Behaviors.same

        case msg: Sound =>
          recorder4ts.record(msg.frame)
          Behaviors.same

        case UpdateFrameQueue(userIdList4Updata:List[String]) =>
          userIdList4Updata.map{
            id=>
              if(frameMapQueue.get(id).isEmpty){
                frameMapQueue.put(id, new LinkedBlockingDeque[Frame]())
              }
          }
          userIdList.map{
            id =>
              if(userIdList4Updata.find(_ == id).isEmpty){
                frameMapQueue -= id
              }
          }
          draw(canvas, graph, lastTime, frameMapQueue, recorder4ts, convert, layout, bgImg, roomId, canvasSize, userIdList4Updata)
      }
    }
  }*/
}

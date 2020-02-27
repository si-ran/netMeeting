package org.seekloud.netMeeting.processor.core

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, FileOutputStream, OutputStream}
import java.nio.ShortBuffer

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import javax.imageio.ImageIO
import org.bytedeco.ffmpeg.global.avutil.AV_PIX_FMT_RGBA
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacv.{FFmpegFrameFilter, FFmpegFrameRecorder, Frame, Java2DFrameConverter}
import org.seekloud.netMeeting.processor.Boot.executor
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.processor.common.AppSettings._

import scala.concurrent.duration._
import org.seekloud.netMeeting.processor.Boot.roomManager

import scala.collection.mutable
import scala.collection.mutable.ListBuffer
import scala.concurrent.Future


/**
  * User: cq
  * Date: 2020/1/16
  */
object RecorderActor {
  var audioChannels = 2 //todo 待议
  val sampleFormat = 1 //todo 待议
  var frameRate = 30
  val layout_x = 2

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case class UpdateUserList(userList:List[String]) extends Command

  case object InitFilter extends Command

  case object RestartRecord extends Command

  case object StopRecorder extends Command

  case object CloseRecorder extends Command

  case class NewFrame(userId: String, frame: Frame) extends Command

//  case class UpdateRecorder(channel: Int, sampleRate: Int, frameRate: Double, width: Int, height: Int, liveId: String) extends Command

  case object TimerKey4Close

  sealed trait VideoCommand

  case class TimeOut(msg: String) extends Command

  case class Image4Mix(liveId:String, frame: Frame) extends VideoCommand

  case class ImageDraw(liveId:String,frame:Frame) extends VideoCommand

  case class Sound(frame: Frame) extends VideoCommand

  case object Image4Test extends VideoCommand

  case class UpdateFrameQueue(userIdList:List[String]) extends VideoCommand

  case class SetLayout(layout: Int) extends VideoCommand

  case class NewRecord4Ts(recorder4ts: FFmpegFrameRecorder) extends VideoCommand

  case object Close extends VideoCommand

  case class Ts4User(var time: Long = 0)

  case class Ts4LastImage(var time: Long = -1)

  case class Ts4LastSample(var time: Long = 0)

  private val emptyAudio = ShortBuffer.allocate(1024 * 2)
  private val emptyAudio4one = ShortBuffer.allocate(1152)

  def create(roomId: Long,userIdList:List[String], pushLiveUrl:String, layout: Int): Behavior[Command] = {
    Behaviors.setup[Command] { ctx =>
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] {
        implicit timer =>
          log.info(s"recorderActor start----")
          log.info(s"${ctx.self} userIdList:${userIdList}")
          avutil.av_log_set_level(-8)
          val recorder4ts = new FFmpegFrameRecorder(pushLiveUrl, 640, 480, audioChannels)
//          val FileOutPath1 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/out.flv"
//          val outputStream = new FileOutputStream(new File(FileOutPath1))
//          val recorder4ts = new FFmpegFrameRecorder(outputStream,640,480,audioChannels)
          recorder4ts.setFrameRate(frameRate)
          recorder4ts.setVideoBitrate(bitRate)
          recorder4ts.setVideoCodec(avcodec.AV_CODEC_ID_H264)
          recorder4ts.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
          recorder4ts.setMaxBFrames(0)
          recorder4ts.setFormat("flv")
          try {
            recorder4ts.startUnsafe()
          } catch {
            case e: Exception =>
              log.error(s" recorder meet error when start:$e")
          }
          ctx.self ! InitFilter
          init(roomId,  userIdList, layout, recorder4ts, null, null, null, 30000, (0, 0))
      }
    }
  }

  def init(roomId: Long, userIdList:List[String], layout: Int,
           recorder4ts: FFmpegFrameRecorder,
           ffFilter: FFmpegFrameFilter,
           drawer: ActorRef[VideoCommand],
           ts4User: List[Ts4User],
           tsDiffer: Int = 30000, canvasSize: (Int, Int))(implicit timer: TimerScheduler[Command],
                                                            stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case InitFilter =>
          if (ffFilter != null) {
            ffFilter.close()
          }
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
          ffFilterN.start()
          roomManager ! RoomManager.RecorderRef(roomId, ctx.self)
          init(roomId, userIdList, layout, recorder4ts, ffFilterN, drawer, ts4User, tsDiffer, canvasSize)

        case NewFrame(userId, frame) =>
          val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
          val frameMapQueue = scala.collection.mutable.Map[String,mutable.Queue[Frame]]()
          userIdList.foreach{
            id => {
              frameMapQueue.put(id,mutable.Queue[Frame]())
            }
          }
          val drawer = ctx.spawn(draw(canvas, canvas.getGraphics, Ts4LastImage(), frameMapQueue, recorder4ts,
           new Java2DFrameConverter, layout, "defaultImg.jpg", roomId, (640, 360), userIdList), s"drawer_${roomId}_$userId")
          ctx.self ! NewFrame(userId, frame)
          work(roomId,userIdList,layout,recorder4ts,ffFilter, drawer,ts4User,tsDiffer,canvasSize)

        case CloseRecorder =>
          try {
            ffFilter.close()
            drawer ! Close
          } catch {
            case e: Exception =>
              log.error(s"$roomId recorder close error ---")
          }
          Behaviors.stopped

        case StopRecorder =>
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 1.seconds)
          Behaviors.same
      }
    }
  }

  def work(roomId: Long, userIdList:List[String], layout: Int,
           recorder4ts: FFmpegFrameRecorder,
           ffFilter: FFmpegFrameFilter,
           drawer: ActorRef[VideoCommand],
           ts4User: List[Ts4User],
           tsDiffer: Int = 30000, canvasSize: (Int, Int))
          (implicit timer: TimerScheduler[Command],
           stashBuffer: StashBuffer[Command]): Behavior[Command] = {
    log.info(s"$roomId recorder to couple behavior")
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case NewFrame(liveId, frame) =>
          if (frame.image != null) {
//            drawer ! Image4Test
            if(liveId == userIdList(0)){
              drawer ! ImageDraw(liveId,frame)
            }else{
              drawer ! Image4Mix(liveId,frame)
            }
          }
          if (frame.samples != null) {
            try {
              val index = userIdList.indexWhere(_ == liveId)
              ffFilter.pushSamples(index, frame.audioChannels, sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
              val f = ffFilter.pullSamples()
              if(f != null && f.samples != null){
//                log.info("record sample")
                drawer ! Sound(f)
//                recorder4ts.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
              }
            } catch {
              case ex: Exception =>
                log.debug(s"$liveId record sample error system: $ex")
            }
          }
          Behaviors.same

        case UpdateUserList(userList4updata:List[String]) =>
          drawer ! UpdateFrameQueue(userList4updata)
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
          work(roomId,userList4updata,layout,recorder4ts,ffFilterN,drawer,ts4User,tsDiffer,canvasSize)

        case msg: UpdateRoomInfo =>
          log.info(s"$roomId got msg: $msg in work.")
          if (msg.layout != layout) {
            drawer ! SetLayout(msg.layout)
          }
          ctx.self ! RestartRecord
          work(roomId,  userIdList, msg.layout, recorder4ts, ffFilter, drawer, ts4User, tsDiffer, canvasSize)

        case m@RestartRecord =>
          log.info(s"couple state get $m")
          Behaviors.same

        case CloseRecorder =>
          try {
            ffFilter.close()
            drawer ! Close
          } catch {
            case e: Exception =>
              log.error(s"$roomId recorder close error ---")
          }
          Behaviors.stopped

        case StopRecorder =>
          timer.startSingleTimer(TimerKey4Close, CloseRecorder, 1.seconds)
          Behaviors.same

        case x =>
          Behaviors.same
      }
    }
  }

  def draw(canvas: BufferedImage, graph: Graphics, lastTime: Ts4LastImage, frameMapQueue: mutable.Map[String,mutable.Queue[Frame]],
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
          frameMapQueue.get(t.liveId).foreach( _ += t.frame)
          Behaviors.same

        case f:ImageDraw =>
          val time = f.frame.timestamp
          val size = frameMapQueue.size
          val layout_y = (size+1)/layout_x
          val width = canvasSize._1/layout_x
          val height = canvasSize._2/layout_y
          frameMapQueue.get(f.liveId).foreach( _ += f.frame)
          for(i <- 0 until userIdList.length){
            val queue = frameMapQueue.get(userIdList(i)).get
            var img:BufferedImage=null
            if(queue.nonEmpty){
              img = convert.convert(queue.dequeue())
            }
            graph.drawImage(img, i%layout_x*width, i/layout_x*height, width,height,null)
            graph.drawString(s"User ${i+1}",i%layout_x*width+50,i/layout_x*height+50)
          }
          val frame = convert.convert(canvas)
//          log.info(s"${ctx.self} frame=$frame, userIdList=${userIdList}")
          recorder4ts.record(frame.clone())
//          log.info("recorded")
          Behaviors.same

        case msg: Sound =>
          recorder4ts.record(msg.frame)
          Behaviors.same

        case UpdateFrameQueue(userIdList4Updata:List[String]) =>
          userIdList4Updata.map{
            id=>
              if(frameMapQueue.get(id).isEmpty){
                frameMapQueue.put(id, mutable.Queue[Frame]())
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
  }
}

package org.seekloud.netMeeting.processor.core

import java.awt.Graphics
import java.awt.image.BufferedImage
import java.io.{File, FileOutputStream, OutputStream}
import java.nio.ShortBuffer

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{Behaviors, StashBuffer, TimerScheduler}
import org.bytedeco.ffmpeg.global.{avcodec, avutil}
import org.bytedeco.javacv.{FFmpegFrameFilter, FFmpegFrameRecorder, Frame, Java2DFrameConverter}
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.processor.common.AppSettings._

import scala.concurrent.duration._
import org.seekloud.netMeeting.processor.Boot.roomManager
import org.seekloud.netMeeting.processor.test.TestPullAndPush.FileOutPath


/**
  * User: cq
  * Date: 2020/1/16
  */
object RecorderActor {
  var audioChannels = 2 //todo 待议
  val sampleFormat = 1 //todo 待议
  var frameRate = 30

  private val log = LoggerFactory.getLogger(this.getClass)

  sealed trait Command

  case class UpdateRoomInfo(roomId: Long, layout: Int) extends Command

  case object InitFilter extends Command

  case object RestartRecord extends Command

  case object StopRecorder extends Command

  case object CloseRecorder extends Command

  case class NewFrame(userId: String, frame: Frame) extends Command

  case class UpdateRecorder(channel: Int, sampleRate: Int, frameRate: Double, width: Int, height: Int, liveId: String) extends Command

  case object TimerKey4Close

  sealed trait VideoCommand

  case class TimeOut(msg: String) extends Command

  case class Image4Mix(liveId:String, frame: Frame) extends VideoCommand

  case class SetLayout(layout: Int) extends VideoCommand

  case class NewRecord4Ts(recorder4ts: FFmpegFrameRecorder) extends VideoCommand

  case object Close extends VideoCommand

  case class Ts4User(var time: Long = 0)

  case class Image(val liveId:String,var frame: Frame = null)

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
          avutil.av_log_set_level(-8)
          val recorder4ts = new FFmpegFrameRecorder(pushLiveUrl, 640, 480, audioChannels)
//          val outputStream = new FileOutputStream(new File(FileOutPath))
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
          roomManager ! RoomManager.RecorderRef(roomId, ctx.self)
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
          val ffFilterN = new FFmpegFrameFilter("[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]", audioChannels)
          ffFilterN.setAudioChannels(audioChannels)
          ffFilterN.setSampleFormat(sampleFormat)
          ffFilterN.setAudioInputs(userIdList.size-1)
          ffFilterN.start()
          init(roomId, userIdList, layout, recorder4ts, ffFilterN, drawer, ts4User, tsDiffer, canvasSize)

        case UpdateRecorder(channel, sampleRate, f, width, height, liveId) =>
            log.info(s"$roomId updateRecorder channel:$channel, sampleRate:$sampleRate, frameRate:$f, width:$width, height:$height")
            recorder4ts.setFrameRate(f)
            recorder4ts.setAudioChannels(channel)
            recorder4ts.setSampleRate(sampleRate)
            ffFilter.setAudioChannels(channel)
            ffFilter.setSampleRate(sampleRate)
            recorder4ts.setImageWidth(width)
            recorder4ts.setImageHeight(height)
            init(roomId, userIdList, layout, recorder4ts, ffFilter, drawer,  ts4User, tsDiffer,  (640,  480))

        case NewFrame(userId, frame) =>
          log.info("first")
          if(userId == userIdList(0)){  //todo 自己的画面暂时不做显示
            log.info("in self frame")
            Behaviors.same
          }else{
            val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
            val convertList = userIdList.drop(1).map(id=>new Java2DFrameConverter)
            val frameList = userIdList.drop(1).map(id => Image(id))
            val drawer = ctx.spawn(draw(canvas, canvas.getGraphics, Ts4LastImage(), frameList, recorder4ts,
              convertList, new Java2DFrameConverter, layout, "defaultImg.jpg", roomId, (640, 480)), s"drawer_$roomId")
            ctx.self ! NewFrame(userId, frame)
            work(roomId,userIdList,layout,recorder4ts,ffFilter, drawer,ts4User,tsDiffer,canvasSize)
          }

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
            drawer ! Image4Mix(liveId,frame)
          }
          if (frame.samples != null) {
            try {
              for(i <- 1 until userIdList.length){
                if(userIdList(i) == liveId){
                  ffFilter.pushSamples(i-1, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
                }
              }
              val f = ffFilter.pullSamples().clone()
              if(f!=null){
                recorder4ts.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
              }
            } catch {
              case ex: Exception =>
                log.debug(s"$liveId record sample error system: $ex")
            }

//              if (liveId == host) {
//                ffFilter.pushSamples(0, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
//              } else if (liveId == client) {
//                ffFilter.pushSamples(1, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
//              } else {
//                log.info(s"wrong liveId, couple got wrong audio")
//              }
//              val f = ffFilter.pullSamples().clone()
//              if (f != null) {
//                recorder4ts.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
//              }
          }
          Behaviors.same

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

  def draw(canvas: BufferedImage, graph: Graphics, lastTime: Ts4LastImage, frameList:List[Image],
           recorder4ts: FFmpegFrameRecorder, convertList: List[Java2DFrameConverter],convert:Java2DFrameConverter,
           layout: Int = 0, bgImg: String, roomId: Long, canvasSize: (Int, Int)): Behavior[VideoCommand] = {
    Behaviors.setup[VideoCommand] { ctx =>
      Behaviors.receiveMessage[VideoCommand] {
        case t:Image4Mix =>
//          log.info("get message Image4Mix")
          val time = t.frame.timestamp
          var index = 0
          val size = frameList.length
          convertList.map{
            convert =>
              if(frameList(index).liveId == t.liveId){
                frameList(index).frame = t.frame
              }
              val img = convert.convert(frameList(index).frame)
              graph.drawImage(img, index*canvasSize._1/size, index*canvasSize._2/size, canvasSize._1/size,canvasSize._2/size,null)
              graph.drawString(s"用户${index+1}",index*canvasSize._1/size+50,index*canvasSize._2/size+50)
              index+=1
          }
//        case t: Image4Host =>
//          val time = t.frame.timestamp
//          val img = convert1.convert(t.frame)
//          val clientImg = convert2.convert(clientFrame.frame)
//          //          graph.clearRect(0, 0, canvasSize._1, canvasSize._2)
//          layout match {
//            case 0 =>
//              graph.drawImage(img, 0, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
//              graph.drawString("主播", 24, 24)
//              graph.drawImage(clientImg, canvasSize._1 / 2, canvasSize._2 / 4, canvasSize._1 / 2, canvasSize._2 / 2, null)
//              graph.drawString("观众", 344, 24)
//
//            case 1 =>
//              graph.drawImage(img, 0, 0, canvasSize._1, canvasSize._2, null)
//              graph.drawString("主播", 24, 24)
//              graph.drawImage(clientImg, canvasSize._1 / 4 * 3, 0, canvasSize._1 / 4, canvasSize._2 / 4, null)
//              graph.drawString("观众", 584, 24)
//
//            case 2 =>
//              graph.drawImage(clientImg, 0, 0, canvasSize._1, canvasSize._2, null)
//              graph.drawString("观众", 24, 24)
//              graph.drawImage(img, canvasSize._1 / 4 * 3, 0, canvasSize._1 / 4, canvasSize._2 / 4, null)
//              graph.drawString("主播", 584, 24)
//
//          }
          //          if (addTs) {
          //            val serverTime = ChannelWorker.pullClient.getServerTimestamp()
          //             val ts = TimeUtil.format(serverTime)
          //             graph.drawString(ts, canvasSize._1 - 200, 40)
          //          }
          val frame = convert.convert(canvas)
//          println(frame)
          recorder4ts.record(frame.clone())
//          log.info("recorded")
          //            lastTime.time = time
          Behaviors.same

//        case t: Image4Client =>
//          clientFrame.frame = t.frame
//          Behaviors.same

//        case m@NewRecord4Ts(recorder4ts) =>
//          log.info(s"got msg: $m")
//          draw(canvas, graph, lastTime, clientFrame, recorder4ts, convert1, convert2,convert, layout, bgImg, roomId, canvasSize)
//
//        case Close =>
//          log.info(s"drawer stopped")
//          recorder4ts.releaseUnsafe()
//          Behaviors.stopped
//
//        case t: SetLayout =>
//          log.info(s"got msg: $t")
//          draw(canvas, graph, lastTime, clientFrame, recorder4ts, convert1, convert2,convert, t.layout, bgImg, roomId, canvasSize)
      }
    }
  }

}

package org.seekloud.netMeeting.processor.test

import java.io.{File, FileOutputStream}
import java.nio.channels.Channels
import java.nio.channels.Pipe.SourceChannel
import java.util.concurrent.{ExecutorService, Executors}

import org.seekloud.netMeeting.processor.Boot.executor
import akka.actor.typed.scaladsl.Behaviors
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder}
import org.seekloud.netMeeting.processor.protocol.SharedProtocol.{CloseConnect, NewConnect, NewConnectRsp, SuccessRsp}
import org.seekloud.netMeeting.processor.test.TestThread2.postJsonRequestSend
import org.slf4j.LoggerFactory

import scala.concurrent.Future
import scala.util.{Failure, Success, Try}

/**
  * User: cq
  * Date: 2020/2/12
  */
object TestPullAndPush {
  import io.circe.generic.auto._
  import io.circe.parser.decode
  import io.circe.syntax._

  case class MediaInfo(
                        imageWidth: Int,
                        imageHeight: Int,
                        pixelFormat: Int,
                        frameRate: Double,
                        videoCodec: Int,
                        videoBitrate: Int,
                        audioChannels: Int,
                        audioBitrate: Int,
                        sampleFormat: Int,
                        sampleRate: Int
                      )

  val FilePath1 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/trailer.mkv"
  val FilePath2 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/mov_bbb.mp4"
  val FilePath3 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/big_buck_bunny.mp4"

  val OutPath1 = "rtmp://47.92.170.2:42069/live/10001"
  val OutPath2 = "rtmp://47.92.170.2:42069/live/10002"
  val OutPath3 = "rtmp://47.92.170.2:42069/live/10003"
  val FileImageOutPath = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/image"
  val FileOutPath1 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/out1.flv"
  val FileOutPath2 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/out2.flv"
  val FileOutPath3 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/out3.flv"
  var audioChannels = 2 //todo 待议
  var frameRate = 30
  val bitRate = 2000000

  class PushPipeThread(filePath:String,outPath:String) extends Runnable {
    override def run(): Unit ={
      println("start thread")
      val grabber = new FFmpegFrameGrabber(filePath)
  //    grabber.setFormat("mkv")
      println(s"grabber = $grabber")
      try {
        grabber.start()
      } catch {
        case e: Exception =>
          println(e)
          println(s"exception occured in grabber start")
      }
      println("grabber started")
      val ffLength = grabber.getLengthInFrames()
      println(s"length = $ffLength")
      val recorder = new FFmpegFrameRecorder(outPath,640,360,audioChannels)
      recorder.setFrameRate(frameRate)
      recorder.setVideoBitrate(bitRate)
      recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
      recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
      recorder.setMaxBFrames(0)
      recorder.setFormat("flv")
      try {
        recorder.startUnsafe()
      } catch {
        case e: Exception =>
          println(s" recorder meet error when start:$e")
      }
      var i = 0
      while (i<ffLength){
        val frame = grabber.grab()
        if(frame != null){
//          println(frame)
          recorder.record(frame)
        }
        i+=1
      }
      println("push over")
    }
  }

  class PullPipeThread() extends Runnable{
    override def run(): Unit = {
      println("start")
      val grabber = new FFmpegFrameGrabber(OutPath1)
      Try(grabber.start()) match {
        case Success(_) =>
          val i = MediaInfo(
            grabber.getImageWidth,
            grabber.getImageHeight,
            grabber.getPixelFormat,
            grabber.getFrameRate,
            grabber.getVideoCodec,
            grabber.getVideoBitrate,
            grabber.getAudioChannels,
            grabber.getAudioBitrate,
            grabber.getSampleFormat,
            grabber.getSampleRate
          )
          println("start success grab")
          println(i)
          val outputStream = new FileOutputStream(new File(FileOutPath1))
          val recorder = new FFmpegFrameRecorder(outputStream,640,480,audioChannels)
          recorder.setFrameRate(frameRate)
          recorder.setVideoBitrate(bitRate)
          recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
          recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)

          recorder.setMaxBFrames(0)
          recorder.setFormat("flv")
          try{
            recorder.startUnsafe()
          }catch{
            case e:Exception =>
              println(s"recorder meet error when start:$e")
          }
          var frame = grabber.grab()
          while (frame != null){
            println(frame)
            recorder.record(frame)
            frame = grabber.grab()
          }

          val ffLength = grabber.getLengthInFrames()
          println(s"length = $ffLength")

        case Failure(e) =>
          println(s"grabber start failed: ${e.getMessage}")
          Behaviors.stopped
      }
    }
  }

//  val processorBaseUrl = "http://127.0.0.1:42068/netMeeting/processor"
  val processorBaseUrl = "http://47.92.170.2:42068/netMeeting/processor"
//  val processorBaseUrl = "http://10.1.29.247:42068/netMeeting/processor"
  private val log = LoggerFactory.getLogger(this.getClass)

  def newConnect(roomId:Long,userIdList:List[String],liveCode:String,layout:Int):Future[Either[String,NewConnectRsp]] = {
    val url = processorBaseUrl + "/newConnect"
    val jsonString = NewConnect(roomId,userIdList,liveCode, layout).asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000,needLogRsp = false).map{
      case Right(v) =>
        decode[NewConnectRsp](v) match {
          case Right(data) =>
            log.info("get data")
            Right(data)
          case Left(e) =>
            log.error(s"connectRoom error:$e")
            Left("error")
        }
      case Left(error) =>
        log.error(s"connectRoom postJsonRequestSend error:$error")
        Left("Error")
    }
  }

  def stop(roomId:Long):Future[Either[String,SuccessRsp]] = {
    val url = processorBaseUrl + "/closeConnect"
    val jsonString = CloseConnect(roomId).asJson.noSpaces
    postJsonRequestSend("post",url,List(),jsonString,timeOut = 60 * 1000,needLogRsp = false).map{
      case Right(v) =>
        decode[SuccessRsp](v) match {
          case Right(data) =>
            log.info("get data")
            Right(data)
          case Left(e) =>
            log.error(s"connectRoom error:$e")
            Left("error")
        }
      case Left(error) =>
        log.error(s"connectRoom postJsonRequestSend error:$error")
        Left("Error")
    }
  }

  def main(args: Array[String]): Unit = {
    val threadPool:ExecutorService = Executors.newFixedThreadPool(60)
    try{
//      threadPool.execute(new PushPipeThread(FilePath1,OutPath1))
//      threadPool.execute(new PushPipeThread(FilePath1,OutPath2))
//      threadPool.execute(new PushPipeThread(FilePath3,OutPath3))
//      Thread.sleep(3000)
      newConnect(11111,List("10001","10002","10003"),"",1)
//        stop(10001)
//      threadPool.execute(new PullPipeThread())
    }finally {
      threadPool.shutdown()
    }
  }
}

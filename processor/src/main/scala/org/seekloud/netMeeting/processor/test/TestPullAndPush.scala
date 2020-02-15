package org.seekloud.netMeeting.processor.test

import java.io.{File, FileOutputStream}
import java.nio.channels.Channels
import java.nio.channels.Pipe.SourceChannel
import java.util.concurrent.{ExecutorService, Executors}

import akka.actor.typed.scaladsl.Behaviors
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder}

import scala.util.{Failure, Success, Try}

/**
  * User: cq
  * Date: 2020/2/12
  */
object TestPullAndPush {
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

  val FilePath = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/trailer.mkv"
  val OutPath = "rtmp://10.1.29.247/live/10001"
  val FileOutPath = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo/out.mkv"
  var audioChannels = 2 //todo 待议
  var frameRate = 30
  val bitRate = 2000000
  var pushover = false

  class PushPipeThread() extends Runnable {
    override def run(): Unit ={
      println("start thread")
      val grabber = new FFmpegFrameGrabber(FilePath)
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
      val recorder = new FFmpegFrameRecorder(OutPath,640,480,audioChannels)
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
      pushover = true
    }
  }

  class PullPipeThread() extends Runnable{
    override def run(): Unit = {
      println("start")
      val grabber = new FFmpegFrameGrabber(OutPath)
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
          val outputStream = new FileOutputStream(new File(FileOutPath))
          val recorder = new FFmpegFrameRecorder(outputStream,640,480,audioChannels)
          recorder.setFrameRate(frameRate)
          recorder.setVideoBitrate(bitRate)
          recorder.setVideoCodec(avcodec.AV_CODEC_ID_MPEG2VIDEO)
          recorder.setAudioCodec(avcodec.AV_CODEC_ID_MP2)
          recorder.setMaxBFrames(0)
          recorder.setFormat("mpegts")
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

  def main(args: Array[String]): Unit = {
    val threadPool:ExecutorService = Executors.newFixedThreadPool(60)
    try{
      Thread.sleep(3000)
      threadPool.execute(new PushPipeThread())
      Thread.sleep(3000)
      threadPool.execute(new PullPipeThread())
    }finally {
      threadPool.shutdown()
    }
  }
}

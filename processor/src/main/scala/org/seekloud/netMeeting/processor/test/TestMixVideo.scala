package org.seekloud.netMeeting.processor.test

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder}
import org.seekloud.netMeeting.processor.test.TestPullAndPush.{audioChannels, bitRate, frameRate}

/**
  * User: cq
  * Date: 2020/2/16
  */
object TestMixVideo {
  class MixVideo(filePath:String,outPath:String) extends Runnable {
    override def run(): Unit = {
      val grabber = new FFmpegFrameGrabber(filePath)
      try{
        grabber.start()
      } catch {
        case e:Exception =>
          println(e)
      }
      println("grabber start")
      val ffLength = grabber.getLengthInFrames()
      println(s"length = $ffLength")
      val recorder = new FFmpegFrameRecorder(outPath,640,480,audioChannels)
      recorder.setFrameRate(frameRate)
      recorder.setVideoBitrate(bitRate)
      recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
      recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
      recorder.setMaxBFrames(0)
      recorder.setFormat("flv")
      try{
        recorder.startUnsafe()
      }catch {
        case e:Exception =>
          println(s" recorder meet error when start:$e")
      }
      var i = 0
      while (i<ffLength){
        val frame = grabber.grab()
        if(frame != null){
          if(frame.samples != null){
//            ffFilter.pushSamples(0, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
//            val f = ffFilter.pullSamples().clone()
//            if(f!=null){
//              recorder4ts.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
//            }
          }
        }
      }
    }
  }

  def main(args: Array[String]): Unit = {

  }

}

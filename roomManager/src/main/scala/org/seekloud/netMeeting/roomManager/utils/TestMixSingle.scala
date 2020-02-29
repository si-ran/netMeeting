package org.seekloud.netMeeting.roomManager.utils

import java.awt.Graphics
import java.awt.image.BufferedImage

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameFilter, FFmpegFrameGrabber, FFmpegFrameRecorder, Frame, Java2DFrameConverter}
import org.seekloud.netMeeting.roomManager.Boot.{executor, scheduler}

/**
  * User: si-ran
  * Date: 2020/2/28
  * Time: 18:28
  */
class TestMixSingle() {

  // setting
  val urlHeader = "rtmp://47.92.170.2:42069/live/"

  var flag = true

  var canvas: BufferedImage = _
  var graph: Graphics = _
  val bigImg = new Java2DFrameConverter()
  val user1Img = new Java2DFrameConverter()

  var grabbers: List[FFmpegFrameGrabber] = Nil
  var recorder: FFmpegFrameRecorder = _
  var ffFilter: Option[FFmpegFrameFilter] = _

  def recordStart(userList: List[Long], roomId: Long) ={
    println("start")
    flag = true

    canvas = new BufferedImage(480 * userList.length, 360, BufferedImage.TYPE_3BYTE_BGR)
    graph = canvas.getGraphics()

    userList.length match {
      case 1 => ffFilter = None
      case 2 => ffFilter = Some(new FFmpegFrameFilter("[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]", 2))
      case 3 => ffFilter = Some(new FFmpegFrameFilter("[0:a][1:a][2:a] amix=inputs=3:duration=longest:dropout_transition=3:weights=1 1[a]", 2))
      case _ => ffFilter = None
    }

    grabbers = userList.map{ id =>
      new FFmpegFrameGrabber(urlHeader + id)
    }
    grabbers.foreach{ grabber =>
      grabber.setOption("rw_timeout", "20000000")
      grabber.start()
    }

    val width = grabbers.head.getImageWidth
    val height = grabbers.head.getImageHeight
    val audioChannels = grabbers.head.getAudioChannels
    val frameRate = grabbers.head.getFrameRate

    recorder = new FFmpegFrameRecorder(urlHeader + roomId + "_t", 640, 360)
    recorder.setImageWidth(480 * userList.length)
    recorder.setImageHeight(height)
    recorder.setAudioChannels(audioChannels)
    recorder.setVideoOption("tune", "zerolatency")
    recorder.setVideoOption("preset", "ultrafast")
    recorder.setVideoOption("crf", "23")
    recorder.setFormat("flv")
    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.setFrameRate(frameRate)
    recorder.start()
    ffFilter.foreach(_.setAudioChannels(audioChannels))
    ffFilter.foreach(_.setSampleFormat(1))
    ffFilter.foreach(_.setAudioInputs(userList.length))
    ffFilter.foreach(_.start())
    while(flag){
      val frames = grabbers.map(_.grabFrame())

      var frameIndex = 0
      frames.foreach{ frame =>
        if(frame != null){
          val img = user1Img.convert(frame)
          graph.drawImage(img, frameIndex * 480, 0, 480, height, null)
        }
        frameIndex += 1
      }
      if(frames.exists(_ != null)){
        recorder.record(bigImg.convert(canvas))
      }

      var frameSampleIndex = 0
      frames.foreach{ frame =>
        if(frame.samples != null){
          ffFilter.foreach(_.pushSamples(frameSampleIndex, frame.audioChannels, frame.sampleRate, ffFilter.get.getSampleFormat, frame.samples: _*))
        }
        frameSampleIndex += 1
      }
      if(frames.exists(_.samples != null)){
        val f = if(ffFilter.isEmpty) frames.head else ffFilter.get.pullSamples()
        if(f != null){
          recorder.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
        }
      }
    }
    println("stop")
    grabbers.foreach(_.stop())
    recorder.stop()
    grabbers = Nil
  }

  def recordStop() ={
    flag = false
  }

//  def main1(args: Array[String]): Unit = {
//    recordStart(List(10011, 10011, 10011))
//    recordStart(List(10011, 10011), 10011)
//    recordStart(List(10011))
//  }
}

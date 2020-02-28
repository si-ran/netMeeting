package org.seekloud.netMeeting.roomManager.utils

import java.awt.image.BufferedImage

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameFilter, FFmpegFrameGrabber, FFmpegFrameRecorder, Frame, Java2DFrameConverter}
import org.seekloud.netMeeting.roomManager.Boot.{executor, scheduler}

/**
  * User: si-ran
  * Date: 2020/2/28
  * Time: 18:28
  */
object TestMix {

  val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
  val graph = canvas.getGraphics()
  val bigImg = new Java2DFrameConverter()
  val user1Img = new Java2DFrameConverter()

  val grabber = new FFmpegFrameGrabber("rtmp://47.92.170.2:42069/live/10011")
  val recorder = new FFmpegFrameRecorder("rtmp://47.92.170.2:42069/live/10011_t", 1280, 360)
  val ffFilter = new FFmpegFrameFilter("[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]", 2)

  def recordStart() ={
    println("start")
    grabber.setOption("rw_timeout", "20000000")
    grabber.start()
    recorder.setImageWidth(grabber.getImageWidth)
    recorder.setImageHeight(grabber.getImageHeight)
    recorder.setAudioChannels(grabber.getAudioChannels)
    recorder.setVideoOption("tune", "zerolatency")
    recorder.setVideoOption("preset", "ultrafast")
    recorder.setVideoOption("crf", "23")
    recorder.setFormat("flv")
    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.setFrameRate(grabber.getFrameRate)
    recorder.start()
    ffFilter.setAudioChannels(grabber.getAudioChannels)
    ffFilter.setSampleFormat(1)
    ffFilter.setAudioInputs(2)
    ffFilter.start()
    while(true){
      val frame = grabber.grabFrame()
      if(null != frame) {
        val img = user1Img.convert(frame)
        graph.drawImage(img, 0, 0, 320, 180, null)
        graph.drawImage(img, 320, 0, 320, 180, null)
        recorder.record(bigImg.convert(canvas))
      }
      if(frame.samples != null) {
        ffFilter.pushSamples(0, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
        ffFilter.pushSamples(1, frame.audioChannels, frame.sampleRate, ffFilter.getSampleFormat, frame.samples: _*)
        val f = ffFilter.pullSamples()
        if(f != null){
          recorder.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
        }
      }
    }
    println("stop")
    grabber.stop()
    recorder.stop()
  }

  def main(args: Array[String]): Unit = {
    recordStart()
  }
}

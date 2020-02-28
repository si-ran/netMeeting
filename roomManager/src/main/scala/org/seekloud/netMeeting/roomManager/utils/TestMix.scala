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

  val grabber1 = new FFmpegFrameGrabber("rtmp://47.92.170.2:42069/live/10011")
  val grabber2 = new FFmpegFrameGrabber("rtmp://47.92.170.2:42069/live/10012")
  val recorder = new FFmpegFrameRecorder("rtmp://47.92.170.2:42069/live/10011_t", 1280, 360)
  val ffFilter = new FFmpegFrameFilter("[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]", 2)

  def recordStart() ={
    println("start")
    grabber1.setOption("rw_timeout", "20000000")
    grabber2.setOption("rw_timeout", "20000000")
    grabber1.start()
    grabber2.start()
    recorder.setImageWidth(grabber1.getImageWidth)
    recorder.setImageHeight(grabber1.getImageHeight)
    recorder.setAudioChannels(grabber1.getAudioChannels)
    recorder.setVideoOption("tune", "zerolatency")
    recorder.setVideoOption("preset", "ultrafast")
    recorder.setVideoOption("crf", "23")
    recorder.setFormat("flv")
    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.setFrameRate(grabber1.getFrameRate)
    recorder.start()
    ffFilter.setAudioChannels(grabber1.getAudioChannels)
    ffFilter.setSampleFormat(1)
    ffFilter.setAudioInputs(2)
    ffFilter.start()
    while(true){
      val frame1 = grabber1.grabFrame()
      val frame2 = grabber2.grabFrame()
      if(null != frame1) {
        val img = user1Img.convert(frame1)
        graph.drawImage(img, 0, 0, 320, 180, null)
      }
      if(null != frame2) {
        val img = user1Img.convert(frame2)
        graph.drawImage(img, 320, 0, 320, 180, null)
      }
      if(null != frame1 || null != frame2){
        recorder.record(bigImg.convert(canvas))
      }

      if(frame1.samples != null) {
        ffFilter.pushSamples(0, frame1.audioChannels, frame1.sampleRate, ffFilter.getSampleFormat, frame1.samples: _*)
      }
      if(frame2.samples != null) {
        ffFilter.pushSamples(1, frame2.audioChannels, frame2.sampleRate, ffFilter.getSampleFormat, frame2.samples: _*)
      }
      if(frame1.samples != null || frame2.samples != null) {
        val f = ffFilter.pullSamples()
        if(f != null){
          recorder.recordSamples(f.sampleRate, f.audioChannels, f.samples: _*)
        }
      }
    }
    println("stop")
    grabber1.stop()
    grabber2.stop()
    recorder.stop()
  }

  def main(args: Array[String]): Unit = {
    recordStart()
  }
}

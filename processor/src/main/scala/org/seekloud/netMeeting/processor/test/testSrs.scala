package org.seekloud.netMeeting.processor.test

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameRecorder, OpenCVFrameGrabber}


object testSrs {

  def main(args: Array[String]): Unit = {
    val width = 640
    val height = 360
    val grabber = new OpenCVFrameGrabber(0)
    grabber.setImageWidth(width)
    grabber.setImageHeight(height)
    grabber.start()
    println(s"=====grabber started=====")
    val frameRate = grabber.getFrameRate

//    val url = "rtmp://10.1.29.247:42069/live/12345"
    val url = "rtmp://47.92.170.2:42069/live/10003"

    val recorder = new FFmpegFrameRecorder(url, width, height)
    recorder.setVideoOption("tune", "zerolatency")
    recorder.setVideoOption("preset", "ultrafast")
    recorder.setVideoOption("crf", "23")
    recorder.setFormat("flv")
    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.setFrameRate(frameRate)
    recorder.start()

    println(s"=====recorder started=====")
    val canvasFrame = new CanvasFrame("camera")

    while(true) {
      val frame = grabber.grab()
      if(null != frame && null != frame.image) {
        recorder.record(frame)
        canvasFrame.showImage(frame)
      }
    }
  }



}

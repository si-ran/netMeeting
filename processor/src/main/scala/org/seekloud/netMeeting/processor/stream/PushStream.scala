package org.seekloud.netMeeting.processor.stream

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameRecorder, OpenCVFrameGrabber}

/**
  * @user: wanruolong
  * @date: 2020/2/4 9:52
  *
  */
object PushStream {
  var startTime: Long = 0

  def main(args: Array[String]): Unit = {
    val width = 640
    val height = 360
    val grabber = new OpenCVFrameGrabber(0)
    grabber.setImageWidth(width)
    grabber.setImageHeight(height)
    grabber.start()
    val chatNum = 4
    var recorderList = List[FFmpegFrameRecorder]()
    val urlList = List[String]("rtmp://10.1.29.247/live/liveStream1", "rtmp://10.1.29.247/live/liveStream2", "rtmp://10.1.29.247/live/liveStream3", "rtmp://10.1.29.247/live/liveStream4")

    (0 until chatNum).foreach{i =>
      val recorder = new FFmpegFrameRecorder(urlList(i), width, height)
      recorder.setFrameRate(30)
      recorder.setFormat("flv")
      recorder.setVideoBitrate(2000000)
      recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
      recorder.start()
      recorderList = recorder::recorderList
    }


//
//    val url = "rtmp://10.1.29.247/live/liveStream"
//    val recorder = new FFmpegFrameRecorder(url, width, height)
//    recorder.setFrameRate(30)
//    recorder.setFormat("flv")
//    recorder.setVideoBitrate(2000000)
//    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
//    recorder.start()

    while (true){
      val frame = grabber.grab()
      recorderList.foreach{recorder =>
        startTime = if (startTime == 0) System.currentTimeMillis() else startTime
//        recorder.setTimestamp((System.currentTimeMillis() - startTime) * 1000)
        recorder.record(frame.clone())
      }
    }
  }

}

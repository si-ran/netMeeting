package org.seekloud.netMeeting.processor

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.OpenCVFrameConverter.ToMat
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder, Frame, OpenCVFrameConverter}
import org.bytedeco.opencv.opencv_core.{Mat, Rect, Scalar, Size}
import org.bytedeco.opencv.global.{opencv_imgproc => OpenCVProc}
import org.bytedeco.opencv.global.{opencv_core => OpenCVCore}

/**
  * @user: wanruolong
  * @date: 2020/2/4 16:22
  *
  */
object Boot {

  val urlList = List[String]("rtmp://10.1.29.247/live/liveStream1", "rtmp://10.1.29.247/live/liveStream2", "rtmp://10.1.29.247/live/liveStream3", "rtmp://10.1.29.247/live/liveStream4")

  val url = "rtmp://10.1.29.247/live/combined"

  var grabberList = List[FFmpegFrameGrabber]()

  val chatNum = 4

  var frame1: Frame = _
  var frame2: Frame = _
  var frame3: Frame = _
  var frame4: Frame = _
//  var frameList = List[Frame](frame1, frame2, frame3, frame4)


  //图片拼接
  var width = 640
  var height = 360
  val bottomSize = new Size(width, height)
  val topSize = new Size(width/2, height/2)
  val toMat = new ToMat()
  val resizeMat = new Mat()
  val canvas = new Mat(bottomSize, OpenCVCore.CV_8UC3, new Scalar(0, 0, 0, 0))
  val converter = new OpenCVFrameConverter.ToIplImage()
  val position = List[(Int, Int)]((0, 0), (width/2, 0), (0, height/2), (width/2, height/2))

  def imgCombination( frameList: List[Frame]) = {
    (0 until frameList.length).foreach{ i =>
      val layMat = toMat.convert(frameList(i))
      OpenCVProc.resize(layMat, resizeMat, topSize)
      val layMask = new Mat(topSize, OpenCVCore.CV_8UC1, new Scalar(1d))
      val layRoi = canvas(new Rect(position(i)._1, position(i)._2, topSize.width(), topSize.height()))
      resizeMat.copyTo(layRoi, layMask)
    }
    val convertFrame = converter.convert(canvas)
    convertFrame
  }

  def main(args: Array[String]): Unit = {
//    val frameQueue = new LinkedBlockingDeque[Frame](4)

    (0 until chatNum).foreach{i =>
      val grabber = new FFmpegFrameGrabber(urlList(i))
      grabber.start()
      grabberList = grabber::grabberList
    }

    val recorder = new FFmpegFrameRecorder(url, width, height)
    recorder.setFrameRate(30)
    recorder.setFormat("flv")
    recorder.setVideoBitrate(2000000)
    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.start()

    while(true) {
      (0 until chatNum).foreach{i =>
        val frame = grabberList(i).grab()
        i match {
          case 0 => frame1 = frame
          case 1 => frame2 = frame
          case 2 => frame3 = frame
          case 3 => frame4 = frame
        }
      }
      var frameList = List[Frame](frame1, frame2, frame3, frame4)
//      recorder.setTimestamp(frame1.timestamp)
      recorder.record(imgCombination(frameList))
    }
  }

}

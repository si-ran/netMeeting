package org.seekloud.netMeeting.processor.stream

import java.util.concurrent.{LinkedBlockingDeque, ScheduledThreadPoolExecutor, TimeUnit}

import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameRecorder, Frame, OpenCVFrameGrabber}

/**
  * @user: wanruolong
  * @date: 2020/2/4 9:52
  *
  */
object PushStream {
  @volatile var startTime: Long = 0

  val frameQueue = new LinkedBlockingDeque[Frame](1)
  val urlList = List[String]("rtmp://10.1.29.247/live/liveStream1", "rtmp://10.1.29.247/live/liveStream2", "rtmp://10.1.29.247/live/liveStream3", "rtmp://10.1.29.247/live/liveStream4")
  val width = 640
  val height = 360

  def main(args: Array[String]): Unit = {

    val grabber = new OpenCVFrameGrabber(0)
    grabber.setImageWidth(width)
    grabber.setImageHeight(height)
    grabber.start()
    val chatNum = 4
    var recorderList = List[FFmpegFrameRecorder]()

    (0 until chatNum).foreach{i =>
//      val recorder = new FFmpegFrameRecorder(urlList(i), width, height)
//      recorder.setFrameRate(30)
//      recorder.setFormat("flv")
//      recorder.setVideoBitrate(2000000)
//      recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
//      recorder.start()
//      recorderList = recorder::recorderList
      val thread = getRecordThread(i)
      thread.start()
    }
    val canvasFrame = new CanvasFrame("camera")

    println("grab start success.")
    while (true){
      val frame = grabber.grab()
      frameQueue.clear()
      frameQueue.offer(frame)
      canvasFrame.showImage(frame)
/*      recorderList.foreach{recorder =>
        startTime = if (startTime == 0) System.currentTimeMillis() else startTime
//        recorder.setTimestamp((System.currentTimeMillis() - startTime) * 1000)
        recorder.record(frame.clone())
      }*/
    }
  }

  def getRecordThread(i: Int): Thread = new Thread(){
    var timeStamp = 0L
    override def run(): Unit = {
      println(s"recorder${i} thread starting.")
      val recorder = new FFmpegFrameRecorder(urlList(i), width, height)
      recorder.setFrameRate(30)
      recorder.setFormat("flv")
      recorder.setVideoBitrate(2000000)
      recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
      recorder.start()

      while (true){
        val frame = frameQueue.peek()
        if(frame != null && frame.image != null){
//          recorder.setTimestamp(timeStamp * 1000)
          recorder.record(frame.clone())
          timeStamp += 33
        }
        Thread.sleep(33)
      }
//      val frameExcutor = new ScheduledThreadPoolExecutor(1)
//      val f = frameExcutor.scheduleAtFixedRate(getRunnable(recorder), 10, 33, TimeUnit.MILLISECONDS)
    }

/*    def getRunnable(recorder: FFmpegFrameRecorder): Runnable = new Runnable {
      override def run(): Unit = {
        val frame = frameQueue.peek()
        if(frame != null && frame.image != null){
          recorder.setTimestamp(timeStamp)
          recorder.record(frame)
          timeStamp += 33
        }
      }
    }*/
  }

}

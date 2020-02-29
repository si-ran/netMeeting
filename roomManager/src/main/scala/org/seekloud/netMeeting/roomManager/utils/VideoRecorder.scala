package org.seekloud.netMeeting.roomManager.utils

import akka.actor.Cancellable
import akka.actor.typed.ActorRef
import akka.pattern.after
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder}
import org.seekloud.netMeeting.roomManager.Boot.{executor, scheduler}
import org.seekloud.netMeeting.roomManager.core.UserActor

import scala.concurrent.Future
import scala.concurrent.duration._
import scala.concurrent._
import scala.util.{Failure, Success}

/**
  * User: si-ran
  * Date: 2020/2/23
  * Time: 18:23
  */
class VideoRecorder(roomId: Long, pullUrl: String) {

  private val outUrl = s"../video/video_${roomId}_${System.currentTimeMillis() / 1000}.mp4"
  private var flag = true //注意只在recordStop中修改
  private var grabber: FFmpegFrameGrabber = _
  private var recorder: FFmpegFrameRecorder = _

  private def recordFrame(): Unit ={

    val first = Future{
      val frame = grabber.grabFrame()

      if(null != frame) {
        recorder.record(frame)
      }
    }

    val timer = after(10000.millis, scheduler){Future.failed(new Exception("time out"))}

    Future.firstCompletedOf(Seq(first, timer)).onComplete{
      case Success(_) =>
        if(flag) {
          recordFrame()
        } else {
          println("stop")
          grabber.stop()
          recorder.stop()
        }
      case Failure(e) =>
        println(e)
        Await.result(first, 1.seconds)
    }
//    val timer = after(10000.millis, scheduler){Future.failed(new Exception())}
//    val timer = scheduler.scheduleOnce(10000.millis){throw new Exception}
  }

  def recordStart() ={
    println("start")
    flag = true
    grabber = new FFmpegFrameGrabber(pullUrl)
    grabber.setOption("rw_timeout", "20000000")
    recorder = new FFmpegFrameRecorder(outUrl, 640, 360)
    grabber.start()
    recorder.setImageWidth(grabber.getImageWidth / 2)
    recorder.setImageHeight(grabber.getImageHeight / 2)
    recorder.setAudioChannels(grabber.getAudioChannels)
    recorder.start()
    while(flag){
      val frame = grabber.grabFrame()
      if(null != frame) {
        recorder.record(frame)
      }
      if(frame.samples != null) {
        if (frame != null) {
          recorder.recordSamples(frame.sampleRate, frame.audioChannels, frame.samples: _*)
        }
      }

    }
//    recordFrame()
    println("stop")
    grabber.stop()
    recorder.stop()
  }

  def recordStop() ={
    flag = false
  }

}

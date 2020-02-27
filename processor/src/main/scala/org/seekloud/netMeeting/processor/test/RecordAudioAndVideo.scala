package org.seekloud.netMeeting.processor.test

import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import javafx.scene.image.PixelFormat
import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, TargetDataLine}
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv.{FFmpegFrameGrabber, FFmpegFrameRecorder, FrameGrabber, FrameRecorder, OpenCVFrameGrabber}

/**
  * Author: Tao Zhang
  * Date: 8/16/2019
  * Time: 6:29 PM
  */
object RecordAudioAndVideo {


  def main(args: Array[String]): Unit = {

    //val outputFile = "data/out/record_av_" + System.currentTimeMillis() + ".ts"
//    val outputFile = "data/out/record_av_" + System.currentTimeMillis() + ".flv"
//    val outputFile = "F:/obs/" + System.currentTimeMillis() + ".flv"
//    val outputFile = "rtmp://media.seekloud.com:62040/live/1234567?rtmpToken=9KTRwdF6CDcLJUElfv8aA2zfoGyb9sUG4WfDeZ6H&userId=100136"
//    val outputFile = "rtmp://txy.live-send.acg.tv/live-txy/?streamname=live_44829093_50571972&key=faf3125e8c84c88ad7f05e4fcc017149"
    val outputFile = "rtmp://47.92.170.2:42069/live/12345"


    val audioChannels = 2

    val videoGrabber = new OpenCVFrameGrabber(0)
    videoGrabber.setImageWidth(640)
    videoGrabber.setImageHeight(360)
    videoGrabber.start()


    val w = videoGrabber.getImageWidth
    val h = videoGrabber.getImageHeight
    val frameRate = videoGrabber.getFrameRate

    println(s"grabber w: $w")
    println(s"grabber h: $h")
    println(s"grabber fr: $frameRate")
    println(s"grabber pixel: ${videoGrabber.getPixelFormat}")


    val recorder = new FFmpegFrameRecorder(outputFile, w, h, audioChannels)

    //recorder.setFormat("mpegts")
    recorder.setFormat("flv")
    recorder.setVideoCodec(avcodec.AV_CODEC_ID_H264)
    recorder.setFrameRate(30)
//    recorder.setVideoOption("preset", "veryfast")
    recorder.setVideoOption("tune", "zerolatency")
//    recorder.setVideoOption("profile", "baseline")
//    recorder.setVideoOption("crf", "18")





    // 不可变(固定)音频比特率
    recorder.setAudioOption("crf", "0")
    // 最高质量
    recorder.setAudioQuality(0)
    // 音频比特率
    recorder.setAudioBitrate(192000)
    // 音频采样率
    recorder.setSampleRate(44100)
    // 双通道(立体声)
    recorder.setAudioChannels(2)
    // 音频编/解码器
    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)

    val audioExecutor = new ScheduledThreadPoolExecutor(1)
    val videoThread = getVideoGrabThread(videoGrabber, recorder)

    recorder.start()

    val f =
      audioExecutor.scheduleAtFixedRate(
        getAudioGrabRunnable(audioChannels, recorder),
        10,
        23,
        TimeUnit.MILLISECONDS)

    videoThread.start()

    println("WAITING 1500 s.")
    Thread.sleep(1500 * 1000)


    (0 until 120).foreach{ i =>

      println(s"+++++++++++++++++++++++++ STOPPING $i ++++++++++++++++++++++++++")
      Thread.sleep(1000)
    }


    f.cancel(false)
    videoThread.interrupt()

    Thread.sleep(1000)
    audioExecutor.shutdown()
    recorder.close()
    videoGrabber.close()
    println("DONE.")
  }


  def getVideoGrabThread(grab: FrameGrabber, recorder: FFmpegFrameRecorder): Thread = new Thread {

    override def run(): Unit = {

      val recordBeginTime: Long = System.nanoTime()

      var frame = grab.grab()
      val pixelFormat = grab.getPixelFormat
      while (!isInterrupted && frame != null) {
        if (frame.image != null) {
          val timestamp = (System.nanoTime() - recordBeginTime) / 1000
          recorder.setTimestamp(timestamp)
          recorder.record(frame)
        }
//        val t1 = System.currentTimeMillis()
        frame = grab.grab()
//        val t2 = System.currentTimeMillis()
//        println(s"grab picture time: ${t2 - t1} ms")
      }

      println("VideoGrabThread finished.")
    }

  }


  def getAudioGrabRunnable(audioChannels: Int, recorder: FFmpegFrameRecorder): Runnable = new Runnable {

    val audioIndex = 0
    /**
      * 设置音频编码器 最好是系统支持的格式，否则getLine() 会发生错误
      * 采样率:44.1k;采样率位数:16位;立体声(stereo);是否签名;true:
      * big-endian字节顺序,false:little-endian字节顺序(详见:ByteOrder类)
      */
    val audioFormat = new AudioFormat(44100.0F, 16, audioChannels, true, false)

    // 通过AudioSystem获取本地音频混合器信息
    //val minfoSet = AudioSystem.getMixerInfo
    // 通过AudioSystem获取本地音频混合器
    //val mixer = AudioSystem.getMixer(minfoSet(audioIndex))
    // 通过设置好的音频编解码器获取数据线信息
    val dataLineInfo = new DataLine.Info(classOf[TargetDataLine], audioFormat)


    val line = AudioSystem.getLine(dataLineInfo).asInstanceOf[TargetDataLine]
    line.open(audioFormat)
    line.start()

    // 获得当前音频采样率// 获得当前音频采样率
    val sampleRate = audioFormat.getSampleRate.asInstanceOf[Int]
    // 获取当前音频通道数量
    val numChannels = audioFormat.getChannels
    // 初始化音频缓冲区(size是音频采样率*通道数)
    val audioBufferSize = sampleRate * numChannels

    val audioBytes = new Array[Byte](audioBufferSize)


    var crabCount = 0

    override def run(): Unit = {
      import java.nio.{ByteBuffer, ByteOrder, ShortBuffer}
      val runTime = System.nanoTime()
      crabCount += 1
      try { // 非阻塞方式读取
        val nBytesRead = line.read(audioBytes, 0, line.available)
        // 因为我们设置的是16位音频格式,所以需要将byte[]转成short[]
        val nSamplesRead = nBytesRead / 2
        val samples = new Array[Short](nSamplesRead)

        /**
          * ByteBuffer.wrap(audioBytes)-将byte[]数组包装到缓冲区
          * ByteBuffer.order(ByteOrder)-按little-endian修改字节顺序，解码器定义的
          * ByteBuffer.asShortBuffer()-创建一个新的short[]缓冲区
          * ShortBuffer.get(samples)-将缓冲区里short数据传输到short[]
          */
        ByteBuffer.wrap(audioBytes).order(ByteOrder.LITTLE_ENDIAN).asShortBuffer.get(samples)
        // 将short[]包装到ShortBuffer
        val sBuff = ShortBuffer.wrap(samples, 0, nSamplesRead)
        // 按通道录制shortBuffer
        recorder.recordSamples(sampleRate, numChannels, sBuff)
      } catch {
        case e: FrameRecorder.Exception =>
          e.printStackTrace()
      }
    }
  }


}
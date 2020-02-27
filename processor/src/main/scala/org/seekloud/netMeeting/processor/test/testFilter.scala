package org.seekloud.netMeeting.processor.test

import java.nio.{ByteBuffer, ByteOrder, ShortBuffer}
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, TargetDataLine}

import scala.collection.mutable
import javax.swing.JFrame
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv._
import org.slf4j.LoggerFactory
import org.bytedeco.ffmpeg.global.avutil

import scala.collection.mutable.ListBuffer
import scala.io.StdIn

object testFilter {

  private val log = LoggerFactory.getLogger(this.getClass)

  def main(args: Array[String]): Unit = {
    val peopleNum = 2
    val audioChannels = 1
    val sampleFormat = 1
    val complexFilter = s"amix=inputs=$peopleNum:duration=longest:dropout_transition=3"
    val sampleRate = 44100
    //音频混流
    var filterStr = complexFilter
    (1 to peopleNum).reverse.foreach { i =>
      filterStr = s"[${i - 1}:a]" + filterStr
    }
    filterStr += "[a]"
//    log.debug(s"audio filter: $filterStr")
    println(s"audio filter: $filterStr")

    val ffFilter = new FFmpegFrameFilter(
      filterStr,
      audioChannels
    )
    ffFilter.setAudioChannels(audioChannels)
    ffFilter.setSampleFormat(sampleFormat)
    ffFilter.setAudioInputs(peopleNum)
    ffFilter.start()
    val buffer1 = ShortBuffer.allocate(1000)
    val buffer2 = ShortBuffer.allocate(500)
    ffFilter.pushSamples(0, audioChannels, sampleRate, ffFilter.getSampleFormat, buffer1)
//    ffFilter.pushSamples(0, audioChannels, sampleRate, ffFilter.getSampleFormat, buffer1)
    val s1 = ffFilter.pullSamples()
    if(s1 != null) {
      println(s1.samples.head)
    }else {
      println("s1 is null")
    }
    ffFilter.pushSamples(1, audioChannels, sampleRate, ffFilter.getSampleFormat, buffer2)
    val s2 = ffFilter.pullSamples()
    if(s2 != null) {
      println(s2.samples.head)
    }else {
      println("s2 is null")
    }
    ffFilter.pushSamples(1, audioChannels, sampleRate, ffFilter.getSampleFormat, buffer2)
    val s3 = ffFilter.pullSamples()
    if(s3 != null) {
      println(s3.samples.head)
    }else {
      println("s3 is null")
    }
    ffFilter.pushSamples(1, audioChannels, sampleRate, ffFilter.getSampleFormat, buffer2)
    val f = ffFilter.pullSamples()
    if(f != null)
      println(f.samples.head)
    else {
      println(s"f is null")
    }
    //    log.info("Starting.")
    ffFilter.pushSamples(0, 1, 44100, ffFilter.getSampleFormat, buffer1)
    val f1 = ffFilter.pullSamples()
//    println(f1.samples.head)

  }
}

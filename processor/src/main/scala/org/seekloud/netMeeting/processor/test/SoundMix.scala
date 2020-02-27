package org.seekloud.netMeeting.processor.test

/**
  * @user: wanruolong
  * @date: 2020/2/4 11:01
  *
  */

import java.nio.{ByteBuffer, ByteOrder, ShortBuffer}
import java.util.concurrent.{ScheduledThreadPoolExecutor, TimeUnit}

import javax.sound.sampled.{AudioFormat, AudioSystem, DataLine, TargetDataLine}

import scala.collection.mutable
import javax.swing.JFrame
import org.bytedeco.ffmpeg.global.avcodec
import org.bytedeco.javacv._
import org.slf4j.LoggerFactory
import org.bytedeco.ffmpeg.global.avutil
import org.seekloud.netMeeting.processor.core.GrabberActor.log

import scala.collection.mutable.ListBuffer
import scala.io.StdIn
import scala.util.{Success, Try}

object SoundMix  {
  //sound
  val peopleNum = 2
  val audioChannels = 2
  val sampleFormat = avutil.AV_SAMPLE_FMT_S16
  val complexFilter = s" amix=inputs=$peopleNum:duration=longest:dropout_transition=3 "
  val sampleRate = 44100

  val FilePath = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestVideo"

  //file
  val urlList = List[String](s"$FilePath/big_buck_bunny.mp4", s"$FilePath/trailer.mkv")
  var grabberList = List[FFmpegFrameGrabber]()


  def main(args: Array[String]): Unit = {
    (0 until peopleNum).foreach{i =>
      val grabber = new FFmpegFrameGrabber(urlList(i))
      Try{
        grabber.start()
      }match {
        case Success(value) =>
          println("start success grab")
        case e: Exception =>
          println(s"exception occured in creant grabber")
      }

      grabberList = grabber :: grabberList
    }

    val recorder = new FFmpegFrameRecorder("H:/wav/recorder.flv", audioChannels)
    recorder.setAudioCodec(avcodec.AV_CODEC_ID_AAC)
    recorder.setAudioChannels(audioChannels)
    recorder.setAudioBitrate(192000)
    recorder.setAudioQuality(0)
    recorder.setFormat("flv")
    recorder.start()



        //音频混流
    var filterStr = complexFilter
    (1 to peopleNum).reverse.foreach { i =>
      filterStr = s"[${i - 1}:a]" + filterStr
    }
    filterStr += "[a]"
    println(s"audio filter: $filterStr")

    val ffFilter = new FFmpegFrameFilter(
      filterStr,
      audioChannels
    )
    ffFilter.setAudioChannels(audioChannels)
    ffFilter.setSampleFormat(sampleFormat)
    ffFilter.setAudioInputs(peopleNum)
    ffFilter.start()

//    val frame = grabberList(0).grab()
//    println(frame.opaque)
//    println(frame.audioChannels)
//    println(frame.sampleRate)



    var continue = true
    while(continue){
      (0 until peopleNum).foreach{i =>
        //            println(i)
        val frame = grabberList(i).grab()
        if(frame != null && frame.samples != null){
          //              println(frame.samples.length)

          /*              val s = frame.samples
                        s.foreach{buff =>
                          print(buff.capacity())
                          print("  ")
                          print(buff.limit())
                        }*/
          //              println()
          try{
            ffFilter.pushSamples(i, audioChannels, sampleRate, sampleFormat, frame.samples: _*)
          } catch {
            case e: Exception =>
              println("encounter null pointer.")
              e.printStackTrace()
          }
        }else{
          continue = false
          println("frame is empty.")
        }
      }
      val frame = ffFilter.pullSamples()
      if(frame != null && frame.samples != null){
        recorder.record(frame)
      }else{
        println("frame is null")
      }
    }


/*    val thread = new Thread(){
      override def run(): Unit = {

        var continue = true
        while(continue){
          (0 until peopleNum).foreach{i =>
            //            println(i)
            val frame = grabberList(i).grab()
            if(frame != null && frame.samples != null){
              //              println(frame.samples.length)

              /*              val s = frame.samples
                            s.foreach{buff =>
                              print(buff.capacity())
                              print("  ")
                              print(buff.limit())
                            }*/
              //              println()
              try{
                ffFilter.pushSamples(i, audioChannels, sampleRate, sampleFormat, frame.samples: _*)
              } catch {
                case e: Exception =>
                  println("encounter null pointer.")
                  e.printStackTrace()
              }
            }else{
              continue = false
              println("frame is empty.")
            }
          }
          val frame = ffFilter.pullSamples()
          if(frame != null && frame.samples != null){
            recorder.record(frame)
          }else{
            println("frame is null")
          }
        }
      }
    }

    thread.start()
    Thread.sleep(20000)
    thread.interrupt()*/

    ffFilter.stop()
    grabberList.foreach(grabber => grabber.stop())
    recorder.stop()
  }
}

package org.seekloud.netMeeting.processor.test

import org.bytedeco.javacv.{FFmpegFrameFilter, FFmpegFrameGrabber}

/**
  * User: cq
  * Date: 2020/2/20
  */
object TestFilter {
  val FilePath1 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestAudio/00.wav"
  val FilePath2 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestAudio/01.wav"
  def main(args: Array[String]): Unit = {
    val grabber1 = new FFmpegFrameGrabber(FilePath1)
    val filter = new FFmpegFrameFilter("[0:a][1:a] amix=inputs=2:duration=longest:dropout_transition=3:weights=1 1[a]",2)

  }
}

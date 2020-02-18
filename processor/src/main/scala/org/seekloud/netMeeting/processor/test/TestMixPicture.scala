package org.seekloud.netMeeting.processor.test

import java.io.File

import javax.imageio.ImageIO
import java.awt.image.BufferedImage

import org.bytedeco.javacv.Java2DFrameConverter

/**
  * User: cq
  * Date: 2020/2/13
  */
object TestMixPicture {
  def main(args: Array[String]): Unit = {
    val filePath1 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestPicture/test1.jpg"
    val filePath2 = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestPicture/test2.jpg"
    val fileOutPath = "D:/ScalaWorkSpace/netMeeting/processor/src/main/scala/org/seekloud/netMeeting/processor/test/TestPicture/testout.jpg"
    val image1 = ImageIO.read(new File(filePath1))
    val image2 = ImageIO.read(new File(filePath2))

    val canvas = new BufferedImage(640, 480, BufferedImage.TYPE_3BYTE_BGR)
    val graph = canvas.getGraphics()
    graph.drawImage(image1, 0, 0, 640/2, 480/2, null)
    graph.drawString("用户1",0+50,0+50)
    graph.drawImage(image2,640/2,480/2,640/2,480/2,null)
    graph.drawString("用户2",640/2+50,480/2+50)
    ImageIO.write(canvas,"JPG",new File(fileOutPath))
  }
}

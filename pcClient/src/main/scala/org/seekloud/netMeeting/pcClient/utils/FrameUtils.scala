package org.seekloud.netMeeting.pcClient.utils

import javafx.embed.swing.SwingFXUtils
import javafx.scene.image.Image
import org.bytedeco.javacv.{Frame, Java2DFrameConverter}

/**
  * @user: wanruolong
  * @date: 2020/2/19 21:00
  *
  */
object FrameUtils {

  def convert(image: Image): Frame = {
    val bufferedImage = SwingFXUtils.fromFXImage(image, null)
    val converter = new Java2DFrameConverter()
    val frame = converter.convert(bufferedImage)
    frame
  }

}

package org.seekloud.netMeeting.pcClient.component


import java.io.File

import javafx.application.Application
import javafx.geometry.Pos
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{GridPane, HBox, VBox}
import javafx.stage.Stage
import javax.imageio.ImageIO
import org.bytedeco.ffmpeg.global.avcodec.{av_jni_set_java_vm, avcodec_register_all}
import org.bytedeco.ffmpeg.global.avdevice.avdevice_register_all
import org.bytedeco.ffmpeg.global.avformat.{av_register_all, avformat_network_init}
import org.bytedeco.ffmpeg.global._
import org.bytedeco.javacpp.Loader
import org.bytedeco.javacv.{CanvasFrame, FFmpegFrameRecorder, FrameRecorder, Java2DFrameConverter}

/**
  * @user: wanruolong
  * @date: 2020/2/19 9:35
  *
  */
class LivePane extends Application{

  override def start(primaryStage: Stage): Unit = {
    val canvas4Self = new Canvas(400, 225)
    val image = new Image("/img/camera.png")
    val image1 = new Image("/img/camera.png")
    canvas4Self.getGraphicsContext2D.drawImage(image, 0, 0, canvas4Self.getWidth, canvas4Self.getHeight)
    val canvas = new Canvas(640, 360)
    canvas.getGraphicsContext2D.drawImage(image1, 0, 0, canvas.getWidth, canvas.getHeight)
    val group = new Group
    val scene = new Scene(group)
    scene.getStylesheets.add(
      this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
    )
    val anchorControl1 = new AnchorControl(90+400, 144)
    anchorControl1.host.setSelected(true)
    anchorControl1.host.setDisable(true)
    val anchorControl2 = new AnchorControl(410+400,144)
    val anchorControl3 = new AnchorControl(90+400,324)
    val anchorControl4 = new AnchorControl(410+400,324)

    val anchorPane1 = anchorControl1.getAnchorPane()
    val anchorPane2 = anchorControl2.getAnchorPane()
    val anchorPane3 = anchorControl3.getAnchorPane()
    val anchorPane4 = anchorControl4.getAnchorPane()

/*    canvas.addEventFilter(MouseEvent.MOUSE_ENTERED, (event: MouseEvent) => {
      print("x" + event.getX + "   ")
      if(event.getX < canvas.getWidth/2 && event.getY < canvas.getHeight/2){
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane1)
      } else if(event.getX >= canvas.getWidth/2 && event.getY < canvas.getHeight/2) {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane2)
      } else if(event.getX < canvas.getWidth/2 && event.getY >= canvas.getHeight/2) {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane3)
      } else {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane4)
      }

      println("y: " + event.getY)
    })

    canvas.addEventFilter(MouseEvent.MOUSE_MOVED, (event: MouseEvent) => {
      if(event.getX < canvas.getWidth/2 && event.getY < canvas.getHeight/2){
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane1)
      } else if(event.getX >= canvas.getWidth/2 && event.getY < canvas.getHeight/2) {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane2)
      } else if(event.getX < canvas.getWidth/2 && event.getY >= canvas.getHeight/2) {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane3)
      } else {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane4)
      }
    })

    canvas.addEventFilter(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      if(group.getChildren.size() > 1) {
        group.getChildren.remove(1, group.getChildren.size())
      }
    })*/


    scene.addEventFilter(MouseEvent.MOUSE_MOVED, (event: MouseEvent) => {
      if(event.getX < (canvas.getWidth/2 + 400) && event.getX > 400 && event.getY < canvas.getHeight/2){
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane1)
      } else if(event.getX >= (canvas.getWidth/2 + 400) && event.getY < canvas.getHeight/2) {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane2)
      } else if(event.getX < (canvas.getWidth/2 + 400) && event.getX > 400 && event.getY >= canvas.getHeight/2) {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane3)
      } else if(event.getX >= (canvas.getWidth/2 + 400) && event.getY >= canvas.getHeight/2) {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
        group.getChildren.add(anchorPane4)
      } else {
        if(group.getChildren.size() > 1){
          group.getChildren.remove(1, group.getChildren.size())
        }
      }
    })

    scene.addEventFilter(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
      if(group.getChildren.size() > 1) {
        group.getChildren.remove(1, group.getChildren.size())
      }
    })


    val vBox = new VBox(canvas4Self)
    vBox.setAlignment(Pos.CENTER)
    val hBox = new HBox(vBox, canvas)
    group.getChildren.addAll(hBox)

    primaryStage.setScene(scene)
    primaryStage.show()
  }
}

object LivePane {

  private var loadingException: Exception = null

  @throws[Exception]
  def tryLoad(): Unit = {
    if (loadingException != null) throw loadingException
    else try {
      Loader.load(classOf[avutil])
      Loader.load(classOf[swresample])
      Loader.load(classOf[avcodec])
      Loader.load(classOf[avformat])
      Loader.load(classOf[swscale])
      /* initialize libavcodec, and register all codecs and formats */ av_jni_set_java_vm(Loader.getJavaVM, null)
      avcodec_register_all()
      av_register_all()
      avformat_network_init
      Loader.load(classOf[avdevice])
      avdevice_register_all()
    } catch {
      case t: Throwable =>
        if (t.isInstanceOf[FrameRecorder.Exception]){
          loadingException = t.asInstanceOf[FrameRecorder.Exception]
          throw loadingException
        }
        else{
          loadingException = new FrameRecorder.Exception("Failed to load " + classOf[FFmpegFrameRecorder], t)
          throw loadingException
        }
    }
  }

  def main(args: Array[String]): Unit = {
    tryLoad()
    val file = new File("pcClient/src/main/resources/img/camera.png")
//    println(file.canRead)
    val bufferedImage = ImageIO.read(file)
    val canvasFrame = new CanvasFrame("file")
    val converter = new Java2DFrameConverter
    val frame = converter.convert(bufferedImage)
    canvasFrame.showImage(frame)
  }
}


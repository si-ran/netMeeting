package org.seekloud.netMeeting.pcClient.component


import java.io.File

import javafx.application.Application
import javafx.geometry.{Insets, Pos}
import javafx.scene.{Group, Scene}
import javafx.scene.canvas.Canvas
import javafx.scene.control.Label
import javafx.scene.image.Image
import javafx.scene.input.MouseEvent
import javafx.scene.layout.{AnchorPane, GridPane, HBox, VBox}
import javafx.scene.paint.Color
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
    val gc4Self = canvas4Self.getGraphicsContext2D

    gc4Self.drawImage(image, 0, 0, canvas4Self.getWidth, canvas4Self.getHeight)
//    canvas4Self.getGraphicsContext2D.rect(0, 0, canvas4Self.getWidth, canvas4Self.getHeight)
    gc4Self.setStroke(Color.BLUE)
    gc4Self.setLineWidth(5)
    gc4Self.strokeRect(0,0,canvas4Self.getWidth, canvas4Self.getHeight)
    val canvas = new Canvas(640, 360)
    val gc = canvas.getGraphicsContext2D
    gc.drawImage(image1, 0, 0, canvas.getWidth, canvas.getHeight)
    gc.setStroke(Color.BLUE)
    gc.setLineWidth(5)
    gc.strokeRect(0,0,canvas.getWidth, canvas.getHeight)
//    gc.rect(0, 0, canvas.getWidth, canvas.getHeight)
    val group = new Group
    val scene = new Scene(group)
    scene.getStylesheets.add(
      this.getClass.getClassLoader.getResource("css/common.css").toExternalForm
    )
    val anchorControl1 = new AnchorControl(90+400+30, 144+15)
    anchorControl1.host.setSelected(true)
    anchorControl1.host.setDisable(true)
    val anchorControl4Self = new AnchorControl(130+15,257+15)
    val anchorControl2 = new AnchorControl(410+400+30,144+15)
    val anchorControl3 = new AnchorControl(90+400+30,324+15)
    val anchorControl4 = new AnchorControl(410+400+30,324+15)

    val anchorPane4Self = anchorControl4Self.getAnchorPane()
    anchorPane4Self.setVisible(true)
    val anchorPane1 = anchorControl1.getAnchorPane()
    val anchorPane2 = anchorControl2.getAnchorPane()
    val anchorPane3 = anchorControl3.getAnchorPane()
    val anchorPane4 = anchorControl4.getAnchorPane()

    val anchorPaneList = List[AnchorPane](anchorPane1, anchorPane2, anchorPane3, anchorPane4)
    anchorPaneList.foreach(_.setVisible(true))

    anchorControl4Self.microphone.setOnAction(_ => {
      if(anchorControl4Self.microphone.isSelected)
        println(s"microphone clicked")
    })


    scene.addEventFilter(MouseEvent.MOUSE_MOVED, (event: MouseEvent) => {
      group.getChildren.remove(1, group.getChildren.size())
      if(event.getX < (canvas.getWidth/2 + 400) && event.getX > 400 && event.getY < canvas.getHeight/2){

        group.getChildren.add(anchorPane1)

      } else if(event.getX >= (canvas.getWidth/2 + 400) && event.getY < canvas.getHeight/2) {

        group.getChildren.add(anchorPane2)

      } else if(event.getX < (canvas.getWidth/2 + 400) && event.getX > 400 && event.getY >= canvas.getHeight/2) {

        group.getChildren.add(anchorPane3)

      } else if(event.getX >= (canvas.getWidth/2 + 400) && event.getY >= canvas.getHeight/2) {

        group.getChildren.add(anchorPane4)

      } else if(event.getX <= 400 && event.getY <= (360-(360-225)/2) && event.getY >= (360-(360-225)/2)-225) {

        group.getChildren.add(anchorPane4Self)

      }
    })

    scene.addEventFilter(MouseEvent.MOUSE_EXITED, (_: MouseEvent) => {
        group.getChildren.remove(1, group.getChildren.size())
    })


    val label = new Label("test")
    label.setTextFill(Color.RED)
    val vBox = new VBox(label, canvas4Self)
    vBox.setAlignment(Pos.CENTER)
    val hBox = new HBox(vBox, canvas)
    hBox.setSpacing(15)
    hBox.setPadding(new Insets(15))
    group.getChildren.addAll(hBox)

    primaryStage.setScene(scene)
    primaryStage.show()
  }
}

/*object LivePane {

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
}*/


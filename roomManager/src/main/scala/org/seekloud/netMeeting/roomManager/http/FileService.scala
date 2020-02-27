package org.seekloud.netMeeting.roomManager.http

/**
  * User: easego
  * Date: 2018/12/19
  * Time: 15:43
  */

import java.io.File

import akka.actor.Scheduler
import akka.http.scaladsl.model.{ContentTypes, HttpEntity}
import akka.http.scaladsl.server.Directives._
import akka.http.scaladsl.server.{Directive1, Route}
import akka.stream.scaladsl.{FileIO, Source}
import akka.util.{ByteString, Timeout}
import io.circe._
import io.circe.generic.auto._
import org.seekloud.netMeeting.protocol.ptcl.WebProtocol.SaveHeadImgRsp
import org.seekloud.netMeeting.roomManager.utils.{FileUtil, HestiaClient, ServiceUtils}
import org.slf4j.LoggerFactory
import org.seekloud.netMeeting.roomManager.Boot.{executor, materializer}
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.seekloud.netMeeting.roomManager.models.dao.WebDAO

import scala.util.{Failure, Success}


trait FileService extends SessionBase with ServiceUtils {

  implicit val timeout: Timeout
  implicit val scheduler: Scheduler

  private val log = LoggerFactory.getLogger(this.getClass)

  private def storeFile(source: Source[ByteString, Any]): Directive1[java.io.File] = {
    val dest = java.io.File.createTempFile("akka-http-upload", ".tmp")
    val file = source.runWith(FileIO.toPath(dest.toPath)).map(_ => dest)
    onComplete[java.io.File](file).flatMap {
      case Success(f) =>
        provide(f)
      case Failure(e) =>
        dest.deleteOnExit()
        failWith(e)
    }
  }

  private val saveHeadImg = (path("saveHeadImg") & post) {
    userAuth{ user =>
      fileUpload("fileUpload") {
        case (fileInfo, fileContent) =>
          storeFile(fileContent) { f =>
            val res = HestiaClient.upload(f, fileInfo.fileName)
            dealFutureResult(
              res.map{ result =>
                val imgName = result.right.get
                WebDAO.updateHeadImg(user.videoUserInfo.userId.toLong, imgName)
                complete(SaveHeadImgRsp(imgName))
              }
            )
          }
      }
    }
  }

//  val getVideo = (path("getVideo") & post){
//    adminAuth{_ =>
//      parameters(
//        'videoSrc.as[Int]
//      ){ path =>
//        val savePath = path match {
//          case 0 => AppSettings.compareSrcSavePath
//          case 1 => AppSettings.compareTstSavePath
//          case _ => AppSettings.compareSrcSavePath
//        }
//        entity(as[Either[Error, GetVideoReq]]){
//          case Right(req) =>
//            val file = new File(savePath)
//            val fileList = file.list()
//            complete(GetVideoRsp(fileList.toList))
//          case Left(e) =>
//            complete(ErrorRsp(200001, s"decode error $e"))
//        }
//      }
//    }
//  }

//  val getVideoNumber = (path("getVideoNumber") & get) {
//    //todo try catch
//    adminAuth{ _ =>
//      val file1 = new File(AppSettings.compareSrcSavePath)
//      val file2 = new File(AppSettings.compareTstSavePath)
//      if(!file1.exists()){
//        file1.mkdirs()
//      }
//      if(!file1.exists()){
//        file2.mkdirs()
//      }
//      val number1 = file1.list().size
//      val number2 = file2.list().size
//      complete(GetVideoNumberRsp(number1, number2))
//    }
//  }

//  private val deleteFile = (path("deleteFile") & post) {
//    entity(as[Either[Error, DeleteVideoReq]]) {
//      case Right(req) =>
//        val deletePath = req.videoRrc match {
//          case 0 => AppSettings.compareSrcSavePath
//          case 1 => AppSettings.compareTstSavePath
//          case _ => AppSettings.compareSrcSavePath
//        }
//        try{
//          val file = new File(deletePath + req.fileName)
//          file.delete()
//        } catch { case error: Exception =>
//          log.debug(s"deleteFile 文件删除错误,error=$error")
//          complete(DeleteVideoRsp(200006, msg = s"文件删除错误，error:$error"))
//        }
//        complete(DeleteVideoRsp())
//      case Left(e) =>
//        log.debug(s"deleteFile 接口请求错误,error=$e")
//        complete(DeleteVideoRsp(200005, msg = s"接口请求错误，error:$e"))
//
//
//    }
//  }

  val fileRoute: Route = pathPrefix("file") {
    saveHeadImg
  }

}


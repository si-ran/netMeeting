package org.seekloud.netMeeting.roomManager.utils

import java.io._
import java.net.URLEncoder

import akka.util.ByteString
import org.slf4j.LoggerFactory

/**
  * User: easego
  * Date: 2018/5/11
  * Time: 10:20
  */

object FileUtil {

  private val log = LoggerFactory.getLogger(this.getClass)

  def copyFile(dest:File,source:File) = {
    var in:InputStream = null
    var out:OutputStream = null
    try{
      in = new FileInputStream(source)
      out = new FileOutputStream(dest)
      val buffer = new Array[Byte](1024)
      var byte = in.read(buffer)
      while(byte >= 0){
        out.write(buffer,0,byte)
        byte = in.read(buffer)
      }
    }catch{
      case e:Exception =>
        log.error(s"copy file ${source.getName} error",e)
    }finally {
      if(in!=null) in.close()
      if(out!=null) out.close()
    }
  }

  def storeFile(fileName: String, file:File, basicDir: String) = {
    val fileDir = basicDir
    val filePath = fileDir  + "/" + fileName
    log.debug(s"fileDir: $fileDir filePath: $filePath")
    val dir = new File(fileDir)
    if(!dir.exists()) dir.mkdirs()
    val dest = new File(filePath)
    if(dest.exists()) dest.delete()
    copyFile(dest,file)
  }

}
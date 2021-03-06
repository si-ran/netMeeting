package org.seekloud.webClient.common

/**
  * User: si-ran
  * Date: 2019/11/8
  * Time: 16:37
  */
object Routes {

  val baseUrl = "/netMeeting"

  object User{

    val base: String = baseUrl + "/user"

    def wsJoinUrl(id: String) = base + s"/websocketJoin?id=$id"

    val signUp = base + s"/signUp"

    val signIn = base + "/signIn"

    val userInfo = base + "/userInfo"

  }

//  object client{
//
//    val base: String = baseUrl + "/client"
//
//    val signIn = base + "/signIn"
//
//  }

  object File{

    val base: String = baseUrl + "/file"

    val saveHeadImg = base + s"/saveHeadImg"

    val getVideo = base + s"/getVideo"

    val getRecord = base + s"/getRecord" //录像路径

  }



}

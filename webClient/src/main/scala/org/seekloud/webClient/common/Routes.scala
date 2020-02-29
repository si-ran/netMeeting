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

  }

  object File{

    val base: String = baseUrl + "/file"

    val saveHeadImg = base + s"/saveHeadImg"

  }



}

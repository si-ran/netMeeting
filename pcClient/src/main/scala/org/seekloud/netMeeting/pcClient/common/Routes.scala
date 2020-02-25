package org.seekloud.netMeeting.pcClient.common

import org.seekloud.netMeeting.pcClient.common.AppSettings._

/**
  * User: Arrow
  * Date: 2019/7/16
  * Time: 11:26
  */
object Routes {


  /*roomManager*/
  val baseUrl: String = rmProtocol + "://" + rmDomain + "/" + rmUrl
//  val baseUrl = rmProtocol + "://" + rmHostName + ":" +  rmPort + "/" + rmUrl


  val userUrl: String = baseUrl + "/user"
  val clientUrl: String = baseUrl + "/client"
  val signInByMail: String = userUrl + "/signInByMail"
  val signUp: String = "http://47.92.170.2:42061/netMeeting#/"
  val signIn: String = clientUrl + "/signIn"
  val getRoomList: String = userUrl + "/getRoomList"
  val searchRoom: String = userUrl + "/searchRoom"
  val temporaryUser: String = userUrl + "/temporaryUser"
  val getRoomInfo: String = userUrl + "/getRoomInfo"

  val recordUrl: String = baseUrl + "/record"
//  val getRecordList: String = recordUrl + "/getRecordList"
  val searchRecord: String = recordUrl + "/searchRecord"

  def getRecordList(sortBy: String, pageNum: Int, pageSize: Int) = recordUrl + "/getRecordList" + s"?sortBy=$sortBy&pageNum=$pageNum&pageSize=$pageSize"
  def getAuthorRecordList(roomId:Long)= recordUrl + "/getAuthorRecordList"+s"?roomId=$roomId"

  val deleteRecord:String=recordUrl + "/deleteRecord"

  val recordCommentUrl: String = baseUrl + "/recordComment"
  val getCommentList: String = recordCommentUrl + "/getRecordCommentList"
  val sendComment: String = recordCommentUrl + "/addRecordComment"

//  val wsBase = rmWsProtocol + "://" + rmHostName + ":" +  rmPort + "/" + rmUrl + "/user"
  val wsBase: String = rmWsProtocol + "://" + rmDomain + "/" + rmUrl + "/user"

  val hostNotLive = 100024

//  val streamBase = "rtmp://10.1.29.247:42069/live"
//
  val streamBase = "rtmp://47.92.170.2:42069/live"

  def getWsUrl(id: Long) = s"$wsBase/websocketJoin?id=$id"

  def getPushUrl(userId: Long) = s"$streamBase/$userId"

  def getPullUrl(roomId: Long, userId: Long) = s"$streamBase/${roomId}_$userId"

  def linkRoomManager(userId: Long, token: String, roomId: Long): String = wsBase + "/setupWebSocket" + s"?userId=$userId&token=$token&roomId=$roomId"

  def uploadImgUrl(imgType: Int, userId: Long): String = baseUrl + s"/file/uploadFile?imgType=$imgType&userId=$userId"

  def changeUserNameUrl(userId: Long, newName: String): String = userUrl + s"/nickNameChange?userId=$userId&newName=$newName"








}

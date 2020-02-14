package org.seekloud.netMeeting.processor.protocol

/**
  * User: cq
  * Date: 2020/2/11
  */
object CommonErrorCode {
  final case class ErrorRsp(
                             errCode: Int,
                             msg: String
                           )

  def internalError(message:String) = ErrorRsp(1000101,s"internal error: $message")

  def noSessionError(message:String="no session") = ErrorRsp(1000102,s"$message")

  def parseJsonError =ErrorRsp(1000103,"parse json error")

  def userAuthError =ErrorRsp(1000104,"your auth is lower than user")

  def adminAuthError=ErrorRsp(1000105,"your auth is lower than admin")

  def signatureError=ErrorRsp(msg= "signature wrong.",errCode = 1000106)

  def operationTimeOut =ErrorRsp(msg= "operation time out.",errCode = 1000107)

  def appIdInvalid =ErrorRsp(msg="appId invalid.",errCode=1000108)

  def getRoomError(msg:String) = ErrorRsp(msg = msg, errCode = 1000199)

  def requestIllegal(body:String = "") = ErrorRsp(msg=s"receive illegal request body;$body.",errCode = 1000109)

  def requestTimeOut = ErrorRsp(1000003, "request timestamp is too old.")

  def requestAskActorTimeOut = ErrorRsp(1000112, "网络繁忙，请重试")

  def loginAuthError = ErrorRsp(1000113, "this interface auth need login")

  def fileNotExistError = ErrorRsp(1000008, "file does not exist")

  def authUserError(e:String) = ErrorRsp(10000115, "Autherror: " + e )

  //用户登录
  def userNotExist = ErrorRsp(1000114, "user not exist")

  def pwdError = ErrorRsp(1000116, "wrong password!")

  //用户注册
  def userAlreadyExists = ErrorRsp(1000117, "user already exists")

  def pwdEmpty = ErrorRsp(1000118, "password can not be empty")

  //chat
  def roomIsNotOn = ErrorRsp(1000119, "room is not on")

  def noChatPermission = ErrorRsp(1000120, "no chat permission")

  def chatIsBusy = ErrorRsp(1000121, "microphone is busy")

  def roomNotExist = ErrorRsp(1000122, "room not exist")

  def hostUrlNotExist = ErrorRsp(1000123, "hostUrl not exist")

  def updateRoomError = ErrorRsp(1000124, "update roomInfo err.")

}

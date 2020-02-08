package org.seekloud.netMeeting.protocol.ptcl

/**
  * User: si-ran
  * Date: 2019/11/9
  * Time: 18:57
  */
object UserProtocol {

  case class StartCompareReq(
    srcUrl: String,
    testUrl: String
  )

  case class StartCompareRsp(
    taskId: Option[Long],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class CompareResultReq(
    taskId: Long
  )

  case class CompareResultRsp(
    state: Int,
    srcUrl: String,
    testUrl: String,
    isSame: Int,
    errCode:Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class CompareStateInfo(
    matchId: Long,
    state: Int,
    result: Float,
    startTime: Long
  )

  case class GetCompareStateRsp(
    data: List[CompareStateInfo] = Nil,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class MatchDataInfo(
    id: Long,
    srcVideoName: String,
    tstVideoName: String,
    isSimilarity: Boolean,
    success: Boolean,
    createTime: Long,
    wrongCode: Long,
    wrongMessage: String
  )

  case class GetMatchDataRsp(
    data: List[MatchDataInfo],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class TestCompareReq(
    srcList: List[Int],
    tstList: List[Int]
                           )

  case class TestCompareRsp(
    errCode: Int = 0,
    msg: String = "ok"
  )extends CommonRsp

  case class AdminLoginReq(
    account:String,
    password: String
  )
}

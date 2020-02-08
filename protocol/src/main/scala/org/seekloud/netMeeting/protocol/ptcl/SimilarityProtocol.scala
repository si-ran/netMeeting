package org.seekloud.netMeeting.protocol.ptcl

/**
  * User: si-ran
  * Date: 2020/1/3
  * Time: 15:46
  */
object SimilarityProtocol {

  case class SimRes(
    id: Long,
    similarity: Double,
    srcName: String,
    tstName: String,
    time: Long
  )

  case class ResultRsp(
    size: Int = 0,
    score: Double = 0,
    wrongMax: Double = 0,
    rightMin: Double = 0,
    rightSimData: List[SimRes] = Nil,
    wrongSimData: List[SimRes] = Nil,
    timeAll: Long = 0,
    timeAvg: Long = 0,
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp

  case class ResultReq(
    startId: Long,
    endId: Long
  )

  case class StartInfoRsp(
    ids: List[Long],
    errCode: Int = 0,
    msg: String = "ok"
  ) extends CommonRsp


}

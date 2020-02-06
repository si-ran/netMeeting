package org.seekloud.netMeeting.roomManager.models.dao

import slick.jdbc.H2Profile.api._
import org.seekloud.netMeeting.roomManager.utils.DBUtil._
import org.seekloud.netMeeting.roomManager.models.SlickTables._
import org.seekloud.netMeeting.roomManager.Boot.executor

import scala.collection.mutable
import scala.concurrent.Future
/**
  * User: si-ran
  * Date: 2019/11/9
  * Time: 20:50
  */
object MatchInfoDAO {

  def addMatchInfo(matchInfo: rMatchInfo) = {
    db.run(tMatchInfo.returning(tMatchInfo.map(_.id)) += matchInfo)
  }

  def updateMatchResult(matchId: Long, submitTime: Long, endTime: Long, result: Double, msg: String) = {
    val action = for {
      b <- tMatchInfo.filter(_.id === matchId).map(t => (t.submitTime, t.finishTime, t.waitTime, t.runningTime, t.success, t.similarity, t.wrongMessage)).update(submitTime, endTime, endTime - submitTime, endTime - submitTime, true, result, msg)
    } yield b
    db.run(action)
  }

  def updateMatchWrong(matchId: Long, errCode: Long, msg: String) = {
    db.run(tMatchInfo.filter(_.id === matchId).map(t =>(t.wrongCode, t.wrongMessage)).update(errCode, msg))
  }

  def getAllMatchInfo() ={
    db.run(tMatchInfo.sortBy(_.createTime.desc).take(20).result)
  }

  def getMatchInfoById(matchId: Long) ={
    db.run(tMatchInfo.filter(_.id === matchId).sortBy(_.createTime.desc).result)
  }

  def getMatchInfoByTime(time: Long) ={
    db.run(tMatchInfo.filter(_.createTime === time).sortBy(_.createTime.desc).result)
  }

  def getMaxMatchId = {
    db.run(tMatchInfo.map(_.id).max.result)
  }

  /*准确率统计接口*/
  //获取所有准确率
  def getResbyId(sId: Long, eId: Long) ={
    db.run(tMatchInfo.filter(f => f.id >= sId && f.id <= eId).result)
  }
  //获取所有的src10视频信息(测试用，每次内部测试视频都以src10开始，此可以方便查找)
  def getsrc10Id() ={
    db.run(tMatchInfo.filter(f => f.id > 1001780l && f.srcVideoName.like(s"%src10%") && f.tstVideoName.like("%tst10%")).result)
  }

}

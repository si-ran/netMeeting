package org.seekloud.netMeeting.pcClient.scene

import akka.actor.typed.ActorRef
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.common.StageContext
import org.seekloud.netMeeting.pcClient.core.RmManager
import org.seekloud.netMeeting.pcClient.core.RmManager.RmCommand
import org.seekloud.netMeeting.pcClient.scene.CreatorStage.{CreatorStageListener, MeetingType}
import org.seekloud.netMeeting.pcClient.scene.HomeScene.HomeSceneListener
import org.seekloud.netMeeting.pcClient.scene.LivingStage.LivingStageListener
import org.slf4j.LoggerFactory

/**
  * @user: wanruolong
  * @date: 2020/2/6 11:12
  *
  */
class PageController(context: StageContext,
                     homeScene: HomeScene,
                     rmManager: ActorRef[RmCommand]) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private var creatorStage: CreatorStage = _

  private var livingStage: LivingStage = _

  homeScene.setListener(new HomeSceneListener {
    override def createNewIssue(meetingType: MeetingType.Value): Unit = {
      setCreatorStage(new CreatorStage(meetingType))
      getCreatorStage.showStage()
      setListener4CreatorStage()
    }
  })

  def setListener4CreatorStage() = {
//    log.debug("set listener 4 creator")
    this.creatorStage.setListener(new CreatorStageListener {
      override def createNewMeeting(meetingType: MeetingType.Value): Unit = {
        setLivingStage(new LivingStage)
        setListener4LivingStage()
        getLivingStage.showStage()
        val gc = getLivingStage.getGC()
        log.debug("got createNewMeeting command.")
        val inputInfo = getCreatorStage.getInput()
        rmManager ! RmManager.StartLive(gc, inputInfo.roomId, inputInfo.userId, inputInfo.url, meetingType)
      }
    })
  }

  def setListener4LivingStage() = {
    this.livingStage.setListener(new LivingStageListener {
      override def stop(): Unit = {
        rmManager ! RmManager.Close
      }
    })

  }

  def setCreatorStage(creatorStage: CreatorStage) = {
    this.creatorStage = creatorStage
  }

  def getCreatorStage: CreatorStage = {
    this.creatorStage
  }

  def setLivingStage(livingStage: LivingStage) = {
    this.livingStage = livingStage
  }

  def getLivingStage: LivingStage = {
    this.livingStage
  }

  def showHomeScene() = {
    Boot.addToPlatform(
      context.switchScene(homeScene.getScene, title = "netMeeting", true)
    )
  }



}

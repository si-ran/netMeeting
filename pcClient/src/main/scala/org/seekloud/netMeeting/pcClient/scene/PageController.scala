package org.seekloud.netMeeting.pcClient.scene

import akka.actor.typed.ActorRef
import org.seekloud.netMeeting.pcClient.Boot.{addToPlatform, executor}
import org.seekloud.netMeeting.pcClient.common.Routes
import org.seekloud.netMeeting.pcClient.core.RmManager
import org.seekloud.netMeeting.pcClient.core.RmManager.RmCommand
import org.seekloud.netMeeting.pcClient.scene.CreatorStage.{CreatorStageListener, MeetingType}
import org.seekloud.netMeeting.pcClient.scene.HomeStage.HomeStageListener
import org.seekloud.netMeeting.pcClient.scene.LivingStage.LivingStageListener
import org.seekloud.netMeeting.pcClient.scene.LoginScene.LoginStageListener
import org.seekloud.netMeeting.pcClient.utils.RMClient
import org.slf4j.LoggerFactory

/**
  * @user: wanruolong
  * @date: 2020/2/6 11:12
  *
  */
class PageController(
                      loginStage: LoginScene,
                      rmManager: ActorRef[RmCommand]
                    ) {

  private[this] val log = LoggerFactory.getLogger(this.getClass)

  private var homeStage: HomeStage = _

  private var creatorStage: CreatorStage = _

  private var livingStage: LivingStage = _

  loginStage.setListener(new LoginStageListener {
    override def login(username: String, password: String): Unit = {
      //todo login 2 room manager.
      RMClient.signIn(username, password).map{
        case Right(signInRsp) =>
          log.debug(s"sign in success.")
          addToPlatform{
            loginStage.close()
            val userId = 10010
//            homeStage = new HomeStage(signInRsp.data.get.userId)
            homeStage = new HomeStage(userId)
            homeStage.showStage()
            setListener4HomeStage()
          }
        case Left(error) =>
          log.error(s"sign in error, ${error.getMessage}")
      }
    }
  })

  def setListener4HomeStage() = {
    this.homeStage.setListener(new HomeStageListener {
      override def createNewIssue(meetingType: MeetingType.Value): Unit = {
        val userId = homeStage.getUserId
        setCreatorStage(new CreatorStage(meetingType, userId))
        if(meetingType == MeetingType.CREATE){
          val roomId = userId
          getCreatorStage.setRoomId(roomId)
          val url = Routes.getPushUrl(userId)
          getCreatorStage.setUrl(url)
        }
        getCreatorStage.showStage()
        setListener4CreatorStage()

      }

      override def close(): Unit = {
        rmManager ! RmManager.ShutDown
      }
    })
  }


  def setListener4CreatorStage() = {
    //    log.debug("set listener 4 creator")
    this.creatorStage.setListener(new CreatorStageListener {
      override def createNewMeeting(meetingType: MeetingType.Value): Unit = {
        setLivingStage(new LivingStage)
        setListener4LivingStage()
        getLivingStage.showStage()
        val gc4Self = getLivingStage.getGc4Self()
        val gc4Pull = getLivingStage.getGc4Pull()
        val inputInfo = getCreatorStage.getInput()
        rmManager ! RmManager.StartLive(gc4Self, gc4Pull, inputInfo.roomId, inputInfo.userId, meetingType)
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
}

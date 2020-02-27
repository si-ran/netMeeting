package org.seekloud.netMeeting.pcClient.scene

import akka.actor.typed.ActorRef
import org.seekloud.netMeeting.pcClient.Boot.{addToPlatform, executor}
import org.seekloud.netMeeting.pcClient.common.Routes
import org.seekloud.netMeeting.pcClient.component.WarningDialog
import org.seekloud.netMeeting.pcClient.core.RmManager
import org.seekloud.netMeeting.pcClient.core.RmManager.RmCommand
import org.seekloud.netMeeting.pcClient.scene.CreatorStage.{CreatorStageListener, MeetingType}
import org.seekloud.netMeeting.pcClient.scene.HomeStage.HomeStageListener
import org.seekloud.netMeeting.pcClient.scene.LivingStage.LivingStageListener
import org.seekloud.netMeeting.pcClient.scene.LoginScene.LoginStageListener
import org.seekloud.netMeeting.pcClient.utils.RMClient
import org.seekloud.netMeeting.protocol.ptcl.CommonInfo.RoomInfo
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
      RMClient.signIn(username, password).map{
        case Right(signInRsp) =>
          log.debug(s"sign in success. ${signInRsp}")
          if(signInRsp.errCode == 0) {
            addToPlatform{
              loginStage.close()
              //            val userId = 10010
              val userId = signInRsp.data.get.userId
              homeStage = new HomeStage(userId)
              homeStage.showStage()
              setListener4HomeStage()
            }
          } else {
            loginStage.loginError()
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
        creatorStage = new CreatorStage(meetingType, userId)
        val url = Routes.getPushUrl(userId)
        creatorStage.setUrl(url)
        if(meetingType == MeetingType.CREATE){
          val roomId = userId
          creatorStage.setRoomId(roomId)
        }
        creatorStage.showStage()
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
      override def newMeeting(meetingType: MeetingType.Value): Unit = {
        val userId = homeStage.getUserId
        livingStage = new LivingStage(userId)
        if(meetingType == MeetingType.CREATE) {
          val roomId = userId
          val roomInfo = RoomInfo(roomId, List[Long](userId), userId)
          livingStage.updateRoomInfo(roomInfo)
        }
        setListener4LivingStage()
        livingStage.showStage()
        val gc4Self = livingStage.getGc4Self()
        val gc4Pull = livingStage.getGc4Pull()
        val inputInfo = creatorStage.getInput()
        rmManager ! RmManager.StartLive(gc4Self, gc4Pull, inputInfo.roomId, inputInfo.userId, meetingType)
      }
    })
  }

  def setListener4LivingStage() = {
    this.livingStage.setListener(new LivingStageListener {
      override def stop(): Unit = {
        rmManager ! RmManager.Close
      }

      override def giveHost2(userId: Long): Unit = {
        livingStage.setHost(false)
        rmManager ! RmManager.GiveHost2User(userId)
      }

      override def mediaControl(userId: Long, needImage: Boolean = true, needSound: Boolean = true): Unit = {
        rmManager ! RmManager.MediaControl(userId, needImage, needSound)
      }

      override def kickOut(userId: Long): Unit = {
        rmManager ! RmManager.KickOut(userId)
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

  def setRoomInfo(roomInfo: RoomInfo) = {
    livingStage.updateRoomInfo(roomInfo)
  }
}

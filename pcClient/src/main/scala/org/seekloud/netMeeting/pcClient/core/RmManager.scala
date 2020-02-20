package org.seekloud.netMeeting.pcClient.core

import akka.Done
import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.StashBuffer
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, TimerScheduler}
import akka.http.scaladsl.Http
import akka.http.scaladsl.model.StatusCodes
import akka.http.scaladsl.model.ws.{BinaryMessage, Message, TextMessage, WebSocketRequest}
import akka.stream.OverflowStrategy
import akka.stream.scaladsl.{Keep, Sink, Source}
import akka.stream.typed.scaladsl.ActorSource
import akka.util.{ByteString, ByteStringBuilder}
import javafx.scene.canvas.GraphicsContext
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.Boot.{executor, materializer, system}
import org.seekloud.netMeeting.pcClient.common.Routes
import org.seekloud.netMeeting.pcClient.component.WarningDialog
import org.seekloud.netMeeting.pcClient.scene.PageController
import org.seekloud.netMeeting.pcClient.scene.CreatorStage.MeetingType
import org.seekloud.netMeeting.protocol.ptcl.CommonInfo.RoomInfo
import org.seekloud.netMeeting.protocol.ptcl.client2manager.websocket.AuthProtocol._
import org.slf4j.LoggerFactory

import scala.concurrent.Future


/**
  * @user: wanruolong
  * @date: 2020/2/6 15:24
  *
  */
object RmManager {

  private val log = LoggerFactory.getLogger(this.getClass)

  var roomId: Option[Long] = None

  var userId: Option[Long] = None

  var pullUrl: String = ""

  var identity: Identity.Value = Identity.Host

  var pushUrl: String = ""

  object Identity extends Enumeration {
    val Host, Client = Value
  }

  sealed trait RmCommand

  final case class StartLive(gc4Self: GraphicsContext, gc4Pull: GraphicsContext, roomId: Long, userId: Long, meetingType: MeetingType.Value) extends RmCommand

  final case object StartJoin extends RmCommand

  final case object HeartBeat extends RmCommand

  final case class GetSender(sender:  ActorRef[WsMsgFront]) extends RmCommand

  final case object BackHome extends RmCommand

  final case class GetPageItem(pageController: Option[PageController]) extends RmCommand

  final case object Close extends RmCommand

  final case object ShutDown extends RmCommand

  private case class ChildDead[U](name: String, childRef: ActorRef[U]) extends RmCommand

  /**
    * host
    */
  final case class HostWsEstablish(pushUrl: String) extends RmCommand

  final case class EstablishMeetingRsp(errorCode: Int = 0, msg: String = "ok") extends RmCommand

  /**
    * client
    */
  final case class ClientJoin() extends RmCommand

  private[this] def switchBehavior(
                                   ctx: ActorContext[RmCommand],
                                   behaviorName: String,
                                   behavior: Behavior[RmCommand]
                                 )(implicit stashBuffer: StashBuffer[RmCommand]) = {
    log.debug(s"${ctx.self.path} becomes $behaviorName behavior.")
    stashBuffer.unstashAll(ctx, behavior)
  }

  def create(): Behavior[RmCommand] = {
    Behaviors.setup[RmCommand]{ctx =>
      log.info("rmManager is starting")
      implicit val stashBuffer: StashBuffer[RmCommand] = StashBuffer[RmCommand](Int.MaxValue)
      Behaviors.withTimers[RmCommand]{ implicit timer =>
        idle()
      }
    }
  }

  def idle(
            pageController: Option[PageController] = None
          )(
            implicit stashBuffer: StashBuffer[RmCommand],
            timer: TimerScheduler[RmCommand]
          ): Behavior[RmCommand] =
    Behaviors.receive[RmCommand]{ (ctx, msg) =>
      msg match {
        case msg: GetPageItem =>
//          log.debug("got msg get page item.")
          idle(msg.pageController)

        case msg: StartLive =>
          roomId = Some(msg.roomId)
          userId = Some(msg.userId)
          pushUrl = Routes.getPushUrl(msg.userId)
          log.debug(s"push stream 2 $pushUrl")
          pullUrl = Routes.getPullUrl(msg.roomId, msg.userId)
          log.debug(s"pull stream from $pullUrl")
          msg.meetingType match {
            case MeetingType.CREATE =>
              ctx.self ! HostWsEstablish(pushUrl)
              switchBehavior(ctx, "hostBehavior", hostBehavior(msg.gc4Self, msg.gc4Pull))

            case MeetingType.JOIN =>
              ctx.self ! ClientJoin()
              switchBehavior(ctx, "clientBehavior", clientBehavior())
          }

        case Close =>
          log.info("close in idle.")
          Behaviors.same

        case x =>
          log.info(s"got unknown msg in idle $x")
          Behaviors.unhandled
      }
    }


  def hostBehavior(
                    gc4Self: GraphicsContext,
                    gc4Pull: GraphicsContext,
                    sender: Option[ActorRef[WsMsgFront]] = None,
                    captureManager: Option[ActorRef[CaptureManager.CaptureCommand]] = None
                  )(
                    implicit stashBuffer: StashBuffer[RmCommand],
                    timer: TimerScheduler[RmCommand]
                  ): Behavior[RmCommand] =
    Behaviors.receive[RmCommand]{(ctx, msg) =>
      msg match {
        case msg: HostWsEstablish =>
          log.debug(s"got msg $msg")
//          roomId = Some(msg.roomId)
//          userId = Some(msg.userId)
          assert(roomId.nonEmpty && userId.nonEmpty)
          def successFunc(): Unit = {

          }
          def failureFunc(): Unit = {
            Boot.addToPlatform {
              WarningDialog.initWarningDialog("连接失败！")
            }
          }

          //for debug
          val pushUrl = "rtmp://10.1.29.247:42069/live/test1"
          val pullUrl = "rtmp://10.1.29.247:42069/live/test1"

          val captureManager = getCaptureManager(ctx, pushUrl, pullUrl, gc4Self, gc4Pull)
          val wsUrl = Routes.getWsUrl(userId.get)
          buildWebsocket(ctx, wsUrl, successFunc(), failureFunc())
          hostBehavior(gc4Self, gc4Pull, sender, Some(captureManager))

        case msg: GetSender =>
          log.debug(s"got msg $msg")
          msg.sender ! EstablishMeetingReq(pushUrl, roomId.get, userId.get)
          //debug
          ctx.self ! EstablishMeetingRsp()
          hostBehavior(gc4Self, gc4Pull, Some(msg.sender), captureManager)

        case msg: EstablishMeetingRsp =>
          if(msg.errorCode == 0){
            captureManager.foreach(_ ! CaptureManager.StartEncode)
          }
          Behaviors.same

        case Close =>
          log.info("close in hostBehavior.")
          captureManager.foreach(_ ! CaptureManager.Close)
          switchBehavior(ctx, "idle", idle())

        case x =>
          log.info(s"got unknown msg in hostBehavior $x")
          Behaviors.unhandled
      }
    }

  def clientBehavior(
                      sender: Option[ActorRef[WsMsgFront]] = None
                    )(
                      implicit stashBuffer: StashBuffer[RmCommand],
                      timer: TimerScheduler[RmCommand]
                    ): Behavior[RmCommand] =
    Behaviors.receive[RmCommand]{ (ctx, msg) =>
      msg match {
        case msg: GetSender =>
          // todo start push stream
          clientBehavior(Some(msg.sender))

        case x =>
          log.info(s"got unknown msg in clientBehavior $x")
          Behaviors.unhandled

      }
    }


  def buildWebsocket(
                    ctx: ActorContext[RmCommand],
                    url: String,
                    successFunc: => Unit,
                    failureFunc: => Unit
                    )(
    implicit timer: TimerScheduler[RmCommand]
  ): Unit = {
    log.debug(s"build websocket with roomManager: $url")
    val wsFlow = Http().webSocketClientFlow(WebSocketRequest(url))
    val source = getSource()

    val sink = getRMSink(ctx)

    val (stream, response) =
      source
        .viaMat(wsFlow)(Keep.both)
        .toMat(sink)(Keep.left)
        .run()

    val connected = response.flatMap{ upgrage =>
      if(upgrage.response.status == StatusCodes.SwitchingProtocols){
        ctx.self ! GetSender(stream)
        successFunc
        Future.successful("link roomMamager successfully.")
      }
      else{
        failureFunc
        throw new RuntimeException(s"link roomManager failed: ${upgrage.response.status}")
      }
    }
    connected.onComplete(i => log.info(i.toString))
  }

  def getSource(): Source[BinaryMessage.Strict, ActorRef[WsMsgFront]] =
    ActorSource.actorRef[WsMsgFront](
      completionMatcher = {
        case CompleteMsgClient =>
          log.info("disconnect from room manager.")
      },
      failureMatcher = {
        case FailMsgClient(ex) =>
          log.error(s"ws failed: $ex")
          ex
      },
      bufferSize = 8,
      overflowStrategy = OverflowStrategy.fail
    ).collect{
      case message: WsMsgClient =>
        val sendBuffer = new MiddleBufferInJvm(409600)
        BinaryMessage.Strict(ByteString(
          message.fillMiddleBuffer(sendBuffer).result()
        ))
    }

  def getRMSink(
               rmManager: ActorContext[RmCommand]
               )(
    implicit timer: TimerScheduler[RmCommand]
  ): Sink[Message, Future[Done]] = {
    Sink.foreach[Message]{
      case TextMessage.Strict(msg) =>
        wsMessageHandler(TextMsg(msg), rmManager)

      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val message = bytesDecode[WsMsgRm](buffer) match {
          case Right(rst) => rst
          case Left(_) => DecodeError
        }
        wsMessageHandler(message, rmManager)

      case msg: BinaryMessage.Streamed =>
        val futureMsg = msg.dataStream.runFold(new ByteStringBuilder().result()){
          case (s, str) => s.++(str)
        }
        futureMsg.map{bMsg =>
          val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
          val message = bytesDecode[WsMsgRm](buffer) match {
            case Right(rst) => rst
            case Left(_) => DecodeError
          }
          wsMessageHandler(message, rmManager)
        }

      case _ => //do nothing
    }
  }

  def wsMessageHandler(data: WsMsgRm, rmManager: ActorContext[RmCommand]) = {
    data match {
      case msg: HeatBeat =>
        log.debug(s"got msg $msg")

      case TextMsg(msg) =>
        log.debug(s"rev ws msg: $msg")

      case x =>
        log.info(s"rev unknown msg $x")
    }
  }

  def getCaptureManager(
                         ctx: ActorContext[RmCommand],
                         pushUrl: String,
                         pullUrl: String,
                         gc4Self: GraphicsContext,
                         gc4Pull: GraphicsContext
                       ): ActorRef[CaptureManager.CaptureCommand] = {
    val childName = "captureManager"
    ctx.child(childName).getOrElse{
      log.debug("new Capture Manager.")
      val actor = ctx.spawn(CaptureManager.create(ctx.self, pushUrl, pullUrl, gc4Self, gc4Pull), childName)
      ctx.watchWith(actor, ChildDead(childName, actor))
      actor
    }.unsafeUpcast[CaptureManager.CaptureCommand]
  }
}

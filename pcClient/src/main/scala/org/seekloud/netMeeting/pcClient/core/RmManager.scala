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
import org.seekloud.byteobject.ByteObject._
import org.seekloud.byteobject.MiddleBufferInJvm
import org.seekloud.netMeeting.pcClient.Boot
import org.seekloud.netMeeting.pcClient.Boot.{executor, materializer, system}
import org.seekloud.netMeeting.pcClient.common.Routes
import org.seekloud.netMeeting.pcClient.component.WarningDialog
import org.seekloud.netMeeting.pcClient.scene.{HostController, ParticipantCotroller}
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

  var roomInfo: Option[RoomInfo] = None

  var userId: Option[Long] = None

  sealed trait RmCommand

  final case class StartLive() extends RmCommand

  final case object HeartBeat extends RmCommand

  final case class GetSender(sender:  ActorRef[WsMsgFront]) extends RmCommand

//host
  final case object HostWsEstablish extends RmCommand


  private[this] def swithBehavior(
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

  def idle()(
          implicit stashBuffer: StashBuffer[RmCommand],
          timer: TimerScheduler[RmCommand]
  ): Behavior[RmCommand] = {
    Behaviors.receive[RmCommand]{ (ctx, msg) =>
      msg match {
        case msg: StartLive =>
          Behaviors.same
      }
    }
  }

  def hostBehavior(sender: Option[ActorRef[WsMsgFront]] = None,
                   hostController: HostController)(
    implicit stashBuffer: StashBuffer[RmCommand],
    timer: TimerScheduler[RmCommand]
  ): Behavior[RmCommand] = {
    Behaviors.receive[RmCommand]{(ctx, msg) =>
      msg match {
        case HostWsEstablish =>
          assert(roomInfo.nonEmpty && userId.nonEmpty)
          def successFunc(): Unit = {

          }
          def failureFunc(): Unit = {
            Boot.addToPlatform {
              WarningDialog.initWarningDialog("连接失败！")
            }
          }
          val url = Routes.getWsUrl(userId.get)
          buildWebsocket(ctx, url, Right(hostController), successFunc(), failureFunc)
          Behaviors.same

        case msg: GetSender =>
          hostBehavior(Some(msg.sender), hostController)
      }
    }
  }

  def participantBehavior(sender: Option[ActorRef[WsMsgFront]] = None)(
    implicit stashBuffer: StashBuffer[RmCommand],
    timer: TimerScheduler[RmCommand]
  ): Behavior[RmCommand] = {
    Behaviors.receive[RmCommand]{ (ctx, msg) =>
      msg match {
        case msg: GetSender =>
          participantBehavior(Some(msg.sender))

      }
    }
  }

  def buildWebsocket(
                    ctx: ActorContext[RmCommand],
                    url: String,
                    controller: Either[ParticipantCotroller, HostController],
                    successFunc: => Unit,
                    failureFunc: => Unit
                    )(
    implicit timer: TimerScheduler[RmCommand]
  ): Unit = {
    log.debug(s"build websocket with roomManager: $url")
    val wsFlow = Http().webSocketClientFlow(WebSocketRequest(url))
    val source = getSource(ctx.self)

    val sink = controller match {
      case Right(hc) => getRMSink(hostController = Some(hc))
      case Left(pc) => getRMSink(participantCotroller = Some(pc))
    }

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

  def getSource(rmManager: ActorRef[RmCommand]): Source[BinaryMessage.Strict, ActorRef[WsMsgFront]] =
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
               hostController: Option[HostController] = None,
               participantCotroller: Option[ParticipantCotroller] = None
               )(
    implicit timer: TimerScheduler[RmCommand]
  ): Sink[Message, Future[Done]] = {
    Sink.foreach[Message]{
      case TextMessage.Strict(msg) =>
        hostController.foreach(_.wsMessageHandler(TextMsg(msg)))
        participantCotroller.foreach(_.wsMessageHandler(TextMsg(msg)))

      case BinaryMessage.Strict(bMsg) =>
        val buffer = new MiddleBufferInJvm(bMsg.asByteBuffer)
        val message = bytesDecode[WsMsgRm](buffer) match {
          case Right(rst) => rst
          case Left(_) => DecodeError
        }
        hostController.foreach(_.wsMessageHandler(message))
        participantCotroller.foreach(_.wsMessageHandler(message))

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
          hostController.foreach(_.wsMessageHandler(message))
          participantCotroller.foreach(_.wsMessageHandler(message))
        }

      case _ => //do nothing
    }
  }




}

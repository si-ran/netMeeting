package org.seekloud.netMeeting.roomManager.core

import java.util.{Date, Properties}
import java.text.SimpleDateFormat

import akka.actor.typed.{ActorRef, Behavior}
import akka.actor.typed.scaladsl.{ActorContext, Behaviors, StashBuffer, TimerScheduler}
import javax.mail.Message.RecipientType
import javax.mail.{Authenticator, PasswordAuthentication, Session, Transport}
import javax.mail.internet.{InternetAddress, MimeBodyPart, MimeMessage, MimeMultipart}
import org.seekloud.netMeeting.protocol.ptcl.ClientProtocol.SendEmailRsp
import org.seekloud.netMeeting.roomManager.Boot.executor
import org.seekloud.netMeeting.roomManager.common.AppSettings
import org.slf4j.LoggerFactory

import scala.concurrent.duration.FiniteDuration
import scala.concurrent.duration._
/**
  * User: si-ran
  * Date: 2020/2/26
  * Time: 0:48
  */
object EmailActor {
  private val log = LoggerFactory.getLogger(this.getClass)

  trait Command

  private case class TimeOut(msg: String) extends Command

  final case class SendEmail(
    emailTo: String,
    from: String,
    to: String,
    time: Long,
    roomId: String,
    replyTo: ActorRef[SendEmailRsp]
  ) extends Command

  private final case class SwitchBehavior(
    name: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error")
  ) extends Command

  private final case object BehaviorChangeKey

  private[this] def switchBehavior(
    ctx: ActorContext[Command],
    behaviorName: String,
    behavior: Behavior[Command],
    durationOpt: Option[FiniteDuration] = None,
    timeOut: TimeOut = TimeOut("busy time error"))
    (implicit stashBuffer: StashBuffer[Command],
      timer: TimerScheduler[Command]): Behavior[Command] = {
    timer.cancel(BehaviorChangeKey)
    durationOpt.foreach(timer.startSingleTimer(BehaviorChangeKey, timeOut, _))
    stashBuffer.unstashAll(ctx, behavior)
  }

  private def busy()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] =
    Behaviors.receive[Command] { (ctx, msg) =>
      msg match {
        case SwitchBehavior(name, b, durationOpt, timeOut) =>
          switchBehavior(ctx, name, b, durationOpt, timeOut)

        case TimeOut(m) =>
          log.debug(s"${ctx.self.path} is time out when busy, msg=$m")
          Behaviors.stopped

        case x =>
          stashBuffer.stash(x)
          Behavior.same

      }
    }

  def init(): Behavior[Command] =
    Behaviors.setup[Command] { ctx =>
      log.info(s"email actor is starting...")
      implicit val stashBuffer: StashBuffer[Command] = StashBuffer[Command](Int.MaxValue)
      Behaviors.withTimers[Command] { implicit timer =>
        switchBehavior(ctx, "idle", idle())
      }
    }

  private def idle()(
    implicit stashBuffer: StashBuffer[Command],
    timer: TimerScheduler[Command]
  ): Behavior[Command] ={
    Behaviors.receive[Command]{(ctx, msg) =>
      msg match {
        case x@SendEmail(emailTo, from, to, time, roomId, replyTo) =>
          log.info(s"I receive msg:$x")
          val session = getEbuptSession
          val message = new MimeMessage(session)
          message.setFrom(new InternetAddress(AppSettings.emailAddresserEmail))
          message.setRecipient(RecipientType.TO,new InternetAddress(emailTo))
          message.setSubject(s"netMeeting会议邀请")
          message.setSentDate(new Date)
          val mainPart = new MimeMultipart
          val html = new MimeBodyPart
          val content = getRegisterEamilHtml(from, to, time, roomId)
          html.setContent(content, "text/html; charset=utf-8")
          mainPart.addBodyPart(html)
          message.setContent(mainPart)
          Transport.send(message)
          replyTo ! SendEmailRsp()
          Behaviors.same

        case _ =>
          Behaviors.same
      }
    }
  }

  def getProperties = {
    val p = new Properties
    p.put("mail.smtp.host", AppSettings.emailHost)
    p.put("mail.smtp.port", AppSettings.emailPort)
    p.put("mail.transport.protocol", "smtp")
    p.put("mail.smtp.auth", "true")
    p
  }

  def getEbuptSession = {
    Session.getInstance(getProperties, new MyAuthenticator(AppSettings.emailAddresserEmail, "SKld1234!@#$"))
  }

  def getRegisterEamilHtml(from: String, to: String, time: Long, roomId: String) = {
    val sb: StringBuilder = new StringBuilder
    sb.append("<html><head><meta http-equiv=\"Content-Type\" content=\"text/html; charset=utf-8\"/></head><body>")

    sb.append("""<table width="100%" bgcolor="#f4f9fd" cellpadding="0" cellspacing="10"><tbody>""")
    sb.append(s"""<tr>	<td height="50" valign="top"><b><font size="4" color="#555555" face="Arial, Helvetica, sans-serif">你好， <span style="border-bottom-width: 1px; border-bottom-style: dashed; border-bottom-color: rgb(204, 204, 204); z-index: 1; position: static;" t="7" onclick="return false;"  isout="1">${to}</span></font></b><br><font size="3" color="#555555" face="Arial, Helvetica, sans-serif">你接收到了来自${from}的一个会议邀请</font></td></tr>""")
    sb.append(s"""<tr>	<td height="50" valign="top"><font size="3" color="#555555" face="Arial, Helvetica, sans-serif">请于${new SimpleDateFormat().format(new Date(time))},参加位于房间Id:${roomId}的会议</font></td></tr>""")
    sb.append(s"""<tr>	<td height="40" valign="top">	<font size="3" color="#555555" face="Arial, Helvetica, sans-serif">祝使用愉快！<br>netMeeting <span style="border-bottom-width: 1px; border-bottom-style: dashed; border-bottom-color: rgb(204, 204, 204); position: relative;" >${new SimpleDateFormat().format(new Date())}<br>	</font></td></tr>""")
    sb.append("""</tbody></table>""")

    sb.append("</body></html>")
    sb.toString()
  }

  case class MyAuthenticator(userName: String, password: String) extends Authenticator {

    override def getPasswordAuthentication: PasswordAuthentication = {
      new PasswordAuthentication(userName, password)
    }
  }

}

import akka.actor.typed.DispatcherSelector
import akka.actor.{ActorSystem, Scheduler}
import akka.dispatch.MessageDispatcher
import akka.event.{Logging, LoggingAdapter}
import akka.stream.ActorMaterializer
import akka.util.Timeout

/**
  * User: cq
  * Date: 2020/1/16
  */
object Boot {
  import concurrent.duration._
  import org.seekloud.netMeeting.processor.common.AppSettings._

  implicit val system:ActorSystem = ActorSystem("processor",config)
  implicit val executor:MessageDispatcher = system.dispatchers.lookup("akka.actor.my-blocking-dispatcher")
  implicit val materializer: ActorMaterializer = ActorMaterializer()
  implicit val scheduler: Scheduler = system.scheduler
  implicit val timeout: Timeout = Timeout(20 seconds) // for actor asks

  val blockingDispatcher: DispatcherSelector = DispatcherSelector.fromConfig("akka.actor.my-blocking-dispatcher")

  val log: LoggingAdapter = Logging(system, getClass)

  def main(args: Array[String]): Unit = {

    log.info("Done")
  }
}

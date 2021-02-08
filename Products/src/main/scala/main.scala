import scala.language.postfixOps
import scala.util.{Success, Failure}
import scala.concurrent.Future
import scala.concurrent.Await
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorSystem, Props, ActorRef, PoisonPill}
import akka.pattern.ask
import akka.util.Timeout
import java.util.concurrent.TimeoutException

object Main extends App{

    import Shop._

    val system = ActorSystem("main")
    val shop1: ActorRef = system.actorOf(Props[Shop], name="shop1")
    val shop2: ActorRef = system.actorOf(Props[Shop], name="shop2")
    // Dead Letter Subscriber to retrieve dead letters
    val deadLettersSubscriber: ActorRef = system.actorOf(Props[Shop], name = "dead-letters-subscriber")
    system.eventStream.subscribe(deadLettersSubscriber, classOf[DeadLetter])

    shop1 ! Add("prod1",12.30)
    shop2 ! Add("prod2",50.01)
    shop1 ! Add("prod3",78.30)
    shop2 ! Add("prod4",69.30)

    shop2 ! Remove(2)

    shop1 ! Get(1)

    // Implicit timeout to perform ask pattern
    try {
        implicit val timeout: Timeout = Timeout(5 seconds)
        val futureDisplay: Future[Any] = shop1 ? Display
        val resultDisplay = Await.result(futureDisplay, timeout.duration)
    }
    catch {
        case ex: TimeoutException => {
            println("TimeoutException: " + ex)
        }
        case ex: Throwable => {
            println("Exception: " + ex)
        }
    }
    
    shop2 ! PoisonPill // One way to stop a process

    system.stop(shop1) // Another way

    // Process won't finish, do Ctrl+C to terminate
    system.deadLetters ! "Dead Message"

    system.terminate

}

case class DeadLetter(message: Any, sender: ActorRef, recipient: ActorRef)
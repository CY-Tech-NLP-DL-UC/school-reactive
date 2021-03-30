import scala.concurrent.Future
import scala.concurrent.ExecutionContext.Implicits.global
import scala.util.{Success, Failure}
import akka.http.scaladsl.marshallers.sprayjson.SprayJsonSupport
import spray.json.DefaultJsonProtocol

// Event class
case class Event(name: String, tickets: Int)
{
    override def toString = "Event : "+name+" | tickets : "+tickets
}

// Json format instances into a support trait
trait EventJsonSupport extends SprayJsonSupport with DefaultJsonProtocol
{
    implicit val eventFormat = jsonFormat2(Event)
}
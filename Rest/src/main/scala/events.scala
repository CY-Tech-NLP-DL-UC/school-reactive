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

// Database of events
object Database
{
    var events=List.empty[Event]

    def add(name:String, tickets:Int)=
    {
        events = Event(name, tickets)::events
    }

    def gets:Future[List[Event]]=
    {
        Future { events }
    }

    def displayAll:Unit=
    {
        gets.onComplete{
            case Success(events) => events.foreach(println)
            case Failure(e) => println(s"Exception: $e")
        }
    }
}

// Json format instances into a support trait
trait EventJsonSupport extends SprayJsonSupport with DefaultJsonProtocol
{
    implicit val eventFormat = jsonFormat2(Event)
}
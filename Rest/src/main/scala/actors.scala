import akka.actor.{ActorSystem, Actor, ActorRef, ActorLogging, Props}
import scala.concurrent.duration._
import akka.pattern.ask
import akka.util.Timeout

// Rest api actions
case class Ticket(name: String)
{
    var id: Int = Ticket.next
    override def toString = "Ticket : "+name+" | number : "+id
}

case object Ticket{
     // counter for index attribute
     var cpt: Int=0
     def next={cpt=cpt+1; cpt}
}

case class TicketVectors(tickets: Vector[Ticket])
case class EventCreated(name: String, tickets: Int)
case class EventDeleted(name: String)

// Rest api + Box office actions
case class CreateEvent(name: String, tickets: Int)
case class RemoveEvent(name: String)
case class TicketRequest(name: String, tickets: Int)
case object GetAllEvents

// Ticket seller actions
case class Buy(name: String, tickets: Int)

// Rest api actor
class RestApiActor(system: ActorSystem) extends Actor with ActorLogging
{
    var boxOffice = system.actorOf(Props(new BoxOfficeActor(system)))
    var origin: ActorRef = null
    implicit val timeout: Timeout = 5.seconds // Request time out.

    override def receive: Receive =
    {
        case CreateEvent(name, tickets)     =>  log.info(s"RestAPI asks box office to create the event $name ($tickets)")
                                                boxOffice ! CreateEvent(name, tickets) 
                                                origin = sender

        case EventCreated(name, tickets)    =>  log.info("RestAPI acknowledges the event creation")
                                                origin ! Map("event" -> name, "tickets" -> tickets)

        case RemoveEvent(name)  =>  log.info(s"RestAPI asks box office to remove the event $name")
                                    boxOffice ! RemoveEvent(name)
                                    origin = sender

        case EventDeleted(name) =>  log.info("RestAPI acknowledges the event deletion")
                                    origin ! name

        case TicketRequest(name, tickets)   =>  log.info(s"RestAPI asks box office to buy $tickets tickets for $name")
                                                origin = sender
                                                boxOffice ! TicketRequest(name, tickets)

        case TicketVectors(tickets)  => var results = Map("event" -> sender.path.name, "entries" -> tickets.map{t => Map("id" -> t.id)})
                                        log.info(s"RestAPI receives $results")
                                        origin ! results
        
        case GetAllEvents =>    val events = boxOffice ? GetAllEvents
                                log.info("RestAPI asks box office about all available events")
                                sender ! events

        case e => log.warning(s"Warning: RestAPI received $e in receive")
    }
}

// Box office actor
class BoxOfficeActor(system: ActorSystem) extends Actor with ActorLogging
{
    var ticketSellers: Vector[ActorRef] = Vector.empty

    override def receive: Receive =
    {
        case CreateEvent(name, tickets) =>  var ticketSeller = system.actorOf(Props(new TicketSellerActor(sender, tickets)), name)
                                            log.info(s"Box office creates an event $name ($tickets)")
                                            ticketSellers = ticketSellers:+ticketSeller
                                            sender ! EventCreated(name, tickets)

        case RemoveEvent(name)  =>  ticketSellers = ticketSellers.filter(_.path.name != name)
                                    log.info(s"Box office deletes the event $name")
                                    sender ! EventDeleted(name)

        case TicketRequest(name, tickets)  =>   var ticketSellerRef = ticketSellers.find(_.path.name == name)
                                                if(ticketSellerRef.isDefined)
                                                {
                                                    log.info(s"Box office asks ticket seller to buy $tickets tickets")
                                                    ticketSellerRef.get ! Buy(name, tickets)
                                                }
                                                else
                                                {
                                                    log.error(s"TicketRequest : ticketSeller is null")
                                                }

        case GetAllEvents => sender ! ticketSellers.map(_.path.name)

        case e => log.warning(s"Warning: BoxOffice received $e in receive")
    }
}

// Ticket seller actor
class TicketSellerActor(ref: ActorRef, var tickets: Int) extends Actor with ActorLogging
{
    override def receive: Receive =
    {
        case Buy(name, n_tickets) =>  if(n_tickets > tickets)
                                {
                                    // Return Not enought tickets message
                                    log.info("Not enough tickets!")
                                }
                                else
                                {
                                    // Send tickets
                                    var ticket_array: Vector[Ticket] = Vector.empty
                                    for (i <- 0 to n_tickets-1)
                                    {
                                        ticket_array = ticket_array:+(new Ticket(name))
                                    }

                                    tickets = tickets - n_tickets
                                    log.info(s"Ticket seller sends $n_tickets")
                                    ref ! TicketVectors(ticket_array)
                                }
    
        case e => log.warning(s"Warning: TicketSeller received $e in receive")
    }
}
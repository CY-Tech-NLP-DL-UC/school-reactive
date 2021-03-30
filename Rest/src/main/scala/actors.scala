import akka.actor.{ActorSystem, Actor, ActorRef, ActorLogging, Props}

// Rest api actions
case class Ticket(name: String, id: Int)
{
    override def toString = "Ticket : "+name+" | number : "+id
}
case class TicketVectors(tickets: Vector[Ticket])

// Rest api + Box office actions
case class CreateEvent(name: String, tickets: Int)
case class TicketRequest(name: String, tickets: Int)

// Ticket seller actions
case class Buy(tickets: Int)

// Rest api actor
class RestApiActor(system: ActorSystem) extends Actor with ActorLogging
{
    var boxOffice = system.actorOf(Props(new BoxOfficeActor(system)))

    override def receive: Receive =
    {
        case CreateEvent(name, tickets) =>  log.info(s"RestAPI asks box office to create the event $name ($tickets)")
                                            boxOffice ! CreateEvent(name, tickets)  
    
        case TicketRequest(name, tickets)   =>  log.info(s"RestAPI asks box office to buy $tickets tickets for $name")
                                                boxOffice ! TicketRequest(name, tickets)

        case TicketVectors(tickets)  => log.info(s"RestAPI receives $tickets")
                                        Map("event" -> tickets(0).name, "entries" -> tickets.map{t => Map("id" -> t.id)})

        case e => log.warning(s"Warning: RestAPI received $e in receive")
    }
}

// Box office actor
class BoxOfficeActor(system: ActorSystem) extends Actor with ActorLogging
{
    var ticketSellers: Vector[ActorRef] = Vector.empty

    override def receive: Receive =
    {
        case CreateEvent(name, tickets) =>  var ticketSeller = system.actorOf(Props(new TicketSellerActor(tickets)), name)
                                            log.info(s"Box office creates an event $name ($tickets)")
                                            ticketSellers = ticketSellers:+ticketSeller

        case TicketRequest(name, tickets)  =>   var ticketSellerRef = ticketSellers.find(_.path.name == name)
                                                var ticketSeller = ticketSellerRef.getOrElse(None)
                                                if(ticketSellerRef != None)
                                                {
                                                    log.info(s"Box office asks ticket seller to buy $tickets tickets")
                                                    //ticketSeller ! Buy(tickets)
                                                }
                                                else
                                                {
                                                    log.error(s"TicketRequest : ticketSeller is null")
                                                }
    
        case e => log.warning(s"Warning: BoxOffice received $e in receive")
    }
}

// Ticket seller actor
class TicketSellerActor(tickets: Int) extends Actor with ActorLogging
{
    override def receive: Receive =
    {
        case Buy(n_tickets) =>  if(n_tickets > tickets)
                                {
                                    // Return Not enought tickets message
                                }
                                else
                                {
                                    // Send tickets
                                }
    
        case e => log.warning(s"Warning: TicketSeller received $e in receive")
    }
}
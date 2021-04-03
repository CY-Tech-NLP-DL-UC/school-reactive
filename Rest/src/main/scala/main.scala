import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import scala.concurrent.{ExecutionContextExecutor, Future, Await}
import scala.concurrent.duration._
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import scala.util.{Try, Success, Failure}
import akka.pattern.ask
import akka.util.Timeout

object eventAPI extends App with EventJsonSupport
{
    // System creation
    implicit val system: ActorSystem = ActorSystem("EventsRouter")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    var restapi = system.actorOf(Props(new RestApiActor(system)))

    implicit val timeout: Timeout = 5.seconds // Request time out.

    // Routes definition
    val routes: Route =
        pathPrefix("events")
        {
            concat(
                pathEnd
                {
                    concat(
                        get
                        {
                            val events = restapi ? GetAllEvents

                            Try(Await.result(events, 5.seconds)) match
                            {
                                case Success(res)   =>  complete(s"$res are available.")
                                case Failure(e)     =>  complete(s"Timeout : $e")
                            }
                        }
                    )
                },
                path(Segment)
                {
                    name => 
                    concat(
                        post
                        {
                            entity(as[Event])
                            {
                                event => val newEvent = restapi ? CreateEvent(name, event.tickets)
                                Try(Await.result(newEvent, 5.seconds)) match
                                {
                                    case Success(res)   =>  complete(s"$res has been created.")
                                    case Failure(e)     =>  complete(s"Timeout : $e")
                                }
                            }
                        },
                        delete
                        {
                            val deletedEvent = restapi ? RemoveEvent(name)

                            Try(Await.result(deletedEvent, 5.seconds)) match
                            {
                                case Success(res)   =>  complete(s"$res has been deleted.")
                                case Failure(e)     =>  complete(s"Timeout : $e")
                            }
                        },
                        path(Segment)
                        {
                            tickets =>
                            get
                            {
                                val boughtTickets = restapi ? TicketRequest(name, tickets.toInt)
                                Try(Await.result(boughtTickets, 5.seconds)) match
                                {
                                    case Success(res)   =>  complete(s"Tickets : $res")
                                    case Failure(e)     =>  complete(s"Timeout : $e")
                                }
                            }
                        }
                    )
                }
            )
        }

    val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
    futureBinding.onComplete
    {
        case Success(binding)   =>  val address = binding.localAddress
                                    system.log.info("Server ###online}:{}/", address.getHostString, address.getPort)

        case Failure(ex)    =>  system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
                                system.terminate
    }
}
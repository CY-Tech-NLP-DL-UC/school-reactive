import akka.actor.{ActorSystem, Props}
import akka.stream.ActorMaterializer
import scala.concurrent.ExecutionContextExecutor
import akka.http.scaladsl.Http
import akka.http.scaladsl.server.Route
import akka.http.scaladsl.server.Directives._
import scala.util.{Success, Failure}

object eventAPI extends App with EventJsonSupport
{
    // System creation
    implicit val system: ActorSystem = ActorSystem("EventsRouter")
    implicit val materializer: ActorMaterializer = ActorMaterializer()
    implicit val ec: ExecutionContextExecutor = system.dispatcher

    var restapi = system.actorOf(Props(new RestApiActor(system)))

    restapi ! CreateEvent("RHCP", 2500)

    // Routes definition
    val routes: Route =
        concat(
            post
            {
                path("saveEvent")
                {
                    post{entity(as[Event])
                    {
                        event => restapi ! CreateEvent(event.name, event.tickets)
                        complete(event + "has been created")
                    }}
                }
            }
        )

    val futureBinding = Http().bindAndHandle(routes, "localhost", 8080)
    futureBinding.onComplete
    {
        case Success(binding)   =>  val address = binding.localAddress
                                    system.log.info("Server ###online}:{}/", address.getHostString, address.getPort)

        case Failure(ex)    =>  system.log.error("Failed to bind HTTP endpoint, terminating system", ex)
                                system.terminate
    }
}
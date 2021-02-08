import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorSystem, Props, ActorRef}

object Centralised extends App
{
    val system = ActorSystem("philosophersSystem")
    val supervisor = system.actorOf(Props(new Supervisor(5, system)), "supervisor")

    for (i <- 0 to 4)
    {
        val philosopher = system.actorOf(Props(new Philosopher(i, supervisor)), "philosopher_" + i)
        supervisor ! Register(i, philosopher)
    }
    supervisor ! EndRegistering

}

object Distributed extends App
{
    val system = ActorSystem("monkSystem")

    val hashis = for(i <- 0 until 5) yield system.actorOf(Props[Hashi], "Hashi-" + i)

    var monks: Array[ActorRef] = new Array[ActorRef](5)
    for(i <- 0 to (monks.length -2))
    {
        monks(i) = system.actorOf(Props(classOf[Monk], i, hashis(i), hashis(i+1)), "Monk-"+i)
    }
    monks(4) = system.actorOf(Props(classOf[Monk], 4, hashis(4), hashis(0)), "Monk-4")
    monks.foreach(_ ! Monk.Thinking)

    Thread.sleep(30000) // Waiting 30 seconds before finishing
    system.terminate
}
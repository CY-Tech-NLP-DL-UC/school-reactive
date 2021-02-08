import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, PoisonPill, ActorLogging, ActorSystem}
import scala.util.Random

// ---------------- //
//    SUPERVISOR    //
// ---------------- //

case class Register(id: Int, philosopher: ActorRef)

// States
object EndRegistering
object Registered

class Supervisor(n_philosophers: Int, system: ActorSystem) extends Actor with ActorLogging
{
    import context._

    var spoons: Array[Int] = Array.fill(n_philosophers){0} // 0 for each available spoon
    var philosophers: Array[ActorRef] = Array.fill(n_philosophers){self}
    def left(id: Int) = {if (id == 0) n_philosophers - 1 else id - 1} // Index of left neighbor
    def right(id: Int) = {if (id == n_philosophers - 1) 0 else id + 1} // Index of right neighbor

    def receive=register

    def register: Receive =
    {
        case Register(id, philosopher) =>   log.info(s"Supervisor registers philosopher $id")
                                            philosophers(id) = philosopher

        case EndRegistering => for (i <- 0 to n_philosophers - 1)
                                {
                                    philosophers(i) ! Thinking
                                }
                                log.info("Registration complete")
                                become(supervise)
        
        case e => log.warning(s"Warning: Supervisor received $e in register")
    }

    def supervise: Receive =
    {
        case Requesting(id) =>  var l = left(id)
                                var r = right(id)
                                if (spoons(l) != 1 && spoons(r) != 1)
                                {
                                    spoons(id) = 1 // This spoon is not available
                                    philosophers(id) ! Allowed
                                }
                                else {
                                    spoons(id) = -1 // This spoon is waiting
                                }
        
        case Finishing(id)  =>  spoons(id) = 2 // Finished
                                philosophers(id) = null // Remove philosopher who finished
                                var l = left(id)
                                var r = right(id)
                                if (spoons(l) == -1 && spoons(left(l)) != 1)
                                {
                                    // Left spoon can take the lead
                                    spoons(l) = 1
                                    philosophers(l) ! Allowed
                                }
                                if (spoons(r) == -1 && spoons(right(r)) != 1)
                                {
                                    // Right spoon can take the lead
                                    philosophers(r) ! Allowed
                                }

                                if (philosophers.filter(_ != null).size == 0)
                                {
                                    system.terminate
                                }
        
        case e => log.warning(s"Warning: Supervisor received $e in register")
    }
}

// ---------------- //
//    PHILOSOPHER   //
// ---------------- //

// States
case object Thinking // Every philosopher starts by thinking
case class Requesting(id: Int) // Philosopher requests to eat
case class Finishing(id: Int) // Philosopher finished eating
case object Allowed // Philosopher is allowed to eat

class Philosopher(id: Int, supervisor: ActorRef) extends Actor with ActorLogging
{
    import context._

    def receive=think

    def think: Receive =
    {
        case Thinking =>    log.info(s"Philosopher $id starts to think")
                            Thread.sleep(Random.nextInt(7000)) // Sleeps for a random time
                            supervisor ! Requesting(id)
                            log.info(s"Philosopher $id is now waiting")
                            become(waiting)
        
        case e => log.warning(s"Warning: Philosopher ($id) received $e in think")
    }

    def waiting: Receive =
    {
        case Allowed => log.info(s"Philosopher $id starts eating")
                        Thread.sleep(Random.nextInt(10000)) // Time to eat
                        supervisor ! Finishing(id)
                        log.info(s"Philosopher $id finished eating")
                        self ! PoisonPill // Terminates thread

        case e => log.warning(s"Warning: Philosopher ($id) received $e in wait")
    }
}
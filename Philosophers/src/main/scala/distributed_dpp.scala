import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, PoisonPill, ActorLogging, ActorSystem}
import scala.util.Random

// ---------------- //
//       HASHI      //
// ---------------- //

case object Hashi
{
    sealed trait Request
    case class PickUp(monk: ActorRef) extends Request
    case class PutDown(monk: ActorRef) extends Request
}

case class Hashi() extends Actor with ActorLogging
{
    import Hashi._
    import Monk._
    import context._

    def receive = available

    def available: Receive =
    {
        case PickUp(monk: ActorRef) =>  log.debug(self.path + " available: Picked up by %s".format(monk.path))
                                        become(taken(monk))
                                        monk ! ReceivedHashi(self)

        case e => log.warning(s"Warning: Hashi received $e in available")
    }

    def taken(monk: ActorRef): Receive =
    {
        case PickUp(other_monk) =>  log.debug(self.path + " taken : Picked up by %s".format(other_monk.path))
                                    other_monk ! NotAllowed(self)

        case PutDown(monk) =>   log.debug(self.path + " taken : Put down by %s".format(monk.path))
                                become(available)

        case e => log.warning(s"Warning: Hashi received $e in taken")
    }
}

// ---------------- //
//        MONK      //
// ---------------- //

case object Monk
{
    sealed trait Request
    case object Thinking extends Request
    case class ReceivedHashi(hashi: ActorRef) extends Request
    case class NotAllowed(hashi: ActorRef) extends Request
    case object FinishedEating extends Request
}

case class Monk(id: Int, rightHashi: ActorRef, leftHashi: ActorRef) extends Actor with ActorLogging
{
    import Monk._
    import Hashi._
    import context._

    def receive = thinking

    def thinking: Receive =
    {
        case Thinking =>    log.debug(self.path + " thinking: Thinking")
                            log.info(s"Monk $id starts to think")
                            Thread.sleep(Random.nextInt(3000)) // Sleeps for a random time
                            become(hungry)
                            log.info(s"Monk $id is now hungry")
                            rightHashi ! PickUp(self)
                            leftHashi ! PickUp(self)
        
        case e => log.warning(s"Warning: Monk ($id) received $e in think")
    }

    def hungry: Receive =
    {
        case ReceivedHashi(`rightHashi`) =>   log.debug(self.path + " hungry: Received rightHashi")
                                            log.info(s"Monk $id got his right hashi")
                                            become(waitForSecondHashi(leftHashi, rightHashi))
        
        case ReceivedHashi(`leftHashi`)  =>   log.debug(self.path + " hungry: Received leftHashi")
                                            log.info(s"Monk $id got his left hashi")
                                            become(waitForSecondHashi(rightHashi, leftHashi))
        
        case NotAllowed(hashi) =>   log.debug(self.path + s" hungry: Not Allowed ($hashi)")
                                    log.info(s"Monk $id was not allowed to pick up $hashi")
                                    become(zeroHashi)

        case e => log.warning(s"Warning: Monk ($id) received $e in hungry")
    }

    def waitForSecondHashi(missingHashi: ActorRef, currentHashi: ActorRef): Receive =
    {
        case ReceivedHashi(missingHashi) => log.debug(self.path + s" waitForSecondHashi: Received $missingHashi")
                                            log.info(s"Monk $id has picked up $missingHashi and starts to eat")
                                            become(eating)
                                            Thread.sleep(Random.nextInt(10000)) // Time to eat
                                            self ! FinishedEating
        
        case NotAllowed(hashi)  =>  log.debug(self.path + s" waitForSecondHashi: Not Allowed ($hashi)")
                                    log.info(s"Monk $id puts down $currentHashi")
                                    become(thinking)
                                    currentHashi ! PutDown(self)
                                    self ! Thinking

        case e => log.warning(s"Warning: Monk ($id) received $e in waitForSecondHashi")
    }

    def zeroHashi: Receive =
    {
        case ReceivedHashi(hashi)   =>  log.debug(self.path + s" zeroHashi: Received $hashi")
                                        become(thinking)
                                        hashi ! PutDown(self)
                                        self ! Thinking

        case NotAllowed(hashi)  =>  log.debug(self.path + s" zeroHashi: Not Allowed ($hashi)")
                                    become(thinking)
                                    self ! Thinking

        case e => log.warning(s"Warning: Monk ($id) received $e in zeroHashi")
    }

    def eating: Receive =
    {
        case FinishedEating =>  log.debug(self.path + " eating: Finished Eating")
                                log.info(s"Monk $id finished eating")
                                rightHashi ! PutDown(self)
                                leftHashi ! PutDown(self)
                                self ! PoisonPill // Should start to think again

        case e  =>  log.warning(s"Warning: Monk ($id) received $e in eating")
    }
}
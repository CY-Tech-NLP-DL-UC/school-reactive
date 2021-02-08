import scala.util.{Success, Failure}
import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.typed.ActorRef
import akka.actor.typed.ActorSystem
import akka.actor.typed.Behavior
import akka.actor.typed.scaladsl.Behaviors
import akka.actor.typed.scaladsl.AskPattern._
import DataBase._

object ShopTyped{
    trait Request
    final case class Add(name: String, qte: Double) extends Request
    final case class Remove(id: Int) extends Request
    final case class Get(id: Int) extends Request
    final case class Display() extends Request
    final case class Stop() extends Request

    var db = DataBase

    def apply(): Behavior[Request] = 
        Behaviors.receive { (context, message) =>
            message match {
                case Add(name, qte) => {
                    context.log.info(s"add product $name ($qte)")
                    db.add(name, qte)
                    Behaviors.same
                }
                case Remove(id) => {
                    context.log.info(s"remove product $id")
                    db.remove(id)
                    Behaviors.same
                }
                case Get(id) => {
                    context.log.info(s"get product $id")
                    db.getProduct(id).onComplete{
                        case Success(prod) => println(prod)
                        case Failure(e) => println(s"exception = $e")
                    }
                    Behaviors.same
                }
                case Display() => {
                    context.log.info("display all")
                    db.displayAll
                    Behaviors.same
                }
                case Stop() => {
                    context.log.info("stop shop actor")
                    Behaviors.stopped
                }
            }
        }
}

object Main2 extends App{
    import ShopTyped._
    val shop: ActorSystem[ShopTyped.Request] = ActorSystem(ShopTyped(), "StoreAkka")

    shop ! Add("prod1", 32.5)
    shop ! Add("prod2", 22.5)
    shop ! Add("prod3", 42.5)
    shop ! Add("prod4", 32.5)

    shop ! Get(3)
    shop ! Remove(3)
    shop ! Display()

    shop ! Stop()
}
import akka.actor.{Actor}
import DataBase._

case class Shop() extends Actor {
    import Shop._

    var db = DataBase

    def receive =
    {
        case Add(name, qte) => db.add(name, qte)
        case Remove(id) => db.remove(id)
        case Get(id) => db.getProduct(id)
        case Display => db.displayAll
        case x => println("Error: type is " + x)
    }

    override def preStart()
    {
        println(s"Instance $self is created, about to getting started")
    }

    override def preRestart(reason: Throwable, message: Option[Any])
    {
        println(s"PreRestart for $self")
    }

    override def postRestart(reason: Throwable)
    {
        println(s"PostRestart for $self")
    }

    override def postStop()
    {
        println(s"Instance $self is stopped")
    }



}

case object Shop {
    trait Request

    case class Add(name:String, var qte:Double) extends Request
    case class Remove(id: Int) extends Request
    case class Get(id: Int) extends Request
    case class Display() extends Request
}
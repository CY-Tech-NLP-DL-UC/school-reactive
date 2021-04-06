import akka.actor._
import akka.cluster._
import com.typesafe.config._

object Main extends App
{
    // First node
    val conf = """
    akka {
    loglevel = INFO
    stdout-loglevel = INFO
    event-handlers = ["akka.event.Logging$DefaultLogger"]
    log-dead-letters = 0
    log-dead-letters-during-shutdown = off
    actor {
    provider = "akka.cluster.ClusterActorRefProvider"
    }
    remote {
    enabled-transports = ["akka.remote.netty.tcp"]
    log-remote-lifecycle-events = off
    netty.tcp {
    hostname = "127.0.0.1"
    hostname = 127.0.0.1
    port = 2551
    }
    }
    cluster {
    seed-nodes = [
    "akka.tcp://words@127.0.0.1:2551",
    "akka.tcp://words@127.0.0.1:2552",
    "akka.tcp://words@127.0.0.1:2553"
    ]
    roles = ["seed"]
    role {
    seed.min-nr-of-members = 1
    }
    }
    }
    """

    val config = ConfigFactory.parseString(conf)
    val seedSystem = ActorSystem("words", config)
    // Exiting paste mode, now interpreting

    val address = Cluster(seedSystem).selfAddress
    Cluster(seedSystem).leave(address)
}
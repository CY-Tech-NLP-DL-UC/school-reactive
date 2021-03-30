import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{ActorSystem, Props, ActorRef}

object WordCount extends App
{
    implicit val system = ActorSystem("Router")

    val messageSize = 4

    // create master
    val master = system.actorOf(Props(new MasterActor(messageSize)), name = "master")

    // create three workers
    val worker1 = system.actorOf(Props(new WorkerActor(master)), name = "worker1")
    val worker2 = system.actorOf(Props(new WorkerActor(master)), name = "worker2")
    val worker3 = system.actorOf(Props(new WorkerActor(master)), name = "worker3")

    // give the master two workloads
    val workload1 = List.fill(1)("Akka Technologies est un groupe d'ingénierie et de conseil en technologies, fondé et dirigé par Maurice Ricci. Le groupe est positionné sur l'ensemble des secteurs d'activités industriels et tertiaires, à savoir notamment aéronautique, automobile, énergie, ferroviaire, défense, spatial, système d'information, télécommunications. Les sociétés du groupe disposent de plus de cinquante implantations en France ainsi qu'en Allemagne, Belgique, Espagne, Inde, Italie, Maroc, Roumanie, Royaume-Uni, Chine, Amérique du Nord et Suisse. Comme de nombreuses autres Entreprises de Services du Numérique (ESN), Akka Technologies effectue des recrutements massifs pour suivre sa croissance et augmenter ses effectifs. Le groupe a la particularité de posséder un centre de recherche et développement interne nommé Akka Research. Akka Technologies est le partenaire titre de l'écurie de sport automobile Akka-ASP team. Comme beaucoup de grands groupes, Akka Technologies souffre de quelques controverses comme des cas de licenciements abusifs, dégradation des acquis des salariés après le rachat de leur société et le salaire de son dirigeant. À l'opposé, le groupe est connu pour s'investir dans des activités de partenariat et de mécénat dans le milieu associatif et culturel, certaines de ses activités durent depuis plusieurs années.")

    master ! Workload(workload1)

    system.terminate()
}
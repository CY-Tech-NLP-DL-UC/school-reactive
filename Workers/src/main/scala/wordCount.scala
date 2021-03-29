import scala.concurrent.ExecutionContext.Implicits.global
import akka.actor.{Actor, ActorRef, PoisonPill, ActorLogging, ActorSystem, Props}
import scala.util.Random
import akka.routing.BroadcastPool
import scala.collection.mutable.ListBuffer

// messages defined
case class WorkerIsAvailable() // worker announces itself to master
case class WorkIsAvailable() // master announces to workers there is work
case class ReadyForOneWorkItem() // worker asks master for a work item
case class JobDone(job: Map[String, Int]) // worker sends the result of his work
case class AssignOneWorkItem(item: List[String]) // master sends worker one work item
case class Workload(s: List[String]) // master gets a workload from somewhere

// master actor
class MasterActor(messageSize: Int) extends Actor with ActorLogging {
  var workitemptr = 0
  var workers = new ListBuffer[ActorRef] // a buffer to hold the workers
  var workitems: List[List[String]] = List[List[String]]() // a buffer to hold the work items
  var wordCount: Map[String, Int] = Map[String, Int]()
  var counter: Int = 0

override def receive: Receive = {
    case Workload(items: List[String]) => // a new workload has arrived
    {
      var splitItems = items(0).split("\\s+").toList
      var messages: List[List[String]] = List(List())

      for (i <- 0 to (splitItems.size - messageSize) by messageSize)
      {
          var slice = splitItems.slice(i, i + messageSize)

          messages = slice :: messages
      }

      var remainder = splitItems.size % messageSize
      if (remainder != 0)
      {
          var slice = splitItems.slice(splitItems.size - remainder, splitItems.size)
          messages = slice :: messages
      }
      workitems = messages
      
      workers.foreach { p => p ! WorkIsAvailable }
    }
    case WorkerIsAvailable => // a worker says he's available for new workloads
      workers += sender()
    case ReadyForOneWorkItem => // a worker wants work from current workload
      if (workitemptr < workitems.size) {
        sender ! AssignOneWorkItem(workitems(workitemptr))
        workitemptr = workitemptr + 1
      }
    case JobDone(mappedWords: Map[String, Int]) =>
        wordCount = wordCount ++ mappedWords.map{case (k, i) => k -> (i + wordCount.getOrElse(k, 0))}
        counter += 1
        if (counter == workitems.size)
        {
            log.info(s"Here is the word count: $wordCount")
        }
  }
}

// worker actor
class WorkerActor(master: ActorRef) extends Actor with ActorLogging {
  // when the actor starts, we register with the master. Also, we
  // tell it we are ready for work, in case a workload is in progress
  override def preStart() {
    master ! WorkerIsAvailable
    master ! ReadyForOneWorkItem
  }

  def countWords(text: List[String]) = text.groupBy(identity).mapValues(_.size).toMap

  override def receive: Receive = {
    case WorkIsAvailable => // master says a workload is available
      sender ! ReadyForOneWorkItem
    case AssignOneWorkItem(text: List[String]) => // master gives us one work item
      log.info(self.path.name+" is working on: "+text)
      var mappedWords = countWords(text)
      sender ! JobDone(mappedWords)
      sender ! ReadyForOneWorkItem
  }

}


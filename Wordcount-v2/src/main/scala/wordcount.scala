import akka.actor.{Actor, ActorRef, FSM, ActorLogging, ActorSystem}
import scala.concurrent.duration._
import scala.collection._
import scala.language.postfixOps

// events
final case class SetTarget(ref: ActorRef)
final case class Count(text: List[String])
case object Report

final case class Register(worker: ActorRef)
case object EndRegistration
final case class Work(text: String)

final case class Done(obj: Map[String, Int])

case object WorkIsAvailable
case object WorkerIsAvailable

// states
sealed trait WorkerState
case object Inactive extends WorkerState
case object Active extends WorkerState
sealed trait MasterState
case object Registration extends MasterState
case object Supervising extends MasterState
case object WorkDone extends MasterState

// data
sealed trait WorkerData
case object Uninitialized extends WorkerData
final case class Counter(target: ActorRef, data: Map[String, Int]) extends WorkerData
sealed trait MasterData
case object Empty extends MasterData
final case class MData(workers: Vector[ActorRef], data: Map[String, Int]) extends MasterData

class WorkerActor extends FSM[WorkerState, WorkerData]{
    
    def countWords(text: List[String]) = text.groupBy(identity).mapValues(_.size).toMap

    startWith(Inactive, Uninitialized)

    when(Inactive) {
        case Event(SetTarget(ref), Uninitialized) =>
            stay using Counter(ref, Map.empty[String, Int])
        case Event(WorkIsAvailable, c @ Counter(ref, _)) => {
            log.info("Work is available")
            ref ! WorkerIsAvailable
            goto(Active) using Counter(ref, Map.empty[String, Int])
        }
    }

    when(Active) {
        case Event(Count(text), c @ Counter(ref, _)) => {
            log.info(s"Working on $text")
            ref ! Done(countWords(text))
            goto(Inactive) using c.copy()
        }
            
    }

    whenUnhandled {
        case Event(e, s) =>
            log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
            stay
    }

    initialize()
}

class MasterActor extends FSM[MasterState, MasterData] {

    var segmented_work = List[List[String]]()
    var index: Int = 0
    var work_load: Int = 4
    var work_done: Int = 0

    startWith(Registration, Empty)

    when(Registration) {
        case Event(Register(worker), Empty) => {
            stay using MData(Vector.empty :+ worker, Map.empty[String, Int])
        }
        case Event(Register(worker), d @ MData(w, _)) => {
            stay using d.copy(workers = w:+worker)
        }
        case Event(EndRegistration, mdata @ MData(w, d)) => {
            log.info(s"Registred workers: $w")
            goto(Supervising) using mdata.copy()
        }
    }

    when(Supervising) {
        case Event(Work(text), mdata @ MData(w, _)) => {
            log.info(s"Input text: $text")
            // remove punctuation
            var cleaned_text = text.replaceAll("""[\p{Punct}]""", "")
            // string to list
            var text_array = cleaned_text.split("\\s+").toList
            // work segmentation
            var tmp_array: List[List[String]] = List(List())
            for (i <- 0 to (text_array.size - work_load) by work_load) {
                var slice = text_array.slice(i, i+work_load)
                tmp_array = slice :: tmp_array
            }
            var remainder = text_array.size % work_load
            if (remainder != 0) {
                var slice = text_array.slice(text_array.size - remainder, text_array.size)
                tmp_array = slice :: tmp_array
            }

            segmented_work = tmp_array
            w.foreach{worker => worker ! WorkIsAvailable}
            stay using mdata.copy()
        }
        case Event(WorkerIsAvailable, mdata @ MData(_, _)) => {
            if (index < segmented_work.size) {
                var work = segmented_work(index)
                log.info(s"Send work: $work")
                sender ! Count(work)
                index += 1
            }
            stay using mdata.copy()
        }
        case Event(Done(new_data), mdata @ MData(_, d)) => {
            work_done += 1
            var data_bearer = new_data ++ d.map{case (k, i) => k -> (i + new_data.getOrElse(k, 0))}
            if (work_done == segmented_work.size -1){
                goto(WorkDone) using mdata.copy(data = data_bearer)
            } else {
                sender ! WorkIsAvailable
                stay using mdata.copy(data = data_bearer)
            }
            
        }
    }

    when(WorkDone) {
        case Event(e, s) => stay
    }

    whenUnhandled {
        case Event(e, s) =>
            log.warning("received unhandled request {} in state {}/{}", e, stateName, s)
            stay
    }

    onTransition {
        case Supervising -> WorkDone => {
            stateData match {
                case MData(_, data) => log.info(s"Result of word count: $data")
                case _ => // nothing to do
            }
        }
    }

    initialize()
}
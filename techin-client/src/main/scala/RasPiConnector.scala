package techin.client


import akka.actor.{
  ActorRef,
  Actor
}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.Await
import techin.CheckConnect

case object Stop
case object Terminate

trait Subscriber {
  def connected(connect : RasPiConnectAddress) : Unit
}

class RasPiConnector(val actorRef : ActorRef) {
  var subscribers : List[Subscriber] = List[Subscriber]()

  def addSubscriber(subscriber: Subscriber){
    subscribers = subscriber :: subscribers
  }

  def connect(address : String) {
    val addr = RasPiConnectAddress(address)
    implicit val timeout = Timeout(5 seconds)
    val future = (actorRef ? addr)
    val c = Await.result(future, timeout.duration).asInstanceOf[RasPiConnectAddress]
    subscribers.foreach(_ connected c.asInstanceOf[RasPiConnectAddress])
  }
}

class RasPiConnectActor extends Actor {
  import context._

  def receive = {
    case connectAddress:RasPiConnectAddress =>
      val address = connectAddress.createPath("/user/techin")
      val selector = system.actorSelection(address)
      val requester = sender
      println("receive address!!!")
      become {
        case CheckConnect =>
          requester ! connectAddress
          unbecome()
        case Terminate =>
          context stop self
      }
      selector ! CheckConnect
    case Terminate =>
      context stop self
    
    
  }

}

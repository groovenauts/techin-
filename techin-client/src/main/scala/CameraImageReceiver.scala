package techin.client

import akka.actor.Actor

import techin.AddSubscriber
import techin.RemoveSubscriber
import techin.CameraImage
import techin.TechinTerminate
import techin.DecodeResult


case object GetImage
case object TerminateClient

class CameraImageReceiver(cameraImageAddress: RasPiConnectAddress) extends Actor {
  val cameraActor = context.actorSelection(cameraImageAddress
                                             createPath
                                             "/user/techin/qrCodeReader")
  var image : Option[Array[Byte]] = None
  cameraActor ! AddSubscriber(self)

  def receive = {
    case CameraImage(raw) =>
      image = Some(raw)

    case DecodeResult(result) =>

    case GetImage =>
      sender ! image

    case TerminateClient =>
      cameraActor ! RemoveSubscriber(self)
  }
}

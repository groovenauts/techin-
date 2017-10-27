package techin.client

import scalafx.scene.image.ImageView
import scalafx.embed.swing.SwingFXUtils

import akka.actor.{
  ActorRef
}
import akka.pattern.ask
import akka.util.Timeout

import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import java.io.ByteArrayInputStream
import javax.imageio.ImageIO

import scala.concurrent.Future

class CamerImageView(val cameraImageActor: ActorRef ) extends ImageView {
  implicit val timeout = Timeout(5 seconds)

  def reply : Future[Any] = (cameraImageActor ? GetImage).map {
    case Some(raw:Array[Byte]) =>
      val is = new ByteArrayInputStream(raw)
      val bufferedImage = ImageIO.read(is)
      val writableImage = SwingFXUtils.toFXImage(bufferedImage, null)
      image = writableImage
    case None =>
  }
}

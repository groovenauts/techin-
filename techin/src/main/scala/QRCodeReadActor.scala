package techin

import com.github.sarxos.webcam.Webcam
import com.github.sarxos.webcam.ds.v4l4j.V4l4jDriver
import akka.actor.{Actor, ActorRef}
import java.io.{ByteArrayOutputStream,
  ByteArrayInputStream}
import javax.imageio.ImageIO
import com.google.zxing.BinaryBitmap
import com.google.zxing.qrcode.{QRCodeReader => ZXingQRCodeReader}
import com.google.zxing.client.j2se.BufferedImageLuminanceSource
import com.google.zxing.common.HybridBinarizer

case class AddSubscriber(ref:ActorRef)
case class RemoveSubscriber(ref:ActorRef)
case class CameraImageSize(width:Int, height:Int)
case class CameraImage(raw: Array[Byte])
case object Reload
case object ReadFailed

case class CameraImagePack(raw: Array[Int])
case object CameraImagePackStart
case object CameraImagePackEnd
case class DecodeResult(text:String)


class QRCodeReader extends Actor {
  var subscribers = Set[ActorRef]()
  var captureMode = false
  var counter = 0


  if(System.getProperty("os.name").toLowerCase() == "linux"){ Webcam.setDriver(new V4l4jDriver()) }
  val webCam = Webcam.getDefault()
  webCam.open()

  implicit val executionContext = context.system.dispatcher

  def receive = {
    case AddSubscriber(ref) =>
      subscribers += ref

    case RemoveSubscriber(ref) =>
      subscribers -= ref

    case Reload =>
      println("reloading!")
      val bufferedImage = webCam.getImage()
      // get image from webcamera
      // send image to subscribers
      val os = new ByteArrayOutputStream()
      ImageIO.write(bufferedImage, "jpg", os)
      val array = os.toByteArray()
      subscribers.foreach(_ ! CameraImage(array))
      counter += 1
      // decode image
      if( counter > 10 ){
        val source = new BufferedImageLuminanceSource(bufferedImage)
        val bitmap = new BinaryBitmap(new HybridBinarizer(source))
        val reader = new ZXingQRCodeReader()
        try {
          val resultText = reader.decode(bitmap).getText
          bufferedImage.flush()
          // send to subscribers DecodeRsult
          println(s"reading count : ${counter}")
          subscribers.foreach(_ ! DecodeResult(resultText))
          sender ! DecodeResult(resultText)
          counter = 0
        } catch {
          case e:Exception =>
            sender() ! ReadFailed
        }
      }else{
        sender() ! ReadFailed
      }
      

  }
}

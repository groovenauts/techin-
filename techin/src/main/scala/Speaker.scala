package techin

import akka.actor.{Actor}
import java.io.{UnsupportedEncodingException, BufferedInputStream}
import javax.sound.sampled.{AudioSystem, Clip, DataLine, LineListener, LineEvent}

case object SayHoldUpQrCode
case object SayReadQrCode
case object SaySendedEmail
case class Said(obj: Any)

class Speaker extends Actor {
  val holdUpQRCodeClip = getAudioClip("/hold-up-qrcode.wav")
  val readedQRCodeClip = getAudioClip("/read-qrcode.wav")
  val sendedEmailCLip = getAudioClip("/sended-email.wav")

  def getAudioClip(filename:String) : Clip = {
    val src = getClass.getResourceAsStream(filename)
    val in = new BufferedInputStream(src)
    val stream = AudioSystem.getAudioInputStream(in)
    val info = new DataLine.Info(classOf[Clip], stream.getFormat)
    val clip : Clip = AudioSystem.getLine(info).asInstanceOf[Clip]
    clip.open(stream)
    stream.close()
    clip
  }

  def receive = {
    case SayHoldUpQrCode =>
      play(holdUpQRCodeClip, SayHoldUpQrCode)
    case SayReadQrCode =>
      play(readedQRCodeClip, SayReadQrCode)
    case SaySendedEmail =>
      play(sendedEmailCLip, SaySendedEmail)
  }

  def play(clip: Clip, message : Any) : Unit = {
    clip.start()
    clip.addLineListener(
      new LineListener{
        def update(event: LineEvent){
          if( event.getType() == LineEvent.Type.STOP){
            val clip = event.getLine().asInstanceOf[Clip]
            clip.stop()
            clip.setFramePosition(0)
            context.parent ! Said(message)
          }
        }
      }
    )
  }
}

package techin

import akka.actor.{Actor, ActorRef, Props}
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

case object TechinTerminate
case object CheckConnect
case object Reset

class Techin extends Actor {
  val qrCodeReader : ActorRef = context.actorOf(Props[QRCodeReader], "qrCodeReader")
  val studentManager : ActorRef = context.actorOf(Props(new StudentManager("techpark")), "studentManager")
  val speaker : ActorRef = context.actorOf(Props[Speaker], "speaker")
  var entryControllers : Map[Int, ActorRef] = Map[Int, ActorRef]()
  var entrySubject = "テックパーク入退室連絡"
  val exitSubject = "テックパーク入退室連絡"


  val mailSender : ActorRef = context.actorOf(Props[MailSender], "mailSender")

  speaker ! SayHoldUpQrCode

  var currentStudetText = ""
  def receive = {
    case Reset =>
      Thread.sleep(500)
      speaker ! SayHoldUpQrCode

    case Said(SayHoldUpQrCode) =>
      qrCodeReader ! Reload

    case ReadFailed =>
      Thread.sleep(100)
      qrCodeReader ! Reload

    case DecodeResult(text) =>
      println(s"running student number: ${text}")
      currentStudetText = text
      speaker ! SayReadQrCode

    case Said(SayReadQrCode) =>
      println("said read qr code")
      val studentNumber = try {
        Some(currentStudetText.toInt)
      }catch{
        case e:Exception => None
      }
      println("check student number" + studentNumber)
      studentNumber match {
        case Some(number) =>
          println("getting student!")
          studentManager ! GetStudent(number)
          println("get student!")
        case None =>
          Thread.sleep(500)
          qrCodeReader ! Reload
      }


    case Some(student:StudentVer2) =>
      (entryControllers get student.number) match {
        case Some(actor) =>
          actor ! EntryManage
        case None =>
          val actor = context.actorOf(Props(new EntryController(student)))
          actor ! EntryManage
          entryControllers = entryControllers + (student.number -> actor)

      }

    case EntryEvent(date, student) =>
      val message = SendMail(student.emailAddress,
                             entrySubject,
                             s"${student.nickName}さんが${format(date)}にテックパークに入室しました。")
      mailSender ! message
      
    case ExitEvent(date, student) =>
      val message = SendMail(student.emailAddress,
                             exitSubject,
                             s"${student.nickName}さんが${format(date)}にテックパークを退室しました。")
      mailSender ! message

    case Sended(emailAddress) =>
      speaker ! SaySendedEmail

    case Said(SaySendedEmail) =>
      Thread.sleep(500)
      qrCodeReader ! Reload

    case SendFailed(s) =>
      println(s)
      Thread.sleep(500)
      qrCodeReader ! Reload

    case Reload =>
      qrCodeReader ! Reload

    case TechinTerminate =>
      context.system.terminate()

    case CheckConnect =>
      println("cheching connect")
      Thread.sleep(100)
      sender ! CheckConnect
  }

  def format(date: LocalDateTime) : String = {
    val dtf = DateTimeFormatter.ofPattern("HH時mm分")
    date.format(dtf)
  }
}

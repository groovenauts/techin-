package techin

import akka.persistence._

import com.google.api.client.googleapis.auth.oauth2.{GoogleCredential,
  GoogleClientSecrets,
  GoogleAuthorizationCodeFlow
}

import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp

import com.google.api.services.gmail.{
  GmailScopes
  , Gmail
}
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport
import com.google.api.client.json.jackson2.JacksonFactory
import com.google.api.client.repackaged.org.apache.commons.codec.binary.Base64
import com.google.api.services.gmail.model.{Message}
import com.google.api.client.util.store.FileDataStoreFactory
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver

import java.io.FileInputStream
import scala.io.Source
import java.util.Collections
import java.util.Properties
import java.io.{ByteArrayOutputStream, InputStreamReader}

import javax.mail.Session
import javax.mail.internet.{MimeMessage, MimeBodyPart, MimeMultipart}

import javax.mail.internet.InternetAddress

case class SendMail(emailAddress:String, subject:String, text:String)
case class Sended(emailAddress:String)
case class SendFailed(e:String)
case class SetMailInfo(accessToken:String, refreshToken:String)

case class InfoChange(newMailSenderInfo: MailSenderInfo)
case class InfoChangeEvent(newMailSenderInfo: MailSenderInfo)

case class MailSenderInfo(
  val fromAddress : String,
  val fromName : String,
  val accessToken : String,
  val refreshToken : String
) {

  def updateAccessToken(newAccessToken : String) =
    this.copy(fromAddress, fromName, newAccessToken, refreshToken)


  def updateRefreshToken(newRefreshToken : String) =
    this.copy(fromAddress, fromName, accessToken, newRefreshToken)
}

class MailSender extends PersistentActor {
  override def persistenceId = "mailsender-gmail-api"
  var info = MailSenderInfo(
    "kids@techpark.jp",
    "Techin",
    "",
    ""
  )

  var credential : Option[GoogleCredential] = None

  val http_transport =
    new com.google.api.client.http.javanet.NetHttpTransport.Builder()
      .trustCertificates(
        com.google.api.client.googleapis.GoogleUtils.getCertificateTrustStore())
      .build()

  val json_factory = JacksonFactory.getDefaultInstance()

  val in = this.getClass.getResourceAsStream("/techin/client_id.json")
  val clientSecret =
    GoogleClientSecrets.load(json_factory, new InputStreamReader(in))

  val receiveRecover : Receive = {
    case InfoChangeEvent(newInfo) =>
      info = newInfo
    case SnapshotOffer(_, snapshot:MailSenderInfo) =>
      info = snapshot
  }

  val snapShotInterval = 30
  val receiveCommand : Receive  = {
    case SetMailInfo(accessToken: String, refreshToken: String) =>
      val newInfo1 = info.updateAccessToken( accessToken )
      val newInfo2 = newInfo1.updateRefreshToken( refreshToken )
      persist(InfoChangeEvent(newInfo2)) { _ =>
        info = newInfo2
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(info)
      }

    case InfoChange(newInfo) =>
      persist(InfoChangeEvent(newInfo)) { _ =>
        info = newInfo
        if (lastSequenceNr % snapShotInterval == 0 && lastSequenceNr != 0)
          saveSnapshot(info)
      }
    case SendMail(emailAddress, subject, text) =>
      send(emailAddress, subject, text)
      sender ! Sended(emailAddress)
  }

  def send(mailAddress: String, subject: String, text: String) : Unit = {

    credential match {
      case None =>
        val newCredential = new GoogleCredential.Builder()
          .setJsonFactory(json_factory)
          .setTransport(http_transport)
          .setClientSecrets(clientSecret).build()

        newCredential.setAccessToken(info.accessToken)
        newCredential.setRefreshToken(info.refreshToken)

        credential = Some(newCredential)
      case _ =>
    }

    if( credential.get.getExpiresInSeconds() < 0 ){
      credential.get.refreshToken()
      info = info.updateAccessToken( credential.get.getAccessToken )
      info = info.updateRefreshToken(credential.get.getRefreshToken )
      persist(InfoChangeEvent(info)) { _ => saveSnapshot(info) }
    }
    
    val service = new Gmail.Builder(http_transport, json_factory, credential.get)
      .setApplicationName("techin").build()

    try {
      val props = new Properties()
      val session = Session.getDefaultInstance(props, null)
      val email = new MimeMessage(session)

      email.setFrom(new InternetAddress(info.fromAddress))
      email.addRecipient(javax.mail.Message.RecipientType.TO,
                         new InternetAddress(mailAddress))
      email.setSubject(subject)
      email.setText(text)

      val buffer = new ByteArrayOutputStream()
      email.writeTo(buffer)
      val bytes = buffer.toByteArray()
      val encodedEmail = Base64.encodeBase64URLSafeString(bytes)
      val message = new Message
      message.setRaw(encodedEmail)

      val msg = service.users().messages().send("me", message).execute()

      println(s"Message id: ${msg.getId}")
      println(msg.toPrettyString())
      sender ! Sended(mailAddress)
    } catch {
      case e: Exception =>
        sender ! SendFailed(e.toString())
    }
  }
}

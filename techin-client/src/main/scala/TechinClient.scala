package techin.client

import scalafx.Includes._
import scalafx.application.JFXApp
import scalafx.stage.WindowEvent
import scalafx.scene.Scene
import scalafx.scene.layout.{
  HBox,
  VBox,
}
import scalafx.scene.control.{
  TabPane,
  Tab
}

import akka.util.Timeout
import akka.actor.{
  Props,
  ActorSelection
}
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import java.awt.image.BufferedImage;
import java.awt.Color
import java.io.File;
import javax.imageio.ImageIO;

import scala.collection.JavaConversions._

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

import techin.{
  StudentVer2,
  UpdateStudent
}


object TechinClient extends JFXApp {
  val http_transport =
    new com.google.api.client.http.javanet.NetHttpTransport.Builder()
      .trustCertificates(
        com.google.api.client.googleapis.GoogleUtils.getCertificateTrustStore())
      .build()

  val json_factory = JacksonFactory.getDefaultInstance()

  val dataStoreDir  = new java.io.File(
    System.getProperty("user.home"), ".credentials/techin-app-3"
  )

  val dataStoreFactory = new FileDataStoreFactory(dataStoreDir)

  
  val in = this.getClass.getResourceAsStream("client_id.json")
  val clientSecret =
    GoogleClientSecrets.load(json_factory, new InputStreamReader(in))

  val flow =
    new GoogleAuthorizationCodeFlow.Builder(http_transport, json_factory, clientSecret, Collections.singleton(GmailScopes.GMAIL_SEND))
      .setDataStoreFactory(dataStoreFactory)
      .setAccessType("offline")
      .build()


  val credential = new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver())
    .authorize("user")

  val qrImages_w = 595
  val qrImages_h = 842
  var qrImages = List(
    new BufferedImage(qrImages_w, qrImages_h,BufferedImage.TYPE_INT_ARGB)
  )
  var page = 0
  var counter = 0
  def createQRCode(source: String, size: Int, studentNumber : Int, name : String) : Unit = {
    val encodeing = "UTF-8"
    val file_path = s"${name}.png"
    val writer = new QRCodeWriter()
    val bitMatrix = writer.encode(source, BarcodeFormat.QR_CODE, size, size, Map(
                    EncodeHintType.ERROR_CORRECTION -> ErrorCorrectionLevel.M,
                    EncodeHintType.CHARACTER_SET -> encodeing,
                    EncodeHintType.MARGIN -> 0
                                  ))
    val qrcodeImage = MatrixToImageWriter.toBufferedImage(bitMatrix)

    val image = new BufferedImage(size * 2, size, BufferedImage.TYPE_INT_ARGB)
    val graphics = image.createGraphics()
    graphics.setColor(Color.BLACK)
    graphics.drawString(s"${studentNumber}", size + 10, 10)
    graphics.drawString(s"${name}", size + 10, 20)
    graphics.drawImage(qrcodeImage, 0, 0, null)
    ImageIO.write(image, "png", new File(s"images/${studentNumber}_${file_path}"))

    if( qrImages.length <= page ){
      qrImages :+= new BufferedImage(qrImages_w, qrImages_h,BufferedImage.TYPE_INT_ARGB)
      
    }

    val qrImage = qrImages(page)
    val qrImageGraphics = qrImage.createGraphics()
    counter match {
      case 0 =>
        qrImageGraphics.drawImage(image, 10, 0, null)
      case 1 =>
        qrImageGraphics.drawImage(image, 10, 110, null)
      case 2 =>
        qrImageGraphics.drawImage(image, 10, 220, null)
      case 3 =>
        qrImageGraphics.drawImage(image, 10, 330, null)
      case 4 =>
        qrImageGraphics.drawImage(image, 10, 440, null)
      case 5 =>
        qrImageGraphics.drawImage(image, 10, 550, null)
      case 6 =>
        qrImageGraphics.drawImage(image, 10, 660, null)
      case 7 =>
        qrImageGraphics.drawImage(image, 210, 0, null)
      case 8 =>
        qrImageGraphics.drawImage(image, 210, 110, null)
      case 9 =>
        qrImageGraphics.drawImage(image, 210, 220, null)
      case 10 =>
        qrImageGraphics.drawImage(image, 210, 330, null)
      case 11 =>
        qrImageGraphics.drawImage(image, 210, 440, null)
      case 12 =>
        qrImageGraphics.drawImage(image, 210, 550, null)
      case 13 =>
        qrImageGraphics.drawImage(image, 210, 660, null)
    }

    counter += 1
    if( counter > 13 ){
      page += 1
      counter = 0
    }
  }

  def createQRCode(csvName: String) : Unit = {
    import com.github.tototoshi.csv._
    import scala.util.control.Exception._
    
    val reader = CSVReader.open(csvName)
    catching(classOf[Exception]) opt {
      val studentListCSV = reader.allWithHeaders()
      for(col <-  studentListCSV ){
        createQRCode(col("No"), 100, col("No").toInt, col("Name"))
        var i = 0
        for(image <- qrImages){
          ImageIO.write(image, "png", new File(s"${i}.png"))
          i += 1
        }
      }
    } 
  }

  def setStudentDataFromCSV(csvName: String, studentManager: ActorSelection) : Unit = {
    import com.github.tototoshi.csv._
    import scala.util.control.Exception._

    val reader = CSVReader.open(csvName)
    catching(classOf[Exception]) opt {
      val studentListCSV = reader.allWithHeaders()
      for(col <-  studentListCSV ){
        val student = StudentVer2(
          col("No").toInt,
          col("名前"),
          col("名前"),
          techin.Addresses(col("to").split(" ").map(_.trim).toSet,
                           col("cc").split(" ").map(_.trim).toSet,
                           col("bcc").split(" ").map(_.trim).toSet
          )
        )
        studentManager ! UpdateStudent(student)
      }
    }
  }

  createQRCode("/Users/NobkzPriv/techin/techin-client/src/main/resources/techin_members.csv")

  val system = StartActorSystem.system

 
  stage = new JFXApp.PrimaryStage {
    var cameraImageView : CamerImageView = null
    title = "techin client"
    width = 720
    height = 580
    
    scene = new Scene() {
      stylesheets = List(TechinClient.getClass.getResource("style.css").toExternalForm())
      
      val connector  = new RasPiConnector(
        system.actorOf(Props[RasPiConnectActor], "remoteConnect")
      )

      val connectRasPiView = new ConnectRasPiView(connector)
      val tabPane = new TabPane()
      val connectRasPiTab = new Tab() {
        styleClass.add("tab")
        text = "connect to raspi"
        content = connectRasPiView
      }
      tabPane.tabs.add(connectRasPiTab)
      tabPane.prefWidth = 720
      tabPane.prefHeight = 580
      content = tabPane

      connector addSubscriber new Subscriber {
        def connected(address: RasPiConnectAddress) : Unit = {
          val studentManagerPath = address createPath "/user/techin/studentManager"
          val studentManagerActor = system.actorSelection(studentManagerPath)

          // setStudentDataFromCSV("/Users/NobkzPriv/techin/techin-client/src/main/resources/techin_members.csv", studentManagerActor)


          val mailSenderActor = system.actorSelection(address createPath "/user/techin/mailSender")

          mailSenderActor ! techin.SetMailInfo(credential.getAccessToken(), credential.getRefreshToken() )

          val studentList= new StudentsList(studentManagerActor)


          val joinStudentForm = new JoinStudentForm(studentManagerActor)
          val studentsTable = new StudentsTable(studentList)
          val cameraLocalActor = system.actorOf(
            Props(new CameraImageReceiver(address)),
            "cameraImageReceiver")
          cameraImageView = new CamerImageView(cameraLocalActor)

          val updateStudentTab = new Tab() {
            text = "update student form"
            content = joinStudentForm
          }

          val studentTableTab = new Tab(){
            text = "student table"
            content = studentsTable
          }

          val cameraTab = new Tab(){
            text = "camera Tab"
            content = cameraImageView
          }

          tabPane.tabs.remove(connectRasPiTab)
          tabPane.tabs.addAll(
            studentTableTab,
            cameraTab,
            updateStudentTab
          )
          
        }
      }


      system.scheduler.schedule(100 millis, 100 millis) {
        if( cameraImageView != null )
          cameraImageView.reply
      }

    }
    handleEvent(WindowEvent.WindowCloseRequest) {
      (event:WindowEvent) =>
      val e = system.terminate()
    }
  }
}


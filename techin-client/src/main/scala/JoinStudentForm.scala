package techin.client

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.layout.VBox
import scalafx.scene.control.{
  Label,
  TextField,
  Button
}

import techin.{
  UpdateStudent,
  StudentVer2,
  Addresses
}


import akka.actor.{
  ActorSelection
}
import akka.pattern.ask
import akka.util.Timeout
import scala.concurrent.duration._
import scala.concurrent.ExecutionContext.Implicits.global
import scala.concurrent.{
  Future,
  Await
}

class JoinStudentForm(val actorRef: ActorSelection) extends VBox {
  val studentNumberLabel = Label("student Number")
  val nicknameLabel = Label("nickname")
  val nameLabel = Label("first name")
  val emailTOLabel = Label("email address to")
  val emailCCLabel = Label("email address cc")
  val emailBCCLabel = Label("email address bcc")

  val studentNumberTextField = new TextField()
  val nickNameTextField = new TextField()
  val nameTextField = new TextField()
  val emailTOTextField = new TextField()
  val emailCCTextField = new TextField()
  val emailBCCTextField = new TextField()

  val createButton = new Button("create student!")
  
  children = Seq(
    studentNumberLabel,
    studentNumberTextField,
    nicknameLabel,
    nickNameTextField,
    nameLabel,
    nameTextField,
    emailTOLabel,
    emailTOTextField,
    emailCCLabel,
    emailCCTextField,
    emailBCCLabel,
    emailBCCTextField,
    createButton
    )

  createButton.onAction = {
    (e:ActionEvent) =>
    val studentNumber = studentNumberTextField.text().toInt
    val nickName = nickNameTextField.text()
    val name = nameTextField.text()
    val emailTO = emailTOTextField.text().split(",").map(_.trim).toSet
    val emailCC = emailCCTextField.text().split(",").map(_.trim).toSet
    val emailBCC = emailBCCTextField.text().split(",").map(_.trim).toSet


    val message = UpdateStudent(
      StudentVer2(
        studentNumber,
        nickName,
        name,
        Addresses(
          emailTO,
          emailCC,
          emailBCC
        )
      )

    )

    implicit val timeout = Timeout(5 seconds)
    Await.result((actorRef ? message), timeout.duration)
  }

}

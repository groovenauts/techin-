package techin.client

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.layout.VBox
import scalafx.scene.control.{
  Label,
  TextField,
  Button
}

import techin.UpdateStudent


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
  val emailLabel = Label("email address")

  val studentNumberTextField = new TextField()
  val nickNameTextField = new TextField()
  val nameTextField = new TextField()
  val emailTextField = new TextField()

  val createButton = new Button("create student!")
  
  children = Seq(
    studentNumberLabel,
    studentNumberTextField,
    nicknameLabel,
    nickNameTextField,
    nameLabel,
    nameTextField,
    emailLabel,
    emailTextField,
    createButton
    )

  createButton.onAction = {
    (e:ActionEvent) =>
    val studentNumber = studentNumberTextField.text().toInt
    val nickName = nickNameTextField.text()
    val name = nameTextField.text()
    val email = emailTextField.text()

    val message = UpdateStudent(studentNumber, nickName, name, email)

    implicit val timeout = Timeout(5 seconds)
    Await.result((actorRef ? message), timeout.duration)
  }

}

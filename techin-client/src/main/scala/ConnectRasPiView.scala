package techin.client

import scalafx.Includes._
import scalafx.event.ActionEvent
import scalafx.scene.layout.VBox
import scalafx.scene.control.{
  Label,
  TextField,
  Button
}



class ConnectRasPiView(val connector: RasPiConnector) extends VBox {
  this.getStyleClass.add("ConnectRasPiView")
  val textFieldLabel = Label("set ip address")
  val textField = new TextField()
  val button = new Button("connected!")

  children = Seq(
    textFieldLabel,
    textField,
    button
  )

  button.onAction = {
    (e:ActionEvent) =>
    connector.connect(textField.text())
  }
}


package techin.client

import akka.actor.Address

case class RasPiConnectAddress(val ipAddress:String){
  val protocol = "akka"
  val remoteSystemName = "techin"
  val port = 23320

  def createAddress : String = {
    val address = Address(protocol, remoteSystemName, ipAddress, port)
    address.toString
  }

  def createPath(actorPath : String) : String = {
    createAddress + actorPath
  }
  
}

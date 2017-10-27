package techin.raspi

import akka.actor.{ActorSystem, Props}
import com.typesafe.config.ConfigFactory
import com.typesafe.config.ConfigValueFactory
import techin.Techin
import java.net.{InetAddress, NetworkInterface, Inet4Address}

object Main extends App {
  val config = ConfigFactory.load("conf/Application.conf")
  val host : InetAddress = getInetAddress
  val hostAddress = host.getHostAddress()
  val hostnameConfigValue = ConfigValueFactory.fromAnyRef(hostAddress)
  val newConfig = config.withValue("akka.remote.artery.canonical.hostname", hostnameConfigValue)
  val system = ActorSystem("techin", newConfig)
  println(newConfig)
  val actorRef = system.actorOf(Props[Techin], "techin")



  def getInetAddress : InetAddress = {
    val netSet = NetworkInterface.getNetworkInterfaces()
    var inet : InetAddress = null
    while(netSet.hasMoreElements() && inet == null){
      val nInterface = netSet.nextElement()
      val list = nInterface.getInetAddresses()
      while(list.hasMoreElements() && inet == null){
        val interfaceAdr = list.nextElement()
        if(!interfaceAdr.isLoopbackAddress() && interfaceAdr.getClass == classOf[Inet4Address]){
          inet = interfaceAdr
        }
      }
    }
    inet
  }
}

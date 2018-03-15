package techin.client


import akka.actor.{
  ActorSystem
}

import com.typesafe.config.{
  ConfigFactory,
  ConfigValueFactory
}

import java.net.{
  InetAddress,
  NetworkInterface,
  Inet4Address
}

object StartActorSystem{
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

  val config = ConfigFactory.load("Application.conf")
  val hostName = getInetAddress
  val hostAddress = hostName.getHostAddress()
  val hostnameConfigValue = ConfigValueFactory.fromAnyRef(hostAddress)
  val newConfig = config.withValue("akka.remote.artery.canonical.hostname", hostnameConfigValue)
  val system = ActorSystem("techin-client", newConfig)
}

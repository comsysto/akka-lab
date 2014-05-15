package clustering

import com.typesafe.config.ConfigFactory
import akka.actor._
import akka.cluster.Cluster
import clustering.SimpleClusterListener.Print
import akka.actor.RootActorPath
import akka.routing.Broadcast

/**
 * Created by sturmm on 13.05.14.
 */
object JoinClusterApp extends App {

  val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2553").
    withFallback(ConfigFactory.load("application-cluster.conf").getConfig("joining"))

  println(config.toString)

  val system = ActorSystem("ClusterSystem", config)

  val a1: Address = Address("akka.tcp", "ClusterSystem", "192.191.1.216", 2551)
//  val a2: Address = Address("akka.tcp", "ClusterSystem", "127.0.0.1", 2552)

  val cluster: Cluster = Cluster(system)
  cluster.joinSeedNodes(a1 :: Nil)

  cluster.registerOnMemberUp({
    system.actorSelection(RootActorPath(a1) / "user" / "exchange" / "orderbooks") ! Broadcast(PoisonPill)
  })

}

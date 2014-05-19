```
██████████╗  █████████╗     █████╗██╗  ████╗  ██╗█████╗     ██╗     █████╗██████╗
╚══██╔══██║  ████╔════╝    ██╔══████║ ██╔██║ ██╔██╔══██╗    ██║    ██╔══████╔══██╗
   ██║  ████████████╗      ████████████╔╝█████╔╝███████║    ██║    █████████████╔╝
   ██║  ██╔══████╔══╝      ██╔══████╔═██╗██╔═██╗██╔══██║    ██║    ██╔══████╔══██╗
   ██║  ██║  █████████╗    ██║  ████║  ████║  ████║  ██║    █████████║  ████████╔╝
   ╚═╝  ╚═╝  ╚═╚══════╝    ╚═╝  ╚═╚═╝  ╚═╚═╝  ╚═╚═╝  ╚═╝    ╚══════╚═╝  ╚═╚═════╝
```

# Reactive Programming with Akka and Scala
## High performant and scalable Applications

During our second akka Lab we wanted to extend our trading app with various use cases to test clustering, monitoring and supervision.

## Updating from Akka 2.2.3 to 2.3.2
We started our lab by upgrading to the latest Akka version 2.3.2 and got some compile errors regards routing. So we went through the [migration guide](http://doc.akka.io/docs/akka/2.3.2/project/migration-guide-2.2.x-2.3.x.html) and found the following comment which seems to belong to us:

> The API for creating custom routers and resizers have changed without keeping the old API as deprecated. 
> That should be a an API used by only a few users and they should be able to migrate to the new API without
> much trouble.
>
>Read more about the new routers in the documentation for Scala and documentation for Java.

With 2.3 there akka introduces two distignushable types of routers: `Pool` and `Group`. Pools are all the routers that manages their routees by itself (creation and termination as child actors) and Group means all the routers	that gets the routees configured from outside. 

So in our case it makes sense to use the `Pool` pattern because the router setup an manage all the `OrderBook` actors by itself. In addition our router later needs to be configurable on runtime and so we end up with a dedicated actor which uses an embedded router as figured out in the [akka documentation](http://doc.akka.io/docs/akka/2.3.2/scala/routing.html#A_Router_Actor). So our router actor can foward messages to it's `OrderBook`s but handle routing configuration messages by itself if necessary:

```
// Actor with routing functionality
class OrderRoutingActor(securities: Seq[Security]) extends Actor with ActorLogging {

  // routes all Asks and Bits belonging to same Security to a single OrderBook
  val routingLogic = new OrderBookRoutingLogic()
  
  // the router
  val router = new Router(routingLogic)

  override def preStart(): Unit = {

    // setup child OrderBook actor for given security   
    securities.foreach { security =>

      val orderBookForSecurity = context.actorOf(Props[OrderBook](
          new OrderBook(security) with SimpleTradeMatcher with AverageMarketPriceCalculator), security.name)

      routingLogic.addOrderBook(security, orderBookForSecurity)
    }

    log.info(s"Orderbook router for $securities is started.")
  }
  
  override def receive: Receive = {

    // handle new router configuration
    case UpdateConfig(config) => // reconfigure routingLogic
    
    // route the rest
    case msg => router.route(msg, sender())
  }
}
```

So lets take a look on whats going on here. As already mentioned we created an actor whose functionality is to route messages using a `Router`. Internally the `Router` uses a `OrderBookRoutingLogic` which simply holds a `Map` of `Security` to `ActorRef`. Each message of type `Order`, which are `Bid`s or `Ask`s on a `Security`, will be routed to the responsible `OrderBook`. All other messages will be broadcasted to all known `ActorRef`s.
This is nearly the same as before, but the router wasn't an `Actor`. You should know, that this can make a difference. Normally putting a message into the mailbox of an `Actor`happens synchronous which also includes the routing. Embedding a `Router` into an `Actor` causes that a message will be sent into the routing actors mailbox synchronous but forwarding the message to the original destination isn't. This will increase the througput time of a message. In a real world trading app this will low throughput time will be a requirement, but in our demo this does'nt really matter. 

## Cluster the Trading App

## The Requirements

## Aside: Akka Cluster Spec

To start with the custom implementation we need an idea of what is possible with Akka. So the first of all we need to add the akka cluster package as dependency in our **build.sbt** :

```
libraryDependencies += "com.typesafe.akka" %% "akka-cluster" % "2.3.2",
```

And afterwards we had to update our **application.conf** as figured out in the [akka doc](http://doc.akka.io/docs/akka/2.3.2/scala/cluster-usage.html#A_Simple_Cluster_Example) and that's all we need to start with clustering. For all actor systems that you will create you can decide whether to join a cluster automatically by connecting to configured seed nodes or to join a cluster programmatically:

```
val config = ConfigFactory.parseString("akka.remote.netty.tcp.port=2552").withFallback(ConfigFactory.load())

val sys = ActorSystem("TradingShard", config)

val seed: Address = Address("akka.tcp", "TradingShard", "127.0.0.1", 2551)

val cluster: Cluster = Cluster(sys).joinSeedNodes(a1 :: Nil)
```

Assuming that we already running an actory system named `TradingSytem` on our machine on port `2551`, we're creating a second actor sytem with the same name - which is quite important - on port `2552`, resolving an address of a seed node and join the cluster using that address. The address can be retrieved e.g. from a database or a REST service or anything else. 
 
Now that we become a member of the cluster, we want to communicate with actors of the other members. This is of course simply possible by using the full address of an actor including the host and port of its actor system. But in most cases this is not what we want. For default purposes akka provides 'cluster aware' router implementations such as `ClusterRouterGroup` or `ClusterRouterPool`. As for 'normal' routers it is possible to define them by configuration or programmatically:

```
val routeesPaths = "/user/tradingShardManager" :: Nil
  
val codedShardManagerRouterConf = ClusterRouterGroup(
  BroadcastGroup(routeesPaths),
  ClusterRouterGroupSettings(
    totalInstances = 100,
    routeesPaths = routeesPaths,
    allowLocalRoutees = false,
    useRole = None
  )
)
  
val codedShardManagerRouter = context.actorOf(codedShardManagerRouterConf.props(), "clusterRouter")
```

In this example - that we'll need later - we're creating a pool whereby the routees are all the actors over the cluster that are mounted to the path `"/user/tradingShardManager"` excluding the actor that belongs to the same actor system (`allowLocalRoutees = false`). That router pool gets a `BroadcastGroup` as strategy what means that it will send all messages to each known routee. Akka ships with many more of these strategies from simple round robin over consistent hashing to metric based strategies such as heap mem or cpu load. Additionally it's of course possible let the router manage the actor instances and create more of them in the cluster when needed.

As you can see from this short paragraph there are a lot of possibilities that akka has onboard. But nevertheless sometimes this might not be enough. In these cases you can dive deeper into the cluster configuration details and write your own protocoll based on cluster events that akka broadcasts if something happens in the cluster. The following types - that we'll need later - are a subset of these messages:

- `MemberUp` which is published when a new system becomes part of the cluster.
- `MemberDown` which is published when a member leaves the cluster (e.g. machine down).
- `LeaderChanged` which is published everytime a member becomes leader of a cluster. There is no election process for who becomes leader. Leader is that member that is first one in an ordered list and this could potentially change everytime a member joins or leaves.

To retrieve these messages you can simply subscribe an actor to the cluster event stream and handle the messages you need:

```
class EventListeningActor extends Actor {
  override def preStart(): Unit = {
	val cluster = Cluster(context.system)
    
    cluster.subscribe(self, initialStateMode = InitialStateAsEvents, classOf[ClusterDomainEvent])
	cluster.sendCurrentClusterState(self)
  }

  override def recieve = {
	case LeaderChanged(newLeader) => 
	case MemberRemoved(member, status) =>
	case MemberUp(member) => 
  }
}
```


	

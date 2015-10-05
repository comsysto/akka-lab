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

During our Lab we wanted to try implementing an application with Akka and Scala, because we're going to evaluate performant and scalable software architectures on the JVM.

In this blog we're describing how to setup an Akka app and showing a very basic demo.

## Bootrapping an Akka/Scala app

The basic setup of the application is quite simple. We're using `sbt` as build tool and therefor we need to create [build.sbt](build.sbt) and add the needed akka artifacts as dependency:

```
name := "The Akka Lab"

version := "0.1"

scalaVersion := "2.10.4	"

libraryDependencies ++= Seq(
  "com.typesafe.akka" %% "akka-actor" % "2.2.3",
  "com.typesafe.akka" %% "akka-testkit" % "2.2.3",
  "org.scalatest" %% "scalatest" % "2.0" % "test",
)
```

You can easily import the project into IntelliJ or use sbt plugins to generate project files for your prefered IDE:

- [IntelliJ: sbt-idea](https://github.com/mpeltonen/sbt-idea) 
- [Eclipse: sbteclipse](https://github.com/typesafehub/sbteclipse)

### Simple Message passing

After importing the project we're implementing our first basic ActorSystem. It's structure will look as depicted below:

![Simple Actor Systems Structure](blog/Basic_ActorSystem.png "Simple Actor Systems Structure")

We have want to create a single ActorSystem called **routing** having a `Receiver` Actor called **single** next to a `RoundRobinRouter` **router** with 10 children of type `Receiver`. All we need to do is creating instantiating the system and creating both the children **single** and **router**. The `RoundRobinRouter` creates it's children by itself:

```
	import scala.concurrent.duration._
    val duration = 3.seconds
    implicit val timeout = Timeout(duration)

    val sys = ActorSystem("Routing")

    val single = sys.actorOf(Props[Receiver](new Receiver(2.seconds.toMillis)), "single")
    val router = sys.actorOf(Props[Receiver].withRouter(RoundRobinRouter(nrOfInstances = 10)), "router")
```

The [`Reciever`](demo/RoutingStrategies.scala) is does recieve messages of type `Message(String)` and prints whatever the message parameter is. After recieving a message we're toggeling the state of our reciever by using Akka's `become` mechanism. So here is the definition of our `Reciever` actor:

```
  class Receiver(timeout: Long) extends Actor with ActorLogging {
    import demo.RoutingStrategies.Receiver._
    
    def this() = this(1000)

    override def receive = fastReceive

    def fastReceive: Receive = {
      case Message(m)=> {
        log.info(m)

        context.become(slowReceive)
      }
    }

    def slowReceive: Receive = {
      case Message(m) => {
        Thread.sleep(timeout)

        log.info(s"Slow: $m")

        context.become(fastReceive)
      }
    }
  }
```
As mentioned before the Actor simply prints out the message. After he recieved a message he toggles his state from `fastRecieve` to `slowRecieve` and vice versa to simple simulate more heavy execution. Now that our system is complete we can start sending Messages to `single` and `router`:

```
// Sending a message by using actors path
sys.actorSelection("user/single") ! Message("Hello You, by path! [fast]") // fast really?

// Sending a message by using ActorRef
single ! Message("Hello You! [slow]") // slow really?
single ! Message("Hello You! [fast]") // fast really?

// Sending a message by using Router
router ! Message("Hello Anybody! [fast]") 	// route message to next Reciever actor
router ! Broadcast(Message("Hello World! [1xslow, 9xfast]")) // route message to all Reciever actors
```

Right here we got our first problem. As you can see, we've expected that Akka preserves the order of the messages and that is the truth... as long as you don't mix up sending messages by `ActorRef` and `ActorSelection`. In this case the only guarantee we have is, that all messages sent to an `ActorRef` will have a defined order and all messages sent by `ActorSelection` have a defined order too. But between these two mechanism of addressing messages there is no gurantee for the order.

The last thing that we want to try is to shutdown the `ActorSystem` after all messages has been processed. And because we're in a multithreaded environemt we cannot simply shutdown the system at the end. We can call `system.shutdown()` and then use `system.awaitTermination()` to wait until all of currently active executions are finished, but we don't know whether all messages has been processed or not. For this reason Akka provides the `gracefulShutdown` mechanism:

Using it means special Message , the `PoisonPill`, takes place on top of the Actors current mailbox and all messages below will be processed and when the `PoisonPill` is processed the Actor terminates and sends a `Terminated` message. After we picked up all `Terminated` messages we can shutdown the system safely:

```
for {
  routerTerminated  <- gracefulStop(router, duration, Broadcast(PoisonPill))
  singleTerminated <- gracefulStop(single, duration)
} {
  sys.shutdown()
}
```

### PingPong: Remote Messages
To try [remoting](http://doc.akka.io/docs/akka/snapshot/scala/remoting.html) in Akka, we have decided to play Actor ping-pong. The basic actor code is quite simple (simplified version):

```
object PingPongActor {
  case class Ping(message : String)
  case class Pong(message : String)
}

class PingPongActor extends Actor with ActorLogging {
  import demo.PingPongActor.{Pong, Ping}
  def receive = {
    case Ping(m) => {
      log.info(s"Ping: $m")
      sender ! Pong(m)
    }

    case Pong(m) => {
      log.info(s"Pong: $m")
      sender ! Ping(m)
    }
  }
}
```

Based on an [Akka Remote Hello-World example](http://alvinalexander.com/scala/simple-akka-actors-remote-example) we wrote a "client" and a "server" application and configured them using Typesafe Config. One of the Actors just needs to kick off the game and then both ping-pong happily ever after. As the message protocol is very simple, the application is well-suited to measure Akka message latencies. Hence, we attached a timestamp to each message using `System#nanoTime()`. However, as stated in the [Javadoc of System#nanoTime()](http://docs.oracle.com/javase/7/docs/api/java/lang/System.html#nanoTime%28%29), it is only suited for time measurements within a single JVM. So, instead of measuring only the latency from one actor to the other, we decided to measure roundtrip time which allows us to use `System#nanoTime()` safely. To measure them, both messages are extended by a timestamp property and `receive` is changed accordingly:

```
def receive = {
    case Ping(m, ts) => {
        log.info(s"Ping: $m")
        //just forward the timestamp
        sender ! Pong(m, ts)
    }

    case Pong(m, ts) => {
        val roundTripTime = System.nanoTime() - ts
        log.info(s"Pong: $m with round trip time $roundTripTime ns.")
        sender ! Ping(m, next, System.nanoTime())
    }
}
```

Our takeaways for this example:

* Actor distribution is easily possible but it is not immediately obvious how actors are distributed (i.e. by writing a client and a server application in our case)
* Time measurement in a distributed system requires some thought but we got away with a very simple solution to measure roundtrip time


### Aside: Typesafe Config

We found that [typesafe config](https://github.com/typesafehub/config) is noteworthy because it has a good to read syntax and easy to use Scala/Java API and it is Akka's configuration mechanism. There are a lot of switches where you can tune your Akka application without changing a single line of code. Tpesafe config has a JSON like syntax called HOCON that allows to use different data types e.g. numbers, strings, arrays or nested "objects". It has also built in support for placeholder replacement. You can use it to override Akka defaults and to provide configuration for you own application too.

So here a structural excerpt from our application config:

```
  # overriding akka defaults
  akka {
    ...
  }

  # server side akka overrides
  server {
    akka {
      ...
    }
  }

  # client side akka overrides
  client {
    ...
  }
```
On server side we're loading our config using the following code snippet:

```
  // load akka defaults, ignore others
  val akkaConf = ConfigFactory.load("application-remoting.conf").withOnlyPath("akka")
  // load server default
  val serverConf = ConfigFactory.load("application-remoting.conf").getConfig("server")

  // merge server and akka config
  val conf = serverConf.withFallback(akkaConf)

```

First we are loading the default configuration into `akkaConf` and afterwards the dedicated server config into `serverConf`. Then we merge them together into one single config called `conf`. Now, when we're reading a property from `conf`, we will get the one from Akka block in server section if it is present or the one from the root Akka block if not. The same way Akka reads the defaults from `reference.conf` and overrides them with the properties from out of `application.conf` if a so named file is present in classpath. If you want to take a look into what defaults are configured you can take a look into the `reference.conf` or into the [Akka documentation](http://doc.akka.io/docs/akka/snapshot/general/configuration.html).


## The Trading App

Finally, we wanted to try a more involved example which needs more domain modeling and a more sophisticated messaging protocol. The example is inspired by the trading application based on the [Akka trading performance example](https://github.com/akka/akka/tree/master/akka-actor-tests/src/test/scala/akka/performance/trading) but we deviated in multiple aspects. In its current state, the trading application has some quirks, lacks a lot of features and it currently even lets money evaporate in certain circumstances.... That's not nice, but we were able to prototype some concepts and it is a starting place for further experiments and enhancements, which we'll discuss later in more detail.

### The Domain

The purpose of the application is to simulate market participants which want to buy securities. Each participant can place orders: buyers place a bid, sellers place an ask. Bids and asks are matched in an orderbook (one per security) and a trade is made. The algorithm is based on [Akka's OrderBook.scala](https://github.com/akka/akka/blob/master/akka-actor-tests/src/test/scala/akka/performance/trading/domain/Orderbook.scala). It basically tries to match the highest bids with the lowest asks as long as possible. If an order cannot be fulfilled entirely, it is split.

All participants' goods are tracked in accounts: securities are kept in a depot, cash is kept in a deposit. Each account is charged as soon as an order is placed to avoid overcommitment. Upon fulfillment of an order the goods are credited.

### Modeling an Akka app

The application consists of two actors which are coupled by a custom [router](http://doc.akka.io/docs/akka/snapshot/scala/routing.html):

* **`MarketParticipant`**: A market participant periodically places orders. It randomly decides whether to place a bid or an ask and also randomly decides on the offered price which is based on the current market price of the security including a random spread.
* **`OrderBook`**: There is one `OrderBook` actor for each security within the system to match trades. It takes orders and periodically matches them. Afterwards, it notifies the involved `MarketParticipant`s of the successful trade.
* **`OrderRouter`**: We decided to couple `MarketParticipant`s and `OrderBooks`s via a custom router. During startup the router creates `OrderBook` actors. When an order arrives, it decides which `OrderBook` is responsible and forwards the order.

![System Structure of the Trading System](blog/TradingAppActors.png "System Structure of the Trading System")

The diagram below shows the message flow of a trade through the system. The market participant place a bid and an ask through the `OrderRouter` which forwards the messages to the corresponding `OrderBook` for this security. It matches the orders and replies to both parties with `BidResponse` and `AskResponse` on success. The can in turn then adjust their account balances accordingly.

![Message flow through the application](blog/TradingAppMessageFlow.png "Message flow through the application")

### Implementation

The simulation exists in two flavours: A single-node implementation which is bootstrapped in `TradingSimulationApp` and a distributed implementation which is implemented in `RemoteClientApp` which simulates the market and `RemoteClientApp` which simulates order books.

To configure various aspects of the application such as the securities or number or market participants we used Typesafe Config. Wiring of specific implementation is achieved with the [Cake pattern](http://jonasboner.com/2008/10/06/real-world-scala-dependency-injection-di/).

### Open Issues

We very able to try a lot of features of Akka such as become/unbecome, stashing, custom routers or remoting. However, the domain allows to expand the example application further in many different aspects which we'll describe shortly below.

### Domain

Regarding the domain we see the following areas of improvement:

* **Losing money**: The application holds money back in case an order is split or even evaporates it if the buying price differs from the bidding price. This hasn't been much of an issue for our short-running simulation but it clearly is a rather severe problem for a real application. This issue can be solved in different ways. For example, we could cancel orders after a specific amount of time if they cannot be fulfilled or just reserve money instead of really charging the deposit.
* **Acknowledgements**: Acknowledgements of order receipt would allow for easier state tracking by market participants.

### Technological

* **Replication and fault tolerance**: Currently, if an `OrderBook` actor would fail, all open trades and the market valuation would be lost. Using a dedicated [supervisor node](http://doc.akka.io/docs/akka/snapshot/general/supervision.html) and a replication mechanism for each `OrderBook` would make the application far more reliable.
* **Monitoring**: The demo could include a monitoring mechanism to visualize different business metrics such as current market prices, number of open orders, or aggregated revenue and also technical metrics such as messages delivered, message latency and throughput of the system.
* **Performance**: The system is not tuned for performance at all. Based on the monitoring and different scenarios we could look into bottlenecks and tweak the system based on the [vast configuration options of Akka](http://doc.akka.io/docs/akka/snapshot/general/configuration.html).

## Final Thoughts

Although our three lab days were very productive we barely scratched the surface of what's possible with Akka. As you may have guessed from reading this blog post we struggled with some aspects but that's a good sign: After all, we only learn something new by struggling first. We had a lot of fun in this lab and we're looking forward to the next one to explore further aspects of Akka.

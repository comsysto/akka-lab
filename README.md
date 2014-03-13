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

scalaVersion := "2.10.3"

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
  object Receiver {
    case class Message(msg: String)
  }

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

Based on a [Akka Remote Hello-World example](http://alvinalexander.com/scala/simple-akka-actors-remote-example) we wrote a "client" and a "server" application and configured them using Typesafe Config. One of the Actors just needs to kick off the game and then both ping-pong happily ever after. As the message protocol is very simple, the application is well-suited to measure Akka message latencies. Hence, we attached a timestamp to each message using `System#nanoTime()`. However, as stated in the [Javadoc of System#nanoTime()](http://docs.oracle.com/javase/7/docs/api/java/lang/System.html#nanoTime%28%29), it is only suited for time measurements within a single JVM. So, instead of measuring only the latency from one actor to the other, we decided to measure roundtrip time which allows us to use System#nanoTime(). To measure them, both messages are extended by a timestamp property and receive is changed accordingly:

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

## The Trading App

Finally, we wanted to try a more involved example which needs more domain modeling and a more sophisticated messaging protocol. The example is inspired by the trading application based on the [Akka trading performance example](https://github.com/akka/akka/tree/master/akka-actor-tests/src/test/scala/akka/performance/trading) but we deviated in multiple aspects. In its current state, the trading application has some quirks, lacks a lot of features and it currently even lets money evaporate in certain circumstances.... That's not nice, but we were able to prototype some concepts and it is a starting place for further experiments and enhancements, which we'll discuss later in more detail.

### The Domain

The purpose of the application is to simulate market participants which want to buy securities. Each participant can place orders: buyers place a bid, sellers place an ask. Bids and asks are matched in an orderbook (one per security) and a trade is made. The algorithm is based on [Akka's OrderBook.scala](https://github.com/akka/akka/blob/master/akka-actor-tests/src/test/scala/akka/performance/trading/domain/Orderbook.scala). [TODO: Describe matching algorithm very shortly]

All participants' goods are tracked in accounts: securities are kept in a depot, cash is kept in a deposit. Each account is charged as soon as an order is placed to avoid overcommitment. Upon fulfillment of an order the goods are credited.

### Modeling an Akka app

The application consists of two actors which are coupled by a custom [router](http://doc.akka.io/docs/akka/snapshot/scala/routing.html):

* **`MarketParticipant`**: A market participants periodically places orders. It randomly decides whether to place a bid or an ask and also randomly decides on the offered price based on the current market price of the security.
* **`OrderBook`**: There is one `OrderBook` actor for each security within the system to match trades. It takes orders and periodically matches them. Afterwards, it notifies the involved `MarketParticipant`s of the successful trade.
* **`OrderRouter`**: We decided to couple `MarketParticipant`s and `OrderBooks`s via a custom router. During startup the router creates `OrderBooks` actors. When an order arrives, it decides which `OrderBook` is responsible and forwards the order.

### Implementation

[TODO: Mention Cake pattern, Typesafe Config]
The simulation exists in two flavours: An in-memory implementation which is bootstrapped in `TradingSimulationnApp` and a distributed implementation which is implemented in `RemoteClientApp` which simulates the market and `RemoteClientApp` which simulates order books.

#### Custom Routing

### Open Issues

Although we very able to try a lot of features (


### Domain
- fault tolerance
-- replication
-- acknowledgements
- we're deleting money
- Order canceling
- Statistics

### Technology
- Monitoring
- Statistics
- Performance
- Scaling
- remote addressing
- cusltering

## Final Thoughts

....

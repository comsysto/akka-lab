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
### Update and prepare OrderBook router
We started our lab by upgrading to the latest Akka version 2.3.2 and got some compile errors regards routing. So we went through the [migration guide](http://doc.akka.io/docs/akka/2.3.2/project/migration-guide-2.2.x-2.3.x.html) and found the following comment which seems to belong to us:

> The API for creating custom routers and resizers have changed without keeping the old API as deprecated. 
> That should be a an API used by only a few users and they should be able to migrate to the new API without
> much trouble.
>
>Read more about the new routers in the documentation for Scala and documentation for Java.

With 2.3 there akka introduces two distignushable types of routers: `Pool` and `Group`. Pools are all the routers that manages their routees by itself (creation and termination as child actors) and Group means all the routers	that gets the routees configured from outside. 

So in our case it makes sense to use the `Pool` pattern because the router setup an manage all the `OrderBook` actors by itself. In addition our router later needs to be configurable on runtime and so we end up with a dedicated actor which uses an embedded router as figured out in the [akka documentation](http://doc.akka.io/docs/akka/2.3.2/scala/routing.html#A_Router_Actor). So our router actor can foward messages to it's `OrderBook`s but handle routing configuration messages by itself if necessary:

```
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
```

[TODO] Describe code!
	

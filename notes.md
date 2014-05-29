Monitoring

Statsd -> visualization is best done with a preconfigured Docker image

How to install Docker on a Mac? http://docs.docker.io/installation/mac/

1. Install Virtualbox: https://www.virtualbox.org/wiki/Downloads
2. brew install boot2docker


boot2docker init
boot2docker up
export DOCKER_HOST=tcp://localhost:4243
docker pull kamon/grafana_graphite
docker run -d -v /etc/localtime:/etc/localtime:ro -p 80:80 -p 8125:8125/udp -p 8126:8126 --name kamon-grafana-dashboard kamon/grafana_graphite

--

After getting acquainted with Akka in [our first Akka lab](http://blog.comsysto.com/2014/05/09/reactive-programming-with-akka-and-scala/), [@RoadRunner12048](https://twitter.com/RoadRunner12048) and [me](https://twitter.com/dmitterd) wanted to try out monitoring Akka to get a better understand and play around with clustering support. We used a very rough Stock trading simulation that we've implemented in the first lab as subject for our experiments.

## Upgrade to Akka 2.3.2

TODO: Describe me - reimplemented Router

## Monitoring Akka

In our last lab we wanted to find out more about the current state of our Akka system at runtime. Therefore, we added monitoring to our Akka sample trading system. To get going fast in our three lab days we thought we'd settle for the rather new project [Kamon](http://kamon.io/), use their [Docker image](https://github.com/kamon-io/docker-grafana-graphite) that includes all necessary components including a fancy dashboard and we're good to go. Unfortunately, it wasn't that easy.

### Gathering Monitoring Data

First, we need to gather some data. Kamon already provides an integration for Akka in the 'kamon-core' module which can measure metrics such as the length of an Actor mailbox or message processing time. The data are gathered using AspectJ proxies, so we need to add an AspectJ weaver at runtime as Java agent as described in the [Getting Started section of the Kamon documentation](http://www.kamon.io/get-started/). The AspectJ weaver can also be applied when using `sbt run`, however the Kamon documentation is outdated and we had to tweak the documented configuration to make it work (see our [akka-lab Github project](https://github.com/comsysto/akka-lab)).

However, gathering data is only part of the story. We have just collected a lot of data so far but we do not see anything yet.

### Monitoring Dashboard

Currently, Kamon provides two possibilities to export monitoring data: [NewRelic](http://www.kamon.io/newrelic/) and [StatsD](http://www.kamon.io/statsd/). The NewRelic integration is intended for Spray applications. As we have just a plain Akka application we went for StatsD. StatsD is a Node.js daemon that receives monitoring data via UDP and forwards them to so-called backends which display data.

The Kamon team provides a [Docker image](https://github.com/kamon-io/docker-grafana-graphite) where StatsD and a suitable backend is already preconfigured. The idea is really great but we weren't able to get it running on our Mac Books. Therefore, we installed StatsD and the Graphite backend ourselves. We followed the installation instructions of [Steve Akers](http://steveakers.com/2013/03/12/installing-graphite-statsd-on-mountain-lion-2/) with the following changes:

First, when installing cairo, Homebrew will show the following caveats:

<code>
==> Caveats
If you need Python to find the installed site-packages:
  mkdir -p ~/Library/Python/2.7/lib/python/site-packages
  echo '/usr/local/lib/python2.7/site-packages' > ~/Library/Python/2.7/lib/python/site-packages/homebrew.pth
</code>

We executed these commands and verified that everything works by entering `import cairo` in a python shell (you should not get any errors). If Graphite does not find cairo, it cannot render any graphics.

Graphite is a Django application. When we opened the Graphite start page on our server, we just got 'Import Error: No module named defaults'. What a pity! It turned out that [we have to install Django 1.5]((http://stackoverflow.com/questions/19962736/django-import-error-no-module-named-django-conf-urls-defaults)) instead of the current version 1.6. Django 1.5 can be installed via `sudo pip install django==1.5`.

After everything is installed, we can start the StatsD daemon and Graphite:

~statsd $ node stats.js config.js
/opt/graphite $ sudo python bin/run-graphite-devel-server.py .

Afterwards, we start a simple Ping Pong server and client Akka application. In Graphite, we can then look at various metrics that are gathered by Kamon, e.g. the mailbox size:

TODO: Insert image here

--
statsd (node) config:

{
  graphitePort: 2003
, graphiteHost: "localhost"
, port: 8125
, backends: [ "./backends/graphite" ]
}

## Clustering

* [Daniel] Idea: Router lokal / Forward remote (zweistufig); 1 zentraler "ShardManager"
* [Martin] Akka Spec: http://doc.akka.io/docs/akka/2.3.2/scala/cluster-usage.html
    - Cluster Events (MemberUp, et.al.), nicht komplett transparent, man muss schon was tun
    - eigene Routerimplementierungen (vgl. Broadcast), aber man muss sich Gedanken machen
    - Pool / Group (nur verweisen); Metrikbasierte Cluster
* [Daniel] Trading Shard Protocol
* [Daniel] (Maybe) some code

## Zusammenfassung / Fazit



---

1. Download Virtualbox and [Vagrant](http://www.vagrantup.com/downloads.html)


vagrant init # creates the Vagrantfile


# Based on http://serversforhackers.com/articles/2014/03/20/getting-started-with-docker/

config.vm.box = "coreos"
config.vm.box_url = "http://storage.core-os.net/coreos/amd64-generic/dev-channel/coreos_production_vagrant.box"
config.vm.network "private_network", ip: "172.12.8.150"

# plugin conflict
if Vagrant.has_plugin?("vagrant-vbguest") then
    config.vbguest.auto_update = false
end


vagrant up # loads CoreOS

# to see running machines - see also https://coreos.com/docs/running-coreos/platforms/vagrant/
~/Documents/workspace/akka-lab $ vagrant status
Current machine states:

default                   running (virtualbox)

~/Documents/workspace/akka-lab $ vagrant ssh default

# You're now in coreos

docker pull kamon/grafana_graphite

#docker run -d -p 80:80 -p 8125:8125/udp -p 8126:8126 --name kamon-grafana-dashboard kamon/grafana_graphite

core@localhost ~ $ docker run -d -p 80:80 -p 8125:8125/udp -p 8126:8126 kamon/grafana_graphite
1674d8d65979476bd93cbe07cf41d23de21ab291bd0fa28c4c3377425bfb50dd

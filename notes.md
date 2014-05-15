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

To get going fast in our three lab days we thought we'd settle for [Kamon](http://kamon.io/), use their [Docker image](https://github.com/kamon-io/docker-grafana-graphite) that includes a fancy dashboard and we're good to go. Unfortunately, it wasn't that easy.

### Gathering Monitoring Data

TODO: Describe integration of Kamon; don't forget sbt integration (AspectJ weaver)

### Monitoring Dashboard

TODO: Describe installation process shortly, mention that we could not get the Docker image to run properly

Follow installation instructions at http://steveakers.com/2013/03/12/installing-graphite-statsd-on-mountain-lion-2/

with the following changes:

#sudo brew install cairo
#sudo brew install py2cairo

==> Caveats
If you need Python to find the installed site-packages:
  mkdir -p ~/Library/Python/2.7/lib/python/site-packages
  echo '/usr/local/lib/python2.7/site-packages' > ~/Library/Python/2.7/lib/python/site-packages/homebrew.pth

verify with:

python
//enter: import cairo // should work

// django 1.6 does not work (opening the Graphite homepage produces an error 'Import Error: No module named defaults' - http://stackoverflow.com/questions/19962736/django-import-error-no-module-named-django-conf-urls-defaults
sudo pip install django==1.5

--
statsd (node) config:

{
  graphitePort: 2003
, graphiteHost: "localhost"
, port: 8125
, backends: [ "./backends/graphite" ]
}

## Clustering

http://doc.akka.io/docs/akka/2.3.2/scala/cluster-usage.html

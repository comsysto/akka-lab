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

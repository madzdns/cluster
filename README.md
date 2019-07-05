# What is MadzDns?

## MadzDNS is a layer 7 DSR (Direct Server Return) load-balancer acting as a service.
It is very configurable and has many additional features.

For example, for some protocols, It quickly acts as 
a proxy for a small part of the request and then, for the rest, it does the DSR job. 
Also it has lots of features to solve different problems for different setups. 
It's not just a load-balancer, it is also a resource manager to monitor services inside 
servers as well as server parameters like `CPU`,`RAM`, `Bandwidth` and so on. 
It also cares about geographical distributions.<br> 

Following is the illustration of MadzDNS's solution:<br>

![MadzDNS distribution](/madzdns/cluster/blob/master/madzvpn_solution.png?raw=true)

<br>

At the above picture, Active load-balancers are MadzDNS's Cluster. 
Servers 1-4 belong to customers. The servers join to MadzDNS's network as connectors. 
load-balancers are all active at the same time acting as a cluster. 
Requests to customer's servers will arrive to these load-balancers and then redirected to the proper server. 

Here, the algorithm is much efficient. MadzDNS distributes load with calculating multiple factors such as the 
server's `CPU` load, `Memory` load, `Network` load in bottle necks, Service type, Physical Location of requesting user relative to servers, 
The time of requests! Different routing paths to servers, Server availability, Service availability and all of these stuff are amazingly configurable.<br>
---------------------------------

MadzDNS cluster, has a fast algorithm to calculate different parameters for servers and based on the limits you have configured will decide what would be the best among your servers to process current request.<br>
When a request comes to one of the load-balancers, it uses the last fresh data collected from the servers. Thus, a failed node would not affect loading time of your service. At the shown picture, a request from North America can be serverd by the Server 3 to make your service much efficient and faster.
If server 3 was dead or it's processor was saturated or even if the service inside the server was not working, the request can be redirected to Server 4 to keep Round Trip Time small (the behaviour is configurable).<br>
   
For `CPU`, `Memory` and `Network` load, while the percentage of each of them will be calculated and compared with each other, you can configure each one of your servers with desired load range and this way, if at any time, some of your servers exceed those limits, will be considered as loaded for those particular parameters and their chances will be very decreased to be elected for that particular request. 
(Imagine a case in which you have configured a server in a way that being physically closed is more important than being loaded. Thus, while a server's physical parameters are not as good as others, if it is close enough to a request source, it'll be elected to serve that particular request. This means you can configure almost everything!)<br>  
For network load, in most cases, server's local network is not the source of congestion and it is some gateway a little far from the server and called bottle neck. MadzDNS does not rely on servers local network and calculates bottle neck loads. This brings a great impact in quality of  your services.<br>
Also you can configure a server to not have these parameters for calculations.<br>
For service types, you can configure a weight to some of your services over others. For example suppose you have a IBM websphere application server installed in one of your servers and Apache tomcat in others, if you believe websphere is more capable than others, your can set a weight on that service in your server.<br>
For physical locations, you can configure your servers to have priority over some areas. For example suppose for a server in Germany you have setup locations in this order: Berlin, DE, EU, * and for the other server in England, you have setup London, GB, EU, * (codes are two letter countries and continents code and * means the whole world) then for a request from a user in Germany, while both the servers are setup to serve Europe, the first server has more priority than the second and more likely to be elected unless it was too busy and the second one had a better results in calculations.<br>
Also suppose another example in which a server configured with Berlin,DE,EU,* and the other with London,GB,US,EU,* for a request from Amsterdam the first server would have more priority because EU was defined in third location while for the second, in the forth.<br>
For time of request, you can setup your servers to serve different locations in different times, for example a server can serve Italy during the day of Italy and nights serve other locations.<br>
You can have multiple setups over multiple ip addresses of the server. Different ip address (configured as secondary) and different aliased nics will be considered as different nodes.<br>
MadzDNS will detect failed server as well as failed services and does not reroute requests to them.<br>
In general, there are two levels of rerouting requests, in DNS level, in Protocol level.<br>
In DNS, dns request comes to MadzDNS and are rerouted to the proper servers. In traditional Geographical DNS solutions (known as GEO DNS or GDNS), GDNS service resolves different IP addresses based on requesting client's location. But it is not very good solution. Because servers are configured statically in GDNS load-balancer and they have nothing to do with servers performance. It only cares about location. If a client requests from india, DNS server only picks a nearby server and don't checks to see if that server is proper or not. MadzDNS has integerated GDNS to its resource management capabilites and thus it is solved in our solution. We have also a way to determine what is the actual service after dns resolution and check if that service is alive in the servers before rerouting. load-balancing in dns level have some advantages and that is being transparent of underling service. A user requests an address that is serving by MadzDNS, then MadzDNS gets the request and choose the best possible server and answers with the ip address(s) of proper server(s).<br>
But it has some limitations. The actual request of requesting client is behind a recursive dns resolver and it is that resolver's ip address that reaches MadzDNS. If dns is configured properly, and clients are using their ISP's dns server, then both the client and recursive dns resolver are in the same location and everything is fine, otherwise if client is using public dns servers like 8.8.8.8, then the location can be completely different and there is no way for MadzDNS to find out. That is why we have implemented another level called protocol level. 
At this level, MadzDNS support some major protocols such as HTTP(`WEB`, `HLS`, `HDS`, `SOAP`, `REST`), `RTMP`, `RTMPT`, `RTMPE`, `RTMPTE`, `RTSP`, continuous presence. 
This way MadzDNS directly accepts clients requests and after determining the exact location, reroutes clients to the proper servers.<br>
There are also other awesome features like resolve hints, Static Geo resolvers, Round robing load-balancing and so on<br>
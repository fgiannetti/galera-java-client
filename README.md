galera-java-client
======

## Overview

`galera-java-client` is a client written in Java for MariaDB Galera Cluster and Percona XtraDB Cluster.

It is designed to be use as an alternative option to connect JVM applications to MariaDB/Percona galera nodes without HAProxy. 

The client has a load balance policy to distribute connection requests, it discovers new joined nodes automatically and activates/deactivates nodes based on Galera specific states, the primary component membership and network errors.

It doesn't implement the mysql protocol or manage jdbc connections by itself. It relies on mariadb-java-client to open connections and HikariCP to manage the connection pools against the MariaDB/Percona nodes.


## Status

`galera-java-client` is in an early stage of development. It is not recommended to be used in production yet.

## Features

* **Ignoring donor nodes:** Configure this flag with `new GaleraClient.Builder().ignoreDonor(true)`. When this flag is enabled, donor nodes are marked as down, so you will not get connections from donor nodes. Default value: true

* **Supporting custom connections:**
  You can get a connection with a simple `client.getConnection()`. In this case, you'll get a connection from any node and the consistency level will be the global value configured in your mariaDB wsrep_sync_wait (or wsrep_causal_reads for earlier versions). 
  But you can also use something like `client.getConnection(ConsistencyLevel.SYNC_READ_UPDATE_DELETE, true)`. 
  
* **GaleraClientListener:** You can extend functionality, for example to report some metrics, setting on the client builder an implementation of GaleraClientListener, which has callbacks for the following events: activating/removing node, marking node as down and selecting a new master node. The default implementation just logs this events.       

## Maven

```xml
<dependency>
    <groupId>com.despegar</groupId>
    <artifactId>galera-java-client</artifactId>
    <version>1.0</version>
</dependency>
```

## How to use it

#### 1) Build the client

```java
  GaleraClient client = new GaleraClient.Builder()
                            .seeds("maria-1, maria-2")
                            .database("myDatabase")
                            .user("user")
                            .password("password")
                            .connectTimeout(2000)
                            .connectionTimeout(2000)
                            .readTimeout(1000)
                            .idleTimeout(3000)
                            .discoverPeriod(2000)
                            .maxConnectionsPerHost(20)
                            .ignoreDonor(true)
                            .retriesToGetConnection(5)
                            .build();
  
```

#### 2) Getting a Connection

```java
Connection connection = client.getConnection(ConsistencyLevel.CAUSAL_READS_ON, false);
```
- The first parameter specifies the consistency level for this connection (it will be reseted to the global value when the connection returns to the pool). 
- The second parameter means holdsMaster. The first time you ask for a connection with holdsMaster in true, galeraClient will choose a master node and all the following connections asked with **holdsMaster=true** will be from that master node (GaleraClient only chooses a new master node when the current one is marked as down/removed). It is a useful feature when you want all your writes in the same node of the cluster.   


#### 3) Releasing resources
```java
client.shutdown()
```

## Implementation details

  * mariadb-client 1.1.7
  * HikariCP 2.3.5

## Contributions

`galera-java-client` is open to the community to collaborations and contributions


galera-java-client
======

## Overview

`galera-java-client` is a client written in Java for MariaDB Galera Cluster and Percona XtraDB Cluster.

It is designed to be use as an alternative option to connect JVM applications to MariaDB/Percona galera nodes without HAProxy. 

The client has a load balance policy to distribute connection requests, it discovers new joined nodes automatically and activates/deactivates nodes based on Galera specific states, the primary component membership and network errors. In order to achieve this, galera-java-client opens a little connection pool that queries the cluster status periodically. This pool is separated from the pool that serves the requests.

It doesn't implement the mysql protocol or manage jdbc connections by itself. It relies on mariadb-java-client to open connections and HikariCP to manage the connection pools against the MariaDB/Percona nodes.


## Features

* **Ignoring donor nodes:** Configure this flag with `new GaleraClient.Builder().ignoreDonor(true)`. When this flag is enabled, donor nodes are marked as down, so you will not get connections from donor nodes. Default value: true

* **Supporting custom connections:**  You can get a connection with a simple `client.getConnection()`. But you can also use something like `client.getConnection(ConsistencyLevel.SYNC_READ_UPDATE_DELETE, SomeElectionNodePolicy)`.
 If you invoke the method without arguments the consistency level will be the value set on `consistencyLevel` property of the galera-java-client. And if this value is null, the global value configured in your mariaDB wsrep_sync_wait (or wsrep_causal_reads for earlier versions) will be used. Regarding the node election policy we will use the one that was configured on `nodeSelectionPolicy` property of the client. 
 The `ConsistencyLevel` values can change depending of the Galera versions as follows: 
  * **Galera 5.5.39 - MariaDB Galera 10.0.x**
    * SYNC_OFF
    * SYNC_READS
    * SYNC_UPDATE_DELETE
    * SYNC_READ_UPDATE_DELETE
    * SYNC_INSERT_REPLACE
  * **Earlier versions**
    * CAUSAL_READS_OFF
    * CAUSAL_READS_ON

* **GaleraClientListener:** You can extend functionality, for example to report some metrics, setting on the client builder an implementation of GaleraClientListener, which has callbacks for the following events: activating/removing node, marking node as down, selecting a new master node and reporting metrics. The default implementation just logs this events.       

* **Metrics:** You can get metrics from Hikari pool (total / active / idle / pending connections & percentile 95 of waiting / usage time) and from de underlying database (threads connected) each time a discovery occurs. You must configure metricsEnabled on galera client. Remember that the default listener only logs the metrics.   

* **ElectionNodePolicy:** You can configure `com.despegar.jdbc.galera.policies.RoundRobinPolicy` (which is the default) or `com.despegar.jdbc.galera.policies.MasterSortingNodesPolicy`. You can also provide a custom election node policy only with supplying a fully qualified name of the implementation of `com.despegar.jdbc.galera.policies.ElectionNodePolicy`. This policy will be used each time you invoke getConnection() in order to select a node and get a connection from it. There is another method, getConnection(..., ElectionNodePolicy) that let you to specify a different election node policy than the default one. 

* **TestMode:** You can use testMode flag in order to disable discovery node capability. This will disable checks for node statuses too. This mode must be used for test purposes only.
 
## Maven

```xml
<dependency>
    <groupId>com.despegar</groupId>
    <artifactId>galera-java-client</artifactId>
    <version>1.0.19</version>
</dependency>
```

## How to use it

#### 1) Build the client

```java
  GaleraClient client = new GaleraClient.Builder()
                            .poolName("testPool")
                            .seeds("maria-1, maria-2")
                            .database("myDatabase")
                            .user("user")
                            .password("password")
                            .discoverPeriod(2000)
                            .ignoreDonor(true)
                            .retriesToGetConnection(5)
                            .build();
  
```
There are few more options for configuration, you can check these in the [source code].

#### 2) Getting a Connection

```java
Connection connection = client.getConnection(ConsistencyLevel.CAUSAL_READS_ON, false);
```
- The first parameter specifies the consistency level for this connection (it will be reseted to the global value when the connection returns to the pool). 
- The second parameter means holdsMaster. The first time you ask for a connection with holdsMaster in true, galeraClient will choose a master node and all the following connections asked with **holdsMaster=true** will be from that master node (GaleraClient only chooses a new master node when the current one is marked as down/removed). It is a useful feature when you want all your writes in the same node of the cluster.   

#### 3) Releasing resources
```java
connection.close();

client.shutdown();
```
The `connection.close()` returns the connection to the pool and `client.shutdown()`  stops all the underlying machinery of the client.   

#### For a more complete example, see [CausalReadsTest].

## Deployment

mvn clean deploy

The new artifact will be on https://oss.sonatype.org/content/repositories/releases/com/despegar/galera-java-client/

## Implementation details

  * mariadb-java-client 1.3.2
  * HikariCP 2.4.3

## Contributions

`galera-java-client` is open to the community to collaborations and contributions

[source code]: https://github.com/despegar/galera-java-client/blob/master/src/main/java/com/despegar/jdbc/galera/GaleraClient.java#L229

[CausalReadsTest]:https://github.com/despegar/galera-java-client/blob/master/src/test/java/com/despegar/jdbc/galera/CausalReadsTest.java

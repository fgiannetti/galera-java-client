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

* [TODO]

## Maven

[TODO]

### Configure

[TODO]

## Implementation details

  * mariadb-client 1.1.7
  * HikariCP 2.2.5

## Contributions

`galera-java-client` is open to the community to collaborations and contributions


Change log
======

## 1.0.13 (2015-12-14)

 *  Upgrading Maria connector to 1.3.2
 *  Upgrading Hikari dependency to 2.4.3 (java 7/8 version)
 *  Now you can name your pool 
 *  Fix: Throwing exception when no active node left
 *  Exposing metrics pool (total / active / idle / pending connections & percentile 95 of waiting / usage time)
 *  Forcing discovery when a client connection fails 
# StockStream Architecture and Design

This document attempts to explain the design of the StockStream service as well as a high-level overview of how the service interacts with other components on the StockStream server.

## Overview

The StockStream server runs 3 major components.

1. The StockStream service. This is the main component whose source is contained inside this source repository. Below, the design of this service will be explained in detail.
2. OBS Studio. This is the component responsible for rendering and publishing the stream. It's setup by using browser instances that connect to the StockStream service as well as a screencapture of the Android emulator.
3. A custom Android Virtual Device (AVD) running inside an Android emulator. Although, the device uses a standard Android system image, the device has been setup with a custom screen resolution to render correctly in the OBS instance.

## StockStream Service

The StockStream Service is the main component. It is responsible for connecting to Twitch and counting votes, connecting to Robinhood and placing trades, loading stock info, news, computing scores and all other necessary operations.
The StockStream Service roughly follows the [JavaBeans](https://en.wikipedia.org/wiki/JavaBeans) pattern for initialization of classes. In short, each class in StockStream (excluding the simple 'data' classes) is intended to act on its own, using the Scheduler, PubSub, MarketClock and GameClock for communication and timing.

### StockStream Service Components

Although StockStream consists of many different classes, there are several major classes and concepts that are core to the overall system.
These are detailed below.

#### SparkServer.java

The SparkServer class creates an instance of [SparkJava](http://sparkjava.com/) running on port 4567, which is the entry point for OBS to connect to the Service.
The src/main/resources/public directory is made available like any other web server. The pages are connected to the APIs only using a simple naming convention- the html page minus the .html should be the API that the page connects to.
For example, if we look at the assetsList.html file, it uses javascript to pull the list of assets from the assetsList API and display/animate them using jquery/velocity.js.

#### PubSub.java

PubSub (short for publish-subscribe) is an in-memory messaging system, used for notifying other classes when instances of certain classes become available.
For example, when an order to buy or sell a stock is completed, an instance of OrderResult is published.
Any classes that have subscribed Functions or Runnables to the OrderResult.class will be called with an instance of the OrderResult instance that was published.


#### Scheduler.java

Scheduler allows you to Schedule Functions or Runnables to be executed on a time-based or event based interval. Many classes within the service use this class to schedule 'tick' functions to run on very short intervals

#### MarketClock.java

MarketClock uses the Robinhood Markets API to determine market open and close times. Its API allows other classes to subscribe to MARKET_OPEN or MARKET_CLOSE events with any time-based offset.

#### Concepts

The StockStream service has been refactored many many times. Its current iteration breaks down groups of classes into common java-package levels. Below is a high-level overview of each package.

* **application** - This package contains high-level components and the Main function.
* **cache** - This is the caching layer. Caches are needed because the UI needs to update very quickly and we don't want to make network calls. Therefore the SparkJava APIs pull data from the in-memory caches. The caches are updated mostly via periodically pulling data via the Scheduler class or updating automatically using the PubSub class.
* **computer** - This group of classes are simply groups of methods where common tasks live.
* **data** - This package mostly contains simple classes. The intention of these classes is the be simple data classes with no logic (unfortunately some classes do have a bit of logic in them).
* **environment** - This is where classes that interact with the operating system live. Currently this contains a class to communicate with OBS and another to communicate with the Android emulator.
* **logic** - This package contains the core logic components (PubSub/Scheduler/MarketClock) as well as two sub-packages which contains the Scoring and core GameEngine components.
* **network** - This package contains classes that communicate with remote services. This includes the Robinhood, Twitch, News and AWS components.
* **util** - Utility classes, fairly straightforward utility classes. This is distictly different from the computer classes in that utility classes do not rely on any other beans.




# StockStream


# Build Instructions

* Clone the latest repository.
* Install a JDK if you don't have one already. (java/javac must be in your PATH)
* Go to the StockStream source directory and run ./gradlew fatJar

# Run Instructions

## There are 4 stages: TEST, LOCAL, GAMMA, PROD

Stages are used to configure different modules to use 

* TEST is used only for unit tests and integration tests. It uses a paper broker and connects to an IRC server on localhost.

* LOCAL is used for stress testing and manual testing. It uses an IRC on localhost and a paper broker.

* GAMMA is mostly used for testing Robinhood integration. It connects to Robinhood but IRC on localhost.

* PROD uses Twitch Chat for input and Robinhood. Don't use this unless you know what you're doing ;)


To actually run the system, take a look at the run-server.sh script in the root directory of the source tree.
Basically you just need to run 'java -jar' and pass the path to the jar you built prior.

You must pass the stage or it will silently exit. Valid arguments are: -t -l -g -p
Each corresponding to the stage.

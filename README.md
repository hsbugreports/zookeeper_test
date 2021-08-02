# Overview
This project simulates multiple cluster groups created using the Apache Curator LeaderSelector recipe. It's designed to recreate an apparent issue where under some circumstances orphaned ephemeral nodes are created during zookeeper failovers/network blips which results in no leader being elected.

# Getting Started
To start the containers run the following command from this project's parent directory: `docker-compose up -d --build`.

Once started, to view the various components run the command `docker-compose ps`

To view the logs from a specific component use: `docker logs -f <component>` using a specfic component name displayed using the prior command. To follow the logs from all components use: `docker-compose logs -f`.

# Simulating Zookeeper Failovers
To restart the zookeeper docker containers run: `./scripts/restart.sh <number_of_restart_cycles>` where the argument is the number of times the containers should be restarted. It typically requires between 50 and 100 restarts before permanently orphaned ephemeral nodes are observed however it can also require many more.

# Detecting Orphaned Ephemeral Nodes
When a node takes leadership it will print a log message saying: "**Taking leadership**", similarly while the node has leadership it will periodically write: "**Leadership heartbeat**" to the logs. If at some point ephemeral nodes are orphaned you will see "**Possible orphaned ephemeral node**" written to the logs. Sometimes the orphaned ephemeral nodes will be reclaimed and leadership will recover, however, in some cases they will not. It is in that case that no leader will be elected again for the cluster. If the ephemeral node is permanently orphaned leadership heartbeats for the node in question will stop. The state of ephemeral nodes on a particular zookeeper node can be dumped by running :`./scripts/dump.sh <node_number>` where the argument is the zookeeper node to be dumped, it should be a number, either 1, 2, or 3.
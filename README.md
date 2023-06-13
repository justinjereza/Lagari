Lagari
======

This Minecraft plugin chops blocks that are in contact with each other. By default, these are trees chopped with any axe. Its behavior can be modified through its [configuration file](https://github.com/justinjereza/Lagari/blob/master/src/main/resources/config.yml) which is reloaded every time a block is broken so it is not necessary to restart the server on each configuration change. What qualifies as a log, leaf, and tool is listed according to the constants specified in the [API reference](https://hub.spigotmc.org/javadocs/bukkit/org/bukkit/Material.html).

Requirements
------------
* GNU make
* podman

How to Build
------------

Execute the following commands to build the plugin:

```console
$ git clone https://github.com/justinjereza/Lagari.git
$ cd Lagari
$ make
```

The resulting plugin is named `Lagari-<version>.jar`.

The download of the `server.jar` file from the Mojang server might fail with a connection reset exception. If that happens, retry running `make` until it succeeds.

If the SpigotMC build is interrupted and then subsequently resumed, it may result in an error similar to the following:

```
[ERROR] Failed to execute goal net.md-5:specialsource-maven-plugin:1.2.4:remap (remap-members) on project spigot: Error creating re
mapped jar: Could not find artifact org.spigotmc:minecraft-server:csrg:maps-spigot-members:1.19.4-R0.1-SNAPSHOT in minecraft-librar
ies (https://libraries.minecraft.net/)
```

If that happens run `make distclean` before running `make` again.

Running a SpigotMC Server
-------------------------

The container image that is built is primarily used as a Java build environment but it can also be used to run a SpigotMC server with the Lagari plugin automatically installed using `make run`. By default, the server data is stored in `spigotmc-data`. The data location can be changed with `make SPIGOTMC_DATA=mydata run`. The server data is not removed by `make distclean`.

The server console can be attached with `podman attach spigotmc` and the container is removed when the server is stopped.

As with server data, `SPIGOTMC_NAME` and `SPIGOTMC_VERSION` can respectively be set to use a different container name and to select a server version to build.

Documentation
-------------

The source documentation can be generated with `make doc` and will be placed in the `doc` directory.

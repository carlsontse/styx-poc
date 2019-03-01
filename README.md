Graphql changes are in [here](https://github.com/carlsontse/styx-poc/tree/master/plugin-examples/src/main/java/com/hotels/styx)

Styx config using the hydra plugin [here](https://github.com/carlsontse/styx-poc/blob/master/distribution/conf/default.yml#L54)

Styx config overriding the file routing reading w/ graphql [here](https://github.com/carlsontse/styx-poc/blob/master/distribution/conf/default.yml#L63)

The graphql plugin-example is a hack, we can totally make Styx take a plugin for reading routes as well..

If you make changes in 'plugin-example', you need to clean and deploy it first since it's not part of the 'make e2e'.
However, the Styx Proxy module needs to take that updated jar.

Do something like the below to create an image
```bash
docker build . -t styx
``` 

Do something like the below to run it
```bash
docker run -p 8089:8089 -p 9000:9000 styx
```

Right now for the poc:
* Everything (graphql server, orly-hydra-styx-plugin) is hardcoded for **192.168.99.100** instead of localhost because assuming you are using a Mac, Docker will use this address.
* The orly-hydra jar file is expected to be copied to the same repo as Styx (as part of docker packaging)

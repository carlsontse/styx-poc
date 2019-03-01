Graphql changes are in [here](https://github.com/carlsontse/styx-poc/tree/master/plugin-examples/src/main/java/com/hotels/styx)

Styx config using the hydra plugin [here](https://github.com/carlsontse/styx-poc/blob/master/distribution/conf/default.yml#L54)

Styx config overriding the file routing reading w/ graphql [here](https://github.com/carlsontse/styx-poc/blob/master/distribution/conf/default.yml#L63)

The graphql plugin-example is a hack, we can totally make Styx take a plugin for reading routes as well..

Any changes made to the ory/hydra plugin will need to be rebuilt and jar copied over to the styx project and checked in since i'm relying on it for the docker image. I know it's ugly but it's how Styx is loading the plugins, can probably have the Docker file get it from artylab possibly. 

1. Deploy the 'plugin-example' which contains the graphql control plane reader
```bash
cd plugin-examples
mvn clean deploy
``

2. Generate the distribution (get out of the plugin-examples folder)
```bash
cd ..
make e2e
````

3. Do something like the below to create an image
```bash
docker build . -t styx
``` 

4. Use the docker compose here which will start the above image up plus Ory/Hydra and the Traffic Director Poc: https://github.expedia.biz/EGPlatform/apigateway-docker-compose-poc 

Right now for the poc:
* Everything (graphql server, orly-hydra-styx-plugin) is hardcoded for **192.168.99.100** instead of localhost because assuming you are using a Mac, Docker will use this address.
* The orly-hydra jar file is expected to be copied to the same repo as Styx (as part of docker packaging)

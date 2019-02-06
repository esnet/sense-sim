A tool for generating OpenNSA configurations based on all NSA and topologies
defined in the NSI-DDS. These configurations are then used to drive a massive
SENSE-RM simulation.

Note: This application is dependent on the nsi-common-lib and nsi-dds-lib
java libraries located at:

	https://github.com/BandwidthOnDemand/nsi-common-lib
	https://github.com/BandwidthOnDemand/nsi-dds-lib 

usage: generate.sh -dds <dds server url> ...
 -dds <arg>      DDS server URL.
 -out <arg>      Directory to write genrated files.
 -pwd <arg>      Database user password for use by SENSE and OpenNSA.
 -rm <arg>       SENSE-NSI-RM configuration template.
 -schema <arg>   Location of OpenNSA database schema file.
 -user <arg>     Database user identifier for use by SENSE and OpenNSA.

Example:
./generate.sh -dds https://nsi-aggr-west.es.net/discovery -out output -user sense -pwd BobIsYourUncle \
	-rm src/main/resources/sense-rm.yaml -schema src/main/resources/schema.sql

Files generated:
nsa0.conf	The OpenNSA configuration file.
nsa0.nrm	The OpenNSA port definition file.
nsa0.tac	The OpenNSA twisted startup file.
sense0.yaml	The SENSE-NSI-RM configuration file.
db.sql		The postgresql schema file.
peer.xml	The NSI-DDS peer discovery URL for OpenNSA instances.
sandbox.sh	The OpenNSA startup scripts for all NSA.


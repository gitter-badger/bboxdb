# Tools

## Distributed Selftest
Warning: The selftest will recreate the distribution group "2_testgroup"

Usage: DistributedSelftest <Cluster-Name> <Cluster-Endpoint1> <Cluster-EndpointN>

Example:

    java -classpath "target/*":"target/lib/*":"conf":"." de.fernunihagen.dna.scalephant.tools.DistributedSelftest mycluster node1:2181
    
## Local Selftest
Warning: The selftest will recreate the table "2_testgroup_testtable"

    java -classpath "target/*":"target/lib/*":"conf":"." de.fernunihagen.dna.scalephant.tools.LocalSelftest
---
layout: page
title: "Tools"
category: tools
date: 2013-07-21 12:18:27
---

# Tools

## Distributed Selftest
Warning: The selftest will recreate the distribution group `2_testgroup`

Usage: `DistributedSelftest <Cluster-Name> <Cluster-Endpoint1> <Cluster-EndpointN>`

Example:

    java -classpath "target/*":"target/lib/*":"conf":"." org.bbox.tools.DistributedSelftest mycluster node1:2181
    
## Local Selftest
Warning: The selftest will recreate the table `2_testgroup_testtable`

Usage: `LocalSelftest <Iterations>`

Example:

    java -classpath "target/*":"target/lib/*":"conf":"." org.bbox.tools.LocalSelftest 10
    
## SSTableExaminer
The SSTableExaminer dumps a tuple for given key from a SSTable.

Usage: `SSTableExaminer <Tablename> <Tablenumber> <Key>` 

Example:

    java -classpath "target/*":"target/lib/*":"conf":"." org.bbox.tools.SSTableExaminer 2_testgroup_testtable 200 951920
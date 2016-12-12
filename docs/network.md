# The network protocol of the bboxdb

The protocol of the bboxdb is based on frames. Each frame consists of a header and a body. The header has a fixed size, the body has a variable size. It exists two types of frames: request and response frames. The request frame is send from the client to the bboxdb, the response frame is send from the bboxdb to the client.

## The request frame

    0         8       16       24       32
	+---------+--------+--------+--------+
	|     Request-ID   |  Request-Type   |
	+---------+--------+-----------------+
	|            Body-Length             |
	|                                    |
	+---------+-----------------+--------+
	| Routed  |       Hop       | Unused |
	+---------+--------+--------+--------+
	|  Length of hosts |  Routing-List   |
	+------------------+-----------------+
	|                                    |
	|               Body                 |
	.                                    .
	.                                    .
	+------------------------------------+
 
### Request Header

* Request-ID - The id of the request, e.g., a consecutive number.
* Request-Type - The type of the request.
* Body length - The length of the body as a long value.
* Routed - Does the package contain routing information (0x01) or not (0x0).
* Hop - The hop of the package. Is set to 0x00 if the package is not routed.
* Length of host - The length of the host list. Will be set to 0x00 if the package is not routed.
* Routing-List - A comma separated list of hosts for package routing. The format of the list is: [host1:port,host2:port,...].

Request Types:

* Type 0x00 - Hello
* Type 0x01 - Insert tuple request
* Type 0x02 - Delete tuple request
* Type 0x03 - Delete table request
* Type 0x04 - List all tables request
* Type 0x05 - Disconnect request
* Type 0x06 - Query request
* Type 0x07 - Transfer SSTable
* Type 0x08 - Create distribution group
* Type 0x09 - Delete distribution group
* Type 0x10 - Compression envelope
* Type 0x11 - Keep alive package
* Type 0x12 - Next page
* Type 0x13 - Cancel query


## The response frame

    0         8       16       24       32
	+---------+--------+--------+--------+
	|     Request-ID   |  Result-Type    | 
	+---------+-----------------+--------+
	|             Body length            |
	|                                    |
	+------------------------------------+
	|                                    |
	|               Body                 |
	.                                    .
	.                                    .
	+------------------------------------+
	
* Request-ID - The id of the request which the response belongs too.
* Result-Type - The result type of the operation.
* Body length - The length of the body as a long value. For Packages without body, the length is set to 0.

Result-Types:

* Type 0x00 - Hello result
* Type 0x01 - Operation Success - with details in the body
* Type 0x02 - Operation Error - with details in the body
* Type 0x03 - Result of the List tables call
* Type 0x04 - A result that contains a tuple
* Type 0x05 - Start multiple tuple result
* Type 0x06 - End multiple tuple result
* Type 0x07 - End page 
* Type 0x10 - Compression envelope
	
### Body for response type = 0x01/0x02 (Success/Error with details)

    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Message-Length |     Message     |
	+------------------+                 |
	.                                    .
	.                                    .
	+------------------------------------+
	
* Message-Length - The length of the error message
* Message - The error message

### Body for response type = 0x04
This is a response body that contains a tuple.

    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Table-Length   |   Key-Length    |
	+------------------+-----------------+
	|            BBox-Length             |
	+------------------------------------+
	|            Data-Length             |
	+------------------------------------|
	|             Timestamp              |
	|                                    |
	+------------------------------------+
	|             Tablename              |
	.                                    .
	+------------------------------------+
	|               Key                  |
	.                                    .
	+------------------------------------+
	|               BBOX                 |
	.                                    .
	+------------------------------------+
	|                                    |
	|               Data                 |
	.                                    .
	.                                    .
	+------------------------------------+
	
Note: All time stamps are 64 bit long and have a resolution of microseconds.
	
### Body for response type = 0x05 / 0x06 / 0x07 
By using the response types 0x05 and 0x06 a set of tuples can be transfered. For example, this could be the result of a query. The begin of the transfer of the tuple set is indicated by the package type 0x06; the end is indicated by the type 0x06. Both package types have an empty body. 

Transferring a set of tuples:

    0         8       16       24       32
    +-------------------------------------+
    |  0x06 - Start multiple tuple result |
    +-------------------------------------+
    |  0x05 - A result tuple              |
    +-------------------------------------+
    |  0x05 - A result tuple              |
    +-------------------------------------+
    |               ....                  |
    +-------------------------------------+
	|  0x05 - A result tuple              |
    +-------------------------------------+
	|  0x07 - End multiple tuple result   |
    +-------------------------------------+
    
Or with paging:

    0         8       16       24       32
    +-------------------------------------+
    |  0x06 - Start multiple tuple result |
    +-------------------------------------+
    |  0x05 - A result tuple              |
    +-------------------------------------+
    |  0x05 - A result tuple              |
    +-------------------------------------+
    |  0x09 - End page                    |
    +-------------------------------------+
    |  Client: 0x12 - Next tuples         |
    +-------------------------------------+
    |               ....                  |
    +-------------------------------------+
	|  0x05 - A result tuple              |
    +-------------------------------------+
	|  0x07 - End multiple tuple result   |
    +-------------------------------------+
    
 ### Body for response type (0x10)
 This is a compression envelope. This package contains another response package in compressed format.
 
     0         8       16       24       32
    +-------------------------------------+
    |               Body size             |
    +----------+--------------------------+
    |  CP Type |            Body          |
    +----------+                          |
    |                                     |
    +-------------------------------------+
    
 Body size - The length of the body in bytes.
 CP Type - Compression type (0x00 = gzip compression).
 
    
## Frame body
The structure of the body depends on the request type. The next sections describe the used structures.

### Hello 
Handshake with the server

#### Request body

The body contains the protocol version and the capabilities of the client.

    0         8       16       24       32
	+---------+--------+--------+--------+
	|          Protocol Version          |
	+------------------------------------+
	|         Client-Capabilities        |
	+------------------------------------+
	
Client features:

Bit 0: GZIP Compression

#### Response body
The body contains the protocol version and the capabilities of the server.

    0         8       16       24       32
	+---------+--------+--------+--------+
	|          Protocol Version          |
	+------------------------------------+
	|         Client-Capabilities        |
	+------------------------------------+
	
Client features:

Bit 0: GZIP Compression

### Insert
This package inserts a new tuple into a given table. The result could be currently response type 0x01 or 0x02.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Table-Length   |   Key-Length    |
	+------------------+-----------------+
	|            BBox-Length             |
	+------------------------------------+
	|            Data-Length             |
	+------------------------------------|
	|             Timestamp              |
	|                                    |
	+------------------------------------+
	|             Tablename              |
	.                                    .
	+------------------------------------+
	|               Key                  |
	.                                    .
	+------------------------------------+
	|               BBOX                 |
	.                                    .
	+------------------------------------+
	|                                    |
	|               Data                 |
	.                                    .
	.                                    .
	+------------------------------------+
	

### Delete Tuple
This package deletes a tuple from a table. The result could be currently response type 0x01 or 0x02.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Table-Length   |   Key-Length    |
	+------------------+-----------------+	
	|              Timestamp             |
	|                                    |
	+------------------------------------|
	|              Tablename             |
	.                                    .
	+------------------------------------+
	|                 Key                |
	.                                    .
	+------------------------------------+
	
### Delete Table
This package deletes a whole table. The result could be currently response type 0x01 or 0x02.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Table-Length   |                 |
	+------------------+                 |
	|              Tablename             |
	.                                    .
	+------------------------------------+
	

### List all tables
This package lists all existing tables

#### Request body

The body of the package is empty

    0         8       16       24       32
	+---------+--------+--------+--------+

#### Response body
The response body contains the names of the existing tables.

    0         8       16       24       32
	+---------+--------+--------+--------+
    |           Number of tables         | 
    +------------------+-----------------+
    | Length of table 1|   Table name 1  |
    +------------------+-----------------+
    |                 ...                |
    +------------------+-----------------+   
    | Length of table n|   Table name n  |
    +------------------------------------+  
    
### Disconnect 
Disconnect from server

#### Request body

The body of the package is empty

    0         8       16       24       32
	+---------+--------+--------+--------+

#### Response body
The result could be currently only response type 0x01. The server waits until all pending operations are completed successfully. Afterwards, the response type 0x01 is send and the connection is closed. 

### Query
This package represents a query.

#### Request body

The request body of a query consists of the query type and specific data for the particular query.

    0         8       16       24       32
	+---------+--------+--------+--------+
	| Q-Type  | Paging |    Page Size    | 
	+---------+--------+-----------------+
	|         Query specific data        |
	.                                    .
	+------------------------------------+

Query type:

* Type 0x01 - Key query
* Type 0x02 - Bounding box query
* Type 0x03 - Time query
* Type 0x04 - Time and Bounding box query

Paging: 
* 0x00 - Paging disabled
* 0x01 - Paging enabled

Page size:
* Number of results per page

### Key-Query
This query asks for a specific key in a particular table.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|  0x01   |  Table-Length   | Key-   | 
	+---------+-----------------+--------+
	| -Length |     Tablename            |
	+---------+                          |
	.                                    .
	+------------------------------------+
	|                 Key                |
	.                                    .
	+------------------------------------+


#### Response body
The result could be currently the response types 0x01, 0x02, 0x05 and 0x06. The result type 0x02 indicates an error. The result type 0x01 means, that the query is processed successfully, but no matching tuple was found. The result type 0x05 indicates that one tuple is found.

### Bounding-Box-Query
This query asks for all tuples, that are covered by the bounding box.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|  0x02   | Paging |    Page Size    | 
	+---------+--------+-----------------+
	|   Table-Length   |     Unused      |
	+------------------+-----------------+
	|              BBOX-Length           | 
	+------------------------------------+ 
	|              Tablename             |
	.                                    .
	+------------------------------------+
	|                 BBOX               |
	.                                    .
	+------------------------------------+

#### Response body
The result could be currently the response types 0x02, 0x03 and 0x06.

### Time-Query
This query asks for all tuples, that are inserted after certain time stamp (time(tuple) > time stamp).

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|  0x03   | Paging |    Page Size    | 
    +---------+--------------------------+
	|              Timestamp             |
    |                                    |
    +-----------------+------------------+
	|   Table-length  |                  |
	+-----------------+                  |
	|              Tablename             |
    +------------------------------------+
    
#### Response body
The result could be currently the response types 0x02, 0x03 and 0x06.


### Time and bounding box query
This query asks for all tuples, that are covered by the bounding box and newer than time.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|  0x04   | Paging |    Page Size    | 
	+---------+--------+-----------------+
	|   Table-Length   |     Unused      |
	+------------------+-----------------+
	|              BBOX-Length           | 
	+------------------------------------+ 
	|              Timestamp             |
    |                                    |
	+------------------------------------+ 
	|              Tablename             |
	.                                    .
	+------------------------------------+
	|                 BBOX               |
	.                                    .
	+------------------------------------+

#### Response body
The result could be currently the response types 0x02, 0x03 and 0x06.


### Transfer SSTable
This request transfers a whole SSTable from one instance to another. This request is send between two bboxdb instances. 

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Table-Length   |      Unused     |
	+------------------+-----------------+
	|          Metadata-Length           |
	|                                    |
	+------------------------------------+	
	|           SSTable-Length           |
	|                                    |
	+------------------------------------+
	|          Keyindex-Length           |
	|                                    |
	+------------------------------------+
	|              Tablename             |
	.                                    .
	+------------------------------------+
	|              Metadata              |
	|                                    |
	.                                    .
	+------------------------------------+
	|               SSTable              |
	|                                    |
	.                                    .
	+------------------------------------+
	|              Keyindex              |
	|                                    |
	.                                    .
	+------------------------------------+
	
#### Response body
The result could be currently the response types 0x00, 0x02 or the receiving server closes the tcp socket during the file transfer. 


### Create distribution group
This package deletes a whole table. The result could be currently response type 0x01, 0x03 and 0x04.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Group-Length   |   Replication   |
	+------------------+-----------------+
	|        Distribution Group          |
	.                                    .
	+------------------------------------+
	
The field 'replication' determines how many replicates are created into the distribution group

### Delete distribution group
This package deletes a whole table. The result could be currently response type 0x01, 0x03 and 0x04.

#### Request body
    0         8       16       24       32
	+---------+--------+--------+--------+
	|   Group-Length   |                 |
	+------------------+                 |
	|        Distribution Group          |
	.                                    .
	+------------------------------------+


### Compression envelope
This is a compression envelope. This package contains another request package in compressed format. The result type depends of the content of the envelope.
 
#### Request body
 
     0         8       16       24       32
    +-------------------------------------+
    |               Body size             |
    +----------+--------------------------+
    |  CP Type |            Body          |
    +----------+                          |
    |                                     |
    +-------------------------------------+
    
 Body size - The length of the body in bytes.
 CP Type - Compression type (0x00 = gzip compression).

### Keep alive package
This package is send periodically to keep the TCP connection open. 

#### Request body
The body of the package is empty. 
     
#### Response body
The result could be currently the response type 0x01.


### Next page
Request the next tuples for a given query.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	| Query Request ID |      Unused     |
	+---------+--------+-----------------+

This package requets the next tuples for the given query

#### Response body
The result could be currently the response types 0x01, 0x02 and 0x05.

### Cancel query
Cancel the given query.

#### Request body

    0         8       16       24       32
	+---------+--------+--------+--------+
	| Query Request ID |      Unused     |
	+---------+--------+-----------------+

#### Response body
The result could be currently the response types 0x01 and 0x02.

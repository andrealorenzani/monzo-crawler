# High Level overview
In a production ready component I would utilise managed (cloud) components to build a crawler like the one described by the [functional requirements](./REQUIREMENTS.md).  
The architecture that I followed is the following  
![Architectural diagram](./assets/crawler_design.svg)

I will go more in detail on each part

## Client and API
**This was not required by the definition of the test**. I just needed to pass an URL and make it trigger the Crawler. But because I am coding in Java and I am using Spring Boot, I thought it was a good idea to expose some actuator and to have an input with an endpoint  

`GET /crawl?url=http://monzo.com`  
This endpoint *simulates* adding a new url to the SQS

## SQS
This can be any queue, not only SQS. Having an external queue would simplify quite a lot the work, because at that point the Crawler could act as a single threaded worker, consuming from the queue and ignoring eventual temporary unavailability of the page(s) by using the retry mechanism.  
The queue contains the urls that the crawler(s instances) has still to process. 

## CACHE
The crawler implements, in an asynchronous and distributed way, a breadth first search algorithm and it enqueues in the queue any ***new*** link that he still needs to visit.  
To do that it needs to store the list of the links that are already visited. A cache (especially Redis or MemCached that guarantee atomicity of the requests) is a preferred choice in this case, because faster than a database.  
Using a database is still possible, but it requires to configure it for consistency, that usually means a slower access in a cluster and a lower availability.

## RDS
This was not at all requested for this test, but I think it can be worth mentioning that, depending on the use case, data may need to be recorded in a database. As it is right now, a good choice may be either a document store (for example OpenSearch) or a database with high throughput in writing new data, like Cassandra.  
Anyway, the requirement was to `print the page that [the crawler] visits and list all the links it founds at the specific page`, there is no need of a database.

## The Crawler
The Crawler is a component that receives new tasks (messages from the queue), check if they are already visited, download the page from internet, parses it and add new links to the queue and the processed link to the set of already visited links.  
Depending on the use case it can be a lambda or a long running task (ie a Fargate).  

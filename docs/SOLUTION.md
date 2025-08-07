# DETAILS OF THE SOLUTION
## Dependencies
I decided to code the solution in **Java**.  
I used **Spring Boot** as main framework, mainly for the following features:  
1. The Inversion of Control (dependency injection)
2. The support for the API (I trigger the process with an API call)
3. The Actuators and micrometer to provide data for the metrics

I also used **JSOUP** to parse and query HTML documents. JSOUP provide methods for retrieving pages directly from an URL, that simplified the code, avoiding to code the retrieve of data from internet.  
I used **Lombok** to avoid boilerplate code

# Code structure
As per any Spring boot application, the main entry point is `MonzoCrawlerApplication`. In there I also coded a post init method (`run`) that starts 4 threads of crawlers.  
This simulates the scaling of the application in a production environment.
## The main service, `CrawlerService`
This is what should be the main process in the production environment. It should listen to a queue and process the messages coming from there.  
Because I couldn't spend too much time setting up `testcontainers` or emulating a production environment, I organised the `CrawlerService` with a method (`listenToQueue`) that simulates the polling of new messages from a queue, and I made a simple in-memory queue managed by the `QueueService`.  

The core of the service is the `crawl` method: it sanitizes the input, updates the set of visited links, parses the page and extracts links, and from those extracted it filters out the one that may need to be enqueued again, and then prints everything.  
To do so, it uses the following components:
- `linkFilter` is the component that encapsulates the logic to filter the links
- `htmlParser` extracts the HTML page at an URL and parses links
- `cacheService` in a production environment would just be the component interacting with the cache. For my projects it also keeps the HashMap used as cache
- `metricService` that abstracts the logic to extract metrics for the observability
- the class defines an additional `queueService` but it is used only to store the queue object and generate the polling process in different threads

## The details of the other Components
- `linkFilter` filters out links based on if they are already visited or if they are not of the same domain
- `htmlParser` uses a concept of `DocumentProcessor`. The only implemented one is the `LinkExtractor`. This structure uses intensively the **JSOUP** library
- `CacheService` should be the service to interact with the cache. Frameworks like **Spring Boot** have annotations to interact with the caches, but I also needed to store the in memory cache. Please note two things:
  - The `put` method is synchronized to handle multithreading
  - When a new message is sent, it clears the cache. That's because obviously not using a professional cache, I cannot set time to live or clean only the domain that I need to process again
- 
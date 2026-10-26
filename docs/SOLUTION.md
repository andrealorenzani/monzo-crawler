# Details of the solution
## Dependencies
I decided to code the solution in **Java 21**. I used **Spring Boot** as main framework, mainly for:
1. The inversion of control (dependency injection) and the typed configuration (`CrawlerProperties`).
2. The support for the API (a crawl is started with an API call).
3. The actuators and Micrometer to provide the metrics.

I used **jsoup** to download, parse and query HTML documents. The code that crawls (the queue, the visited pages, which links to follow, the concurrency) is mine.

## How to build and run
```bash
./mvnw verify
java -jar target/monzo-crawler-1.0.0.jar
curl -X POST 'localhost:8080/crawl?url=https://monzo.com/'
```
Settings (`src/main/resources/application.yml`, they can be overridden with environment variables such as `CRAWLER_WORKERS=8`):

| Property | Default | Meaning |
|----------|---------|---------|
| `crawler.workers` | 4 | pages downloaded at the same time |
| `crawler.max-pages-per-crawl` | 10000 | a crawl stops after this many pages |
| `crawler.request-timeout` | 5s | how long to wait for a page |
| `crawler.poll-timeout` | 500ms | how long an idle worker waits before checking whether it has to stop |
| `crawler.user-agent` | monzo-crawler | how the crawler introduces itself |

## Code structure
Package `name.lorenzani.andrea.monzo_crawler`:

| Package | Content |
|---------|---------|
| *(root)* | `MonzoCrawlerApplication`, the entry point |
| `controllers` | `CrawlerController`: `POST /crawl` and `GET /`; `ErrorHandler`: invalid URL → 400 |
| `crawl` | `CrawlerService`: the algorithm. `CrawlerWorkers`: the threads. `CrawlSession`: the progress of a crawl. `Links`: normalisation of URLs. `CrawlReporter`/`ConsoleCrawlReporter`: the output |
| `queue` | `CrawlQueue` and `InMemoryCrawlQueue` |
| `visited` | `VisitedStore` and `InMemoryVisitedStore` |
| `fetch` | `PageFetcher`, `JsoupPageFetcher`, `LinkExtractor`, `Page` |
| `metrics` | `MetricService`: counters in Micrometer |
| `config` | `CrawlerProperties` |

### The core, `CrawlerService`
* `start(url)` normalises the URL, creates the `CrawlSession` of its host (unless that host is being crawled), forgets the visited pages of the host and queues the first link.
* `process(link)` is what a worker does with a link of the queue:
  1. `markVisited`: if somebody else visited the page, stop. If the crawl reached the maximum number of pages, stop.
  2. download and parse the page (`PageFetcher`); on failure count it and stop.
  3. report the page and **all** the links found (`CrawlReporter`).
  4. if the page is still on the host (no redirect elsewhere), normalise the links, keep those on the same host that are not visited yet, and queue them.
  5. always, at the end, tell the session that the link is done.

## Concurrency
* Workers share only the queue, the visited store and the sessions, and all of them are thread safe.
* Two workers can queue the same link at the same time; the atomic `markVisited` makes sure that only one visits it. Checking `isVisited` before queueing is only an optimisation that keeps the queue short.
* The workers are virtual threads: they wait for the network without using an operating system thread each.

## Observability
Counters (in `GET /` and at `/actuator/metrics/monzoCustomMetric.<name>`): `linkReceived`, `linkVisited`, `linkParsed`, `linkToVisit`, `pageParseErrors`. Logs tell when a crawl starts and finishes and why a page failed.

## Tests
`./mvnw test` runs 74 tests. The crawler is tested without a network, on `FakeSite`, a website in memory (cycles, external links, subdomains, redirects, failures, the maximum number of pages); the concurrency with 8 workers on a site of 400 pages that links to itself in many ways (each page must be downloaded exactly once and the crawl must finish); the downloader and the whole application against a real HTTP server.

## Limitations
* **State in memory**: the queue and the visited pages are lost when the application stops, and a second instance would not share them. That is what the interfaces are for.
* **No `robots.txt` and no rate limiting**: a production crawler must respect the rules of the sites and wait between requests to the same host.
* **Static HTML only**: links created by JavaScript are not seen.
* **Content**: only `text/html` pages are visited; the first 2 MB of each page are parsed (jsoup default).
* **`http` and `https` versions of the same page are different pages** if the site links to both.
* **Query strings are part of the page**: `/search?q=a` and `/search?q=b` are two pages, so a site generating endless URLs is bounded only by `crawler.max-pages-per-crawl`.

![Upgraded with Claude](https://img.shields.io/badge/Upgraded%20with-Claude-D97757)

# Monzo crawler

A web crawler written as the solution to a technical test.

> We'd like you to write a simple web crawler in a programming language you're familiar with. Given a starting URL,
> the crawler should visit each URL it finds on the same domain. It should print each URL visited, and a list of links
> found on that page. The crawler should be limited to one subdomain - so when you start with *https://monzo.com/*, it
> would crawl all pages on the monzo.com website, but not follow external links, for example to facebook.com or
> community.monzo.com.
>
> Please do not use frameworks like scrapy or go-colly which handle all the crawling behind the scenes or someone
> else's code. You are welcome to use libraries to handle things like HTML parsing. Ideally, write it as you would a
> production piece of code: we are more interested in how you design software, the trade-offs you've made, what
> behaviour the program exhibits, and your use of concurrency, test coverage, and so on.

## Quick start

Requires JDK 21 or newer.

```bash
./mvnw verify                                  # build and test
java -jar target/monzo-crawler-1.0.0.jar       # start the crawler on port 8080

curl -X POST 'localhost:8080/crawl?url=https://monzo.com/'   # start crawling (202 Accepted)
curl localhost:8080/                                         # how is it going?
```

The crawler prints what it finds to the standard output:

```
Visited https://monzo.com/ - found 3 links
	https://monzo.com/about
	https://monzo.com/blog
	https://community.monzo.com/
Visited https://monzo.com/about - found 1 links
	https://monzo.com/
...
```

`GET /` answers with the queued links, one entry per crawl (host, state `RUNNING` or `FINISHED`, pages visited, pages
that failed, links still to process) and the counters. The actuator is at `/actuator/{health,info,metrics}`.

## Upgraded after 1 year

The project was written in August 2025 (Spring Boot 3.5, Lombok, the project had grown a few rough edges while
simulating a distributed system) and was upgraded in 2026 with Claude.

| Area | Before (2025) | Now |
|------|---------------|-----|
| Spring Boot | 3.5.3 | 4.1.2 (Spring Framework 7) |
| jsoup | 1.21.1 | 1.23.2 |
| Boilerplate | Lombok, field `@Autowired` | records, constructor injection |
| Workers | 4 `@Async` platform threads sleeping 1s when the queue was empty | virtual threads that block on the queue; `crawler.workers` is a setting |
| Tests | JUnit 5 and Mockito, every class tested with mocks | JUnit 5 with fakes (an in-memory website) and real HTTP servers, 74 tests |
| Maven | wrapper 3.9.10 | wrapper 3.9.11 |
| License | none | Apache 2.0 |

What changed in the code, and the problems it fixed:

* **A crawl has a start, a progress and an end.** The old crawler ran forever and never said when it was done.
  `CrawlerService` now counts the links queued or being visited per crawl: when the count reaches zero the crawl is
  `FINISHED` and it is logged.
* **Starting a crawl no longer breaks the running ones.** `POST /crawl` used to clear the *whole* cache of visited pages,
  also those of crawls in progress, which then visited pages twice. The visited pages are kept per host and a
  crawl is forgotten only when its own host is crawled again after it finished. Asking for a host that is being crawled
  answers `202` with the header `X-Crawl: ALREADY_RUNNING` and changes nothing.
* **Only web pages are followed.** `mailto:`, `tel:`, `javascript:` and `ftp:` links are reported (they are links) but
  never visited; the URL normalisation (`Links`) also writes an empty path as `/`, lower-cases scheme and host, drops
  the user and password and the fragment, so `http://monzo.com` and `http://monzo.com/` are one page.
* **Thread safe counting.** The visited set was a `HashMap` that was read without synchronisation (`size`, `keySet`),
  and the queue a `LinkedList`; they are a concurrent set per host and a `LinkedBlockingQueue`. Marking a page as visited
  is one atomic operation, so exactly one worker visits it.
* **Redirects**: if a page redirects to another host, the page is reported but its links are not followed.
* **Safety limits**: at most `crawler.max-pages-per-crawl` pages per crawl (10,000), request timeout, a User-Agent that
  says who is crawling. Failures of a page (404, not HTML, timeout, a bug) are counted and logged but never stop a worker.
* **`GET /` is cheap**: it used to send the whole list of visited pages. It now sends the counts.
* **Actuator**: `beans` and `env`, which show the internals of the application, are no longer exposed, and the
  endpoints are under `/actuator` instead of `/`.
* **Design for replacement**: the queue (`CrawlQueue`), the visited pages (`VisitedStore`), the downloader
  (`PageFetcher`) and the output (`CrawlReporter`) are interfaces with in-memory/console implementations. These are
  the places where SQS, Redis, a headless browser or a database go in production.

## Documentation

1. [Requirements (as I understood them)](docs/REQUIREMENTS.md)
2. [Architectural design of the solution](docs/DESIGN.md)
3. [Structure of the code and how to run it](docs/SOLUTION.md)

## License

[Apache License 2.0](LICENSE), Copyright 2025-2026 Andrea Lorenzani.

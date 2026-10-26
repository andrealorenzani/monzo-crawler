# Functional requirements
1. The crawler should take a `starting URL`.
2. The crawler should visit each URL `in the same domain`.
3. The crawler should print the page that it visits (according to point 2 only those in the same domain) and list `all the links it finds at the specific page` (no matter if they belong to the same domain or not).
4. The crawler should treat nested subdomains or a subdomain different from the starting URL as external links and not follow them: starting from `monzo.com`, neither `www.monzo.com` nor `community.monzo.com` are visited.

# Non functional requirements
1. Ideally, I should write it as I would a production piece of code.
   1. This means considering consistency, availability, partition tolerance (CAP theorem), scalability, observability, ...

# Decisions on what the requirements do not say
* *Same domain* means *same host*: the scheme (`http` or `https`) and the port do not matter, so a link from `http://monzo.com` to `https://monzo.com/x` is followed.
* Only `http` and `https` links are visited. Other links (`mailto:`, `tel:`, ...) are listed but not followed.
* A link that points to the same page with a different fragment (`/a#top`, `/a#bottom`) is the same page.
* A page that redirects to another host is out of the crawl, and its links are not followed.
* The crawler stops after a (configurable) number of pages per crawl, as a protection against sites without an end.
* The crawler is not polite in the sense of `robots.txt` or rate limiting: it is a test, see [the limitations](SOLUTION.md#limitations).

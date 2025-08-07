# Functional requirements
1. The crawler should take a `starting URL`
2. The crawler should visit each URL `in the same domain`
3. The crawler should print the page that it visits (according to point 2 only those in the same domain) and list `all the links it founds at the specific page` (no matter if they belong to the same domain or not)
4. The crawler should treat nested subdomains or subdomain different from the starting URL as external link and not follow them

# Non functional requirements
1. Ideally, I should write it as I would a production piece of code
   1. This means considering consistency, availability, partition tollerance (cap theorem), scalability, ...
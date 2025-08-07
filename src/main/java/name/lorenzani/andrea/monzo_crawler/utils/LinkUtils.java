package name.lorenzani.andrea.monzo_crawler.utils;

import lombok.extern.log4j.Log4j2;
import name.lorenzani.andrea.monzo_crawler.services.MetricService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.net.URI;
import java.net.URISyntaxException;

@Log4j2
@Component
public class LinkUtils {

    @Autowired MetricService metricService;

    public String normalizeLink(String url) {
        StringBuilder sb = new StringBuilder();
        char[] urlChars = url.toCharArray();
        for(int i=0; i< urlChars.length; i++) {
            boolean shouldBreak = false;
            switch(urlChars[i]) {
                case ' ' -> sb.append("%20");
                case '#' -> shouldBreak = true;
                default -> sb.append(urlChars[i]);
            }
            if(shouldBreak) break;
        }
        url = sb.toString();
        //if(url.length()>=1 && url.charAt(url.length()-1) == '/') url = url.substring(0, url.length()-1);
        try {
            url = new URI(url).normalize().toString();
        } catch(URISyntaxException use) {
            log.error(use);
            metricService.incrementDecodeErrors();
        }
        return url;
    }

}

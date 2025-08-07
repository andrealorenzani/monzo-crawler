package name.lorenzani.andrea.monzo_crawler.services;

import name.lorenzani.andrea.monzo_crawler.models.CrawlLink;
import org.springframework.stereotype.Service;

import java.util.LinkedList;
import java.util.Queue;

@Service
public class QueueService {

    Queue<CrawlLink> awsSqs = new LinkedList<>();

    // This simulates AWS SQS (or any activeMQ). Sending data to an external queue
    // does NOT trigger concurrency issues, therefore I made this synchronized
    public synchronized void sendMessage(CrawlLink message) {
        awsSqs.add(message);
    }

    // As above, receiving a message is "thread safe" with an external tool
    // while deleting and reading may result in inconsistent state
    public synchronized CrawlLink receiveMessage() {
        return awsSqs.isEmpty()? null : awsSqs.poll();
    }

    public int numQueuedMessages() {
        return awsSqs.size();
    }
}

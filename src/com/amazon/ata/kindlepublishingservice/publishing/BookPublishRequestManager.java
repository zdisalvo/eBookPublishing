package com.amazon.ata.kindlepublishingservice.publishing;

import javax.inject.Inject;
import javax.inject.Singleton;
import java.util.LinkedList;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

//TODO - Swastik added @Singleton
@Singleton
public class BookPublishRequestManager {

    Queue<BookPublishRequest> requests;

    @Inject
    BookPublishRequestManager() {
        requests = new ConcurrentLinkedQueue<>();
    }

    public void addBookPublishRequest(BookPublishRequest bookPublishRequest) {
        requests.offer(bookPublishRequest);
    }

    public BookPublishRequest getBookPublishRequestToProcess() {
        if (requests.isEmpty()) {
            return null;
        }
        return requests.poll();
    }
}

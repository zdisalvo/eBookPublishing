package com.amazon.ata.kindlepublishingservice.publishing;

import java.util.LinkedList;
import java.util.Queue;

public class BookPublishRequestManager {

    Queue<BookPublishRequest> requests;

    BookPublishRequestManager() {
        requests = new LinkedList<>();
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

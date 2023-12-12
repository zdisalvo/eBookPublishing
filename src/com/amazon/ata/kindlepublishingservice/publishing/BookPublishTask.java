package com.amazon.ata.kindlepublishingservice.publishing;

import com.amazon.ata.kindlepublishingservice.dao.CatalogDao;
import com.amazon.ata.kindlepublishingservice.dao.PublishingStatusDao;
import com.amazon.ata.kindlepublishingservice.dynamodb.models.CatalogItemVersion;
import com.amazon.ata.kindlepublishingservice.dynamodb.models.PublishingStatusItem;
import com.amazon.ata.kindlepublishingservice.enums.PublishingRecordStatus;
import com.amazon.ata.kindlepublishingservice.exceptions.BookNotFoundException;
import com.amazon.ata.kindlepublishingservice.models.PublishingStatus;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import javax.inject.Inject;
import java.util.ArrayList;
import java.util.List;

public class BookPublishTask implements Runnable{

    private static final Logger LOGGER = LogManager.getLogger(BookPublisher.class);

    private final CatalogDao catalogDao;
    private final PublishingStatusDao publishingStatusDao;
    private final BookPublishRequestManager bookPublishRequestManager;
    private BookPublishRequest bookPublishRequest;





    @Inject
    public BookPublishTask(CatalogDao catalogDao, PublishingStatusDao publishingStatusDao,
                           BookPublishRequestManager bookPublishRequestManager) {
        this.catalogDao = catalogDao;
        this.publishingStatusDao = publishingStatusDao;
        this.bookPublishRequestManager = bookPublishRequestManager;
    }

    @Override
    public void run() {
        BookPublishRequest bookPublishRequest = bookPublishRequestManager.getBookPublishRequestToProcess();

        if (bookPublishRequest == null) {
            return;
        }

        String bookId = bookPublishRequest.getBookId();

        if (bookId == null) {
            bookId = catalogDao.createBookId();
            CatalogItemVersion catalogItemVersion = new CatalogItemVersion();

            //creates CatalogItemVersion from BookPublishRequest
            catalogItemVersion.setBookId(bookId);
            catalogItemVersion.setInactive(false);
            catalogItemVersion.setAuthor(bookPublishRequest.getAuthor());
            catalogItemVersion.setTitle(bookPublishRequest.getTitle());
            catalogItemVersion.setText(bookPublishRequest.getText());
            catalogItemVersion.setVersion(1);
            catalogItemVersion.setGenre(bookPublishRequest.getGenre());

            //adds CatalogItemVersion to catalog
            catalogDao.addCatalogItemVersion(catalogItemVersion);

            PublishingStatusItem publishingStatusItem = publishingStatusDao.setPublishingStatus
                    (bookPublishRequest.getPublishingRecordId(), PublishingRecordStatus.IN_PROGRESS, bookId);
            publishingStatusDao.setPublishingStatus(publishingStatusItem.getPublishingRecordId(),
                    PublishingRecordStatus.SUCCESSFUL, publishingStatusItem.getBookId());

        } else if (catalogDao.validateBookExists(bookId)) {
            PublishingStatusItem publishingStatusItem = publishingStatusDao.setPublishingStatus
                    (bookPublishRequest.getPublishingRecordId(), PublishingRecordStatus.IN_PROGRESS,
                    bookId);

            publishingStatusDao.setPublishingStatus(publishingStatusItem.getPublishingRecordId(),
                    PublishingRecordStatus.SUCCESSFUL, publishingStatusItem.getBookId());

            catalogDao.updateLatestVersionOfBook(bookId);

            KindleFormatConverter.format(bookPublishRequest);

            //TODO complete failed process


        } else {
            throw new BookNotFoundException("This book does not exist in the catalog");
        }

    }
}

package com.amazon.ata.kindlepublishingservice.dao;

import com.amazon.ata.kindlepublishingservice.dynamodb.models.CatalogItemVersion;
import com.amazon.ata.kindlepublishingservice.exceptions.BookNotFoundException;
import com.amazon.ata.kindlepublishingservice.publishing.KindleFormattedBook;
import com.amazon.ata.kindlepublishingservice.utils.KindlePublishingUtils;

import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBDeleteExpression;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBMapper;
import com.amazonaws.services.dynamodbv2.datamodeling.DynamoDBQueryExpression;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.ListIterator;
import java.util.UUID;
import javax.inject.Inject;

public class CatalogDao {

    private final DynamoDBMapper dynamoDbMapper;

    /**
     * Instantiates a new CatalogDao object.
     *
     * @param dynamoDbMapper The {@link DynamoDBMapper} used to interact with the catalog table.
     */
    @Inject
    public CatalogDao(DynamoDBMapper dynamoDbMapper) {
        this.dynamoDbMapper = dynamoDbMapper;
    }

    /**
     * Returns the latest version of the book from the catalog corresponding to the specified book id.
     * Throws a BookNotFoundException if the latest version is not active or no version is found.
     * @param bookId Id associated with the book.
     * @return The corresponding CatalogItem from the catalog table.
     */
    public CatalogItemVersion getBookFromCatalog(String bookId) {
        CatalogItemVersion book = getLatestVersionOfBook(bookId);

        if (book == null || book.isInactive()) {
            throw new BookNotFoundException(String.format("No book found for id: %s", bookId));
        }

        return book;
    }

    // Returns null if no version exists for the provided bookId
    public CatalogItemVersion getLatestVersionOfBook(String bookId) {
        CatalogItemVersion book = new CatalogItemVersion();
        book.setBookId(bookId);

        DynamoDBQueryExpression<CatalogItemVersion> queryExpression = new DynamoDBQueryExpression()
            .withHashKeyValues(book)
            .withScanIndexForward(false)
            .withLimit(1);

        List<CatalogItemVersion> results = dynamoDbMapper.query(CatalogItemVersion.class, queryExpression);
        if (results.isEmpty()) {
            return null;
        }
        return results.get(0);
    }

    /**
     * Marks the current book version inactive and creates an incremented
     * latest version of the same book
     * @param bookId
     * @return
     */
    public CatalogItemVersion updateLatestVersionOfBook(String bookId) {
        CatalogItemVersion catalogItemVersion = new CatalogItemVersion();
        catalogItemVersion.setBookId(bookId);

        DynamoDBQueryExpression<CatalogItemVersion> queryExpression =
                new DynamoDBQueryExpression<CatalogItemVersion>()
                        .withHashKeyValues(catalogItemVersion)
                        .withScanIndexForward(false)
                        .withLimit(1);

        List<CatalogItemVersion> results = dynamoDbMapper.query(CatalogItemVersion.class, queryExpression);

        //sets the previous version to inactive
        catalogItemVersion = results.get(0);
        results.get(0).setInactive(true);
        dynamoDbMapper.save(catalogItemVersion);

        //increments the latest version of the book
        catalogItemVersion.setVersion(results.get(0).getVersion() + 1);
        catalogItemVersion.setInactive(false);
        dynamoDbMapper.save(catalogItemVersion);

        return catalogItemVersion;

    }

    public CatalogItemVersion removeBookFromCatalog(String bookId) {
        CatalogItemVersion book = new CatalogItemVersion();
        book.setBookId(bookId);

        DynamoDBQueryExpression<CatalogItemVersion> queryExpression = new DynamoDBQueryExpression()
                .withHashKeyValues(book)
                .withLimit(10);

        List<CatalogItemVersion> results = new ArrayList<>();
        List<CatalogItemVersion> queryResults = dynamoDbMapper.query(CatalogItemVersion.class, queryExpression);

        for (CatalogItemVersion item : queryResults) {
            results.add(item);
        }

        if (results.isEmpty()) {
            throw new BookNotFoundException("This book is inactive or does not exist");
        }



        ListIterator<CatalogItemVersion> listIterator = results.listIterator();

        while(listIterator.hasNext()) {
            CatalogItemVersion item = listIterator.next();
            if (!listIterator.hasNext()) {
                if (item.isInactive()) {
                    throw new BookNotFoundException("This book is inactive");
                } else {
                    item.setInactive(true);
                    dynamoDbMapper.save(item);
                }
            }
        }

//        while(resu)
//            if (catalogItemVersion.isInactive()) {
//                throw new BookNotFoundException("This book is inactive");
//            } else {
//                catalogItemVersion.setInactive(true);
//                dynamoDbMapper.save(catalogItemVersion);
//            }
//        }


        return results.get(results.size() - 1);
    }

    public boolean validateBookExists(String bookId) {
        CatalogItemVersion book = new CatalogItemVersion();
        book.setBookId(bookId);

        DynamoDBQueryExpression<CatalogItemVersion> queryExpression = new DynamoDBQueryExpression()
                .withHashKeyValues(book)
                .withScanIndexForward(false)
                .withLimit(1);

        List<CatalogItemVersion> results = dynamoDbMapper.query(CatalogItemVersion.class, queryExpression);
        if (results.isEmpty()) {
            return false;
        }
        return true;
    }

    public String createBookId() {
        return "book." + UUID.randomUUID().toString();
    }

    public CatalogItemVersion addCatalogItemVersion(CatalogItemVersion catalogItemVersion) {
        dynamoDbMapper.save(catalogItemVersion);
        return catalogItemVersion;
    }
}

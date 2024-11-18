package com.krickert.search.indexer.solr.vector.event;

import com.google.common.collect.Maps;
import jakarta.inject.Singleton;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import reactor.core.publisher.Flux;
import reactor.core.scheduler.Scheduler;
import reactor.core.scheduler.Schedulers;

import java.util.concurrent.ConcurrentMap;

/**
 * The SubscriptionManager class is responsible for managing subscriptions to document publishers
 * and delegating the processing of documents to respective document listeners.
 * It subscribes to the document flux provided by various publishers and ensures that
 * the documents are processed using the appropriate listeners.
 * <br>
 * It includes methods to subscribe listeners to publishers and process the documents
 * with the listeners in a parallel, non-blocking manner using reactive programming paradigms.
 * <br>
 * @see SolrSourceDocumentPublisher
 * @see InlineDocumentListener
 * @see SolrChunkDocumentPublisher
 * @see ChunkDocumentListener
 */
@Singleton
public class SubscriptionManager {
    private static final Logger log = LoggerFactory.getLogger(SubscriptionManager.class);

    private final SolrSourceDocumentPublisher solrSourceDocumentPublisher;
    private final InlineDocumentListener inlineDocumentListener;
    private final SolrChunkDocumentPublisher solrChunkDocumentPublisher;
    private final ChunkDocumentListener chunkDocumentListener;
    private final ConcurrentMap<String, Scheduler> schedulerCache = Maps.newConcurrentMap();

    public SubscriptionManager(SolrSourceDocumentPublisher solrSourceDocumentPublisher,
                               SolrChunkDocumentPublisher solrChunkDocumentPublisher,
                               InlineDocumentListener inlineDocumentListener,
                               ChunkDocumentListener chunkDocumentListener) {
        this.solrSourceDocumentPublisher = solrSourceDocumentPublisher;
        this.solrChunkDocumentPublisher = solrChunkDocumentPublisher;
        this.inlineDocumentListener = inlineDocumentListener;
        this.chunkDocumentListener = chunkDocumentListener;

        subscribeListeners();
    }

    private void subscribeListeners() {
        subscribeToPublisher(solrSourceDocumentPublisher.getDocumentFlux(), inlineDocumentListener);
        subscribeToPublisher(solrChunkDocumentPublisher.getDocumentFlux(), chunkDocumentListener);
    }

    private void subscribeToPublisher(Flux<SolrInputDocument> documentFlux, DocumentListener documentListener) {
        String listenerName = documentListener.getClass().getSimpleName();
        Scheduler scheduler = schedulerCache.computeIfAbsent(listenerName, k -> Schedulers.newParallel(listenerName, 60));
        documentFlux
                .parallel()
                .runOn(scheduler)
                .doOnNext(document -> processDocumentWithListener(document, documentListener))
                .doOnError(throwable -> log.error("Error in Flux pipeline: ", throwable))
                .subscribe();
    }

    private void processDocumentWithListener(SolrInputDocument document, DocumentListener listener) {
        try {
            listener.processDocument(document);
        } catch (Exception e) {
            log.error("Error processing document: {}", document.getFieldValue("id"), e);
        }
    }
}
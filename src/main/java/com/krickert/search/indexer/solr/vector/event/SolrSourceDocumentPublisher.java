package com.krickert.search.indexer.solr.vector.event;

import org.apache.solr.common.SolrInputDocument;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Sinks;


@Singleton
public class SolrSourceDocumentPublisher implements SourceSolrDocumentListener {

    Logger log = org.slf4j.LoggerFactory.getLogger(SolrSourceDocumentPublisher.class);
    private final Sinks.Many<SolrInputDocument> sink;

    public SolrSourceDocumentPublisher() {
        this.sink = Sinks.many().multicast().onBackpressureBuffer(30000, false);
    }

    @Override
    public void publishDocument(SolrInputDocument document) {
        Sinks.EmitResult result = this.sink.tryEmitNext(document);
        if (result.isFailure()) {
            log.warn("Failed to publish document: {}", document.getFieldValue("id"));
        }
    }

    @Override
    public Flux<SolrInputDocument> getDocumentFlux() {
        return sink.asFlux();
    }
}
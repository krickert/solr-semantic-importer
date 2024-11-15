package com.krickert.search.indexer.solr.vector.event;

import org.apache.solr.common.SolrInputDocument;

import java.util.UUID;

public interface DocumentListener {
    void processDocument(SolrInputDocument document);
}
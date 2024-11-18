package com.krickert.search.indexer.solr.vector.event;

import com.krickert.search.indexer.config.VectorConfig;
import org.apache.solr.common.SolrInputDocument;

public class ChunkDocumentRequest {
    private final SolrInputDocument document;
    private final String fieldName;
    private final VectorConfig vectorConfig;
    private final String origDocId;
    private String fieldData;
    private String crawlId;
    private String parentCollection;
    private Object dateCreated;

    public ChunkDocumentRequest(SolrInputDocument document, String fieldName, VectorConfig vectorConfig, String origDocId, String parentCollection) {
        this.document = document;
        this.fieldName = fieldName;
        this.vectorConfig = vectorConfig;
        this.origDocId = origDocId;
    }

    public SolrInputDocument getDocument() {
        return document;
    }

    public String getFieldName() {
        return fieldName;
    }

    public VectorConfig getVectorConfig() {
        return vectorConfig;
    }

    public String getOrigDocId() {
        return origDocId;
    }

    public String getFieldData() {
        return fieldData;
    }

    public void setFieldData(String fieldData) {
        this.fieldData = fieldData;
    }

    public String getCrawlId() {
        return crawlId;
    }

    public void setCrawlId(String crawlId) {
        this.crawlId = crawlId;
    }

    public Object getDateCreated() {
        return dateCreated;
    }

    public void setDateCreated(Object dateCreated) {
        this.dateCreated = dateCreated;
    }

    public String getParentCollection() {
        return parentCollection;
    }
    public void setParentCollection(String parentCollection) {
        this.parentCollection = parentCollection;
    }
}

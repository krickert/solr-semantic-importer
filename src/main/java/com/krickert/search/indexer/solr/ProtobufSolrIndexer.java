package com.krickert.search.indexer.solr;

import com.google.protobuf.Message;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import jakarta.inject.Inject;
import jakarta.inject.Singleton;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.impl.Http2SolrClient;
import org.apache.solr.common.SolrInputDocument;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Singleton
public class ProtobufSolrIndexer {
    private static final Logger log = LoggerFactory.getLogger(ProtobufSolrIndexer.class);

    private final ProtobufToSolrDocument protobufToSolrDocument;
    private final IndexerConfiguration indexerConfiguration;
    private final SolrClient solrClient;

    @Inject
    public ProtobufSolrIndexer(ProtobufToSolrDocument protobufToSolrDocument,
                               IndexerConfiguration indexerConfiguration,
                               SolrClient solrClient) {
        this.protobufToSolrDocument = protobufToSolrDocument;
        this.indexerConfiguration = indexerConfiguration;
        this.solrClient = solrClient;
        log.info("ProtobufSolrIndexer creatted.");
    }

    public void exportProtobufToSolr(Collection<Message> protos) {
        List<SolrInputDocument> solrDocuments = protos.stream().map(protobufToSolrDocument::convertProtobufToSolrDocument).collect(Collectors.toList());

        String collection = indexerConfiguration.getDestinationSolrConfiguration().getCollection();
        try {
            solrClient.add(collection, solrDocuments);
            solrClient.commit(collection);
        } catch (SolrServerException | IOException e) {
            log.error("Commit solr failed for collection {}", collection, e);
        }

    }


}

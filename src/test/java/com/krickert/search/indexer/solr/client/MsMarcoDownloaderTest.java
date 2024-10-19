package com.krickert.search.indexer.solr.client;

import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.*;

import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class MsMarcoDownloaderTest {

    @Mock
    private MsMarcoDownloader msMarcoDownloader;

    @BeforeEach
    public void setUp() {
        MockitoAnnotations.initMocks(this);
        when(msMarcoDownloader.getSolrQueue()).thenReturn(new LinkedBlockingQueue<SolrInputDocument>(10_000_000));
    }

    @Test
    public void testDownloadAndProcessTgz() {
        msMarcoDownloader.downloadAndProcessTgz();

        verify(msMarcoDownloader, times(1)).downloadAndProcessTgz();

        BlockingQueue<SolrInputDocument> solrQueue = msMarcoDownloader.getSolrQueue();

        assertNotNull(solrQueue);
    }
}
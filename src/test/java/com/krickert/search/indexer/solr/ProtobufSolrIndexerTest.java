package com.krickert.search.indexer.solr;

import com.google.protobuf.Message;
import com.krickert.search.indexer.config.IndexerConfiguration;
import com.krickert.search.indexer.config.SolrConfiguration;
import com.krickert.search.indexer.enhancers.ProtobufToSolrDocument;
import org.apache.solr.client.solrj.SolrClient;
import org.apache.solr.client.solrj.SolrServerException;
import org.apache.solr.client.solrj.response.UpdateResponse;
import org.apache.solr.common.SolrInputDocument;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.*;

import java.io.IOException;
import java.util.Collection;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class ProtobufSolrIndexerTest {

    @Mock
    private ProtobufToSolrDocument protobufToSolrDocument;

    @Mock
    private IndexerConfiguration indexerConfiguration;

    @Mock
    private SolrClient solrClient;

    @Captor
    private ArgumentCaptor<Collection<SolrInputDocument>> solrDocumentsCaptor;

    @InjectMocks
    private ProtobufSolrIndexer protobufSolrIndexer;
    private AutoCloseable autoCloseable;

    @BeforeEach
    void setUp()  {

        this.autoCloseable = MockitoAnnotations.openMocks(this);
        SolrConfiguration configuration = new SolrConfiguration("testCollection");
        configuration.setCollection("testCollection");
        SolrConfiguration.Connection connection = new SolrConfiguration.Connection();
        connection.setUrl("http://localhost:8983/solr");
        configuration.setConnection(connection);
        when(indexerConfiguration.getDestinationSolrConfiguration()).thenReturn(configuration);
        // Mock solrClient add and commit methods to avoid real HTTP calls
        try {
            when(solrClient.add(eq("testCollection"), ArgumentMatchers.<Collection<SolrInputDocument>>any()))
                    .thenReturn(new UpdateResponse());
            when(solrClient.commit("testCollection")).thenReturn(new UpdateResponse());
            // Use doReturn to simulate the non-void methods without actual HTTP calls
            doReturn(null).when(solrClient).add(eq("testCollection"), ArgumentMatchers.<Collection<SolrInputDocument>>any());
            doReturn(null).when(solrClient).commit("testCollection");
        } catch (SolrServerException | IOException e) {
            fail(e);
        }
    }

    @AfterEach
    void tearDown() throws Exception {
        autoCloseable.close();
    }

    @Test
    void testExportProtobufToSolr() throws SolrServerException, IOException {
        // Arrange
        Message mockMessage = mock(Message.class);
        SolrInputDocument mockSolrDocument = new SolrInputDocument();
        when(protobufToSolrDocument.convertProtobufToSolrDocument(mockMessage)).thenReturn(mockSolrDocument);

        // Act
        protobufSolrIndexer.exportProtobufToSolr(List.of(mockMessage));

        // Assert
        verify(solrClient, times(1)).add(eq("testCollection"), solrDocumentsCaptor.capture());
        verify(solrClient, times(1)).commit("testCollection");
        Collection<SolrInputDocument> capturedDocuments = solrDocumentsCaptor.getValue();
        assertEquals(1, capturedDocuments.size());
        assertEquals(mockSolrDocument, capturedDocuments.iterator().next());
    }

    @Test
    void testExportProtobufToSolr_IOException() throws SolrServerException, IOException {
        // Arrange
        Message mockMessage = mock(Message.class);
        SolrInputDocument mockSolrDocument = new SolrInputDocument();
        when(protobufToSolrDocument.convertProtobufToSolrDocument(mockMessage)).thenReturn(mockSolrDocument);
        doThrow(new IOException("Test IOException")).when(solrClient).add(any(String.class), ArgumentMatchers.<Collection<SolrInputDocument>>any());

        // Act
        protobufSolrIndexer.exportProtobufToSolr(List.of(mockMessage));

        // Assert
        verify(solrClient, times(1)).add(eq("testCollection"), ArgumentMatchers.<Collection<SolrInputDocument>>any());
        verify(solrClient, never()).commit(any(String.class));
    }

    @Test
    void testExportProtobufToSolr_SolrServerException() throws SolrServerException, IOException {
        // Arrange
        Message mockMessage = mock(Message.class);
        SolrInputDocument mockSolrDocument = new SolrInputDocument();
        when(protobufToSolrDocument.convertProtobufToSolrDocument(mockMessage)).thenReturn(mockSolrDocument);
        doThrow(new SolrServerException("Test SolrServerException")).when(solrClient).commit(any(String.class));

        // Act
        protobufSolrIndexer.exportProtobufToSolr(List.of(mockMessage));

        // Assert
        verify(solrClient, times(1)).add(eq("testCollection"), ArgumentMatchers.<Collection<SolrInputDocument>>any());
        verify(solrClient, times(1)).commit(any(String.class));
    }
}

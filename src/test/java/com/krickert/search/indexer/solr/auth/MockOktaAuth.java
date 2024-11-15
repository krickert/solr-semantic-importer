package com.krickert.search.indexer.solr.auth;

import com.krickert.search.indexer.solr.client.OktaAuth;
import jakarta.inject.Singleton;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;

@Singleton
public class MockOktaAuth implements OktaAuth {
    private static final Logger log = LoggerFactory.getLogger(MockOktaAuth.class);

    public MockOktaAuth() {
        log.info("Mock of okta auth has been created.");
    }

    @Override
    public String getAccessToken() throws IOException {
        return "access-toke-fake";
    }

}

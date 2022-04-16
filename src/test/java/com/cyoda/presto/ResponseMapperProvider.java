package com.cyoda.presto;

import com.facebook.presto.example.TestExampleClient;
import com.google.common.collect.ImmutableMap;
import com.google.common.io.Resources;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import java.io.IOException;
import java.net.URL;
import java.util.Map;
import java.util.Optional;

public class ResponseMapperProvider {

    private Optional<ImmutableMap<String,ResponseMapper>> pathInfoToMapper;

    public ResponseMapperProvider() {
    }

    public ResponseMapperProvider withResponseMapperProvider(ImmutableMap<String,ResponseMapper> pathInfoToMapper) {
        this.pathInfoToMapper = Optional.of(pathInfoToMapper);
        return this;
    }

    public void copyTo(HttpServletRequest request, ServletOutputStream outputStream) throws IOException {
        String resourceLocation = pathInfoToMapper
                .orElseThrow(()->new IllegalStateException("need to provide a ResponeMapper"))
                .get(request.getPathInfo()).resolveResponse();
        URL dataUrl = Resources.getResource(TestExampleClient.class, resourceLocation);
        Resources.asByteSource(dataUrl).copyTo(outputStream);
    }
}

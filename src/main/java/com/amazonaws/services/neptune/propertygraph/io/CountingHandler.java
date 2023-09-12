package com.amazonaws.services.neptune.propertygraph.io;

import java.io.IOException;

class CountingHandler<T> implements GraphElementHandler<T> {

    private final GraphElementHandler<T> parent;
    private long counter = 0;

    CountingHandler(GraphElementHandler<T> parent) {
        this.parent = parent;
    }

    @Override
    public void handle(T input, boolean allowTokens) throws IOException {
        parent.handle(input, allowTokens);
        counter++;
    }

    long numberProcessed() {
        return counter;
    }

    @Override
    public void close() throws Exception {
        parent.close();
    }
}

package org.north.core.io;

import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Iterator;

public interface ResourceLoader {

    InputStream loadAsStream(String path);

    String loadAsString(String path) throws IOException;

    URL getResourceUrl(String path);

    Iterator<URL> getAllResourceUrl(String path) throws IOException;

}

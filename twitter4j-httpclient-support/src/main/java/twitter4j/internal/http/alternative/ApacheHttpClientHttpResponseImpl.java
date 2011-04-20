/*
 * Copyright 2007 Yusuke Yamamoto
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package twitter4j.internal.http.alternative;

import org.apache.http.Header;
import org.apache.http.HeaderElement;
import org.apache.http.HttpResponse;
import twitter4j.internal.http.HttpClientConfiguration;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

/**
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @since Twitter4J 2.1.2
 */
final class ApacheHttpClientHttpResponseImpl extends twitter4j.internal.http.HttpResponse {
    private HttpResponse res;

    ApacheHttpClientHttpResponseImpl(HttpResponse res, HttpClientConfiguration conf) throws IOException {
        super(conf);
        this.res = res;
        is = res.getEntity().getContent();
        statusCode = res.getStatusLine().getStatusCode();
        if (null != is && "gzip".equals(getResponseHeader("Content-Encoding"))) {
            // the response is gzipped
            is = new GZIPInputStream(is);
        }
    }

    /**
     * {@inheritDoc}
     */
    public final String getResponseHeader(String name) {
        Header[] headers = res.getHeaders(name);
        if (null != headers && headers.length > 0) {
            return headers[0].getValue();
        } else {
            return null;
        }
    }

    @Override
    public Map<String, List<String>> getResponseHeaderFields() {
        Header[] headers = res.getAllHeaders();
        Map<String, List<String>> maps = new HashMap<String, List<String>>();
<<<<<<< HEAD
        for(Header header : headers){
            HeaderElement[] elements = header.getElements();
            for(HeaderElement element : elements){
                List<String> values;
                if(null == (values = maps.get(element.getName()))){
=======
        for (Header header : headers) {
            HeaderElement[] elements = header.getElements();
            for (HeaderElement element : elements) {
                List<String> values;
                if (null == (values = maps.get(element.getName()))) {
>>>>>>> e94561b24ae0ceb99f9e34e52703c85a6849ea21
                    values = new ArrayList<String>(1);
                    maps.put(element.getName(), values);
                }
                values.add(element.getValue());
            }
        }
        return maps;
    }

    /**
     * {@inheritDoc}
     */
    public void disconnect() throws IOException {
        if (null != res) {
            res.getEntity().consumeContent();
        }
    }
}

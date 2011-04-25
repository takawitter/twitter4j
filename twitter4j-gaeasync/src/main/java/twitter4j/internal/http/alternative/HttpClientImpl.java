package twitter4j.internal.http.alternative;

import static twitter4j.internal.http.RequestMethod.POST;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPMethod;
import com.google.appengine.api.urlfetch.HTTPRequest;
import com.google.appengine.api.urlfetch.URLFetchService;
import com.google.appengine.api.urlfetch.URLFetchServiceFactory;

import twitter4j.TwitterException;
import twitter4j.gaeasync.HttpResponseAdapter;
import twitter4j.internal.http.HttpClient;
import twitter4j.internal.http.HttpClientConfiguration;
import twitter4j.internal.http.HttpParameter;
import twitter4j.internal.http.HttpRequest;
import twitter4j.internal.http.HttpResponse;
import twitter4j.internal.logging.Logger;
import twitter4j.internal.util.T4JInternalStringUtil;

public class HttpClientImpl implements HttpClient{
	public HttpClientImpl(HttpClientConfiguration conf) {
    }

	public HttpResponse request(HttpRequest req) throws TwitterException {
    	URLFetchService service = URLFetchServiceFactory.getURLFetchService();
    	HTTPRequest request = null;
    	try{
    		request = new HTTPRequest(
	    			new URL(req.getURL())
	    			, HTTPMethod.valueOf(req.getMethod().name())
	    			);
    	} catch(MalformedURLException e){
    		throw new TwitterException(e);
    	}
    	
        int responseCode = -1;
        ByteArrayOutputStream os = null;
        try {
            setHeaders(req, request);
            if (req.getMethod() == POST) {
                if (HttpParameter.containsFile(req.getParameters())) {
                    String boundary = "----Twitter4J-upload" + System.currentTimeMillis();
                    request.setHeader(new HTTPHeader("Content-Type", "multipart/form-data; boundary=" + boundary));
                    boundary = "--" + boundary;
                    os = new ByteArrayOutputStream();
                    DataOutputStream out = new DataOutputStream(os);
                    for (HttpParameter param : req.getParameters()) {
                        if (param.isFile()) {
                            write(out, boundary + "\r\n");
                            write(out, "Content-Disposition: form-data; name=\"" + param.getName() + "\"; filename=\"" + param.getFile().getName() + "\"\r\n");
                            write(out, "Content-Type: " + param.getContentType() + "\r\n\r\n");
                            BufferedInputStream in = new BufferedInputStream(
                            		param.hasFileBody() ? param.getFileBody() :new FileInputStream(param.getFile())
                            		);
                            int buff = 0;
                            while ((buff = in.read()) != -1) {
                                out.write(buff);
                            }
                            write(out, "\r\n");
                            in.close();
                        } else {
                            write(out, boundary + "\r\n");
                            write(out, "Content-Disposition: form-data; name=\"" + param.getName() + "\"\r\n");
                            write(out, "Content-Type: text/plain; charset=UTF-8\r\n\r\n");
                            logger.debug(param.getValue());
//                                    out.write(encode(param.getValue()).getBytes("UTF-8"));
                            out.write(param.getValue().getBytes("UTF-8"));
                            write(out, "\r\n");
                        }
                    }
                    write(out, boundary + "--\r\n");
                    write(out, "\r\n");
                } else {
                    request.setHeader(new HTTPHeader(
                    		"Content-Type",
                            "application/x-www-form-urlencoded"
                    		));
                    String postParam = HttpParameter.encodeParameters(req.getParameters());
                    logger.debug("Post Params: ", postParam);
                    byte[] bytes = postParam.getBytes("UTF-8");
//                    request.setHeader(new HTTPHeader(
//                    		"Content-Length",
//                            Integer.toString(bytes.length)
//                            ));
                    os = new ByteArrayOutputStream();
                    os.write(bytes);
                }
                request.setPayload(os.toByteArray());
            }
            return new HttpResponseAdapter(service.fetchAsync(request));
        } catch (IOException ioe) {
            // connection timeout or read timeout
        	throw new TwitterException(ioe.getMessage(), ioe, responseCode);
        }
	}

	public void shutdown() {
	}
	
    private void setHeaders(HttpRequest req, HTTPRequest request) {
        if (logger.isDebugEnabled()) {
            logger.debug("Request: ");
            logger.debug(req.getMethod().name() + " ", req.getURL());
        }

        String authorizationHeader;
        if (null != req.getAuthorization() && null != (authorizationHeader = req.getAuthorization().getAuthorizationHeader(req))) {
            if (logger.isDebugEnabled()) {
                logger.debug("Authorization: ", T4JInternalStringUtil.maskString(authorizationHeader));
            }
            request.setHeader(new HTTPHeader("Authorization", authorizationHeader));
        }
        if (null != req.getRequestHeaders()) {
            for (String key : req.getRequestHeaders().keySet()) {
            	request.addHeader(new HTTPHeader(key, req.getRequestHeaders().get(key)));
                logger.debug(key + ": " + req.getRequestHeaders().get(key));
            }
        }
    }

    private void write(DataOutputStream out, String outStr) throws IOException {
        out.writeBytes(outStr);
        logger.debug(outStr);
    }

    private static Logger logger = Logger.getLogger(HttpClientImpl.class);
}

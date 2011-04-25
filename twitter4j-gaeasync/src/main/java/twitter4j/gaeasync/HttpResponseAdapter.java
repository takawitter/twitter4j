package twitter4j.gaeasync;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.logging.Logger;

import twitter4j.conf.ConfigurationContext;
import twitter4j.internal.http.HttpResponse;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPResponse;

public class HttpResponseAdapter extends HttpResponse{
	public HttpResponseAdapter(Future<HTTPResponse> futureResponse){
		super(ConfigurationContext.getInstance());
		this.future = futureResponse;
	}

	@Override
	public String getResponseHeader(String name) {
		ensureResponse();
		return headers.get(name);
	}

	@Override
	public Map<String, List<String>> getResponseHeaderFields() {
		ensureResponse();
		Map<String, List<String>> ret = new TreeMap<String, List<String>>();
		for(Map.Entry<String, String> entry : headers.entrySet()){
			ret.put(entry.getKey(), Arrays.asList(entry.getValue()));
		}
		return ret;
	}

	@Override
	public void disconnect() throws IOException {
		if(!future.isDone() && !future.isCancelled()){
			future.cancel(true);
		}
	}

	private void ensureResponse(){
		if(responseGot) return;
		Logger.getAnonymousLogger().warning("ensureResponse called");
		if(future.isCancelled()) throw new IllegalStateException("HttpResponse already disconnected.");
		try{
			HTTPResponse r = future.get();
			headers = new HashMap<String, String>();
			for(HTTPHeader h : r.getHeaders()){
				headers.put(h.getName(), h.getValue());
			}
			statusCode = r.getResponseCode();
			byte[] content = r.getContent();
			Logger.getAnonymousLogger().warning(new String(content, "UTF-8"));
			is = new ByteArrayInputStream(content);
			responseGot = true;
		} catch(ExecutionException e){
			throw new RuntimeException(e);
		} catch(InterruptedException e){
			throw new RuntimeException(e);
		} catch (UnsupportedEncodingException e) {
			throw new RuntimeException(e);
		}
	}

	private Future<HTTPResponse> future;
	private boolean responseGot;
	private Map<String, String> headers;
}

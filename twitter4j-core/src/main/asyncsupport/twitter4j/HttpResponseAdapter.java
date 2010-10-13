package twitter4j;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import com.google.appengine.api.urlfetch.HTTPHeader;
import com.google.appengine.api.urlfetch.HTTPResponse;

import twitter4j.internal.http.HttpResponse;

public class HttpResponseAdapter extends HttpResponse{
	public HttpResponseAdapter(Future<HTTPResponse> futureResponse){
		this.future = futureResponse;
	}

	@Override
	public int getStatusCode() {
		ensureResponse();
		return statusCode;
	}

	@Override
	public String getResponseHeader(String name) {
		ensureResponse();
		return headers.get(name);
	}

	@Override
	public InputStream asStream() {
		ensureResponse();
		return stream;
	}

	@Override
	public void disconnect() throws IOException {
		if(!future.isDone() && !future.isCancelled()){
			future.cancel(true);
		}
	}

	private void ensureResponse(){
		if(responseGot) return;
		if(future.isCancelled()) throw new IllegalStateException("HttpResponse already disconnected.");
		try{
			HTTPResponse r = future.get();
			headers = new HashMap<String, String>();
			for(HTTPHeader h : r.getHeaders()){
				headers.put(h.getName(), h.getValue());
			}
			statusCode = r.getResponseCode();
			stream = new ByteArrayInputStream(r.getContent());
			responseGot = true;
		} catch(ExecutionException e){
			throw new RuntimeException(e);
		} catch(InterruptedException e){
			throw new RuntimeException(e);
		}
	}

	private Future<HTTPResponse> future;
	private boolean responseGot;
	private int statusCode;
	private Map<String, String> headers;
	private InputStream stream;
}

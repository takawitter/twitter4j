package twitter4j.gaeasync;

import twitter4j.TwitterException;

public abstract class TwitterFutureAdapter<T> implements TwitterFuture<T>{
	public T get() throws TwitterException{
		if(result == null){
			result = doGet();
		}
		return result;
	}

	protected abstract T doGet()
	throws TwitterException;

	private T result;
}

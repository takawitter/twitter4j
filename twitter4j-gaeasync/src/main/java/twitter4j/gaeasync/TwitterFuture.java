package twitter4j.gaeasync;

import twitter4j.TwitterException;

public interface TwitterFuture<T>{
	T get() throws TwitterException;
}

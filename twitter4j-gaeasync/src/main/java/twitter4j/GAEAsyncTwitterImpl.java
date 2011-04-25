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

package twitter4j;

import static twitter4j.internal.http.HttpParameter.getParameterArray;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import twitter4j.auth.Authorization;
import twitter4j.conf.Configuration;
import twitter4j.gaeasync.TwitterFuture;
import twitter4j.gaeasync.TwitterFutureAdapter;
import twitter4j.internal.http.HttpParameter;
import twitter4j.internal.http.HttpResponse;
import twitter4j.internal.org.json.JSONException;
import twitter4j.internal.util.T4JInternalStringUtil;

/**
 * A java representation of the <a href="http://apiwiki.twitter.com/">Twitter API</a><br>
 * This class is thread safe and can be cached/re-used and used concurrently.<br>
 * Currently this class is not carefully designed to be extended. It is suggested to extend this class only for mock testing purporse.<br>
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
class GAEAsyncTwitterImpl extends TwitterBaseImpl
        implements GAEAsyncTwitter {
    private static final long serialVersionUID = -1486360080128882436L;

    /*package*/
    GAEAsyncTwitterImpl(Configuration conf) {
        super(conf);
        INCLUDE_ENTITIES = new HttpParameter("include_entities", conf.isIncludeEntitiesEnabled());
        INCLUDE_RTS = new HttpParameter("include_rts", conf.isIncludeRTsEnabled());
    }
    
    /*package*/
    GAEAsyncTwitterImpl(Configuration conf, Authorization auth) {
        super(conf, auth);
        INCLUDE_ENTITIES = new HttpParameter("include_entities", conf.isIncludeEntitiesEnabled());
        INCLUDE_RTS = new HttpParameter("include_rts", conf.isIncludeRTsEnabled());
    }

    private final HttpParameter INCLUDE_ENTITIES;
    private final HttpParameter INCLUDE_RTS;


    private HttpParameter[] mergeParameters(HttpParameter[] params1, HttpParameter[] params2) {
        if (null != params1 && null != params2) {
            HttpParameter[] params = new HttpParameter[params1.length + params2.length];
            System.arraycopy(params1, 0, params, 0, params1.length);
            System.arraycopy(params2, 0, params, params1.length, params2.length);
            return params;
        }
        if (null == params1 && null == params2) {
            return new HttpParameter[0];
        }
        if (null != params1) {
            return params1;
        } else {
            return params2;
        }
    }

    private HttpParameter[] mergeParameters(HttpParameter[] params1, HttpParameter params2) {
        if (null != params1 && null != params2) {
            HttpParameter[] params = new HttpParameter[params1.length + 1];
            System.arraycopy(params1, 0, params, 0, params1.length);
            params[params.length - 1] = params2;
            return params;
        }
        if (null == params1 && null == params2) {
            return new HttpParameter[0];
        }
        if (null != params1) {
            return params1;
        } else {
            return new HttpParameter[]{params2};
        }
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<QueryResult> search(final Query query) throws TwitterException {
    	final HttpResponse res = get(conf.getSearchBaseURL()
                + "search.json", query.asHttpParameterArray());
    	return new TwitterFutureAdapter<QueryResult>(){
    		@Override
    		protected QueryResult doGet() throws TwitterException {
    	        try {
    	            return new QueryResultJSONImpl(res, conf);
    	        } catch (TwitterException te) {
    	            if (404 == te.getStatusCode()) {
    	                return new QueryResultJSONImpl(query);
    	            } else {
    	                throw te;
    	            }
    	        }
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Trends> getTrends() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "trends.json");
    	return new TwitterFutureAdapter<Trends>(){
    		@Override
    		protected Trends doGet() throws TwitterException {
    			return new TrendsJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Trends> getCurrentTrends() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "trends/current.json");
    	return new TwitterFutureAdapter<Trends>(){
    		@Override
    		protected Trends doGet() throws TwitterException {
    			return TrendsJSONImpl.createTrendsList(res, conf.isJSONStoreEnabled()).get(0);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Trends> getCurrentTrends(boolean excludeHashTags) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "trends/current.json"
                + (excludeHashTags ? "?exclude=hashtags" : ""));
    	return new TwitterFutureAdapter<Trends>(){
    		@Override
    		protected Trends doGet() throws TwitterException {
    			return TrendsJSONImpl.createTrendsList(res, conf.isJSONStoreEnabled()).get(0);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<List<Trends>> getDailyTrends() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "trends/daily.json");
    	return new TwitterFutureAdapter<List<Trends>>(){
    		@Override
    		protected List<Trends> doGet() throws TwitterException {
    			return TrendsJSONImpl.createTrendsList(res, conf.isJSONStoreEnabled());
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<List<Trends>> getDailyTrends(Date date, boolean excludeHashTags) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "trends/daily.json?date=" + toDateStr(date)
                + (excludeHashTags ? "&exclude=hashtags" : ""));
    	return new TwitterFutureAdapter<List<Trends>>(){
    		@Override
    		protected List<Trends> doGet() throws TwitterException {
    			return TrendsJSONImpl.createTrendsList(res, conf.isJSONStoreEnabled());
    		}
    	};
    }

    private String toDateStr(Date date) {
        if (null == date) {
            date = new Date();
        }
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd");
        return sdf.format(date);
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<List<Trends>> getWeeklyTrends() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "trends/weekly.json");
    	return new TwitterFutureAdapter<List<Trends>>(){
    		@Override
    		protected List<Trends> doGet() throws TwitterException {
    			return TrendsJSONImpl.createTrendsList(res, conf.isJSONStoreEnabled());
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<List<Trends>> getWeeklyTrends(Date date, boolean excludeHashTags) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "trends/weekly.json?date=" + toDateStr(date)
                + (excludeHashTags ? "&exclude=hashtags" : ""));
    	return new TwitterFutureAdapter<List<Trends>>(){
    		@Override
    		protected List<Trends> doGet() throws TwitterException {
    			return TrendsJSONImpl.createTrendsList(res, conf.isJSONStoreEnabled());
    		}
    	};
    }

    /* Status Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getPublicTimeline() throws
            TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/public_timeline.json?include_entities=" + conf.isIncludeEntitiesEnabled() + "&include_rts=" + conf.isIncludeRTsEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getHomeTimeline() throws
            TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + "statuses/home_timeline.json?include_entities=" + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getHomeTimeline(Paging paging) throws
            TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/home_timeline.json", mergeParameters(paging.asPostParameterArray(), INCLUDE_ENTITIES));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getFriendsTimeline() throws
            TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/friends_timeline.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&include_rts=" + conf.isIncludeRTsEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getFriendsTimeline(Paging paging) throws
            TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/friends_timeline.json",
                mergeParameters(new HttpParameter[]{INCLUDE_RTS, INCLUDE_ENTITIES}
                        , paging.asPostParameterArray()));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }


    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserTimeline(String screenName, Paging paging)
            throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/user_timeline.json",
                mergeParameters(new HttpParameter[]{new HttpParameter("screen_name", screenName)
                        , INCLUDE_RTS
                        , INCLUDE_ENTITIES}
                        , paging.asPostParameterArray()));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserTimeline(long userId, Paging paging)
            throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/user_timeline.json",
                mergeParameters(new HttpParameter[]{new HttpParameter("user_id", userId)
                        , INCLUDE_RTS
                        , INCLUDE_ENTITIES}
                        , paging.asPostParameterArray()));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserTimeline(String screenName) throws TwitterException {
        return getUserTimeline(screenName, new Paging());
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserTimeline(long userId) throws TwitterException {
        return getUserTimeline(userId, new Paging());
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserTimeline() throws
            TwitterException {
        return getUserTimeline(new Paging());
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserTimeline(Paging paging) throws
            TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/user_timeline.json",
                mergeParameters(new HttpParameter[]{INCLUDE_RTS
                        , INCLUDE_ENTITIES}
                        , paging.asPostParameterArray()));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getMentions() throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/mentions.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&include_rts=" + conf.isIncludeRTsEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getMentions(Paging paging) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/mentions.json",
                mergeParameters(new HttpParameter[]{INCLUDE_RTS
                        , INCLUDE_ENTITIES}
                        , paging.asPostParameterArray()));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedByMe() throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/retweeted_by_me.json?include_entities=" + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedByMe(Paging paging) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/retweeted_by_me.json", mergeParameters(paging.asPostParameterArray()
                        , INCLUDE_ENTITIES));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedToMe() throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/retweeted_to_me.json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedToMe(Paging paging) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/retweeted_to_me.json", mergeParameters(paging.asPostParameterArray()
                        , INCLUDE_ENTITIES));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetsOfMe() throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/retweets_of_me.json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetsOfMe(Paging paging) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/retweets_of_me.json", mergeParameters(paging.asPostParameterArray()
                        , INCLUDE_ENTITIES));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedToUser(String screenName, Paging paging) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/retweeted_to_user.json", mergeParameters(paging.asPostParameterArray()
                        , new HttpParameter[]{
                                new HttpParameter("screen_name", screenName)
                                , INCLUDE_ENTITIES}));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedToUser(long userId, Paging paging) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/retweeted_to_user.json", mergeParameters(paging.asPostParameterArray()
                        , new HttpParameter[]{
                                new HttpParameter("user_id", userId)
                                , INCLUDE_ENTITIES}));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedByUser(String screenName, Paging paging) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/retweeted_by_user.json", mergeParameters(paging.asPostParameterArray()
                        , new HttpParameter[]{
                                new HttpParameter("screen_name", screenName)
                                , INCLUDE_ENTITIES}));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweetedByUser(long userId, Paging paging) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "statuses/retweeted_by_user.json", mergeParameters(paging.asPostParameterArray()
                        , new HttpParameter[]{
                                new HttpParameter("user_id", userId)
                                , INCLUDE_ENTITIES}));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> getRetweetedBy(final long statusId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/" + statusId + "/retweeted_by.json?count=100&include_entities"
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<ResponseList<User>>(){
        	public twitter4j.ResponseList<User> doGet()
        	throws TwitterException {
       			return UserJSONImpl.createUserList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getRetweetedByIDs(long statusId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/" + statusId + "/retweeted_by/ids.json?count=100&include_entities"
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<IDs>(){
        	public IDs doGet() throws TwitterException {
       			return new IDsJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Status> showStatus(long id) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "statuses/show/" + id + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<Status>(){
        	public Status doGet() throws TwitterException {
       			return new StatusJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Status> updateStatus(String status) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "statuses/update.json",
                new HttpParameter[]{new HttpParameter("status", status)
                , INCLUDE_ENTITIES});
        return new TwitterFutureAdapter<Status>(){
        	public Status doGet()
        	throws TwitterException {
        		return new StatusJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Status> updateStatus(StatusUpdate latestStatus) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL()
                + "statuses/update.json",
                mergeParameters(latestStatus.asHttpParameterArray(),
                        INCLUDE_ENTITIES));
        return new TwitterFutureAdapter<Status>(){
        	public Status doGet()
        	throws TwitterException {
        		return new StatusJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Status> destroyStatus(long statusId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL()
                + "statuses/destroy/" + statusId + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<Status>(){
        	public Status doGet()
        	throws TwitterException {
       			return new StatusJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Status> retweetStatus(long statusId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL()
                + "statuses/retweet/" + statusId + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<Status>(){
        	public Status doGet()
        	throws TwitterException {
       			return new StatusJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getRetweets(long statusId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/retweets/" + statusId + ".json?count=100&include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<ResponseList<Status>>(){
        	public ResponseList<Status> doGet()
        	throws TwitterException {
        		return StatusJSONImpl.createStatusList(res, conf);
        	}
        };
    }

    /* User Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> showUser(String screenName) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "users/show.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> showUser(long userId) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "users/show.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> lookupUsers(String[] screenNames) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "users/lookup.json", new HttpParameter[]{
            new HttpParameter("screen_name", T4JInternalStringUtil.join(screenNames))
            , INCLUDE_ENTITIES});
    	return new TwitterFutureAdapter<ResponseList<User>>(){
    		@Override
    		protected ResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> lookupUsers(long[] ids) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "users/lookup.json", new HttpParameter[]{
            new HttpParameter("user_id", T4JInternalStringUtil.join(ids))
            , INCLUDE_ENTITIES});
    	return new TwitterFutureAdapter<ResponseList<User>>(){
    		@Override
    		protected ResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> searchUsers(String query, int page) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                "users/search.json", new HttpParameter[]{
            new HttpParameter("q", query),
            new HttpParameter("per_page", 20),
            new HttpParameter("page", page)
            , INCLUDE_ENTITIES});
    	return new TwitterFutureAdapter<ResponseList<User>>(){
    		@Override
    		protected ResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Category>> getSuggestedUserCategories() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() +
    			"users/suggestions.json");
    	return new TwitterFutureAdapter<ResponseList<Category>>(){
    		@Override
    		protected ResponseList<Category> doGet() throws TwitterException {
    			return CategoryJSONImpl.createCategoriesList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> getUserSuggestions(String categorySlug) throws TwitterException {
        final HttpResponse res = get(conf.getRestBaseURL() + "users/suggestions/"
                + categorySlug + ".json");
    	return new TwitterFutureAdapter<ResponseList<User>>(){
    		@Override
    		protected ResponseList<User> doGet() throws TwitterException {
    			try {
		            return UserJSONImpl.createUserList(res.asJSONObject().getJSONArray("users"), res, conf);
		        } catch (JSONException jsone) {
		            throw new TwitterException(jsone);
		        }
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> getMemberSuggestions(String categorySlug) throws TwitterException {
        final HttpResponse res = get(conf.getRestBaseURL() + "users/suggestions/"
                + categorySlug + "/members.json");
    	return new TwitterFutureAdapter<ResponseList<User>>(){
    		@Override
    		protected ResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createUserList(res.asJSONArray(), res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ProfileImage> getProfileImage(String screenName, ProfileImage.ImageSize size) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "users/profile_image/"
                + screenName + ".json?size=" + size.getName());
    	return new TwitterFutureAdapter<ProfileImage>(){
    		@Override
    		protected ProfileImage doGet() throws TwitterException {
    			return new ProfileImageImpl(res);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getFriendsStatuses(long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/friends.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getFriendsStatuses(String screenName, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/friends.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName + "&cursor="
                + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getFriendsStatuses(long userId, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/friends.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId
                + "&cursor=" + cursor, null);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getFollowersStatuses(long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/followers.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getFollowersStatuses(String screenName, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/followers.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getFollowersStatuses(long userId, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "statuses/followers.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /*List Methods*/

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> createUserList(String listName, boolean isPublicList, String description) throws TwitterException {
        ensureAuthorizationEnabled();
        List<HttpParameter> httpParams = new ArrayList<HttpParameter>();
        httpParams.add(new HttpParameter("name", listName));
        httpParams.add(new HttpParameter("mode", isPublicList ? "public" : "private"));
        if (description != null) {
            httpParams.add(new HttpParameter("description", description));
        }
    	final HttpResponse res = post(conf.getRestBaseURL() + getScreenName() +
                "/lists.json",
                httpParams.toArray(new HttpParameter[httpParams.size()]));
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> updateUserList(int listId, String newListName, boolean isPublicList, String newDescription) throws TwitterException {
        ensureAuthorizationEnabled();
        List<HttpParameter> httpParams = new ArrayList<HttpParameter>();
        if (newListName != null) {
            httpParams.add(new HttpParameter("name", newListName));
        }
        httpParams.add(new HttpParameter("mode", isPublicList ? "public" : "private"));
        if (newDescription != null) {
            httpParams.add(new HttpParameter("description", newDescription));
        }
    	final HttpResponse res = post(conf.getRestBaseURL() + getScreenName() + "/lists/"
                + listId + ".json", httpParams.toArray(new HttpParameter[httpParams.size()]));
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<UserList>> getUserLists(String listOwnerScreenName, long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                listOwnerScreenName + "/lists.json?cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<UserList>>(){
    		@Override
    		protected PagableResponseList<UserList> doGet() throws TwitterException {
    			return UserListJSONImpl.createPagableUserListList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> showUserList(String listOwnerScreenName, int id) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + listOwnerScreenName + "/lists/"
                + id + ".json");
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> destroyUserList(int listId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = delete(conf.getRestBaseURL() + getScreenName() +
                "/lists/" + listId + ".json");
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserListStatuses(String listOwnerScreenName, int id, Paging paging) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + listOwnerScreenName +
                "/lists/" + id + "/statuses.json", mergeParameters(paging.asPostParameterArray(Paging.SMCP, Paging.PER_PAGE)
                        , INCLUDE_ENTITIES));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getUserListStatuses(long listOwnerId, int id, Paging paging) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + listOwnerId +
                "/lists/" + id + "/statuses.json", mergeParameters(paging.asPostParameterArray(Paging.SMCP, Paging.PER_PAGE)
                        , INCLUDE_ENTITIES));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		@Override
    		protected ResponseList<Status> doGet() throws TwitterException {
    			return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<UserList>> getUserListMemberships(String listMemberScreenName, long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                listMemberScreenName + "/lists/memberships.json?cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<UserList>>(){
    		@Override
    		protected PagableResponseList<UserList> doGet() throws TwitterException {
    			return UserListJSONImpl.createPagableUserListList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<UserList>> getUserListSubscriptions(String listOwnerScreenName, long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                listOwnerScreenName + "/lists/subscriptions.json?cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<UserList>>(){
    		@Override
    		protected PagableResponseList<UserList> doGet() throws TwitterException {
    			return UserListJSONImpl.createPagableUserListList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<UserList>> getAllUserLists(String screenName)
            throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "lists/all.json?screen_name=" + screenName);
    	return new TwitterFutureAdapter<ResponseList<UserList>>(){
    		@Override
    		protected ResponseList<UserList> doGet() throws TwitterException {
    			return UserListJSONImpl.createUserListList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<UserList>> getAllUserLists(long userId)
            throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "lists/all.json?user_id=" + userId);
    	return new TwitterFutureAdapter<ResponseList<UserList>>(){
    		@Override
    		protected ResponseList<UserList> doGet() throws TwitterException {
    			return UserListJSONImpl.createUserListList(res, conf);
    		}
    	};
    }

    /*List Members Methods*/

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getUserListMembers(String listOwnerScreenName, int listId
            , long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                listOwnerScreenName + "/" + listId + "/members.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getUserListMembers(long listOwnerId, int listId
            , long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                listOwnerId + "/" + listId + "/members.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> addUserListMember(int listId, long userId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + getScreenName() +
                "/" + listId + "/members.json?id=" + userId);
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> addUserListMembers(int listId, long[] userIds) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + getScreenName() +
                "/" + listId + "/members/create_all.json?user_id=" + T4JInternalStringUtil.join(userIds));
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> addUserListMembers(int listId, String[] screenNames) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + getScreenName() +
                "/" + listId + "/members/create_all.json?screen_name=" + T4JInternalStringUtil.join(screenNames));
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> deleteUserListMember(int listId, long userId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = delete(conf.getRestBaseURL() + getScreenName() +
                "/" + listId + "/members.json?id=" + userId);
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> checkUserListMembership(String listOwnerScreenName, int listId, long userId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + listOwnerScreenName + "/" + listId
                + "/members/" + userId + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /*List Subscribers Methods*/

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<PagableResponseList<User>> getUserListSubscribers(String listOwnerScreenName
            , int listId, long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() +
                listOwnerScreenName + "/" + listId + "/subscribers.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<PagableResponseList<User>>(){
    		@Override
    		protected PagableResponseList<User> doGet() throws TwitterException {
    			return UserJSONImpl.createPagableUserList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> subscribeUserList(String listOwnerScreenName, int listId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + listOwnerScreenName +
                "/" + listId + "/subscribers.json");
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<UserList> unsubscribeUserList(String listOwnerScreenName, int listId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = delete(conf.getRestBaseURL() + listOwnerScreenName +
                "/" + listId + "/subscribers.json?id=" + getId());
    	return new TwitterFutureAdapter<UserList>(){
    		@Override
    		protected UserList doGet() throws TwitterException {
    			return new UserListJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> checkUserListSubscription(String listOwnerScreenName, int listId, long userId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + listOwnerScreenName + "/" + listId
                + "/subscribers/" + userId + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /*Direct Message Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<DirectMessage>> getDirectMessages() throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL()
                + "direct_messages.json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<ResponseList<DirectMessage>>() {
        	@Override
        	protected ResponseList<DirectMessage> doGet()
        			throws TwitterException {
                return DirectMessageJSONImpl.createDirectMessageList(res, conf);
        	}
		};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<DirectMessage>> getDirectMessages(Paging paging) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL()
                + "direct_messages.json", mergeParameters(paging.asPostParameterArray()
                        , INCLUDE_ENTITIES));
        return new TwitterFutureAdapter<ResponseList<DirectMessage>>() {
        	@Override
        	protected ResponseList<DirectMessage> doGet()
        			throws TwitterException {
        		return DirectMessageJSONImpl.createDirectMessageList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<DirectMessage>> getSentDirectMessages() throws
            TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() +
                "direct_messages/sent.json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<ResponseList<DirectMessage>>() {
        	@Override
        	protected ResponseList<DirectMessage> doGet()
        			throws TwitterException {
        		return DirectMessageJSONImpl.createDirectMessageList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<DirectMessage>> getSentDirectMessages(Paging paging) throws
            TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() +
                "direct_messages/sent.json", mergeParameters(paging.asPostParameterArray()
                        , INCLUDE_ENTITIES));
        return new TwitterFutureAdapter<ResponseList<DirectMessage>>() {
        	@Override
        	protected ResponseList<DirectMessage> doGet()
        			throws TwitterException {
        		return DirectMessageJSONImpl.createDirectMessageList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<DirectMessage> sendDirectMessage(String screenName, String text) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "direct_messages/new.json",
                new HttpParameter[]{new HttpParameter("screen_name", screenName)
                , new HttpParameter("text", text)
                , INCLUDE_ENTITIES});
        return new TwitterFutureAdapter<DirectMessage>() {
        	@Override
        	protected DirectMessage doGet()
        			throws TwitterException {
        		return new DirectMessageJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<DirectMessage> sendDirectMessage(long userId, String text)
            throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "direct_messages/new.json",
                new HttpParameter[]{new HttpParameter("user_id", userId),
            new HttpParameter("text", text)
            , INCLUDE_ENTITIES});
        return new TwitterFutureAdapter<DirectMessage>() {
        	@Override
        	protected DirectMessage doGet()
        			throws TwitterException {
        		return new DirectMessageJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<DirectMessage> destroyDirectMessage(long id) throws
            TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() +
                "direct_messages/destroy/" + id + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<DirectMessage>() {
        	@Override
        	protected DirectMessage doGet()
        			throws TwitterException {
        		return new DirectMessageJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<DirectMessage> showDirectMessage(long id) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL()
                + "direct_messages/show/" + id + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<DirectMessage>() {
        	@Override
        	protected DirectMessage doGet()
        			throws TwitterException {
        		return new DirectMessageJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> createFriendship(String screenName) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/create.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> createFriendship(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/create.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> createFriendship(String screenName, boolean follow) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/create.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName + "&follow=" + follow);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> createFriendship(long userId, boolean follow) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/create.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId + "&follow=" + follow);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> destroyFriendship(String screenName) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/destroy.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> destroyFriendship(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/destroy.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Boolean> existsFriendship(String userA, String userB) throws TwitterException {
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/exists.json",
        	getParameterArray("user_a", userA, "user_b", userB));
        return new TwitterFutureAdapter<Boolean>(){
        	@Override
        	protected Boolean doGet() throws TwitterException {
                return -1 != res.asString().indexOf("true");
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Relationship> showFriendship(String sourceScreenName, String targetScreenName) throws TwitterException {
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/show.json",
                getParameterArray("source_screen_name", sourceScreenName,
                        "target_screen_name", targetScreenName));
        return new TwitterFutureAdapter<Relationship>(){
        	@Override
        	protected Relationship doGet() throws TwitterException {
                return new RelationshipJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Relationship> showFriendship(long sourceId, long targetId) throws TwitterException {
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/show.json",
                new HttpParameter[]{
            new HttpParameter("source_id", sourceId),
            new HttpParameter("target_id", targetId)});
        return new TwitterFutureAdapter<Relationship>(){
        	@Override
        	protected Relationship doGet() throws TwitterException {
                return new RelationshipJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getIncomingFriendships(long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/incoming.json?cursor=" + cursor);
        return new TwitterFutureAdapter<IDs>(){
        	@Override
        	protected IDs doGet() throws TwitterException {
                return new IDsJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getOutgoingFriendships(long cursor) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/outgoing.json?cursor=" + cursor);
        return new TwitterFutureAdapter<IDs>(){
        	@Override
        	protected IDs doGet() throws TwitterException {
                return new IDsJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Friendship>> lookupFriendships(String[] screenNames) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/lookup.json?screen_name=" + T4JInternalStringUtil.join(screenNames));
        return new TwitterFutureAdapter<ResponseList<Friendship>>(){
        	@Override
        	protected ResponseList<Friendship> doGet() throws TwitterException {
                return FriendshipJSONImpl.createFriendshipList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Friendship>> lookupFriendships(long[] ids) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/lookup.json?user_id=" + T4JInternalStringUtil.join(ids));
        return new TwitterFutureAdapter<ResponseList<Friendship>>(){
        	@Override
        	protected ResponseList<Friendship> doGet() throws TwitterException {
                return FriendshipJSONImpl.createFriendshipList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Relationship> updateFriendship(String screenName, boolean enableDeviceNotification
            , boolean retweets) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/update.json",
                new HttpParameter[]{
			            new HttpParameter("screen_name", screenName),
			            new HttpParameter("device", enableDeviceNotification),
			            new HttpParameter("retweets", enableDeviceNotification)
	        	});
        return new TwitterFutureAdapter<Relationship>(){
        	@Override
        	protected Relationship doGet() throws TwitterException {
                return new RelationshipJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Relationship> updateFriendship(long userId, boolean enableDeviceNotification
            , boolean retweets) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "friendships/update.json",
                new HttpParameter[]{
			            new HttpParameter("user_id", userId),
			            new HttpParameter("device", enableDeviceNotification),
			            new HttpParameter("retweets", enableDeviceNotification)
			    });
        return new TwitterFutureAdapter<Relationship>(){
        	@Override
        	protected Relationship doGet() throws TwitterException {
                return new RelationshipJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getNoRetweetIds() throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "friendships/no_retweet_ids.json");
        return new TwitterFutureAdapter<IDs>(){
        	@Override
        	protected IDs doGet() throws TwitterException {
                return new IDsJSONImpl(res, conf);
        	}
        };
    }

    /* Social Graph Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getFriendsIDs(long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "friends/ids.json?cursor=" + cursor);
    	return new TwitterFutureAdapter<IDs>(){
    		protected IDs doGet() throws TwitterException {
    	    	return new IDsJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getFriendsIDs(long userId, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "friends/ids.json?user_id=" + userId +
                "&cursor=" + cursor);
    	return new TwitterFutureAdapter<IDs>(){
    		protected IDs doGet() throws TwitterException {
    	    	return new IDsJSONImpl(res, conf);
    		};
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getFriendsIDs(String screenName, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "friends/ids.json?screen_name=" + screenName
                + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<IDs>(){
    		protected IDs doGet() throws TwitterException {
    	    	return new IDsJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getFollowersIDs(long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "followers/ids.json?cursor=" + cursor);
    	return new TwitterFutureAdapter<IDs>(){
    		protected IDs doGet() throws TwitterException {
    	    	return new IDsJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getFollowersIDs(long userId, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "followers/ids.json?user_id=" + userId
                + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<IDs>(){
    		protected IDs doGet() throws TwitterException {
    	    	return new IDsJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getFollowersIDs(String screenName, long cursor) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "followers/ids.json?screen_name="
                + screenName + "&cursor=" + cursor);
    	return new TwitterFutureAdapter<IDs>(){
    		protected IDs doGet() throws TwitterException {
    	    	return new IDsJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> verifyCredentials() throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = http.get(conf.getRestBaseURL() + "account/verify_credentials.json?include_entities="
                + conf.isIncludeEntitiesEnabled(), auth);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        	    User user = new UserJSONImpl(res, conf);
	            GAEAsyncTwitterImpl.this.screenName = user.getScreenName();
	            GAEAsyncTwitterImpl.this.id = user.getId();
	            return user;
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<RateLimitStatus> getRateLimitStatus() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "account/rate_limit_status.json");
    	return new TwitterFutureAdapter<RateLimitStatus>(){
    		@Override
    		protected RateLimitStatus doGet() throws TwitterException{
    			return new RateLimitStatusJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> updateProfile(String name, String url
            , String location, String description) throws TwitterException {
        ensureAuthorizationEnabled();
        List<HttpParameter> profile = new ArrayList<HttpParameter>(4);
        addParameterToList(profile, "name", name);
        addParameterToList(profile, "url", url);
        addParameterToList(profile, "location", location);
        addParameterToList(profile, "description", description);
        profile.add(INCLUDE_ENTITIES);
        final HttpResponse res = post(conf.getRestBaseURL() + "account/update_profile.json"
                , profile.toArray(new HttpParameter[profile.size()]));
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<AccountTotals> getAccountTotals() throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "account/totals.json");
        return new TwitterFutureAdapter<AccountTotals>(){
        	@Override
        	protected AccountTotals doGet() throws TwitterException {
                return new AccountTotalsJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<AccountSettings> getAccountSettings() throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "account/settings.json");
        return new TwitterFutureAdapter<AccountSettings>(){
        	@Override
        	protected AccountSettings doGet() throws TwitterException {
        		return new AccountSettingsJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> updateProfileColors(
            String profileBackgroundColor,
            String profileTextColor,
            String profileLinkColor,
            String profileSidebarFillColor,
            String profileSidebarBorderColor)
            throws TwitterException {
        ensureAuthorizationEnabled();
        List<HttpParameter> colors = new ArrayList<HttpParameter>(6);
        addParameterToList(colors, "profile_background_color"
                , profileBackgroundColor);
        addParameterToList(colors, "profile_text_color"
                , profileTextColor);
        addParameterToList(colors, "profile_link_color"
                , profileLinkColor);
        addParameterToList(colors, "profile_sidebar_fill_color"
                , profileSidebarFillColor);
        addParameterToList(colors, "profile_sidebar_border_color"
                , profileSidebarBorderColor);
        colors.add(INCLUDE_ENTITIES);
        final HttpResponse res = post(conf.getRestBaseURL() +
                "account/update_profile_colors.json",
                colors.toArray(new HttpParameter[colors.size()]));
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    private void addParameterToList(List<HttpParameter> colors,
                                    String paramName, String color) {
        if (null != color) {
            colors.add(new HttpParameter(paramName, color));
        }
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> updateProfileImage(File image) throws TwitterException {
        checkFileValidity(image);
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL()
                + "account/update_profile_image.json"
                , new HttpParameter[]{new HttpParameter("image", image)
                        , INCLUDE_ENTITIES});
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> updateProfileImage(InputStream image) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL()
                + "account/update_profile_image.json"
                , new HttpParameter[]{new HttpParameter("image", "image", image)
                        , INCLUDE_ENTITIES});
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> updateProfileBackgroundImage(File image, boolean tile)
            throws TwitterException {
        ensureAuthorizationEnabled();
        checkFileValidity(image);
        final HttpResponse res = post(conf.getRestBaseURL()
                + "account/update_profile_background_image.json",
                new HttpParameter[]{new HttpParameter("image", image)
                        , new HttpParameter("tile", tile)
                        , INCLUDE_ENTITIES});
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> updateProfileBackgroundImage(InputStream image, boolean tile)
            throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL()
                + "account/update_profile_background_image.json",
                new HttpParameter[]{new HttpParameter("image", "image", image)
                        , new HttpParameter("tile", tile)
                        , INCLUDE_ENTITIES});
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * Check the existence, and the type of the specified file.
     *
     * @param image image to be uploaded
     * @throws TwitterException when the specified file is not found (FileNotFoundException will be nested)
     *                          , or when the specified file object is not representing a file(IOException will be nested).
     */
    private void checkFileValidity(File image) throws TwitterException {
        if (!image.exists()) {
            //noinspection ThrowableInstanceNeverThrown
            throw new TwitterException(new FileNotFoundException(image + " is not found."));
        }
        if (!image.isFile()) {
            //noinspection ThrowableInstanceNeverThrown
            throw new TwitterException(new IOException(image + " is not a file."));
        }
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getFavorites() throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "favorites.json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		protected ResponseList<Status> doGet() throws TwitterException {
    	    	return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getFavorites(int page) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + "favorites.json"
                , new HttpParameter[]{new HttpParameter("page", page)
                , INCLUDE_ENTITIES});
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		protected ResponseList<Status> doGet() throws TwitterException {
    	    	return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getFavorites(String id) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "favorites/" + id + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		protected ResponseList<Status> doGet() throws TwitterException {
    	    	return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Status>> getFavorites(String id, int page) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + "favorites/" + id + ".json",
                mergeParameters(getParameterArray("page", page)
                        , INCLUDE_ENTITIES));
    	return new TwitterFutureAdapter<ResponseList<Status>>(){
    		protected ResponseList<Status> doGet() throws TwitterException {
    	    	return StatusJSONImpl.createStatusList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Status> createFavorite(long id) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "favorites/create/" + id + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<Status>(){
    		protected Status doGet() throws TwitterException {
    	    	return new StatusJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Status> destroyFavorite(long id) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "favorites/destroy/" + id + ".json?include_entities="
                + conf.isIncludeEntitiesEnabled());
    	return new TwitterFutureAdapter<Status>(){
    		protected Status doGet() throws TwitterException {
    	    	return new StatusJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> enableNotification(String screenName) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "notifications/follow.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> enableNotification(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "notifications/follow.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> disableNotification(String screenName) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "notifications/leave.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> disableNotification(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "notifications/leave.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /* Block Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> createBlock(String screenName) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "blocks/create.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
                return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> createBlock(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "blocks/create.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> destroyBlock(String screen_name) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "blocks/destroy.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screen_name);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> destroyBlock(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = post(conf.getRestBaseURL() + "blocks/destroy.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
        return new TwitterFutureAdapter<User>(){
        	@Override
        	protected User doGet() throws TwitterException {
        		return new UserJSONImpl(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Boolean> existsBlock(String screenName) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "blocks/exists.json?screen_name=" + screenName);
        return new TwitterFutureAdapter<Boolean>(){
        	@Override
        	protected Boolean doGet() throws TwitterException {
		        try {
		            return -1 == res.asString().indexOf("You are not blocking this user.");
		        } catch (TwitterException te) {
		            if (te.getStatusCode() == 404) {
		                return false;
		            }
		            throw te;
		        }
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Boolean> existsBlock(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "blocks/exists.json?user_id=" + userId);
        return new TwitterFutureAdapter<Boolean>(){
        	@Override
        	protected Boolean doGet() throws TwitterException {
		        try {
		            return -1 == res.asString().indexOf(
		            		"<error>You are not blocking this user.</error>");
		        } catch (TwitterException te) {
		            if (te.getStatusCode() == 404) {
		                return false;
		            }
		            throw te;
		        }
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> getBlockingUsers() throws
            TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() +
                "blocks/blocking.json?include_entities="
                + conf.isIncludeEntitiesEnabled());
        return new TwitterFutureAdapter<ResponseList<User>>(){
        	@Override
        	protected ResponseList<User> doGet() throws TwitterException {
        		return UserJSONImpl.createUserList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<User>> getBlockingUsers(int page) throws
            TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() +
                "blocks/blocking.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&page=" + page);
        return new TwitterFutureAdapter<ResponseList<User>>(){
        	@Override
        	protected ResponseList<User> doGet() throws TwitterException {
        		return UserJSONImpl.createUserList(res, conf);
        	}
        };
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<IDs> getBlockingUsersIDs() throws TwitterException {
        ensureAuthorizationEnabled();
        final HttpResponse res = get(conf.getRestBaseURL() + "blocks/blocking/ids.json");
        return new TwitterFutureAdapter<IDs>(){
        	@Override
        	protected IDs doGet() throws TwitterException {
        		return new IDsJSONImpl(res, conf);
        	}
        };
    }

    /* Spam Reporting Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> reportSpam(long userId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "report_spam.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&user_id=" + userId);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<User> reportSpam(String screenName) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "report_spam.json?include_entities="
                + conf.isIncludeEntitiesEnabled() + "&screen_name=" + screenName);
    	return new TwitterFutureAdapter<User>(){
    		@Override
    		protected User doGet() throws TwitterException {
    			return new UserJSONImpl(res, conf);
    		}
    	};
    }

    /* Saved Searches Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<List<SavedSearch>> getSavedSearches() throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + "saved_searches.json");
    	return new TwitterFutureAdapter<List<SavedSearch>>(){
    		@Override
    		protected List<SavedSearch> doGet() throws TwitterException {
    			return SavedSearchJSONImpl.createSavedSearchList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<SavedSearch> showSavedSearch(int id) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + "saved_searches/show/" + id
                + ".json");
    	return new TwitterFutureAdapter<SavedSearch>(){
    		@Override
    		protected SavedSearch doGet() throws TwitterException {
    			return new SavedSearchJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<SavedSearch> createSavedSearch(String query) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL() + "saved_searches/create.json"
                , new HttpParameter[]{new HttpParameter("query", query)});
    	return new TwitterFutureAdapter<SavedSearch>(){
    		@Override
    		protected SavedSearch doGet() throws TwitterException {
    			return new SavedSearchJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<SavedSearch> destroySavedSearch(int id) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = post(conf.getRestBaseURL()
                + "saved_searches/destroy/" + id + ".json");
    	return new TwitterFutureAdapter<SavedSearch>(){
    		@Override
    		protected SavedSearch doGet() throws TwitterException {
    			return new SavedSearchJSONImpl(res, conf);
    		}
    	};
    }
    /* Local Trends Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Location>> getAvailableTrends() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "trends/available.json");
    	return new TwitterFutureAdapter<ResponseList<Location>>(){
    		@Override
    		protected ResponseList<Location> doGet() throws TwitterException {
    			return LocationJSONImpl.createLocationList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Location>> getAvailableTrends(GeoLocation location) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "trends/available.json",
                new HttpParameter[]{new HttpParameter("lat", location.getLatitude())
                        , new HttpParameter("long", location.getLongitude())
                });
    	return new TwitterFutureAdapter<ResponseList<Location>>(){
    		@Override
    		protected ResponseList<Location> doGet() throws TwitterException {
    			return LocationJSONImpl.createLocationList(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Trends> getLocationTrends(int woeid) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "trends/" + woeid + ".json");
    	return new TwitterFutureAdapter<Trends>(){
    		@Override
    		protected Trends doGet() throws TwitterException {
    			return new TrendsJSONImpl(res, conf);
    		}
    	};
    }

    /* Geo Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Place>> searchPlaces(GeoQuery query) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "geo/search.json", query.asHttpParameterArray());
    	return new TwitterFutureAdapter<ResponseList<Place>>(){
    		@Override
    		protected ResponseList<Place> doGet() throws TwitterException {
    			try {
		            return PlaceJSONImpl.createPlaceList(res, conf);
		        } catch (TwitterException te) {
		            if (te.getStatusCode() == 404) {
		                return new ResponseListImpl<Place>(0, null);
		            } else {
		                throw te;
		            }
		        }
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<SimilarPlaces> getSimilarPlaces(GeoLocation location, String name, String containedWithin, String streetAddress) throws TwitterException {
        List<HttpParameter> params = new ArrayList<HttpParameter>(3);
        params.add(new HttpParameter("lat", location.getLatitude()));
        params.add(new HttpParameter("long", location.getLongitude()));
        params.add(new HttpParameter("name", name));
        if (null != containedWithin) {
            params.add(new HttpParameter("contained_within", containedWithin));
        }
        if (null != streetAddress) {
            params.add(new HttpParameter("attribute:street_address", streetAddress));
        }
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "geo/similar_places.json", params.toArray(new HttpParameter[params.size()]));
    	return new TwitterFutureAdapter<SimilarPlaces>(){
    		@Override
    		protected SimilarPlaces doGet() throws TwitterException {
    			return SimilarPlacesImpl.createSimilarPlaces(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<ResponseList<Place>> reverseGeoCode(GeoQuery query) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL()
                + "geo/reverse_geocode.json", query.asHttpParameterArray());
    	return new TwitterFutureAdapter<ResponseList<Place>>(){
    		@Override
    		protected ResponseList<Place> doGet() throws TwitterException {
    			try {
		            return PlaceJSONImpl.createPlaceList(res, conf);
		        } catch (TwitterException te) {
		            if (te.getStatusCode() == 404) {
		                return new ResponseListImpl<Place>(0, null);
		            } else {
		                throw te;
		            }
		        }
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Place> getGeoDetails(String id) throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "geo/id/" + id
                + ".json");
    	return new TwitterFutureAdapter<Place>(){
    		@Override
    		protected Place doGet() throws TwitterException {
    			return new PlaceJSONImpl(res, conf);
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Place> createPlace(String name, String containedWithin, String token, GeoLocation location, String streetAddress) throws TwitterException {
        ensureAuthorizationEnabled();
        List<HttpParameter> params = new ArrayList<HttpParameter>(3);
        params.add(new HttpParameter("name", name));
        params.add(new HttpParameter("contained_within", containedWithin));
        params.add(new HttpParameter("token", token));
        params.add(new HttpParameter("lat", location.getLatitude()));
        params.add(new HttpParameter("long", location.getLongitude()));
        if (null != streetAddress) {
            params.add(new HttpParameter("attribute:street_address", streetAddress));
        }
    	final HttpResponse res = post(conf.getRestBaseURL() + "geo/place.json"
                , params.toArray(new HttpParameter[params.size()]));
    	return new TwitterFutureAdapter<Place>(){
    		@Override
    		protected Place doGet() throws TwitterException {
    			return new PlaceJSONImpl(res, conf);
    		}
    	};
    }

    /* Legal Resources */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<String> getTermsOfService() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "legal/tos.json");
    	return new TwitterFutureAdapter<String>(){
    		@Override
    		protected String doGet() throws TwitterException {
    			try {
		            return res.asJSONObject().getString("tos");
		        } catch (JSONException e) {
		            throw new TwitterException(e);
		        }
    		}
    	};
    }

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<String> getPrivacyPolicy() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "legal/privacy.json");
    	return new TwitterFutureAdapter<String>(){
    		@Override
    		protected String doGet() throws TwitterException {
    			try {
		            return res.asJSONObject().getString("privacy");
		        } catch (JSONException e) {
		            throw new TwitterException(e);
		        }
    		}
    	};
    }

    /* #newtwitter Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<RelatedResults> getRelatedResults(long statusId) throws TwitterException {
        ensureAuthorizationEnabled();
    	final HttpResponse res = get(conf.getRestBaseURL() + "related_results/show/"
                + Long.toString(statusId) + ".json");
    	return new TwitterFutureAdapter<RelatedResults>(){
    		@Override
    		protected RelatedResults doGet() throws TwitterException {
    			return new RelatedResultsJSONImpl(res, conf);
    		}
    	};
    }

    /* Help Methods */

    /**
     * {@inheritDoc}
     */
    public TwitterFuture<Boolean> test() throws TwitterException {
    	final HttpResponse res = get(conf.getRestBaseURL() + "help/test.json");
    	return new TwitterFutureAdapter<Boolean>(){
    		@Override
    		protected Boolean doGet() throws TwitterException {
    			return -1 != res.asString().indexOf("ok");
    		}
    	};
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        if (!super.equals(o)) return false;

        GAEAsyncTwitterImpl twitter = (GAEAsyncTwitterImpl) o;

        if (!INCLUDE_ENTITIES.equals(twitter.INCLUDE_ENTITIES)) return false;
        if (!INCLUDE_RTS.equals(twitter.INCLUDE_RTS)) return false;

        return true;
    }

    private HttpResponse get(String url) throws TwitterException {
        if (!conf.isMBeanEnabled()) {
            return http.get(url, auth);
        } else {
            // intercept HTTP call for monitoring purposes
            HttpResponse response = null;
            long start = System.currentTimeMillis();
            try {
                response = http.get(url, auth);
            } finally {
                long elapsedTime = System.currentTimeMillis() - start;
                TwitterAPIMonitor.getInstance().methodCalled(url, elapsedTime, isOk(response));
            }
            return response;
        }
    }

    private HttpResponse get(String url, HttpParameter[] parameters) throws TwitterException {
        if (!conf.isMBeanEnabled()) {
            return http.get(url, parameters, auth);
        } else {
            // intercept HTTP call for monitoring purposes
            HttpResponse response = null;
            long start = System.currentTimeMillis();
            try {
                response = http.get(url, parameters, auth);
            } finally {
                long elapsedTime = System.currentTimeMillis() - start;
                TwitterAPIMonitor.getInstance().methodCalled(url, elapsedTime, isOk(response));
            }
            return response;
        }
    }

    private HttpResponse post(String url) throws TwitterException {
        if (!conf.isMBeanEnabled()) {
            return http.post(url, auth);
        } else {
            // intercept HTTP call for monitoring purposes
            HttpResponse response = null;
            long start = System.currentTimeMillis();
            try {
                response = http.post(url, auth);
            } finally {
                long elapsedTime = System.currentTimeMillis() - start;
                TwitterAPIMonitor.getInstance().methodCalled(url, elapsedTime, isOk(response));
            }
            return response;
        }
    }

    private HttpResponse post(String url, HttpParameter[] parameters) throws TwitterException {
        if (!conf.isMBeanEnabled()) {
            return http.post(url, parameters, auth);
        } else {
            // intercept HTTP call for monitoring purposes
            HttpResponse response = null;
            long start = System.currentTimeMillis();
            try {
                response = http.post(url, parameters, auth);
            } finally {
                long elapsedTime = System.currentTimeMillis() - start;
                TwitterAPIMonitor.getInstance().methodCalled(url, elapsedTime, isOk(response));
            }
            return response;
        }
    }

    private HttpResponse delete(String url) throws TwitterException {
        if (!conf.isMBeanEnabled()) {
            return http.delete(url, auth);
        } else {
            // intercept HTTP call for monitoring purposes
            HttpResponse response = null;
            long start = System.currentTimeMillis();
            try {
                response = http.delete(url, auth);
            } finally {
                long elapsedTime = System.currentTimeMillis() - start;
                TwitterAPIMonitor.getInstance().methodCalled(url, elapsedTime, isOk(response));
            }
            return response;
        }
    }

    private boolean isOk(HttpResponse response) {
        return response != null && response.getStatusCode() < 300;
    }

    @Override
    public int hashCode() {
        int result = super.hashCode();
        result = 31 * result + INCLUDE_ENTITIES.hashCode();
        result = 31 * result + INCLUDE_RTS.hashCode();
        return result;
    }

    @Override
    public String toString() {
        return "TwitterImpl{" +
                "INCLUDE_ENTITIES=" + INCLUDE_ENTITIES +
                ", INCLUDE_RTS=" + INCLUDE_RTS +
                '}';
    }
}

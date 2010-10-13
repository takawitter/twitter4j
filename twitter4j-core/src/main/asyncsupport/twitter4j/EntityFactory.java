package twitter4j;

import twitter4j.internal.http.HttpResponse;

public abstract class EntityFactory {
	public static EntityFactory getInstance(){
		return instance;
	}
/*
	public abstract Category createCategory(HttpResponse response);
	public abstract DirectMessage createDirectMessage(HttpResponse response);
	public abstract IDs createIDs(HttpResponse response);
	public abstract Location createLocation(HttpResponse response);
	public abstract Place createPlace(HttpResponse response);
	public abstract QueryResult createQueryResult(HttpResponse response);
	public abstract RateLimitStatus createRateLimitStatus(HttpResponse response);
	public abstract Relationship createRelationship(HttpResponse response);
	public abstract SavedSearch createSavedSearch(HttpResponse response);
*/	public abstract Status createStatus(HttpResponse response) throws TwitterException;
/*	public abstract Trend createTrend(HttpResponse response);
	public abstract Trends createTrends(HttpResponse response);
	public abstract Tweet createTweet(HttpResponse response);
	public abstract User createUser(HttpResponse response);
	public abstract UserList createUserList(HttpResponse response);
*/
	private static class JSONImplEntityFactory extends EntityFactory{
		@Override
		public Status createStatus(HttpResponse response) throws TwitterException {
			return new StatusJSONImpl(response);
		}
	}
	private static EntityFactory instance;
	static {
		String factoryClass = System.getProperty("twitter4j.entityFactory");
		if(factoryClass != null){
			try {
				instance = (EntityFactory)Class.forName(factoryClass).newInstance();
			} catch(ClassNotFoundException e){
			} catch(InstantiationException e){
			} catch(IllegalAccessException e){
			} catch(ClassCastException e){
			}
		}
		if(instance == null){
			instance = new JSONImplEntityFactory();
		}
	}
}

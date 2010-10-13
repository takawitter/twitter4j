package twitter4j;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.logging.Logger;

import twitter4j.internal.http.HttpResponse;

public class LazyEntityFactory extends EntityFactory{
	@Override
	public Status createStatus(HttpResponse response) throws TwitterException {
		return createLazyModel(response, Status.class, StatusJSONImpl.class);
	}

	@SuppressWarnings("unchecked")
	private <T> T createLazyModel(HttpResponse response, Class<T> modelClass, Class<? extends T> implClass)
	throws TwitterException{
		try {
			return (T)Proxy.newProxyInstance(
					LazyEntityFactory.class.getClassLoader()
					, new Class<?>[]{modelClass}
					, new LazyModelHandler<T>(response, implClass)
					);
		} catch (IllegalArgumentException e) {
			throw new TwitterException(e);
		} catch (SecurityException e) {
			throw new TwitterException(e);
		} catch (NoSuchMethodException e) {
			throw new TwitterException(e);
		}
	}

	private static class LazyModelHandler<T> implements InvocationHandler{
		public LazyModelHandler(HttpResponse response, Class<? extends T> implClass)
		throws SecurityException, NoSuchMethodException{
			this.response = response;
			this.implCtor = implClass.getDeclaredConstructor(HttpResponse.class);
			this.implCtor.setAccessible(true);
		}

		@Override
		public Object invoke(Object proxy, Method method, Object[] args)
		throws Throwable {
			logger.warning(method.getName() + " called.");
			if(model == null){
				logger.warning("initialize instance...");
				model = (T)implCtor.newInstance(response);
			}
			return method.invoke(model, args);
		}
		private HttpResponse response;
		private T model;
		private Constructor<? extends T> implCtor;
		private static Logger logger = Logger.getLogger(LazyModelHandler.class.getName());
	}
}

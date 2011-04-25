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

package twitter4j.api;

import twitter4j.ResponseList;
import twitter4j.Status;
import twitter4j.TwitterException;
import twitter4j.gaeasync.TwitterFuture;

/**
 * @author Joern Huxhorn - jhuxhorn at googlemail.com
 */
public interface FavoriteMethodsGAEAsync {
    /**
     * Returns the 20 most recent favorite statuses for the authenticating user or user specified by the ID parameter in the requested format.
     * <br>This method calls http://api.twitter.com/1/favorites.json
     *
     * @return List<Status>
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/favorites">GET favorites | dev.twitter.com</a>
     * @since Twitter4J 2.0.1
     */
    TwitterFuture<ResponseList<Status>> getFavorites()
            throws TwitterException;

    /**
     * Returns the 20 most recent favorite statuses for the authenticating user or user specified by the ID parameter in the requested format.
     * <br>This method calls http://api.twitter.com/1/favorites.json
     *
     * @param page the number of page
     * @return ResponseList<Status>
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/favorites">GET favorites | dev.twitter.com</a>
     * @since Twitter4J 2.0.1
     */
    TwitterFuture<ResponseList<Status>> getFavorites(int page)
            throws TwitterException;

    /**
     * Returns the 20 most recent favorite statuses for the authenticating user or user specified by the ID parameter in the requested format.
     *
     * @param id the ID or screen name of the user for whom to request a list of favorite statuses
     * @return ResponseList<Status>
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/favorites">GET favorites | dev.twitter.com</a>
     * @since Twitter4J 2.0.1
     */
    TwitterFuture<ResponseList<Status>> getFavorites(String id)
            throws TwitterException;

    /**
     * Returns the 20 most recent favorite statuses for the authenticating user or user specified by the ID parameter in the requested format.
     * <br>This method calls http://api.twitter.com/1/favorites/[id].json
     *
     * @param id   the ID or screen name of the user for whom to request a list of favorite statuses
     * @param page the number of page
     * @return ResponseList<Status>
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/get/favorites">GET favorites | dev.twitter.com</a>
     * @since Twitter4J 2.0.1
     */
    TwitterFuture<ResponseList<Status>> getFavorites(String id, int page)
            throws TwitterException;

    /**
     * Favorites the status specified in the ID parameter as the authenticating user.  Returns the favorite status when successful.
     * <br>This method calls http://api.twitter.com/1/favorites/create/[id].json
     *
     * @param id the ID of the status to favorite
     * @return Status
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/favorites/create/:id">POST favorites/create/:id | dev.twitter.com</a>
     */
    TwitterFuture<Status> createFavorite(long id)
            throws TwitterException;

    /**
     * Un-favorites the status specified in the ID parameter as the authenticating user.  Returns the un-favorited status in the requested format when successful.
     * <br>This method calls http://api.twitter.com/1/favorites/destroy/[id].json
     *
     * @param id the ID of the status to un-favorite
     * @return Status
     * @throws TwitterException when Twitter service or network is unavailable
     * @see <a href="http://dev.twitter.com/doc/post/favorites/destroy/:id">POST favorites/destroy/:id | dev.twitter.com</a>
     */
    TwitterFuture<Status> destroyFavorite(long id)
            throws TwitterException;
}

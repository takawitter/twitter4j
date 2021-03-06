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

import java.util.List;


/**
 * List of TwitterResponse.
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 */
public interface ResponseList<T> extends TwitterResponse, List<T> {

    /**
     * {@inheritDoc}
     */
    public RateLimitStatus getRateLimitStatus();

    /**
     * Returns the current feature-specific rate limit status if available.<br>
     * This method is available in conjunction with Twitter#searchUsers()<br>
     *
     * @return current rate limit status
     * @see <a href="http://dev.twitter.com/pages/rate-limiting">Rate Limiting | dev.twitter.com</a>
     * @see <a href="http://dev.twitter.com/pages/rate_limiting_faq">Rate Limiting FAQ | dev.twitter.com</a>
     * @since Twitter4J 2.1.2
     */
    public RateLimitStatus getFeatureSpecificRateLimitStatus();

}

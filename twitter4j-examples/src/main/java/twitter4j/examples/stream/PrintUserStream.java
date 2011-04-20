/*
<<<<<<< HEAD
Copyright (c) 2007-2010, Yusuke Yamamoto
All rights reserved.

Redistribution and use in source and binary forms, with or without
modification, are permitted provided that the following conditions are met:
 * Redistributions of source code must retain the above copyright
      notice, this list of conditions and the following disclaimer.
 * Redistributions in binary form must reproduce the above copyright
      notice, this list of conditions and the following disclaimer in the
      documentation and/or other materials provided with the distribution.
 * Neither the name of the Yusuke Yamamoto nor the
      names of its contributors may be used to endorse or promote products
      derived from this software without specific prior written permission.

THIS SOFTWARE IS PROVIDED BY Yusuke Yamamoto ``AS IS'' AND ANY
EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
DISCLAIMED. IN NO EVENT SHALL Yusuke Yamamoto BE LIABLE FOR ANY
DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
(INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
(INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package twitter4j.examples.stream;

import twitter4j.*;

import java.util.HashSet;
import java.util.Set;


=======
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

package twitter4j.examples.stream;

import twitter4j.DirectMessage;
import twitter4j.Status;
import twitter4j.StatusDeletionNotice;
import twitter4j.TwitterException;
import twitter4j.TwitterStream;
import twitter4j.TwitterStreamFactory;
import twitter4j.User;
import twitter4j.UserList;
import twitter4j.UserStreamListener;
>>>>>>> e94561b24ae0ceb99f9e34e52703c85a6849ea21

/**
 * <p>
 * This is a code example of Twitter4J Streaming API - user stream.<br>
 * Usage: java twitter4j.examples.PrintUserStream. Needs a valid twitter4j.properties file with Basic Auth _and_ OAuth properties set<br>
 * </p>
 *
 * @author Yusuke Yamamoto - yusuke at mac.com
 * @author Rémy Rakic - remy dot rakic at gmail.com
 */
<<<<<<< HEAD
public final class PrintUserStream implements StatusListener {
    public static void main(String[] args) throws TwitterException {
        PrintUserStream printSampleStream = new PrintUserStream();
        printSampleStream.startConsuming();
    }

    private TwitterStream twitterStream;

    // used for getting user info
    private Twitter twitter;
    private int currentUserId;

    public PrintUserStream() {
        twitterStream = new TwitterStreamFactory().getInstance();
        twitter = new TwitterFactory().getInstance();

        try {
            User currentUser = twitter.verifyCredentials();
            currentUserId = currentUser.getId();
        } catch (TwitterException e) {
            System.out.println("Unexpected exception caught while trying to retrieve the current user: " + e);
            e.printStackTrace();
        }
    }

    private void startConsuming() throws TwitterException {
        // the user() method internally creates a thread which manipulates
        // TwitterStream and calls these adequate listener methods continuously.
        twitterStream.setStatusListener(this);
        twitterStream.user();
    }

    private Set<Integer> friends;

    public void onFriendList(int[] friendIds) {
        System.out.println("Received friends list - Following " + friendIds.length + " people");

        friends = new HashSet<Integer>(friendIds.length);
        for (int id : friendIds)
            friends.add(id);
    }

    public void onStatus(Status status) {
//        int replyTo = status.getInReplyToUserId();
//        if (replyTo > 0 && !friends.contains(replyTo) && currentUserId != replyTo){
//            System.out.print("[Out of band] "); // I've temporarily labeled "out of band" messages that are sent to people you don't follow
//        }
//
        User user = status.getUser();

        System.out.println(user.getName() + " [" + user.getScreenName() + "] : " + status.getText());
    }

    public void onDirectMessage(DirectMessage dm) {
        System.out.println("DM from " + dm.getSenderScreenName() + " to " + dm.getRecipientScreenName() + ": " + dm.getText());
    }

    public void onDeletionNotice(StatusDeletionNotice notice) {
        User user = friend(notice.getUserId());
        if (user == null)
            return;
        System.out.println(user.getName() + " [" + user.getScreenName() + "] deleted the tweet "
                + notice.getStatusId());
    }

    private User friend(int userId) {
        try {
            return twitter.showUser(userId);
        } catch (TwitterException e) {
            System.out.println("Unexpected exception caught while trying to show user " + userId + ": " + e);
            e.printStackTrace();
        }

        return null;
    }

    public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
        System.out.println("track limitation: " + numberOfLimitedStatuses);
    }

    public void onException(Exception ex) {
        ex.printStackTrace();
    }

    public void onFavorite(User source, User target, Status favoritedStatus) {
        System.out.println(source.getName() + " [" + source.getScreenName() + "] favorited "
                + target.getName() + "'s [" + target.getScreenName() + "] tweet: " + favoritedStatus.getText());
    }

    public void onUnfavorite(User source, User target, Status unfavoritedStatus) {
        System.out.println(source.getName() + " [" + source.getScreenName() + "] unfavorited "
                + target.getName() + "'s [" + target.getScreenName() + "] tweet: " + unfavoritedStatus.getText());
    }

    public void onFollow(User source, User target) {
        System.out.println(source.getName() + " [" + source.getScreenName() + "] started following "
                + target.getName() + " [" + target.getScreenName() + "]");
    }

    public void onUnfollow(User source, User target) {
        System.out.println(source.getName() + " [" + source.getScreenName() + "] unfollowed "
                + target.getName() + " [" + target.getScreenName() + "]");

        if (source.getId() == currentUserId)
            friends.remove(target);
    }

    public void onUserSubscribedToList(User subscriber, User listOwner, UserList list) {
        System.out.println(subscriber.getName() + " [" + subscriber.getScreenName() + "] subscribed to "
                + listOwner.getName() + "'s [" + listOwner.getScreenName() + "] list: " + list.getName()
                + " [" + list.getFullName() + "]");
    }

    public void onUserCreatedList(User listOwner, UserList list) {
        System.out.println(listOwner.getName() + " [" + listOwner.getScreenName() + "] created list: " + list.getName()
                + " [" + list.getFullName() + "]");
    }

    public void onUserUpdatedList(User listOwner, UserList list) {
        System.out.println(listOwner.getName() + " [" + listOwner.getScreenName() + "] updated list: " + list.getName()
                + " [" + list.getFullName() + "]");
    }

    public void onUserDestroyedList(User listOwner, UserList list) {
        System.out.println(listOwner.getName() + " [" + listOwner.getScreenName() + "] destroyed list: " + list.getName()
                + " [" + list.getFullName() + "]");
    }

    public void onRetweet(User source, User target, Status retweetedStatus) {
    }

    public void onBlock(User source, User target) {
        System.out.println(source.getName() + " [" + source.getScreenName() + "] blocked "
                + target.getName() + " [" + target.getScreenName() + "]");
    }

    public void onUnblock(User source, User target) {
        System.out.println(source.getName() + " [" + source.getScreenName() + "] unblocked "
                + target.getName() + " [" + target.getScreenName() + "]");
    }
=======
public final class PrintUserStream {
    public static void main(String[] args) throws TwitterException {
        TwitterStream twitterStream = new TwitterStreamFactory().getInstance();
        twitterStream.addListener(listener);
        // user() method internally creates a thread which manipulates TwitterStream and calls these adequate listener methods continuously.
        twitterStream.user();
    }

    static UserStreamListener listener = new UserStreamListener() {
        public void onStatus(Status status) {
            System.out.println("onStatus @" + status.getUser().getScreenName() + " - " + status.getText());
        }

        public void onDeletionNotice(StatusDeletionNotice statusDeletionNotice) {
            System.out.println("Got a status deletion notice id:" + statusDeletionNotice.getStatusId());
        }

        public void onDeletionNotice(long directMessageId, long userId) {
            System.out.println("Got a direct message deletion notice id:" + directMessageId);
        }

        public void onTrackLimitationNotice(int numberOfLimitedStatuses) {
            System.out.println("Got a track limitation notice:" + numberOfLimitedStatuses);
        }

        public void onScrubGeo(long userId, long upToStatusId) {
            System.out.println("Got scrub_geo event userId:" + userId + " upToStatusId:" + upToStatusId);
        }

        public void onFriendList(long[] friendIds) {
            System.out.print("onFriendList");
            for (long friendId : friendIds) {
                System.out.print(" " + friendId);
            }
            System.out.println();
        }

        public void onFavorite(User source, User target, Status favoritedStatus) {
            System.out.println("onFavorite source:@"
                    + source.getScreenName() + " target:@"
                    + target.getScreenName() + " @"
                    + favoritedStatus.getUser().getScreenName() + " - "
                    + favoritedStatus.getText());
        }

        public void onUnfavorite(User source, User target, Status unfavoritedStatus) {
            System.out.println("onUnFavorite source:@"
                    + source.getScreenName() + " target:@"
                    + target.getScreenName() + " @"
                    + unfavoritedStatus.getUser().getScreenName()
                    + " - " + unfavoritedStatus.getText());
        }

        public void onFollow(User source, User followedUser) {
            System.out.println("onFollow source:@"
                    + source.getScreenName() + " target:@"
                    + followedUser.getScreenName());
        }

        public void onRetweet(User source, User target, Status retweetedStatus) {
            System.out.println("onRetweet @"
                    + retweetedStatus.getUser().getScreenName() + " - "
                    + retweetedStatus.getText());
        }

        public void onDirectMessage(DirectMessage directMessage) {
            System.out.println("onDirectMessage text:"
                    + directMessage.getText());
        }

        public void onUserListMemberAddition(User addedMember, User listOwner, UserList list) {
            System.out.println("onUserListMemberAddition added member:@"
                    + addedMember.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        public void onUserListMemberDeletion(User deletedMember, User listOwner, UserList list) {
            System.out.println("onUserListMemberDeleted deleted member:@"
                    + deletedMember.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        public void onUserListSubscription(User subscriber, User listOwner, UserList list) {
            System.out.println("onUserListSubscribed subscriber:@"
                    + subscriber.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        public void onUserListUnsubscription(User subscriber, User listOwner, UserList list) {
            System.out.println("onUserListUnsubscribed subscriber:@"
                    + subscriber.getScreenName()
                    + " listOwner:@" + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        public void onUserListCreation(User listOwner, UserList list) {
            System.out.println("onUserListCreated  listOwner:@"
                    + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        public void onUserListUpdate(User listOwner, UserList list) {
            System.out.println("onUserListUpdated  listOwner:@"
                    + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        public void onUserListDeletion(User listOwner, UserList list) {
            System.out.println("onUserListDestroyed  listOwner:@"
                    + listOwner.getScreenName()
                    + " list:" + list.getName());
        }

        public void onUserProfileUpdate(User updatedUser) {
            System.out.println("onUserProfileUpdated user:@" + updatedUser.getScreenName());
        }

        public void onBlock(User source, User blockedUser) {
            System.out.println("onBlock source:@" + source.getScreenName()
                    + " target:@" + blockedUser.getScreenName());
        }

        public void onUnblock(User source, User unblockedUser) {
            System.out.println("onUnblock source:@" + source.getScreenName()
                    + " target:@" + unblockedUser.getScreenName());
        }

        public void onException(Exception ex) {
            ex.printStackTrace();
            System.out.println("onException:" + ex.getMessage());
        }
    };
>>>>>>> e94561b24ae0ceb99f9e34e52703c85a6849ea21
}
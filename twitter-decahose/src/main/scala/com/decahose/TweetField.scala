/**
 * (C) Copyright IBM Corp. 2015, 2016
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 */
package com.decahose

/* Mapping tweet fields */
object TweetField
{
	// Defines if the Twitter data is using archive format
	val fromArchive = LoadConf.sparkConf.getBoolean("decahose.loadFromArchive")

	val jsonPrefix = if (fromArchive) null else "tweets"
	// unique id of the tweet as string format
	val tweet_id = if (fromArchive) "id" else "message.id"
	// tweet text as string format
	val tweet_text = if (fromArchive) "body" else "message.body"
	// tweet created date as string format
	val tweet_created_at = if (fromArchive) "postedTime" else "message.postedTime"
	// user description as string format
	val user_description = if (fromArchive) "actor.summary" else "message.actor.summary"
	// user profile image URL as string format
	val user_profileImgURL = if (fromArchive) "actor.image" else "message.actor.image"
	// user followers count as long format
	val user_followers_count = if (fromArchive) "actor.followersCount" else "message.actor.followersCount"
	// user name as string format
	val user_name = if (fromArchive) "actor.displayName" else "message.actor.displayName"
	// user handle as string format
	val user_handle = if (fromArchive) "actor.preferredUsername" else "message.actor.preferredUsername"
	// user id as long format
	val user_id = if (fromArchive) "actor.id" else "message.actor.id"
	// user language as string format
	val language = if (fromArchive) "actor.languages" else "message.actor.languages"
	// defines the type of the tweet obj
	val verb = if (fromArchive) "verb" else "message.verb"
}

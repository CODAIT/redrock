/**
 * (C) Copyright IBM Corp. 2015, 2015
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
	// unique id of the tweet as string format
	val tweet_id = "id_str"
	// tweet text as string format
	val tweet_text = "text"
	// tweet created date as string format
	val tweet_created_at = "created_at"
	// user description as string format
	val user_description = "user.description"
	//user profile image URL as string format
	val user_profileImgURL = "user.profile_image_url"
	//user followers count as long format
	val user_followers_count = "user.followers_count"
	// user name as string format
	val user_name = "user.name"
	// user handle as string format
	val user_handle =  "user.screen_name"
	//user id as long format
	val user_id = "user.id"
	//user language as string format
	val language = "lang"
}
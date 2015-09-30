package com.redRock

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
/**
 * $RCSfile$
 * $Revision$
 * $Date$
 *
 * Copyright (C) 2003 Jive Software. All rights reserved.
 *
 * This software is the proprietary information of Jive Software. Use is subject to license terms.
 */

package org.jivesoftware.webchat;

/**
 * A Filter that converts ASCII emoticons into image equivalents.
 * This filter should only be run after any HTML stripping filters.<p>
 *
 * The filter must be configured with information about where the image files
 * are located. A table containing all the supported emoticons with their
 * ASCII representations and image file names is as follows:<p>
 *
 * <table border=1>
 *     <tr><td><b>Emotion</b></td><td><b>ASCII</b></td><td><b>Image</b></td></tr>
 *
 *     <tr><td>Happy</td><td>:) or :-)</td><td>happy.gif</td></tr>
 *     <tr><td>Sad</td><td>:( or :-(</td><td>sad.gif</td></tr>
 *     <tr><td>Grin</td><td>:D</td><td>grin.gif</td></tr>
 *     <tr><td>Love</td><td>:x</td><td>love.gif</td></tr>
 *     <tr><td>Mischief</td><td>;\</td><td>mischief.gif</td></tr>
 *     <tr><td>Cool</td><td>B-)</td><td>cool.gif</td></tr>
 *     <tr><td>Devil</td><td>]:)</td><td>devil.gif</td></tr>
 *     <tr><td>Silly</td><td>:p</td><td>silly.gif</td></tr>
 *     <tr><td>Angry</td><td>X-(</td><td>angry.gif</td></tr>
 *     <tr><td>Laugh</td><td>:^O</td><td>laugh.gif</td></tr>
 *     <tr><td>Wink</td><td>;) or ;-)</td><td>wink.gif</td></tr>
 *     <tr><td>Blush</td><td>:8}</td><td>blush.gif</td></tr>
 *     <tr><td>Cry</td><td>:_|</td><td>cry.gif</td></tr>
 *     <tr><td>Confused</td><td>?:|</td><td>confused.gif</td></tr>
 *     <tr><td>Shocked</td><td>:O</td><td>shocked.gif</td></tr>
 *     <tr><td>Plain</td><td>:|</td><td>plain.gif</td></tr>
 *  </table>
 *
 * Note: special thanks to August Harrison for implementing an earlier version of this filter.
 */
public class EmoticonFilter {

    private static String imageHappy    = "happy.gif";
    private static String imageSad      = "sad.gif";
    private static String imageGrin     = "grin.gif";
    private static String imageLove     = "love.gif";
    private static String imageMischief = "mischief.gif";
    private static String imageCool     = "cool.gif";
    private static String imageDevil    = "devil.gif";
    private static String imageSilly    = "silly.gif";
    private static String imageAngry    = "angry.gif";
    private static String imageLaugh    = "laugh.gif";
    private static String imageWink     = "wink.gif";
    private static String imageBlush    = "blush.gif";
    private static String imageCry      = "cry.gif";
    private static String imageConfused = "confused.gif";
    private static String imageShocked  = "shocked.gif";
    private static String imagePlain    = "plain.gif";
    private static String imageURL      = "images/emoticons";

    // Placeholders for the built image tags
    private static String imgHappy;
    private static String imgSad;
    private static String imgGrin;
    private static String imgLove;
    private static String imgMischief;
    private static String imgCool;
    private static String imgDevil;
    private static String imgSilly;
    private static String imgAngry;
    private static String imgLaugh;
    private static String imgWink;
    private static String imgBlush;
    private static String imgCry;
    private static String imgConfused;
    private static String imgShocked;
    private static String imgPlain;

    public EmoticonFilter() {
        buildImageTags();
    }

    public String applyFilter(String string) {
        if (string == null || string.length() < 1) {
            return string;
        }

        int length = string.length();
        StringBuffer filtered = new StringBuffer(string.length() + 100);
        char[] chars = string.toCharArray();

        int	length1 = length - 1;
        int	length2 = length - 2;

        int index = -1, i = 0, oldend = 0;
		String imgTag;

        // Replace each of the emoticons, expanded search for performance
        while (++index < length1) {
			// no tag found yet...
			imgTag = null;

			switch (chars[i = index]) {
                case ']':
                    // "]:)"
                    if (i < length2 && chars[++i] == ':' && chars[++i] == ')') {
                        imgTag = imgDevil;
                    }
                    break;
                case ':':
                    switch (chars[++i]) {
                        case ')':
                            // ":)"
                            imgTag = imgHappy;
                            break;
                        case '-':
                            // ":-)"
                            if (i < length1 && chars[++i] == ')') {
                                imgTag = imgHappy;
                            }
                            // ":-("
                            else if (chars[i] == '(') {
                                imgTag = imgSad;
                            }
                            break;
                        case '(':
                            // ":("
                            imgTag = imgSad;
                            break;
                        case 'D':
                            // ":D"
                            imgTag = imgGrin;
                            break;
                        case 'x':
                            // ":x"
                            imgTag = imgLove;
                            break;
                        case 'p':
                            // ":p"
                            imgTag = imgSilly;
                            break;
                        case '^':
                            // ":^O"
                            if (i < length1 && chars[++i] == 'O') {
                                imgTag = imgLaugh;
                            }
                            break;
                        case '8':
                            // ":8}"
                            if (i < length1 && chars[++i] == '}') {
                                imgTag = imgBlush;
                            }
                            break;
                        case '_':
                            // ":_|"
                            if (i < length1 && chars[++i] == '|') {
                                imgTag = imgCry;
                            }
                            break;
                        case 'O':
                            // ":O"
                            imgTag = imgShocked;
                            break;
                        case '|':
                            // ":|"
                            imgTag = imgPlain;
                            break;
                        default:
                            break;
                    }
                    break;
                case ';':
                    switch (chars[++i]) {
                        case ')':
                            // ";)"
                            imgTag = imgWink;
                            break;
                        case '-':
                            // ";-)"
                            if (i < length1 && chars[++i] == ')') {
                                imgTag = imgWink;
                            }
                            break;
                        case '\\':
                            // ";\\"
                            imgTag = imgMischief;
                            break;
                        default:
                            break;
                    }
                    break;
                case 'B':
                        // "B-)"
                        if (i < length2 && chars[++i] == '-' && chars[++i] == ')') {
                            imgTag = imgCool;
                        }
                    break;
                case 'X':
                        // "X-("
                        if (i < length2 && chars[++i] == '-' && chars[++i] == '(') {
                            imgTag = imgAngry;
                        }
                    break;
                case '?':
                        // "?:|"
                        if (i < length2 && chars[++i] == ':' && chars[++i] == '|') {
                            imgTag = imgConfused;
                        }
                    break;
                default:
                    break;
            }

			// if we found one, replace
			if (imgTag != null) {
		        filtered.append(chars, oldend, index-oldend);
		        filtered.append(imgTag);

				oldend = i + 1;
		        index = i;
        	}
        }

        if (oldend < length) {
	        filtered.append(chars, oldend, length-oldend);
        }

        return filtered.toString();
    }

    /**
     * Returns the base URL for emoticon images. This can be specified as
     * an absolute or relative path.
     *
     * @return the base URL for smiley images.
     */
    public String getImageURL() {
        return imageURL;
    }

    /**
     * Sets the base URL for emoticon images. This can be specified as
     * an absolute or relative path.
     *
     * @param imageURL the base URL for emoticon images.
     */
    public void setImageURL(String imageURL) {
		if (imageURL != null && imageURL.length() > 0) {
			if (imageURL.charAt(imageURL.length()-1) == '/') {
                imageURL = imageURL.substring(0, imageURL.length()-1);
			}
		}
        this.imageURL = imageURL;

        // rebuild the image tags
        buildImageTags();
    }

    /**
     * Build image tags
     */
    private void buildImageTags() {
        imgHappy    = buildURL(imageHappy);
        imgSad      = buildURL(imageSad);
        imgGrin     = buildURL(imageGrin);
        imgLove     = buildURL(imageLove);
        imgMischief = buildURL(imageMischief);
        imgCool     = buildURL(imageCool);
        imgDevil    = buildURL(imageDevil);
        imgSilly    = buildURL(imageSilly);
        imgAngry    = buildURL(imageAngry);
        imgLaugh    = buildURL(imageLaugh);
        imgWink     = buildURL(imageWink);
        imgBlush    = buildURL(imageBlush);
        imgCry      = buildURL(imageCry);
        imgConfused = buildURL(imageConfused);
        imgShocked  = buildURL(imageShocked);
        imgPlain    = buildURL(imagePlain);
    }

	/**
     * Returns an HTML image tag using the base image URL and image name.
     */
    private String buildURL(String imageName) {
	    String tag = "<img border='0' src='" + imageURL + "/" + imageName + "'>";

		return tag;
    }
}
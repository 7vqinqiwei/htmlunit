package com.gargoylesoftware.htmlunit.consts;

/**
 * @author blank
 */
public interface WindowTarget {

    /** target "_blank". */
    String TARGET_BLANK = "_blank";
    /** target "_parent". */
    String TARGET_SELF = "_self";
    /** target "_parent". */
    String TARGET_PARENT = "_parent";
    /** target "_top". */
    String TARGET_TOP = "_top";

    /** "about:". */
    String ABOUT_SCHEME = "about:";
    /** "about:blank". */
    String ABOUT_BLANK = ABOUT_SCHEME + "blank";

}

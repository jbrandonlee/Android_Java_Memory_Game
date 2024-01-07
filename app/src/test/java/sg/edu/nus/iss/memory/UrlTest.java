package sg.edu.nus.iss.memory;

import junit.framework.TestCase;

public class UrlTest extends TestCase {

    public void testIsValidImgSrc() {
        assertFalse(MainActivity.isValidImgSrc("https://stocksnap.io/"));
        assertFalse(MainActivity.isValidImgSrc("https://stocksnap.io/svg/dots.svg"));
        assertFalse(MainActivity.isValidImgSrc("https://stocksnap.io/img/shutterstock-white.svg?1521262023"));
        assertTrue(MainActivity.isValidImgSrc("https://cdn.stocksnap.io/img-thumbs/280h/hands-holding_RSMLE9JAVA.jpg"));
        assertTrue(MainActivity.isValidImgSrc("https://stocksnap.io/img/inline-feature-bg.jpg?122206092023"));
        assertTrue(MainActivity.isValidImgSrc("https://media.istockphoto.com/id/173880475/photo/asian-market.jpg?s=612x612&w=0&k=20&c=gQuNksrQYoo_GrugBkvwkXU99eZeqBC1hYhEEwgrp24="));
    }
}
package com.maniasin.pingpongleague.service.crawler;

public interface SiteCrawler {
    String getSiteName();
    void scrape(String playerName) throws Exception;
}
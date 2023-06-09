package io.ds.crawler;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Queue;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class SearcherThread extends Thread {

    private String rootUrl;
    //private Pattern email = Pattern.compile("[a-zA-Z\\.]{2,20}@[a-zA-Z\\.-]{2,20}\\.[az]{2}");
    private Pattern link = Pattern.compile("href=\"([a-zA-Z\\./-:0-9]{5,25})\"");
    private Pattern email = Pattern.compile("(?:[a-z0-9!#$%&'*+/=?^_`{|}~-]+(?:\\.[a-z0-9!#$%&'*+/=?^_`{|}~-]+)*|\"(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21\\x23-\\x5b\\x5d-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])*\")@(?:(?:[a-z0-9](?:[a-z0-9-]*[a-z0-9])?\\.)+[a-z0-9](?:[a-z0-9-]*[a-z0-9])?|\\[(?:(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9]))\\.){3}(?:(2(5[0-5]|[0-4][0-9])|1[0-9][0-9]|[1-9]?[0-9])|[a-z0-9-]*[a-z0-9]:(?:[\\x01-\\x08\\x0b\\x0c\\x0e-\\x1f\\x21-\\x5a\\x53-\\x7f]|\\\\[\\x01-\\x09\\x0b\\x0c\\x0e-\\x7f])+)\\])");

    private Set<String> emails = new HashSet<String>();

    private Queue<String> links = new LinkedList<String>();
    private List<String> trackedLinks = new LinkedList<String>();

    public SearcherThread(String rootUrl) {
        this.rootUrl = rootUrl;
    }

    private String requestUrl(URL url) throws ProtocolException, IOException {

        String webSite = "";

        HttpURLConnection connection = (HttpURLConnection) url.openConnection();

        connection.setRequestMethod("GET");

        BufferedReader in = new BufferedReader(new InputStreamReader(connection.getInputStream()));

        String inputLine;

        while ((inputLine = in.readLine()) != null) webSite += inputLine;

        in.close();

        return webSite;
    }

    private void matchWebsite(String webSite, String currentUrl) {

        Matcher matcher;

        matcher = link.matcher(webSite);

        while (matcher.find()) {
            String link = matcher.group(1);

            if (link.startsWith("http") || link.startsWith("www")) {

                if (!link.endsWith("/")) link += "/";

                if (!trackedLinks.contains(link)) {
                    links.add(link);
                    trackedLinks.add(link);
                }

                continue;
            }

            if (link.startsWith("/")) link = link.substring(1);

            link = currentUrl + link;

            link = link.replace(".html", "");
            link = link.replace(".php", "");

            if (!link.endsWith("/")) link += "/";

            if (!trackedLinks.contains(link)) {
                links.add(link);
                trackedLinks.add(link);
            }
        }

        try {
            matcher = email.matcher(webSite);

            while (matcher.find()) {
                emails.add(matcher.group());
            }
        } catch (StackOverflowError e) {
            System.out.println("Stackoverflow error at email regex for page " + currentUrl);
        }
    }

    @Override
    public void run() {

        URL baseUrl;

        try {
            baseUrl = new URL(rootUrl);
        } catch (MalformedURLException e) {
            throw new RuntimeException("The given input URL is malformed.");
        }

        try {
            String webSite = requestUrl(baseUrl);
            matchWebsite(webSite, rootUrl);
        } catch (ProtocolException e) {
            System.out.println(e);
        } catch (IOException e) {
            System.out.println(e);
        }

        while (!links.isEmpty()) {

            if (Thread.interrupted()) {
                System.out.println(emails);
                return;
            }

            String link = links.remove();

            System.out.println("Visiting link: " + link);

            try {
                URL url = new URL(link);

                String webSite = requestUrl(url);

                matchWebsite(webSite, link);
            } catch (MalformedURLException e) {
                System.out.println("Link " + link + " does not work.");
            } catch (ProtocolException e) {
                System.out.println(e);
            } catch (IOException e) {
                System.out.println(e);
            }
        }

        System.out.println(emails);
    }
}

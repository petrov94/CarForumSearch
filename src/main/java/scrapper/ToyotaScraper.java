package scrapper;

import java.io.FileOutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
import java.io.File;
import java.io.IOException;
import java.util.stream.Collectors;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.gargoylesoftware.htmlunit.WebClient;
import com.gargoylesoftware.htmlunit.html.HtmlAnchor;
import com.gargoylesoftware.htmlunit.html.HtmlElement;
import com.gargoylesoftware.htmlunit.html.HtmlPage;
import models.Comment;
import models.Theme;
import models.Topic;

public class ToyotaScraper {

    static List<String> technicalDiscussions = List.of("Общи технически въпроси","Двигатели","Газови уредби","Електрическа система, навигация, музика","Хибридно задвижване - Hybrid Synergy Drive (HSD)","Трансмисия","Спирачна система","Окачване","Интериор/екстериор");
//    static List<String> technicalDiscussions = List.of("Окачване","Интериор/екстериор");
    static String exportFolder = "/resources/CarForumExportData/";
    static ObjectMapper objectMapper = new ObjectMapper();
    static final String baseUrl = "https://www.toyotabg.eu/";
    static public WebClient client;
    static {
        client = new WebClient();
        client.getOptions().setCssEnabled(false);
        client.getOptions().setJavaScriptEnabled(false);
        client.getOptions().setRedirectEnabled(false);
        client.getOptions().setThrowExceptionOnScriptError(false);
        client.getOptions().setThrowExceptionOnFailingStatusCode(false);
        client.getOptions().setTimeout(80000000);
    }

    public static void main(String[] args) {
        List<Topic> mainTopics = getScrappedDocument();
        Map<String,List<String>> subTopics = iterateSubTopics(mainTopics);
        createDiscussion(subTopics);
    }

    public static List<Topic> getScrappedDocument() {
        List<Topic> mainTopics = new ArrayList<>();
        String baseUrlForTechnicalDiscussions = "https://www.toyotabg.eu/viewforum.php?f=2";
        try {
            HtmlPage page = client.getPage(baseUrlForTechnicalDiscussions);
            Thread.sleep(5000);
            List<HtmlElement> itemList = page.getByXPath("//a[@class='jumpbox-sub-link']");
            if (itemList.isEmpty()) {
                System.out.println("No item found");
            } else {
                List<HtmlElement> elements = itemList.stream().filter(item-> {
                    String element = item.getTextContent();
                    return technicalDiscussions.stream().anyMatch(element::contains);
                }).collect(Collectors.toList());
                for (HtmlElement htmlItem : elements) {
                    mainTopics.add(new Topic(((HtmlAnchor) htmlItem).getHrefAttribute(),htmlItem.getLastChild().getTextContent()));
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        return mainTopics;
    }
//dl[contains(@class, 'row-item sticky_read') or contains(@class, 'row-item topic_read')]
    public static Map<String,List<String>> iterateSubTopics(List<Topic> mainTopics) {
        Map<String,List<String>> subTopics = new LinkedHashMap<>();
        try {
            for (Topic topic : mainTopics) {
                HtmlPage page = client.getPage(baseUrl + topic.getHref());
                Thread.sleep(2000);
                System.out.println(page.getTitleText());
                List<HtmlElement> itemList = page.getByXPath("//div[@class='list-inner']/a[@class='topictitle']");
                if (itemList.isEmpty()) {
                    System.out.println("No item found");
                } else {
                    List<String> topics = itemList.stream().map(item -> ((HtmlAnchor) item).getHrefAttribute()).collect(Collectors.toList());
                    subTopics.put(getCategory(topic.getTopic()),topics);
//                        htmlItem.getFirstByXPath("//div[@class='list-inner']/a[@class='topictitle']");
//                        topics.add(new Topic(htmlItem.getElementsByTagName("a").get(0).getAttributes().item(0).getNodeValue(),));
//                        System.out.println(htmlItem.getLastChild().getTextContent());
//                        HtmlAnchor href = (HtmlAnchor) htmlItem.getByXPath("//a[contains(@class, 'topictitle')]").get(0);
//                        href.getHrefAttribute();
//                        mainTopics.add(new Topic(((HtmlAnchor) htmlItem).getHrefAttribute(), htmlItem.getLastChild().getTextContent()))
                    }
                }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return subTopics;
    }

    public static void createDiscussion(Map<String,List<String>> subTopics) {
        for (Map.Entry<String, List<String>> entry : subTopics.entrySet()) {
            List<Theme> allThemesPerCategory = new ArrayList<>();
            System.out.println("creating all topics under "+entry.getKey());
            for (String url : entry.getValue()) {
                HtmlPage page = null;
                try {
                    page = client.getPage(baseUrl + url);
                    Thread.sleep(2000);
                } catch (IOException | InterruptedException e) {
                    e.printStackTrace();
                }
                System.out.println(page.getTitleText());
                List<Comment> allComments = new ArrayList<>();
                createPage(page).ifPresent(allComments::addAll);
                iterateDiscussion(page,allComments);
                allThemesPerCategory.add(new Theme(page.getTitleText(),allComments));
            }
            createAllDiscussionsPerCategory(entry.getKey(),allThemesPerCategory);
        }
    }

    private static void createFileStructure(List<Topic> mainTopics){
        mainTopics.forEach(category ->
        {
            Path currentRelativePath = Paths.get("");
            String s = currentRelativePath.toAbsolutePath().toString();
            new File(s+exportFolder+getCategory(category.getTopic())).mkdirs();
        });
    }

    private static void createAllDiscussionsPerCategory(String category, List<Theme> themes) {
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        final Path path = Paths.get(s + exportFolder + getCategory(category));

        if (!Files.exists(path)) {
            try {
                Files.createDirectories(path);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        for (Theme theme : themes) {
            File ftheme = new File(s + exportFolder + getCategory(category) + "/" + getThemeTopic(theme.getName()) + ".json");
            try {
                ftheme.createNewFile();
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (ftheme.exists()) {
                try {
                    createFile(ftheme.getAbsolutePath(), objectMapper.writeValueAsBytes(theme));
                } catch (JsonProcessingException e) {
                    System.out.println("File cannot be created " + ftheme.getAbsolutePath());
                }
            }
        }
    }

    private static String getThemeTopic(String themeTopic){
        return themeTopic.replaceAll(" - Тойота форум България","").replaceAll("\\s+","").replaceAll("\\/", "");
    }

    private static void createFile(String fullPath, byte[] content) {
        try (final FileOutputStream fos = new FileOutputStream(fullPath)) {
            fos.write(content);
        } catch (final IOException e) {
            System.out.println("Could not generate file {}");
        }
    }

    private static String getCategory(String topic){
        return topic.replaceAll(" - Тойота форум България","").replaceAll("\\s+","").replaceAll("\\/", "");
    }

    private static void iterateDiscussion(HtmlPage currentPage,List<Comment> allcomments) {
        List<HtmlElement> itemList = currentPage.getByXPath("//a[@class='button']");
        List<String> pagination = itemList.stream().map(item -> ((HtmlAnchor) item).getHrefAttribute()).filter(s -> s.contains("viewtopic")).collect(Collectors.toList());
        if(pagination.isEmpty()){
            return;
        }
        String firstPage = pagination.get(0);
        String lastPage = pagination.get(pagination.size()-1);
        int lastPageNumber = Integer.parseInt(lastPage.substring(lastPage.indexOf("start=")+6));
        for (int i = 15; i<=lastPageNumber; i+=15){
                HtmlPage page = null;
                try {
                    System.out.println(baseUrl + firstPage.substring(0,lastPage.indexOf("start=")+6)+i);
                    page = client.getPage(baseUrl + firstPage.substring(0,lastPage.indexOf("start=")+6)+i);
                    Thread.sleep(2000);
                } catch (IOException | InterruptedException e) {
                    System.out.println(e.getMessage());
                }
                createPage(page).ifPresent(allcomments::addAll);
            }
    }

    private static Optional<List<Comment>> createPage(HtmlPage page){
        List<HtmlElement> itemList = page.getByXPath("//div[@class='content']");
        if (itemList.isEmpty()) {
            System.out.println("No item found");
            return Optional.empty();
        }
        return Optional.of(itemList.stream().map(node ->{
            try {
                return new Comment(node.asNormalizedText());
            }catch (Exception e){
                //ignore in case of forwarded request
            }
            return null;
        }).filter(Objects::nonNull).collect(Collectors.toList()));
    }
}


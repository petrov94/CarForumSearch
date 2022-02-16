package search;

import com.fasterxml.jackson.databind.ObjectMapper;
import models.Theme;
import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.TokenStream;
import org.apache.lucene.analysis.bg.BulgarianAnalyzer;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.*;
import org.apache.lucene.search.highlight.*;
import org.apache.lucene.search.similarities.BM25Similarity;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.util.AttributeSource;

import javax.sound.midi.Soundbank;
import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.*;
public class ForumSearch {

    private static final String exportFolder = "/resources/CarForumExportData/";
    private static final ObjectMapper objectMapper = new ObjectMapper();
    private static SearcherManager searcherManager;
    public static final String ANSI_GREEN = "\u001B[32m";
    public static final String ANSI_RED = "\u001B[31m";
    public static final String ANSI_BLUE = "\u001B[34m";
    public static final String ANSI_YELLOW = "\u001B[33m";
    public static final String ANSI_RESET = "\u001B[0m";

    public static void main(String[] args) throws IOException, ParseException {
        indexAllForumPosts();
    }

    private static void indexAllForumPosts() throws IOException, ParseException {
        Path currentRelativePath = Paths.get("");
        String s = currentRelativePath.toAbsolutePath().toString();
        Path indexPath = Paths.get("data", "lucene_index");
        Directory indexDir = FSDirectory.open(indexPath);
        BulgarianAnalyzer analyzer = new BulgarianAnalyzer();
        IndexWriterConfig iwc = new IndexWriterConfig(analyzer);
        iwc.setSimilarity(new BM25Similarity());
        iwc.setOpenMode(IndexWriterConfig.OpenMode.CREATE_OR_APPEND);
        createIndex(s,indexDir,analyzer);

        System.out.println("Enter your query");
        Scanner sc = new Scanner(System.in);
        String userQuery = sc.nextLine();
        IndexSearcher searcher = new IndexSearcher(DirectoryReader.open(indexDir));
        int hitsPerPage = 15;
        Query query = null;
        try {
            query = new QueryParser("content", analyzer).parse(userQuery);
        } catch (ParseException e) {
            System.out.println("Error parsing the query" + e.getMessage());
        }

        // references to the top documents returned by a search
        TopDocs docs = searcher.search(query, hitsPerPage);
        System.out.println(ANSI_BLUE+"Total results: " + docs.totalHits+ANSI_BLUE);
        for (int i = 0; i < docs.scoreDocs.length; i++) {
            System.out.println(ANSI_GREEN+"Result " + (i+1)+ANSI_GREEN);
            Document doc = searcher.doc(docs.scoreDocs[i].doc);
            System.out.println(ANSI_YELLOW+"Topic:" + doc.get("display_topic")+ANSI_YELLOW);
            System.out.println(ANSI_YELLOW+"Post: " + doc.get("content")+ANSI_YELLOW);
//            System.out.println(ANSI_YELLOW+"Category :" + doc.get("mainTopic")+ANSI_YELLOW);
        }
        System.out.println("Choose a document number?");
        int docId = sc.nextInt();
        Document choosen = searcher.doc(docs.scoreDocs[docId-1].doc);
        System.out.println("#####################Show the whole theme######################");
        File display  = new File(choosen.getField("file").stringValue());
        Theme theme = objectMapper.readValue(display,Theme.class);
        System.out.println(ANSI_RED+"Theme's name : "+theme.getName()+ANSI_RED);
        for(int i=0; i<theme.getComments().size()-15; i+=15){
            System.out.println("######PAGE " + (i+15)/15 +"#########");
            theme.getComments().subList(i,i+15).forEach(comment -> {
                    System.out.println(ANSI_RED+"user's comment:   "+ANSI_RED);
                    System.out.println(ANSI_RESET+comment.getComment());
            });
        }

    }

    private static void getTopicsFiles(IndexWriter writer,String mainTopic, File topicDir){
        for(File file : Objects.requireNonNull(topicDir.listFiles())){
            if (file.isFile() && file.getName().contains(".json")){
                System.out.println("Indexing Main Topic"+file.getName());
                try {
                    Theme theme = objectMapper.readValue(file,Theme.class);
                    addDocument(writer,theme,file,mainTopic);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private static void createIndex(String s, Directory indexDir, Analyzer analyzer){
        File forumDirectory = new File(s + exportFolder);
        File[] directories = forumDirectory.listFiles();
        try (IndexWriter writer = new IndexWriter(indexDir, new IndexWriterConfig(analyzer))) {
            for (File dir : Objects.requireNonNull(directories)) {
                if (dir.isDirectory()) {
                    System.out.println("Indexing Main Topic"+dir.getName());
                    getTopicsFiles(writer,dir.getName(), new File(dir.getAbsolutePath()));
                }
            }
            searcherManager =  new SearcherManager(writer,null);
            System.out.println("Indexed documents number: " + getCount());
        } catch (IOException e) {
            System.out.println("Index cannot be created"+ e.getMessage());
        }
    }

    public static void addDocument(IndexWriter writer,Theme theme,File file,String mainTopic) throws IOException {
        if (file.isDirectory()) {
            return;
        }
        for(int i=0; i<theme.getComments().size(); i++) {
            System.out.println("Indexing Theme"+theme.getName());
            Document doc = new Document();
            doc.add(new StringField("id", String.valueOf(i), Field.Store.NO));
            doc.add(new StringField("file", file.getPath(), Field.Store.YES));
            doc.add(new TextField("display_topic", theme.getName().substring(0,theme.getName().length()-23), Field.Store.YES));
            doc.add(new TextField("content", theme.getComments().get(i).getComment(), Field.Store.YES));
            doc.add(new TextField("mainTopic", mainTopic.substring(2), Field.Store.YES));
            writer.addDocument(doc);
        }
    }

    public static int getCount() {
        try {
            IndexSearcher indexSearcher = searcherManager.acquire();
            try {
                return indexSearcher.getIndexReader().numDocs();
            } finally {
                searcherManager.release(indexSearcher);
            }
        } catch (IOException e) {
            System.out.println("Failed to determine lucene document count.");
            return 0;
        }
    }
}

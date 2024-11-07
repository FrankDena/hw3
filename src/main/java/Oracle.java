import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
import org.apache.lucene.queryparser.classic.QueryParser;
import org.apache.lucene.search.IndexSearcher;
import org.apache.lucene.search.Query;
import org.apache.lucene.search.ScoreDoc;
import org.apache.lucene.search.TopDocs;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;

import java.io.IOException;
import java.nio.file.Path;
import java.time.Duration;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;

public class Oracle {

    Directory dir;
    private IndexReader reader;

    Analyzer perFieldAnalyzer;

    Analyzer whiteLowerAnalyzer;
    private String[] fields = {"title", "authors", "abstract", "fullPaper"};
    private Map<String, Float> weights = new HashMap<>();
    private MultiFieldQueryParser parser;

    private Scanner scanner;

    private IndexSearcher searcher;

    public Oracle(Path idxPath) throws IOException {
        dir = FSDirectory.open(idxPath);
        whiteLowerAnalyzer = CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter("lowercase")
                .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                .build();
        Map<String,Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("title",whiteLowerAnalyzer);
        perFieldAnalyzers.put("authors",whiteLowerAnalyzer);
        perFieldAnalyzers.put("abstract",whiteLowerAnalyzer);
        perFieldAnalyzers.put("fullPaper",new StandardAnalyzer());
        perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                perFieldAnalyzers);
        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        weights.put("title", 1.0f);
        weights.put("authors", 0.8f);
        weights.put("abstract", 0.6f);
        weights.put("fullPaper", 0.4f);
        parser = new MultiFieldQueryParser(fields, perFieldAnalyzer, weights);
    }

    public void executeUserQuery() throws ParseException, IOException {
        int numOfResults = 10; //default is 10 results per search
        long queriesMade = 0;
        Duration totalTime = Duration.ZERO;
        System.out.println("Rules of the search engine: \n");
        System.out.println("+word: word is mandatory");
        System.out.println("-word: word is prohibited");
        System.out.println("word: word is optional");
        System.out.println("\"phrase\": phrase query\n");
        while(true) {
            int option;
            scanner = new Scanner(System.in);
            System.out.println("Choose the number of results you want to see at most: ");
            if (scanner.hasNextInt()) {
                int input = scanner.nextInt();
                if (input!=0) {
                    numOfResults = input;
                } else {
                    System.out.println("The integer must be greater than 0!\n");
                    continue;
                }
            } else {
                System.out.println("Insert an integer please!\n");
                continue;
            }
            System.out.println("Choose an option to run a query:\n" +
                    "[0] Run a general query, not on a specific field;\n" +
                    "[1] Run a query on the TITLE of the documents;\n" +
                    "[2] Run a query on the AUTHORS of the documents;\n" +
                    "[3] Run a query on the ABSTRACT of the documents;\n" +
                    "[4] Run a query on FULL PAPERS;\n" +
                    "[5] Exit the search engine.\n");
            System.out.println("Choice: ");
            if (scanner.hasNextInt()) {
                option = scanner.nextInt();
                if(option == 0) {
                    queriesMade = queriesMade + 1;
                    executeGeneralQuery(numOfResults);
                    totalTime = totalTime.plus(executeGeneralQuery(numOfResults));
                }
                if(option == 1) {
                    queriesMade = queriesMade + 1;
                    executeTitleQuery(numOfResults);
                    totalTime = totalTime.plus(executeTitleQuery(numOfResults));
                }
                if(option == 2) {
                    queriesMade = queriesMade + 1;
                    executeAuthorsQuery(numOfResults);
                    totalTime = totalTime.plus(executeAuthorsQuery(numOfResults));
                }
                if(option == 3) {
                    queriesMade = queriesMade + 1;
                    executeAbstractQuery(numOfResults);
                    totalTime = totalTime.plus(executeAbstractQuery(numOfResults));
                }
                if(option == 4) {
                    queriesMade = queriesMade + 1;
                    totalTime = totalTime.plus(executeFullPaperQuery(numOfResults));
                }
                if(option == 5)
                    if(queriesMade != 0) {
                        System.out.println("Total elapsed time since the beginning of the search session: " + totalTime.toSeconds() + " seconds\n" +
                                "Mean research elapsed time: " + totalTime.dividedBy(queriesMade).toMinutes() + " minutes and " + totalTime.dividedBy(queriesMade).toSeconds() + " seconds\n\n");
                        break;
                    } else {
                        System.out.println("You've successfully exited the search system");
                    }
                    break;
            } else {
                System.out.println("Insert an integer please!\n");
            }
        }
    }

    private Duration executeFullPaperQuery(int numOfResults) {
        Duration elapsedTime1 = Duration.ZERO;
        try {

            whiteLowerAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer("whitespace")
                    .addTokenFilter("lowercase")
                    .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                    .build();

            //Initializing the parser (for the query on the single field)
            QueryParser p = new QueryParser("fullPaper", whiteLowerAnalyzer);

            //Read the user query
            scanner = new Scanner(System.in);
            System.out.println("Insert a query on the FULL PAPER: ");
            String userInput = this.scanner.nextLine();
            Query query = p.parse(userInput);

            //Retrieve the result documents
            Instant startQueryTime = Instant.now();
            TopDocs hits = this.searcher.search(query, numOfResults);
            Instant endQueryTime = Instant.now();
            elapsedTime1 = Duration.between(startQueryTime, endQueryTime);
            System.out.println("Found results: " + hits.totalHits.value + "\n" +
                                "Elapsed time for the query: " + elapsedTime1.toSeconds() + " seconds\n\n");
            if(hits.totalHits.value == 0){
                System.out.println("Results not found!\n");
            }

            //Print the results
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document document = this.searcher.doc(scoreDoc.doc);
                System.out.println("Title of the paper in which body the query matched: " + document.get("title") + "\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return elapsedTime1;
    }

    private Duration executeAbstractQuery(int numOfResults) {
        Duration elapsedTime2 = Duration.ZERO;
        try {

            whiteLowerAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer("whitespace")
                    .addTokenFilter("lowercase")
                    .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                    .build();

            //Initializing the parser (for the query on the single field)
            QueryParser p = new QueryParser("abstract", whiteLowerAnalyzer);

            //Read the user query
            scanner = new Scanner(System.in);
            System.out.println("Insert a query on the ABSTRACT: ");
            String userInput = this.scanner.nextLine();
            Query query = p.parse(userInput);

            //Retrieve the result documents
            Instant startQueryTime = Instant.now();
            TopDocs hits = this.searcher.search(query, numOfResults);
            Instant endQueryTime = Instant.now();
            elapsedTime2 = Duration.between(startQueryTime, endQueryTime);
            System.out.println("Found results: " + hits.totalHits.value + "\n" +
                    "Elapsed time for the query: " + elapsedTime2.toSeconds() + " seconds\n\n");
            if(hits.totalHits.value == 0){
                System.out.println("Results not found!\n");
            }


            //Print the results
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document document = this.searcher.doc(scoreDoc.doc);
                System.out.println("Abstract: " + document.get("abstract") + "\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return elapsedTime2;
    }

    private Duration executeAuthorsQuery(int numOfResults) {
        Duration elapsedTime3 = Duration.ZERO;
        try {

            whiteLowerAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer("whitespace")
                    .addTokenFilter("lowercase")
                    .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                    .build();

            //Initializing the parser (for the query on the single field)
            QueryParser p = new QueryParser("authors", whiteLowerAnalyzer);

            //Read the user query
            scanner = new Scanner(System.in);
            System.out.println("Insert a query on the AUTHORS: ");
            String userInput = this.scanner.nextLine();
            Query query = p.parse(userInput);

            //Retrieve the result documents
            Instant startQueryTime = Instant.now();
            TopDocs hits = this.searcher.search(query, numOfResults);
            Instant endQueryTime = Instant.now();
            elapsedTime3 = Duration.between(startQueryTime, endQueryTime);
            System.out.println("Found results: " + hits.totalHits.value + "\n" +
                    "Elapsed time for the query: " + elapsedTime3.toSeconds() + " seconds\n\n");
            if(hits.totalHits.value == 0){
                System.out.println("Results not found!\n");
            }


            //Print the results
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document document = this.searcher.doc(scoreDoc.doc);
                System.out.println("Authors: " + document.get("authors") + "\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return elapsedTime3;
    }

    private Duration executeTitleQuery(int numOfResults) throws IOException{
        Duration elapsedTime4 = Duration.ZERO;
        try {

            whiteLowerAnalyzer = CustomAnalyzer.builder()
                    .withTokenizer("whitespace")
                    .addTokenFilter("lowercase")
                    .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                    .build();

            //Initializing the parser (for the query on the single field)
            QueryParser p = new QueryParser("title", whiteLowerAnalyzer);

            //Read the user query
            scanner = new Scanner(System.in);
            System.out.println("Insert a query on the TITLE: ");
            String userInput = this.scanner.nextLine();
            Query query = p.parse(userInput);

            //Retrieve the result documents
            Instant startQueryTime = Instant.now();
            TopDocs hits = this.searcher.search(query, numOfResults);
            Instant endQueryTime = Instant.now();
            elapsedTime4 = Duration.between(startQueryTime, endQueryTime);
            System.out.println("Found results: " + hits.totalHits.value + "\n" +
                    "Elapsed time for the query: " + elapsedTime4.toSeconds() + " seconds\n\n");
            if(hits.totalHits.value == 0){
                System.out.println("Results not found!\n");
            }


            //Print the results
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document document = this.searcher.doc(scoreDoc.doc);
                System.out.println("Title: " + document.get("title") + "\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
        return elapsedTime4;
    }

    private Duration executeGeneralQuery(int numOfResults) throws ParseException, IOException {
        Duration elapsedTime5 = Duration.ZERO;
        weights.put("title", 1.0f);
        weights.put("authors", 0.8f);
        weights.put("abstract", 0.6f);
        weights.put("fullPaper", 0.4f);
        parser = new MultiFieldQueryParser(fields, perFieldAnalyzer, weights);
        try {
            scanner = new Scanner(System.in);
            System.out.println("Insert a query: ");
            String userInput = scanner.nextLine(); // Read the user query

            Query query = this.parser.parse(userInput);

            Instant startQueryTime = Instant.now();
            TopDocs hits = this.searcher.search(query, numOfResults);
            Instant endQueryTime = Instant.now();
            elapsedTime5 = Duration.between(startQueryTime, endQueryTime);
            System.out.println("Found results: " + hits.totalHits.value + "\n" +
                    "Elapsed time for the query: " + elapsedTime5.toSeconds() + " seconds\n\n");
            if(hits.totalHits.value == 0){
                System.out.println("Results not found!\n");
            }


            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document document = this.searcher.doc(scoreDoc.doc);
                System.out.println("Title: " + document.get("title") + "\n");
                System.out.println("Authors: " + document.get("authors") + "\n");
                System.out.println("Abstract: " + document.get("abstract") + "\n");
                System.out.println("Full paper: " + document.get("fullPaper") + "\n");
            }
            //reader.close();
            //dir.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return elapsedTime5;
    }

    public void closeDirAndReader () throws IOException {
        this.reader.close();
        this.dir.close();
    }

}
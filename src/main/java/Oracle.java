import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
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
import java.util.List;
import java.util.Map;
import java.util.Scanner;

public class Oracle {

    Directory dir;
    private IndexReader reader;

    Analyzer perFieldAnalyzer;

    Analyzer whiteLowerAnalyzer;
    private String[] fields = {"caption", "table", "references", "footnotes"};
    private Map<String, Float> weights = new HashMap<>();
    private MultiFieldQueryParser parser;

    private Scanner scanner;

    private IndexSearcher searcher;

    public Oracle(Path idxPath) throws IOException {
        dir = FSDirectory.open(idxPath);
        Analyzer customAnalyzer = CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter("lowercase")
                .addTokenFilter(WordDelimiterGraphFilterFactory.class, createWordDelimiterGraphOptions())
                .addTokenFilter("stop")
                .build();
        Map<String,Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("caption",customAnalyzer);
        perFieldAnalyzers.put("table",customAnalyzer);
        perFieldAnalyzers.put("references",customAnalyzer);
        perFieldAnalyzers.put("footnotes",customAnalyzer);
        Analyzer perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                perFieldAnalyzers);
        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        weights.put("caption", 1.0f);
        weights.put("table", 0.8f);
        weights.put("references", 0.6f);
        weights.put("footnotes", 0.4f);
        CharArraySet stopWords = new CharArraySet(List.of("a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with"), true);
        parser = new MultiFieldQueryParser(fields, new StandardAnalyzer(stopWords), weights);
    }

    private static Map<String, String> createWordDelimiterGraphOptions() {
        Map<String, String> options = new HashMap<>();
        options.put("generateWordParts", "1");
        options.put("generateNumberParts", "1");
        options.put("catenateWords", "0");
        options.put("catenateNumbers", "0");
        options.put("catenateAll", "0");
        options.put("splitOnCaseChange", "1");
        options.put("preserveOriginal", "0");
        options.put("splitOnNumerics", "0");         // Set to 0 to index "f1" as a full term
        options.put("stemEnglishPossessive", "1");

        return options;
    }

    public void executeUserQuery() throws ParseException, IOException {
        int numOfResults = 5; //default is 10 results per search
        long queriesMade = 0;
        Duration totalTime = Duration.ZERO;
        System.out.println("Rules of the search engine: \n");
        /*System.out.println("+word: word is mandatory");
        System.out.println("-word: word is prohibited");*/
        System.out.println("word: word is optional");
        System.out.println("\"phrase\": phrase query\n");
        while(true) {
            int option;
            scanner = new Scanner(System.in);/*
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
            }*/
            System.out.println("Choose an option to run a query:\n" +
                    "[1] Run a general query;\n" +
                    "[0] Exit the search engine.\n");
            System.out.println("Choice: ");
            if (scanner.hasNextInt()) {
                option = scanner.nextInt();
                if(option == 1) {
                    queriesMade = queriesMade + 1;
                    totalTime = totalTime.plus(executeGeneralQuery(numOfResults));
                }
                if(option == 0) {
                    if (queriesMade != 0) {
                        System.out.println("\nTotal search time : " + totalTime.toMillis()+ " milliseconds\n" +
                                "Number of queries: " + queriesMade + "\n" +
                                "Mean search time: " + totalTime.dividedBy(queriesMade).toMillis() + " milliseconds\n\n");
                        break;
                    } else {
                        System.out.println("You've successfully exited the search engine!");
                    }
                    break;
                }
            } else {
                System.out.println("Insert an integer please!\n");
            }
        }
    }


    private Duration executeGeneralQuery(int numOfResults) throws ParseException, IOException {
        Duration elapsedTime5 = Duration.ZERO;
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
                    "Elapsed time for the query: " + elapsedTime5.toMillis() + " milliseconds\n\n");
            if(hits.totalHits.value == 0){
                System.out.println("Results not found!\n");
            }


            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document document = this.searcher.doc(scoreDoc.doc);
                System.out.println(scoreDoc.score+"\n");
                System.out.println("caption: " + document.get("caption") + "\n");
                System.out.println("table: " + document.get("table") + "\n");
                System.out.println("references: " + document.get("references") + "\n");
                System.out.println("footnotes: " + document.get("footnotes") + "\n");
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
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
    private String[] fields = {"caption", "table", "references", "footnotes"};
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
        perFieldAnalyzers.put("caption",whiteLowerAnalyzer);
        perFieldAnalyzers.put("table",whiteLowerAnalyzer);
        perFieldAnalyzers.put("references",whiteLowerAnalyzer);
        perFieldAnalyzers.put("footnotes",whiteLowerAnalyzer);
        perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                perFieldAnalyzers);
        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        weights.put("caption", 1.0f);
        weights.put("table", 0.8f);
        weights.put("references", 0.6f);
        weights.put("footnotes", 0.4f);
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
                    "[5] Exit the search engine.\n");
            System.out.println("Choice: ");
            if (scanner.hasNextInt()) {
                option = scanner.nextInt();
                if(option == 0) {
                    queriesMade = queriesMade + 1;
                    totalTime = totalTime.plus(executeGeneralQuery(numOfResults));
                }
                if(option == 5) {
                    if (queriesMade != 0) {
                        System.out.println("\nTotal search time : " + totalTime.toMillis()+ " milliseconds\n" +
                                "Number of queries: " + queriesMade + "\n" +
                                "Mean search time: " + totalTime.dividedBy(queriesMade).toMillis() + " milliseconds\n\n");
                        break;
                    } else {
                        System.out.println("You've successfully exited the search system");
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
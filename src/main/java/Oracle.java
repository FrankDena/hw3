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
        while(true) {
            int option;
            scanner = new Scanner(System.in);
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
                if(option == 0)
                    executeGeneralQuery();
                if(option == 1)
                    executeTitleQuery();
                if(option == 2)
                    executeAuthorsQuery();
                if(option == 3)
                    executeAbstractQuery();
                if(option == 4)
                    executeFullPaperQuery();
                if(option == 5)
                    break;
            } else {
                System.out.println("Insert an integer please!\n");
            }
        }
    }

    private void executeFullPaperQuery() {
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
            TopDocs hits = this.searcher.search(query, 10);
            System.out.println("Found results: " + hits.totalHits.value);
            if(hits.totalHits.value == 0){
                System.out.println("Results not found!\n");
            }

            //Print the results
            for (ScoreDoc scoreDoc : hits.scoreDocs) {
                Document document = this.searcher.doc(scoreDoc.doc);
                System.out.println("Full paper: " + document.get("fullPaper") + "\n");
            }
        } catch(Exception e) {
            e.printStackTrace();
        }
    }

    private void executeAbstractQuery() {
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
            TopDocs hits = this.searcher.search(query, 10);
            System.out.println("Found results: " + hits.totalHits.value);
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
    }

    private void executeAuthorsQuery() {
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
            TopDocs hits = this.searcher.search(query, 10);
            System.out.println("Found results: " + hits.totalHits.value);
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
    }

    private void executeTitleQuery() throws IOException{
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
            TopDocs hits = this.searcher.search(query, 10);
            System.out.println("Found results: " + hits.totalHits.value);
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
    }

    private void executeGeneralQuery() throws ParseException, IOException {
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
            TopDocs hits = this.searcher.search(query, 10);

            System.out.println("Found results: " + hits.totalHits.value);
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
            reader.close();
            dir.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
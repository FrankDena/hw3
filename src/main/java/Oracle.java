import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.document.Document;
import org.apache.lucene.index.DirectoryReader;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.queryparser.classic.MultiFieldQueryParser;
import org.apache.lucene.queryparser.classic.ParseException;
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
    private String[] fields = {"title", "authors", "abstract", "fullPaper"};
    private float[] weights = {2.0f, 1.5f, 1.0f, 0.5f};
    private MultiFieldQueryParser parser;

    private IndexSearcher searcher;

    public Oracle(Path idxPath) throws IOException {
        dir = FSDirectory.open(idxPath);
        Analyzer whiteLowerAnalyzer = CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter("lowercase")
                .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                .build();
        Map<String,Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("title",whiteLowerAnalyzer);
        perFieldAnalyzers.put("authors",whiteLowerAnalyzer);
        perFieldAnalyzers.put("abstract",whiteLowerAnalyzer);
        perFieldAnalyzers.put("fullPaper",new StandardAnalyzer());
        Analyzer perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                perFieldAnalyzers);
        reader = DirectoryReader.open(dir);
        searcher = new IndexSearcher(reader);
        parser = new MultiFieldQueryParser(fields, perFieldAnalyzer);
    }

    public void executeUserQuery(this.searcher, this.parser) throws ParseException, IOException {
        Scanner scanner = new Scanner(System.in);
        System.out.println("Insert a query: ");
        String userInput = scanner.nextLine(); // Read the user query

        Query query = parser.parse(userInput);
        TopDocs hits = searcher.search(query, 10);

        System.out.println("Found results: " + hits.totalHits.value);

        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document document = searcher.doc(scoreDoc.doc);
            System.out.println("Title: " + document.get("title"));
            System.out.println("Authors: " + document.get("authors"));
            System.out.println("Abstract: " + document.get("abstract"));
            System.out.println("Full paper: " + document.get("fullPaper"));
        }
        reader.close();
        dir.close();
    }

}

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
import org.apache.lucene.analysis.en.EnglishAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.miscellaneous.WordDelimiterGraphFilterFactory;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;
import org.apache.lucene.index.IndexWriter;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.document.Document;
import org.apache.lucene.document.Field;
import org.apache.lucene.document.TextField;
import org.apache.lucene.index.IndexWriterConfig;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.IOException;
import java.nio.file.Path;
import java.io.File;
import java.util.HashMap;
import java.util.Map;

public class Indexer {
    private IndexWriter writer;
    public Indexer(Path idxPath) throws IOException {
        Directory dir = FSDirectory.open(idxPath);
        Analyzer whiteLowerAnalyzer = CustomAnalyzer.builder()
                .withTokenizer("whitespace")
                .addTokenFilter("lowercase")
                .addTokenFilter(WordDelimiterGraphFilterFactory.class)
                .build();
        Map<String,Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("title",whiteLowerAnalyzer);
        perFieldAnalyzers.put("authors",whiteLowerAnalyzer);
        perFieldAnalyzers.put("abstract",whiteLowerAnalyzer);
        perFieldAnalyzers.put("fullPaper",new EnglishAnalyzer());
        Analyzer perFieldAnalyzer = new PerFieldAnalyzerWrapper(new StandardAnalyzer(),
                perFieldAnalyzers);
        IndexWriterConfig config = new IndexWriterConfig(perFieldAnalyzer);
        writer = new IndexWriter(dir, config);
        SimpleTextCodec codec = new SimpleTextCodec();
        config.setCodec(codec);
        writer.deleteAll();
    }

    public void commitAndClose() throws IOException {
        writer.commit();
        writer.close();
    }
    public void indexHtmlDocs(String docsDir) throws IOException {
        File[] files = new File(docsDir).listFiles();
        if (files == null) {
            System.out.println("Empyt or not found directory: " + docsDir);
            return;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".html")) {
                indexHtmlDoc(file);
            }
        }
    }

    private void indexHtmlDoc(File file) {
        try {
            // Parsing HTML file
            org.jsoup.nodes.Document htmlDoc = Jsoup.parse(file, "UTF-8");

            // Field extraction
            String title = extractText(htmlDoc, "title");
            String authors = extractText(htmlDoc, "span.ltx_personname");
            String abstractText = extractAbstract(htmlDoc, "div.ltx_abstract");
            String fullPaper = extractAllPaper(htmlDoc, "*");

            //String title = "tile";
            //String authors = "authors";
            //String abstractText = "abstract";
            //String fullPaper = "fullPaper";

            // Creating lucene document and adding fields
            Document luceneDoc = new Document();
            luceneDoc.add(new TextField("title", title, Field.Store.YES));
            luceneDoc.add(new TextField("authors", authors, Field.Store.YES));
            luceneDoc.add(new TextField("abstract", abstractText, Field.Store.YES));
            luceneDoc.add(new TextField("fullPaper", fullPaper, Field.Store.YES));

            // Indexing new document
            writer.addDocument(luceneDoc);
            System.out.println("Indexed: " + file.getName());
        } catch (IOException e) {
            System.err.println("Error indexing file " + file.getName() + ": " + e.getMessage());
        }
    }

    private String extractText(org.jsoup.nodes.Document htmlDoc, String selector) {
        Element element = htmlDoc.selectFirst(selector);
        return element != null ? element.ownText() : "";
    }

    private String extractAllPaper(org.jsoup.nodes.Document htmlDoc, String selector) {
        Elements elements = htmlDoc.select(selector);
        String text = "";
        for (Element element : elements) {
            if (element != null) {
                text = text.concat(element.text());
            }
        }
        return text;
    }

    private String extractAbstract(org.jsoup.nodes.Document htmlDoc, String selector) {
        Element element = htmlDoc.selectFirst(selector+" > p");

        return element != null ? element.text() : "";
    }

}

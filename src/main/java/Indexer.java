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

import java.io.IOException;
import java.nio.file.Path;
import java.io.File;

public class Indexer {
    private IndexWriter writer;
    public Indexer(Path idxPath) throws IOException {
        Directory dir = FSDirectory.open(idxPath);
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
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
            /*String authors = extractText(htmlDoc, "meta[name=authors]");
            String abstractText = extractText(htmlDoc, "meta[name=abstract]");
            String body = extractText(htmlDoc, "body");*/

            //String title = "tile";
            String authors = "authors";
            String abstractText = "abstract";
            String body = "body";

            // Creating lucene document and adding fields
            Document luceneDoc = new Document();
            luceneDoc.add(new TextField("title", title, Field.Store.YES));
            luceneDoc.add(new TextField("authors", authors, Field.Store.YES));
            luceneDoc.add(new TextField("abstract", abstractText, Field.Store.YES));
            luceneDoc.add(new TextField("body", body, Field.Store.YES));

            // Indexing new document
            writer.addDocument(luceneDoc);
            System.out.println("Indexed: " + file.getName());
        } catch (IOException e) {
            System.err.println("Error indexing file " + file.getName() + ": " + e.getMessage());
        }
    }

    private String extractText(org.jsoup.nodes.Document htmlDoc, String selector) {
        Element element = htmlDoc.selectFirst(selector);
        return element != null ? element.text() : "";
    }

}

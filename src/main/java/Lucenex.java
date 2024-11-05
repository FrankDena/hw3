import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.WhitespaceAnalyzer;
import org.apache.lucene.analysis.miscellaneous.PerFieldAnalyzerWrapper;
import org.apache.lucene.analysis.standard.StandardAnalyzer;
import org.apache.lucene.codecs.Codec;
import org.apache.lucene.document.*;
import org.apache.lucene.index.*;
import org.apache.lucene.search.*;
import org.apache.lucene.store.Directory;
import org.apache.lucene.store.FSDirectory;
import org.apache.lucene.codecs.simpletext.SimpleTextCodec;

//import class Indexer;

public class Lucenex {
    public static void main(String[] args) throws IOException {
        Path projectDir = Paths.get("").toAbsolutePath();
        Path idxPath = projectDir.resolve("lucene-idx");
        String docsDir = "html";
        //matchAllDocs(idxPath);
        try {
            Indexer indexer = new Indexer(idxPath);
            indexer.retrieveHtmlDocs(docsDir);
            indexer.commitAndClose();
            System.out.println("Completed indexing.\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Oracle oracle = new Oracle(idxPath);
            oracle.executeUserQuery();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private static void matchAllDocs(Path path) throws IOException {
        Query query = new MatchAllDocsQuery();

        try (Directory directory = FSDirectory.open(path)) {
            indexDocs(directory, new SimpleTextCodec());
            try (IndexReader reader = DirectoryReader.open(directory)) {
                IndexSearcher searcher = new IndexSearcher(reader);
                runQuery(searcher, query);
                //reader.close();
            } finally {
                directory.close();
            }

        }
    }

    private static void runQuery(IndexSearcher searcher, Query query) throws IOException {
        StandardAnalyzer analyzer = new StandardAnalyzer();
        TopDocs hits = searcher.search(query, 10);


        // 6. Gestisci i risultati
        System.out.println("Risultati trovati: " + hits.totalHits.value);

        for (ScoreDoc scoreDoc : hits.scoreDocs) {
            Document document = searcher.doc(scoreDoc.doc);
            System.out.println("Documento trovato: " + document.get("contenuto"));
        }


    }

    private static void indexHTMLDocs(Directory directory, Codec codec) throws IOException {
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer());
        IndexWriter writer = new IndexWriter(directory, config);

    }

    private static void indexDocs(Directory directory, Codec codec) throws IOException {
        Analyzer defaultAnalyzer = new StandardAnalyzer();
        CharArraySet stopWords = new CharArraySet(Arrays.asList("in", "dei", "di"), true);
        Map<String, Analyzer> perFieldAnalyzers = new HashMap<>();
        perFieldAnalyzers.put("contenuto", new StandardAnalyzer(stopWords));
        perFieldAnalyzers.put("titolo", new WhitespaceAnalyzer());

        Analyzer analyzer = new PerFieldAnalyzerWrapper(defaultAnalyzer, perFieldAnalyzers);
        IndexWriterConfig config = new IndexWriterConfig(analyzer);
        if (codec != null) {
            config.setCodec(codec);
        }
        IndexWriter writer = new IndexWriter(directory, config);
        writer.deleteAll();

        Document doc1 = new Document();
        doc1.add(new TextField("titolo", "Come diventare un ingegnere dei dati, Data Engineer?", Field.Store.YES));
        doc1.add(new TextField("contenuto", "Sembra che oggigiorno tutti vogliano diventare un Data Scientist  ...", Field.Store.YES));
        doc1.add(new StringField("data", "12 ottobre 2016", Field.Store.YES));

        Document doc2 = new Document();
        doc2.add(new TextField("titolo", "Curriculum Ingegneria dei Dati - Sezione di Informatica e Automazione", Field.Store.YES));
        doc2.add(new TextField("contenuto", "Curriculum. Ingegneria dei Dati. Laurea Magistrale in Ingegneria Informatica ...", Field.Store.YES));

        writer.addDocument(doc1);
        writer.addDocument(doc2);

        writer.commit();
        writer.close();
    }
}

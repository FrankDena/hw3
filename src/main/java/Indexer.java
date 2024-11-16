import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.custom.CustomAnalyzer;
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

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.file.Path;
import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.json.JSONObject;
import org.json.JSONTokener;
import java.util.Iterator;


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
        perFieldAnalyzers.put("caption",whiteLowerAnalyzer);
        perFieldAnalyzers.put("table",whiteLowerAnalyzer);
        perFieldAnalyzers.put("references",whiteLowerAnalyzer);
        perFieldAnalyzers.put("footnotes",whiteLowerAnalyzer);
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
    public void retrieveTables(String docsDir) throws IOException {
        File[] files = new File(docsDir).listFiles();
        if (files == null) {
            System.out.println("Empty or not found directory: " + docsDir);
            return;
        }

        for (File file : files) {
            if (file.isFile() && file.getName().endsWith(".json")) {
                ArrayList<Table> tableList = extractTablesFromJSON(file.getAbsolutePath());
                for (Table table : tableList) {
                    indexJsonTable(table);
                }
            }
        }
    }

    private void indexJsonTable(Table table) {
        try {

            // Creating lucene document and adding fields
            Document luceneDoc = new Document();
            luceneDoc.add(new TextField("caption", table.getCaption(), Field.Store.YES));
            luceneDoc.add(new TextField("table", table.getTable(), Field.Store.YES));
            luceneDoc.add(new TextField("references", table.getReferences(), Field.Store.YES));
            luceneDoc.add(new TextField("footnotes", table.getFootnotes(), Field.Store.YES));

            // Indexing new document
            writer.addDocument(luceneDoc);
        } catch (IOException e) {
            System.err.println("Error indexing table " + e.getMessage());
        }
    }


    /*public static List<String> extractDictionaryNames(String filePath) {
        List<String> tableIDs = new ArrayList<>();

        try (FileInputStream fis = new FileInputStream(filePath)) {
            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject nestedObject = jsonObject.optJSONObject(key);
                if (nestedObject != null && containsRequiredKeys(nestedObject)) {
                    tableIDs.add(key);
                    Iterator<String> jsonFieldsKeys = nestedObject.keys();
                    System.out.println(nestedObject.toString());
                    System.out.println(nestedObject.get("references").toString());
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tableIDs;
    }*/

    public static ArrayList<Table> extractTablesFromJSON(String filePath) {

        ArrayList<Table> tableList = new ArrayList();

        try (FileInputStream fis = new FileInputStream(filePath)) {
            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));
            Iterator<String> keys = jsonObject.keys();

            while (keys.hasNext()) {
                String key = keys.next();
                JSONObject nestedObject = jsonObject.optJSONObject(key);
                if (nestedObject != null && containsRequiredKeys(nestedObject)) {
                    Iterator<String> jsonFieldsKeys = nestedObject.keys();
                    Table tableObj = new Table();
                    tableObj.setReferences(nestedObject.get("references").toString());
                    tableObj.setCaption(nestedObject.get("caption").toString());
                    tableObj.setTable(nestedObject.get("table").toString());
                    tableObj.setFootnotes(nestedObject.get("footnotes").toString());
                    tableList.add(tableObj);
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tableList;
    }

    private static boolean containsRequiredKeys(JSONObject jsonObject) {
        return jsonObject.has("caption") && jsonObject.has("table") &&
                jsonObject.has("footnotes") && jsonObject.has("references");
    }


}

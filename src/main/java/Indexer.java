import org.apache.lucene.analysis.Analyzer;
import org.apache.lucene.analysis.CharArraySet;
import org.apache.lucene.analysis.core.StopFilterFactory;
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
import org.json.JSONException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.io.FileInputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
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
        //List<String> stopWords = List.of("a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with");
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
        CharArraySet stopWords = new CharArraySet(List.of("a", "an", "and", "are", "as", "at", "be", "by", "for", "from", "has", "he", "in", "is", "it", "its", "of", "on", "that", "the", "to", "was", "were", "will", "with"), true);
        IndexWriterConfig config = new IndexWriterConfig(new StandardAnalyzer(stopWords));
        writer = new IndexWriter(dir, config);
        //SimpleTextCodec codec = new SimpleTextCodec();
        //config.setCodec(codec);
        writer.deleteAll();
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
                if (!tableList.isEmpty()) {
                    for (Table table : tableList) {
                        if (table != null) {
                            indexJsonTable(table);
                        }
                    }
                }
            }
        }
    }

    private void indexJsonTable(Table table) {
        try {

            // Creating lucene document (for a Table object) and adding fields
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
            /*fis.mark(0);//mark the fis to reset it after reading the first 3 characters
            String firstThreeChars = new String(fis.readNBytes(3), StandardCharsets.UTF_8);
            System.out.println(firstThreeChars);
            if (!firstThreeChars.startsWith("{")) {
                System.err.println("The JSON file " + new File(filePath).getName() + " does not start with '{'!\n");
                return tableList;
            }
            fis.reset();*/
            JSONObject jsonObject = new JSONObject(new JSONTokener(fis));
            if (jsonObject.isEmpty()) { //controlling if the json file is empty
                System.err.println("The JSON file " + new File(filePath).getName() + " is empty!\n");
                return tableList;
            }
            /*if (!jsonObject.toString().startsWith("{")) {
                System.err.println("The JSON file " + new File(filePath).getName() + " does not start with '{'!\n");
                return null;
            }*/ else {
                Iterator<String> keys = jsonObject.keys();
                while (keys.hasNext()) {
                    String key = keys.next();
                    JSONObject nestedObject = jsonObject.optJSONObject(key);
                    if (nestedObject != null && containsRequiredKeys(nestedObject)) {
                        Iterator<String> jsonFieldsKeys = nestedObject.keys();
                        Table tableObj = new Table();
                        tableObj.setReferences(nestedObject.get("references").toString());
                        tableObj.setCaption(nestedObject.get("caption").toString());
                        String htmlTable = nestedObject.get("table").toString();
                        String textTable = Jsoup.parse(htmlTable).text();
                        tableObj.setTable(textTable);
                        tableObj.setFootnotes(nestedObject.get("footnotes").toString());

                        tableList.add(tableObj);
                    }
                }
            }
        } catch (IOException e) {
            e.printStackTrace();
        } catch (JSONException e) {
            System.err.println(e.getMessage());
        }

        return tableList;
    }

    private static boolean containsRequiredKeys(JSONObject jsonObject) {
        return jsonObject.has("caption") && jsonObject.has("table") &&
                jsonObject.has("footnotes") && jsonObject.has("references");
    }


}

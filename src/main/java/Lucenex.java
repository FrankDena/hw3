import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayList;
import java.util.List;


public class Lucenex {
    public static void main(String[] args) throws IOException {
        Path projectDir = Paths.get("").toAbsolutePath();
        Path idxPath = projectDir.resolve("idx-test");
        String docsDir = "test_tables";
        //List<String> tableIds = Indexer.extractDictionaryNames("all_tables/0905.1755.json");
        //ArrayList tableList = Indexer.extractTablesFromJSON("all_tables/0905.1755.json");
        //System.out.println(tableList);
        //System.out.println(tableIds);
        try {
            Indexer indexer = new Indexer(idxPath);
            Instant startIndexingTime = Instant.now();
            indexer.retrieveTables(docsDir);
            Instant endIndexingTime = Instant.now();
            Duration elapsedTime = Duration.between(startIndexingTime,endIndexingTime);
            indexer.commitAndClose();
            System.out.println("Completed indexing.");
            System.out.println("Indexing took: " + elapsedTime.toSeconds() + " seconds\n\n");
        } catch (IOException e) {
            e.printStackTrace();
        }
        try {
            Oracle oracle = new Oracle(idxPath);
            oracle.executeUserQuery();
            oracle.closeDirAndReader();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


}

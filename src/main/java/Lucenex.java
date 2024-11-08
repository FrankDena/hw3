import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.time.Instant;


public class Lucenex {
    public static void main(String[] args) throws IOException {
        Path projectDir = Paths.get("").toAbsolutePath();
        Path idxPath = projectDir.resolve("lucene-idx");
        String docsDir = "all_htmls";
        try {
            Indexer indexer = new Indexer(idxPath);
            Instant startIndexingTime = Instant.now();
            indexer.retrieveHtmlDocs(docsDir);
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

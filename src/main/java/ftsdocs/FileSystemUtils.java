package ftsdocs;

import java.awt.Desktop;
import java.awt.Desktop.Action;
import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Collection;
import java.util.Collections;
import java.util.stream.Stream;

import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.time.StopWatch;

@Slf4j
public class FileSystemUtils {

    private FileSystemUtils() {}

    public static void openDocument(Path path) throws IOException {
        final Desktop desktop = Desktop.getDesktop();
        if (desktop.isSupported(Action.OPEN)) {
            File file = path.toFile();
            desktop.open(file);
        }
    }

    public static Collection<File> readFileTree(File file) {
        Collection<File> result = Collections.emptyList();

        StopWatch stopWatch = new StopWatch();
        stopWatch.start();

        if (!file.isDirectory()) {
            result = Collections.singletonList(file);
        }
        try (Stream<Path> pathStream = Files.walk(file.toPath())) {
            result = pathStream
                    .map(Path::toFile)
                    .filter(File::isFile)
                    .toList();
        } catch (Exception e) {
            log.error("Error while reading directory tree", e);
        }
        stopWatch.stop();
        log.info("{} finished reading file tree of {} in {}, Found {} files",
                Thread.currentThread().getName(),
                file,
                stopWatch,
                result.size());
        return result;
    }

    public static FileSystem getFileSystem(){
        return FileSystems.getDefault();
    }
}

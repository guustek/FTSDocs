package ftsdocs.service;

import ftsdocs.model.configuration.Configuration;
import io.methvin.watcher.visitor.FileTreeVisitor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

@Slf4j
public class IndexedFilesVisitor implements FileTreeVisitor {

    private final Path indexedPath;
    private final Configuration configuration;

    public IndexedFilesVisitor(Path indexedPath, Configuration configuration) {
        this.indexedPath = indexedPath;
        this.configuration = configuration;
    }

    @Override
    public void recursiveVisitFiles(Path file, Callback onDirectory, Callback onFile)
            throws IOException {
        FileVisitor<Path> visitor = new FileVisitor<>() {
            @Override
            public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs)
                    throws IOException {
                if (indexedPath.toFile().isFile()) {
                    //Is parent of indexed file
                    if (dir.equals(indexedPath.getParent())) {
                        onDirectory.call(dir);
                        return FileVisitResult.CONTINUE;
                    }
                    return FileVisitResult.SKIP_SUBTREE;
                } else if (indexedPath.toFile().isDirectory()) {
                    //Is an indexed directory
                    if (dir.equals(indexedPath)) {
                        onDirectory.call(dir);
                        return FileVisitResult.CONTINUE;
                    }
                    //Is parent or subdirectory of indexed directory
                    if (dir.equals(indexedPath.getParent()) || dir.startsWith(indexedPath)) {
                        onDirectory.call(dir);
                        return FileVisitResult.CONTINUE;
                    }
                }
                return FileVisitResult.SKIP_SUBTREE;
            }

            @Override
            public FileVisitResult visitFile(Path file, BasicFileAttributes attrs)
                    throws IOException {

                //Is indexed file
                if (indexedPath.toFile().isFile() && file.equals(indexedPath)) {
                    onFile.call(file);
                    return FileVisitResult.TERMINATE;
                }
                //Is file in indexed directory subtree
                else if (indexedPath.toFile().isDirectory() && file.startsWith(indexedPath)) {
                    if (configuration.isFileFormatSupported(file.toFile())) {
                        onFile.call(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult visitFileFailed(Path file, IOException exc) {
                return FileVisitResult.CONTINUE;
            }

            @Override
            public FileVisitResult postVisitDirectory(Path dir, IOException exc) {
                return FileVisitResult.CONTINUE;
            }
        };
        log.info("{}, started walking file tree of {}",
                Thread.currentThread().getName(),
                indexedPath);
        Files.walkFileTree(file, visitor);
    }
}

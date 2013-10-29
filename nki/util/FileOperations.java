package nki.util;

import static java.nio.file.StandardCopyOption.*;
import static java.nio.file.FileVisitResult.*;
import java.nio.file.*;
import java.nio.file.attribute.*;
import java.io.IOException;
import java.util.*;

public class FileOperations{
	
	private Path source;
	private Path destination;
	private String pattern;
	private CopyOption[] options;
	private ArrayList<Path> results = new ArrayList<Path>();
	private List<Path> exclusions; 

	public FileOperations(Path source, Path destination, CopyOption[] options){
		this.source = source;
		this.destination = destination;
		this.options = options;
	//	exclusions = new ArrayList<Path>(source.listFiles());
	}

	public FileOperations(Path source, String pattern){
		this.source = source;
		this.pattern = pattern;
	}

	public boolean recursiveCopy() throws IOException{
		if( source == null || 
			destination == null ||
			options == null){
				return false;
			}

		if(Files.isDirectory(source)){

			Files.walkFileTree(source, EnumSet.of(FileVisitOption.FOLLOW_LINKS), Integer.MAX_VALUE,	new SimpleFileVisitor<Path>()
			{
            
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException
				{
					Path targetdir = destination.resolve(source.relativize(dir));

					if(!targetdir.getFileName().equals(targetdir.getParent().getFileName())){		
						
						try {
							Files.copy(dir, targetdir);
						} catch (FileAlreadyExistsException e) {
							if (!Files.isDirectory(targetdir)){
								throw e;
							}
						}
					}
					return CONTINUE;
             	}

				@Override
				public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException
				{
					Files.copy(file, destination.resolve(source.relativize(file)));
					System.out.println("File " + file + "\t Dest res: " + destination.resolve(source.relativize(file)) );
					return CONTINUE;
				}
			});
		}
		return true;
	}

	private void singleCopy(Path source, Path target) throws IOException{
		Files.copy(source, target, options);
	}

	public void findFilesGlobbing() throws IOException{
		if(!pattern.equals("") || source == null){
			try{
				Files.walkFileTree(source, new SimpleFileVisitor<Path>() {
					@Override
					public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
						if (file.toString().endsWith(pattern)){
							results.add(file);
						}
						return FileVisitResult.CONTINUE;
					}

					@Override
					public FileVisitResult visitFileFailed(Path file, IOException e) {
						return FileVisitResult.CONTINUE;
					}
				});
			}catch(IOException IO){
				throw IO;	
			}
		}
	}

	public ArrayList<Path> getResults(){
		return results;
	}

	public int getResultsSize(){
		return results.size();
	}
}

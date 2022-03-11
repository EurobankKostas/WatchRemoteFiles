package fileeventlistener.com.service;

import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.googleapis.auth.oauth2.GoogleCredential;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.gson.GsonFactory;
import com.google.api.services.drive.Drive;
import com.google.api.services.drive.model.FileList;
import com.jcraft.jsch.*;
import fileeventlistener.com.entity.SharedFile;
import fileeventlistener.com.repository.SharedFileRepo;
import org.apache.tomcat.util.http.fileupload.FileUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.kafka.annotation.KafkaListener;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.stereotype.Service;

import java.io.*;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.GeneralSecurityException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
public class FileEventServiceImpl implements FileEventListener {
    private static final String APPLICATION_NAME = "File Event Manager";
    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY = GsonFactory.getDefaultInstance();

    @Autowired
    private SharedFileRepo sharedFileRepo;

    @Autowired
    private KafkaTemplate<String, String> kafkaTemplate;

    private static boolean isDirEmpty(final Path directory) throws IOException {
        try (DirectoryStream<Path> dirStream = Files.newDirectoryStream(directory)) {
            return !dirStream.iterator().hasNext();
        }
    }

    @Override
    public void triggerPolling() throws JSchException, SftpException, IOException, GeneralSecurityException {
        List<File> fileListLocal = new ArrayList<>();
        JSch jsch = new JSch();
        FileUtils.forceMkdir(new File("src/main/resources/newState/"));
        jsch.addIdentity("C:\\Users\\user\\Downloads\\kostas1.pem");
        Session session = jsch.getSession("ec2-user", "52.48.225.198", 22);
        session.setConfig("StrictHostKeyChecking", "no");
        session.connect();
        ChannelSftp channel = (ChannelSftp) session.openChannel("sftp");
        channel.connect();
        Vector<ChannelSftp.LsEntry> entries = channel.ls("/home/ec2-user/fileEvent");
        System.out.println(entries);

        entries.forEach(e -> {
            if (e.getFilename().equals(".") || e.getFilename().equals("..") || e.getAttrs().isDir()) {
                return;
            }
            System.out.println(e.getFilename());
            try {
                channel.get("/home/ec2-user/fileEvent/" + e.getFilename(), "src/main/resources/newState/" + e.getFilename());
                File temp = new File("src/main/resources/newState/" + e.getFilename());
                fileListLocal.add(temp);
            } catch (SftpException sftpException) {
                sftpException.printStackTrace();
            }
        });
        List<String> fileNames = fileListLocal.stream().map(File::getName).collect(Collectors.toList());
        channel.exit();
        session.disconnect();
        File newState = Paths.get("src/main/resources/newState/").toFile();
        File oldState = Paths.get("src/main/resources/previousState/").toFile();
        if (isDirEmpty(Paths.get("src/main/resources/previousState/"))) {
            copyDirectory(newState, oldState);
        }

        try (Stream<Path> path = Files.walk(Paths.get("src/main/resources/previousState/"))) {
            Optional<Path> pathFinalFromLocal = path.filter(e -> fileNames.contains(e.toFile().getName())).findFirst();
            if (pathFinalFromLocal.isEmpty()) {
                System.out.println("Rename case");
                //send message to kafka
                kafkaTemplate.send("fileEvent", "Error Rename , File :  "+fileListLocal.get(0).getName());

            }
            if(pathFinalFromLocal.isPresent()) {
                Optional<File> pathFromServer = fileListLocal.stream().filter(e -> e.getName().equalsIgnoreCase(pathFinalFromLocal.get().toFile().getName())).findFirst();
                byte[] f1 = Files.readAllBytes(pathFinalFromLocal.get());
                byte[] f2 = Files.readAllBytes(pathFromServer.get().toPath());
                if (!Arrays.equals(f1, f2)) {
                    System.out.println("Content error");
                    kafkaTemplate.send("fileEvent", "Content Error , File : "+pathFromServer.get().getName());

                }
            }
        }
        FileUtils.deleteDirectory(oldState);
        FileUtils.forceMkdir(Paths.get("src/main/resources/previousState/").toFile());
        copyDirectory(newState, oldState);
        FileUtils.deleteDirectory(newState);
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Drive service = new Drive.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials())
                .setApplicationName(APPLICATION_NAME)
                .build();

        // Print the names and IDs for up to 10 files.
        FileList result = service.files().list()
                .setPageSize(10)
                .setFields("nextPageToken, files(id, name)")
                .execute();
        List<com.google.api.services.drive.model.File> files = result.getFiles();
        if (files == null || files.isEmpty()) {
            System.out.println("No files found.");
        } else {
            System.out.println("Files:");
            for (com.google.api.services.drive.model.File file : files) {
                System.out.printf("%s (%s)\n", file.getName(), file.getId());
            }
        }
        List<com.google.api.services.drive.model.File> sharedFiled =files.stream().filter(e->fileNames.contains(e.getName())).collect(Collectors.toList());
        if(!sharedFiled.isEmpty()){
            kafkaTemplate.send("fileEvent", "File Shared "+sharedFiled.get(0));
        }
    }
    @KafkaListener(topics = "fileEvent" ,groupId = "fileEvent")
    public void listenFileEvent(String message) {
        System.out.println("Received Message in group : " + message);
        if (message.contains("File Shared")) {
            SharedFile sharedFile = new SharedFile();
            sharedFile.setDescription(message);
            sharedFileRepo.save(sharedFile);
        }
    }


    private void copyDirectory(File source, File target) throws IOException {
        if (!target.exists()) {
            target.mkdir();
        }

        for (String f : source.list()) {
            copy(new File(source, f), new File(target, f));
        }
    }

    public void copy(File sourceLocation, File targetLocation) throws IOException {
        if (sourceLocation.isDirectory()) {
            copyDirectory(sourceLocation, targetLocation);
        } else {
            copyFile(sourceLocation, targetLocation);
        }

    }
    private static Credential getCredentials()  {
        //returns an authorized Credential object.
        return new GoogleCredential().setAccessToken("ya29.A0ARrdaM8ufZXOX3IUGOi0kuA6GGLTYWzTYxz2DrRTH-mPBixRwEf0wok9R5tQvRiVgxWN91jrrkP0wLavcQ7fgzzw-6OesEdH3eN_ZShPbckOlqv_Aa-Vk_bdMTf76wb8Ns4F7WoMksn_BSJdhHg80vVH-ressw");
    }

    private void copyFile(File source, File target) throws IOException {
        try (
                InputStream in = new FileInputStream(source);
                OutputStream out = new FileOutputStream(target)
        ) {
            byte[] buf = new byte[1024];
            int length;
            while ((length = in.read(buf)) > 0) {
                out.write(buf, 0, length);
            }
        }
    }
}

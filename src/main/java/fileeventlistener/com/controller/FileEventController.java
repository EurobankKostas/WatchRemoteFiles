package fileeventlistener.com.controller;

import com.jcraft.jsch.*;
import fileeventlistener.com.service.FileEventListener;
import org.apache.tomcat.util.http.fileupload.IOUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.io.*;
import java.nio.file.*;
import java.security.GeneralSecurityException;
import java.util.Vector;

@RestController
@RequestMapping("/fileEvent/v1")
public class FileEventController {

    @Autowired
    private FileEventListener fileEventListener;

    @PostMapping("/trigger/{param}")
    public ResponseEntity<Void> triggerPolling(@PathVariable String param) throws JSchException, SftpException, IOException, GeneralSecurityException, InterruptedException {
        fileEventListener.triggerPolling( param);
        return new ResponseEntity<>(HttpStatus.OK);
    }


}

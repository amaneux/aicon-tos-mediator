package com.aicon.tos.connect.web.pages;

import com.aicon.tos.shared.config.ConfigSettings;
import com.vaadin.flow.component.html.Anchor;
import com.vaadin.flow.component.html.H1;
import com.vaadin.flow.component.html.H3;
import com.vaadin.flow.component.orderedlayout.VerticalLayout;
import com.vaadin.flow.component.upload.Receiver;
import com.vaadin.flow.component.upload.Upload;
import com.vaadin.flow.router.PageTitle;
import com.vaadin.flow.router.Route;
import com.vaadin.flow.server.StreamResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.OutputStream;

@PageTitle("Down/Upload")
@Route(value = "upload-view", layout = MainLayout.class)
public class ConfigUploadView extends VerticalLayout implements Receiver {

    private static final Logger LOG = LoggerFactory.getLogger(ConfigUploadView.class);

    private static final String EXT_XML = ".xml";
    private static final String EXT_WAR = ".war";
    private static final String DIR_WEBAPPS = "../webapps/";

    private String fullFilePath;
    private File file = null;

    public ConfigUploadView() {
        ConfigSettings config = ConfigSettings.getInstance();
        fullFilePath = config.getFullFilename();

        // Create the Upload component
        // Note: this only works when running under tomcat 10, tomcat 11 is not supported by this Vaadin version)
        Upload upload = new Upload(this);

        upload.setAcceptedFileTypes(EXT_XML);
//        upload.setAcceptedFileTypes(EXT_SQL, EXT_XML, EXT_WAR);   this doesn't work when running in docker

        // Handle successful upload
        upload.addSucceededListener(event -> {
            String fileName;
            if (file != null) {
                fileName = file.getAbsolutePath();
            } else {
                fileName = event.getFileName();
            }
            if (ConfigSettings.getInstance().getConfigFile().getName().equals(event.getFileName())) {
                ConfigSettings.getInstance().read();
                LOG.info(ViewHelper.notifyInfo(String.format("Upload saved to: %s and config re-read.", fileName)));
            } else {
                LOG.info(ViewHelper.notifyInfo(String.format("Upload of non-config file saved to: %s", fileName)));
            }
        });

        // Handle failed upload
        upload.addFailedListener(event -> {
            LOG.error(ViewHelper.notifyError("Upload failed: " + event.getReason().getMessage()));
        });

        upload.addFileRejectedListener(event -> {
            LOG.error(ViewHelper.notifyError("Upload rejected: " + event.getErrorMessage()));
        });

        // Create the download component; a StreamResource
        StreamResource resource = new StreamResource(config.getConfigFile().getName(), () -> {
            try {
                return new FileInputStream(fullFilePath);
            } catch (FileNotFoundException e) {
                LOG.error(ViewHelper.notifyError("Can't find file " + fullFilePath));
                return null;
            }
        });

        // Create an Anchor component to serve the download
        Anchor download = new Anchor(resource, fullFilePath);
        download.getElement().setAttribute("download", true);

        H3 title = new H3("Downloads available:");
        H3 comment = new H3("Upload");
        H1 space = new H1("");

        add(title, download, space, comment, upload);//, uploadFileHandler);
    }

    @Override
    public OutputStream receiveUpload(String filename, String mimeType) {
        try {
            if (ConfigSettings.getInstance().getConfigFile().getName().equals(filename)) {
                file = new File(ConfigSettings.getInstance().getFullFilename());
            } else if (filename.endsWith(EXT_WAR)) {        // this does not work in docker, because war is packed within jar.
                file = new File(DIR_WEBAPPS + filename);
            } else {
                throw new FileNotFoundException("Upload for this file not supported");
            }
            LOG.info("Uploading file: " + file.getAbsolutePath());

            return new FileOutputStream(file);
        } catch (Exception e) {
            LOG.error(ViewHelper.notifyError(String.format("Upload of %s failed, reason: %s", filename, e.getMessage())));
            return null;
        }
    }
}

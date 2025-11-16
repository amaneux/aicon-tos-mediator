package com.aicon.tos.connect.web;

import com.avlino.common.utils.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Properties;

import static com.aicon.tos.connect.web.AppComposer.APP_VAR_ENVIRONMENT;
import static com.aicon.tos.connect.web.AppComposer.APP_VAR_TERMINAL;
import static com.avlino.common.Constants.DASH;
import static com.avlino.common.Constants.UNKNOWN;

/**
 * Reads version, build and commit information for the application.
 */
public class AppVersion {
    public static final String VERSION_COMMIT_ID        = "version.commit";
    public static final String VERSION_COMMIT_ID_FULL   = "version.fullCommit";
    public static final String VERSION_COMMIT_AUTHOR    = "version.author";
    public static final String VERSION_COMMIT_TIME      = "version.timeAgo";
    public static final String VERSION_COMMIT_TIMESTAMP = "version.commitTimestamp";
    public static final String VERSION_BUILD_TIME       = "version.date";
    public static final String VERSION_BUILD_NUMBER     = "version.build";
    public static final String VERSION_BUILD_FOR_HOST   = "version.buildForHost";

    private static final Logger LOG = LoggerFactory.getLogger(AppVersion.class);

    private static final String VERSION_FILE = "version.properties";
    private static final String GIT_REPO_URL = "https://github.com/amaneux/Aicon-Tos-Mediator";
    private static final String JENKINS_URL = "http://avlino-jenkins.avlino.az:8081/job/Aicon-Tos-Mediator-Builder/";

    private static Properties props = null;

    /**
     * Provides the properties as read from the version file and fills in blank properties with unknown value.
     *
     * The file will only be read once the first time it gets called, next calls will only return the loaded list.
     *
     * @return the properties, keys can be found here like VERSION_COMMIT.
     */
    static public Properties getVersionInfo() {
        if (props != null) {
            return props;
        }

        Properties defaultProps = new Properties();
        defaultProps.setProperty(VERSION_COMMIT_ID          , UNKNOWN);
        defaultProps.setProperty(VERSION_COMMIT_ID_FULL     , UNKNOWN);
        defaultProps.setProperty(VERSION_COMMIT_AUTHOR      , UNKNOWN);
        defaultProps.setProperty(VERSION_COMMIT_TIME        , UNKNOWN);
        defaultProps.setProperty(VERSION_COMMIT_TIMESTAMP   , UNKNOWN);
        defaultProps.setProperty(VERSION_BUILD_TIME         , UNKNOWN);
        defaultProps.setProperty(VERSION_BUILD_NUMBER       , UNKNOWN);
        defaultProps.setProperty(VERSION_BUILD_FOR_HOST     , UNKNOWN);

        props = new Properties(defaultProps);
        try {
            ClassLoader cl = AppVersion.class.getClassLoader();
            props.load(cl.getResourceAsStream(VERSION_FILE));
            LOG.info("Loaded {}:\n {}", VERSION_FILE, props);
        } catch (Exception e) {
            LOG.warn("Could not load version information from {}, reason: {}", VERSION_FILE, e.getMessage());
        }
        // Set default values when missing

        String build4host = props.getProperty(VERSION_BUILD_FOR_HOST);
        LOG.info("Host {} read from version.properties", build4host);
        if (StringUtils.hasContent(build4host) && !StringUtils.hasContent(System.getProperty(APP_VAR_TERMINAL))) {
            String[] parts = build4host.split(DASH);
            if (parts.length >= 2) {
                System.setProperty(APP_VAR_TERMINAL   , parts[0].toUpperCase());
                System.setProperty(APP_VAR_ENVIRONMENT, parts[1].toUpperCase());
                LOG.info("Host read from version.properties, {}={}, {}={}",
                        APP_VAR_TERMINAL   , System.getProperty(APP_VAR_TERMINAL),
                        APP_VAR_ENVIRONMENT, System.getProperty(APP_VAR_ENVIRONMENT));
            }
        }
        return props;
    }

    static public String getCommitUrl() {
        String commitIdFull = getVersionInfo().getProperty(VERSION_COMMIT_ID_FULL);
        if (UNKNOWN.equals(commitIdFull)) {
            return GIT_REPO_URL;
        }
        return GIT_REPO_URL + "/commit/" + commitIdFull;
    }

    static public String getJenkinsBuildUrl() {
        String buildNumber = getVersionInfo().getProperty(VERSION_BUILD_NUMBER);
        if (UNKNOWN.equals(buildNumber) || "dev-build".equals(buildNumber)) {
            return JENKINS_URL;
        }
        return JENKINS_URL + "/" + buildNumber + "/console";
    }
}
CREATE TABLE COMMITS (
                       ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                       AUTHOR VARCHAR(63),
                       TIMESTAMP DATETIME,
                       PARENT_ID LONG
);

CREATE TABLE BRANCHES (
                        NAME VARCHAR(63) NOT NULL PRIMARY KEY,
                        COMMIT_ID LONG NOT NULL REFERENCES COMMITS(ID)
);

CREATE TABLE ENVIRONMENTS (
                            COMMIT_ID BIGINT NOT NULL REFERENCES COMMITS(ID),
                            NAME VARCHAR(63) NOT NULL,
                            DISABLED BOOL NOT NULL,
                            ABSTRACTED BOOL,
                            DESCRIPTION VARCHAR(255),
                            PARENTS VARCHAR(255),
                            LABELS VARCHAR(255),

                            PRIMARY KEY (COMMIT_ID, NAME)
);

CREATE TABLE HOST_GROUPS (
                           COMMIT_ID BIGINT NOT NULL REFERENCES COMMITS(ID),
                           ENV VARCHAR(63) NOT NULL,
                           NAME VARCHAR(63) NOT NULL,
                           DISABLED BOOL NOT NULL,
                           OVERRIDE BOOL,
                           DESCRIPTION VARCHAR(255),
                           HOSTS VARCHAR(1024),
                           INHERITS VARCHAR(255),
                           INHERITS_RETIRED VARCHAR(255),

                           PRIMARY KEY (COMMIT_ID, ENV, NAME)
);

CREATE TABLE HOSTS (
                     COMMIT_ID BIGINT NOT NULL REFERENCES COMMITS(ID),
                     ENV VARCHAR(63) NOT NULL,
                     NAME VARCHAR(63) NOT NULL,
                     DISABLED BOOL NOT NULL,
                     USER VARCHAR(63),
                     PORT SMALLINT,
                     CHANNEL VARCHAR(15),
                     RETIRED BOOL,
                     LABELS VARCHAR(1024),
                     DESCRIPTION VARCHAR(255),

                     PRIMARY KEY (COMMIT_ID, ENV, NAME)
);

CREATE TABLE PROJECTS (
                        COMMIT_ID BIGINT NOT NULL REFERENCES COMMITS(ID),
                        NAME VARCHAR(63) NOT NULL,
                        DISABLED BOOL NOT NULL,
                        ABSTRACTED BOOL,
                        NAME_GENERATOR VARCHAR(255),
                        VARIABLE_GROUP_GENERATOR VARCHAR(255),
                        PROJECT_KEY VARCHAR(63),
                        ACTIVE_IN_ENV VARCHAR(63),
                        DESCRIPTION VARCHAR(255),
                        PARENTS VARCHAR(255),
                        PLAYBOOK_NAME VARCHAR(63),
                        PROJECT_WHEN VARCHAR(255),
                        INCLUDED_IN_ENV VARCHAR(255),
                        INCLUDED_ONLY_IN_ENV VARCHAR(255),
                        EXCLUDED_IN_ENV VARCHAR(255),
                        SHARING VARCHAR(1024),

                        PRIMARY KEY (COMMIT_ID, NAME)
);

CREATE TABLE ASSIGNMENTS (
                           COMMIT_ID BIGINT NOT NULL REFERENCES COMMITS(ID),
                           VARIABLE_KEY VARCHAR(255) NOT NULL,
                           DISABLED BOOL NOT NULL,
                           ENV VARCHAR(63),
                           PROJECT VARCHAR(63),
                           SCOPE VARCHAR(31),
                           VARIABLE_INFO VARCHAR(2048),

                           PRIMARY KEY (COMMIT_ID, VARIABLE_KEY)
);

CREATE TABLE BUILDS (
                        ID BIGINT AUTO_INCREMENT PRIMARY KEY,
                        PARENT_ID BIGINT NULL REFERENCES BUILDS(ID),
                        DEPLOY_USER_NAME VARCHAR(63) NULL,
                        JOB_NAME VARCHAR(63) NOT NULL,
                        ENV_NAME VARCHAR(63) NOT NULL,
                        PROJECT_NAME VARCHAR(63) NOT NULL,
                        JENKINS_BUILD BOOL NOT NULL,

                        RECORD_COMMIT_ID BIGINT NULL REFERENCES COMMITS(ID),
                        CONFIG_COMMIT_HASH VARCHAR(63) NULL,

                        PLAYS VARCHAR(255) NOT NULL,
                        TASKS_TO_SKIP VARCHAR(255) NOT NULL,
                        SERVERS VARCHAR(4095) NOT NULL,

                        STARTED_TIME DATETIME NOT NULL,
                        FINISHED_TIME DATETIME NULL,
                        FAILED BOOL NULL,
                        FAILED_TYPE VARCHAR(63) NULL,
                        FAILED_MESSAGE VARCHAR(4095) NULL,

                        PROJECT_COMMIT_BRANCH VARCHAR(63) NULL,
                        PROJECT_COMMIT_HASH VARCHAR(63) NULL,
                        PROJECT_COMMIT_EMAIL VARCHAR(63) NULL,
                        PROJECT_COMMIT_DETAIL VARCHAR(4095) NULL,
                        PROJECT_COMMIT_DATE DATETIME NULL,

                        USER_PARAMETERS text NULL
);

CREATE TABLE CONFIG (
    ID BIGINT AUTO_INCREMENT PRIMARY KEY,
    ENV_NAME VARCHAR(63) NULL,
    PROJECT_NAME VARCHAR(63) NULL,
    NAMESPACE VARCHAR(63) NOT NULL,
    CONFIG_KEY VARCHAR(127) NOT NULL,
    CONFIG_VALUE LONGTEXT NULL,
    LOCKED_BY VARCHAR(127) NULL,
    LOCKED_TIME DATETIME NULL
);

CREATE UNIQUE INDEX CONFIG_UNIQUE_KEYS_INDEX
ON CONFIG(ENV_NAME, PROJECT_NAME, NAMESPACE, CONFIG_KEY);
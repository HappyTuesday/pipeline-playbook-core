package com.yit.deploy.core.global.resource;

/**
 * a collection of pre-defined resources
 */
public class Resources {
    /**
     * control the parallel of starting containers
     */
    public static PlentyResource startupContainer = new PlentyResource(5);

    /**
     * avoid to visit ALI RDS API too frequently
     */
    public static PlentyResource visitRDS = new PlentyResource(2);

    /**
     * avoid to SSH to certain server too frequently
     */
    public static PlentyResource ssh = new PlentyResource(10);

    /**
     * control RDS DB renaming process
     */
    public static SingletonResource renameRDSDB = new SingletonResource();

    /**
     * implement to access certain path exclusively
     */
    public static SingletonResource path = new SingletonResource();

    /**
     * generic lock
     */
    public static SingletonResource lock = new SingletonResource();

    /**
     * generic read-write lock
     */
    public static RWResource rw = new RWResource();

    /**
     * read-write lock for container label
     */
    public static RWResource startupContainerLabel = new RWResource();

    /**
     * read-write lock for docker deploy
     */
    public static RWResource dockerDeploy = new RWResource();

    /**
     * lock used to manage db
     */
    public static SingletonResource db = new SingletonResource();

    /**
     * implement to access certain path exclusively
     */
    public static SingletonResource slb = new SingletonResource();

    /**
     * deploy partitions
     */
    public static DeployPartitions deployPartitions = new DeployPartitions();
}

/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package com.github.stephenc.javascript.jszip;

import org.apache.maven.plugin.AbstractMojo;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugin.MojoFailureException;
import org.apache.maven.project.MavenProject;
import org.codehaus.plexus.archiver.zip.ZipArchiver;
import org.codehaus.plexus.components.io.resources.AbstractPlexusIoResource;
import org.codehaus.plexus.util.IOUtil;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.util.Properties;

/**
 * @phase package
 * @goal jszip
 */
public class JSZipMojo extends AbstractMojo {

    /**
     * The maven project.
     *
     * @parameter expression="${project}"
     * @required
     * @readonly
     */
    private MavenProject project;


    /**
     * Directory containing the classes.
     *
     * @parameter expression="src/main/js"
     * @required
     */
    private File contentDirectory;

    /**
     * Directory containing the generated ZIP.
     *
     * @parameter expression="${project.build.directory}"
     * @required
     */
    private File outputDirectory;

    /**
     * Name of the generated ZIP.
     *
     * @parameter expression="${zip.finalName}" default-value="${project.build.finalName}"
     * @required
     */
    private String finalName;

    /**
     * Classifier to add to the artifact generated. If given, the artifact will be an attachment instead.
     *
     * @parameter
     */
    private String classifier;

    /**
     * The Jar archiver.
     *
     * @parameter expression="${component.org.codehaus.plexus.archiver.Archiver#zip}"
     * @required
     */
    private ZipArchiver zipArchiver;

    /**
     * Include or not empty directories
     *
     * @parameter expression="${zip.includeEmptyDirs}" default-value="false"
     */
    private boolean includeEmptyDirs;

    /**
     * Whether creating the archive should be forced.
     *
     * @parameter expression="${zip.forceCreation}" default-value="false"
     */
    private boolean forceCreation;

    /**
     * Adding pom.xml and pom.properties to the archive.
     *
     * @parameter expression="${addMavenDescriptor}" default-value="true"
     */
    private boolean addMavenDescriptor;


    protected File getZipFile(File basedir, String finalName, String classifier) {
        if (classifier == null) {
            classifier = "";
        } else if (classifier.trim().length() > 0 && !classifier.startsWith("-")) {
            classifier = "-" + classifier;
        }

        return new File(basedir, finalName + classifier + ".zip");
    }

    /**
     * @see org.apache.maven.plugin.Mojo#execute()
     */
    public void execute()
            throws MojoExecutionException, MojoFailureException {
        try {

            File zipFile = getZipFile(outputDirectory, finalName, classifier);

            zipArchiver.setDestFile(zipFile);
            zipArchiver.setIncludeEmptyDirs(includeEmptyDirs);
            zipArchiver.setCompress(true);
            zipArchiver.setForced(forceCreation);

            if (addMavenDescriptor) {
                if (project.getArtifact().isSnapshot()) {
                    project.setVersion(project.getArtifact().getVersion());
                }

                String groupId = project.getGroupId();

                String artifactId = project.getArtifactId();

                zipArchiver.addFile(project.getFile(), "META-INF/maven/" + groupId + "/" + artifactId + "/pom.xml");
                zipArchiver.addResource(new PomPropertiesResource(project),
                        "META-INF/maven/" + groupId + "/" + artifactId + "/pom.properties",
                        zipArchiver.getOverrideFileMode());
            }
            if (contentDirectory.isDirectory()) {
                zipArchiver.addDirectory(contentDirectory);
            }
            zipArchiver.createArchive();
            project.getArtifact().setFile(zipFile);

        } catch (Exception e) {
            throw new MojoExecutionException("Error assembling ZIP", e);
        }

    }

    private static class PomPropertiesResource extends AbstractPlexusIoResource {
        private static final String GENERATED_BY_MAVEN = "Generated by Maven";
        private final byte[] bytes;
        private final MavenProject project;

        public PomPropertiesResource(MavenProject project) throws IOException {
            this.project = project;

            Properties p = new Properties();

            p.setProperty("groupId", project.getGroupId());

            p.setProperty("artifactId", project.getArtifactId());

            p.setProperty("version", project.getVersion());

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                p.store(os, GENERATED_BY_MAVEN);
                os.close();
            } finally {
                IOUtil.close(os);
            }

            bytes = os.toByteArray();
        }

        @Override
        public boolean isFile() {
            return true;
        }

        @Override
        public boolean isExisting() {
            return true;
        }

        @Override
        public long getLastModified() {
            return project.getFile().lastModified();
        }

        @Override
        public long getSize() {
            return bytes.length;
        }

        public URL getURL() throws IOException {
            return null;
        }

        public InputStream getContents() throws IOException {
            return new ByteArrayInputStream(bytes);
        }
    }
}

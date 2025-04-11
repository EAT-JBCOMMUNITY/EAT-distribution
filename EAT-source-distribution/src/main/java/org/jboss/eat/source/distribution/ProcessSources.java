/*
 * JBoss, Home of Professional Open Source.
 * Copyright 2016, Red Hat, Inc., and individual contributors
 * as indicated by the @author tags. See the copyright.txt file in the
 * distribution for a full listing of individual contributors.
 *
 * This is free software; you can redistribute it and/or modify it
 * under the terms of the GNU Lesser General Public License as
 * published by the Free Software Foundation; either version 2.1 of
 * the License, or (at your option) any later version.
 *
 * This software is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU
 * Lesser General Public License for more details.
 *
 * You should have received a copy of the GNU Lesser General Public
 * License along with this software; if not, write to the Free
 * Software Foundation, Inc., 51 Franklin St, Fifth Floor, Boston, MA
 * 02110-1301 USA, or see the FSF site: http://www.fsf.org.
 */
package org.jboss.eat.source.distribution;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.FileUtils;

/**
 *
 * @author panos
 */
public class ProcessSources {

    public static void AdditionalTestSuiteAnnotationProcessing(String basedir, String sourcePath, String server, String version, String versionOrderDir, boolean disableAllTests, FeatureData featureDataList, ArrayList<String> excludedFiles, String disableSnapshotVersions, String gitDir, String prePath, String postPath, String activemodules) {
        File folder = new File(sourcePath);
        File[] listOfFiles = folder.listFiles();

        if (listOfFiles == null) {
            return;
        }

        try {
            for (File file : listOfFiles) {
                if (file.isDirectory()) {
                    AdditionalTestSuiteAnnotationProcessing(basedir, file.getAbsolutePath(), server, version, versionOrderDir, disableAllTests, featureDataList, excludedFiles, disableSnapshotVersions, gitDir, prePath, postPath, activemodules);
                } else if (!excludedFiles.contains(file.getAbsolutePath())) {
                    ArrayList<FileData> output = checkFileForAnnotation(file.getAbsolutePath(), "@EAT", server, gitDir, prePath, postPath, activemodules);
                    for (FileData dest : output) {
                        if(dest.commitExists!=null){
                            if(dest.commitExists.equals("true")){
                                System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                copyWithStreams(file, new File(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName), false);
                                if (disableAllTests) {
                                    disableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.fileName);
                                }
                                String destFile = basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName;
                                checkMethodInclusion(file, destFile, server, basedir, version, versionOrderDir, featureDataList, disableSnapshotVersions, gitDir, prePath, postPath, dest.activemodule);
                            }
                            continue;
                        }
                        
                        if (dest.minVersion != null) {
                            boolean isSnapshot = false;
                            if (disableSnapshotVersions != null && disableSnapshotVersions.contains("true")) {
                                isSnapshot = version.contains("SNAPSHOT");
                            }
                            String[] versionRelease = version.split("-");
                            int verRelease1 = 0;
                            String[] verPart = versionRelease[0].split("\\.");
                            int v11=0;
                            int v12=0;
                            int v13=0;
                            if (verPart.length > 2) {
                                verRelease1 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                                v11 = Integer.parseInt(verPart[0]);
                                v12 = Integer.parseInt(verPart[1]);
                                v13 = Integer.parseInt(verPart[2]);
                            }
                            String[] subVersions = dest.minVersion.split("-");
                            verPart = subVersions[0].split("\\.");
                            int verRelease2 = 0;
                            int v21=0;
                            int v22=0;
                            int v23=0;
                            if (verPart.length > 2) {
                                verRelease2 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                                v21 = Integer.parseInt(verPart[0]);
                                v22 = Integer.parseInt(verPart[1]);
                                v23 = Integer.parseInt(verPart[2]);
                            }

                            int verRelease3 = 0;
                            int v31=0;
                            int v32=0;
                            int v33=0;
                            String[] subVersionsMax = null;

                            if (dest.maxVersion != null) {
                                subVersionsMax = dest.maxVersion.split("-");
                                verPart = subVersionsMax[0].split("\\.");

                                if (verPart.length > 2) {
                                    verRelease3 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                                    v31 = Integer.parseInt(verPart[0]);
                                    v32 = Integer.parseInt(verPart[1]);
                                    v33 = Integer.parseInt(verPart[2]);
                                }
                            }

                            if ((subVersions.length >= 1 && verRelease1 == verRelease2)) {

                                String[] vf = new String[2];
                                if (verRelease1 == verRelease2 && subVersions[0].split("\\.").length > 3) {
                                    vf[0] = subVersions[0].substring(0, subVersions[0].lastIndexOf("."));
                                    vf[1] = subVersions[0].substring(subVersions[0].lastIndexOf(".") + 1);
                                } else {
                                    vf[0] = subVersions[0];
                                    vf[1] = null;
                                }

                                File versionFolder = new File(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
                                if (vf != null && versionFolder.exists()) {
                                    String versionsString = readFile(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
                                    if (versionsString != null) {
                                        String[] versions = null;
                                        if (vf[1] != null) {
                                            versions = versionsString.substring(versionsString.indexOf(vf[1])).split(",");
                                        } else {
                                            versions = versionsString.split(",");
                                        }
                                        for (String versionPart : versionRelease) {
                                            if (versionPart.contains(".")) {
                                                String[] versionNums = versionPart.split("\\.");
                                                String lastPart = versionNums[versionNums.length - 1];
                                                if (!lastPart.matches("[0-9]+")) {
                                                    for (String ver : versions) {
                                                        if (lastPart.contains(ver)) {
                                                            if (!(ver.equals(lastPart) && isSnapshot)) {
                                                                if ((verRelease3 == 0 || verRelease1 < verRelease3)) {
                                                                    System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                                                    copyWithStreams(file, new File(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName), false);
                                                                    if (disableAllTests) {
                                                                        disableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.fileName);
                                                                    }
                                                                    String destFile = basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName;
                                                                    checkMethodInclusion(file, destFile, server, basedir, version, versionOrderDir, featureDataList, disableSnapshotVersions, gitDir, prePath, postPath, dest.activemodule);
                                                                } else if (verRelease1 == verRelease3) {
                                                                    procedure0(file, dest, server, basedir, version, versionOrderDir, verRelease1, verRelease3, subVersionsMax, versionRelease, featureDataList, disableSnapshotVersions, isSnapshot, disableAllTests, gitDir, prePath, postPath);
                                                                }
                                                            }
                                                        }
                                                    }
                                                } else {
                                                    if (!(verRelease1 == verRelease2 && isSnapshot)) {
                                                        if ((verRelease3 == 0 || verRelease1 < verRelease3)) {
                                                            System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                                            copyWithStreams(file, new File(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName), false);
                                                            if (disableAllTests) {
                                                                disableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.fileName);
                                                            }
                                                            String destFile = basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName;
                                                            checkMethodInclusion(file, destFile, server, basedir, version, versionOrderDir, featureDataList, disableSnapshotVersions, gitDir, prePath, postPath, dest.activemodule);
                                                        } else if (verRelease1 == verRelease3) {
                                                            procedure0(file, dest, server, basedir, version, versionOrderDir, verRelease1, verRelease3, subVersionsMax, versionRelease, featureDataList, disableSnapshotVersions, isSnapshot, disableAllTests, gitDir, prePath, postPath);
                                                        }
                                                    }
                                                }

                                            }
                                        }
                                    }
                                }
                            } else if (verRelease2==0 || (verRelease1 >= verRelease2 && v11>=v21 && v12>=v22 && v13>=v23)) {
                                if (!(verRelease1 == verRelease2 && isSnapshot)) {
                                    if (verRelease3 == 0 || verRelease1 < verRelease3) {
                                        if (verRelease1 != verRelease2 || !isSnapshot) {
                                            System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                            copyWithStreams(file, new File(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName), false);
                                            if (disableAllTests) {
                                                disableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.fileName);
                                            }
                                            String destFile = basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName;
                                            checkMethodInclusion(file, destFile, server, basedir, version, versionOrderDir, featureDataList, disableSnapshotVersions, gitDir, prePath, postPath, dest.activemodule);
                                        }
                                    } else if (verRelease1 == verRelease3) {
                                        procedure0(file, dest, server, basedir, version, versionOrderDir, verRelease1, verRelease3, subVersionsMax, versionRelease, featureDataList, disableSnapshotVersions, isSnapshot, disableAllTests, gitDir, prePath, postPath);
                                    }
                                }
                            }
                        } else {
                            System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                            copyWithStreams(file, new File(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName), false);
                            if (disableAllTests) {
                                disableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.fileName);
                            }
                            String destFile = basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName;
                            checkMethodInclusion(file, destFile, server, basedir, version, versionOrderDir, featureDataList, disableSnapshotVersions, gitDir, prePath, postPath,dest.activemodule);
                        }
                    }

                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    private static void procedure0(File file, FileData dest, String server, String basedir, String version, String versionOrderDir, int verRelease1, int verRelease3, String[] subVersionsMax, String[] versionRelease, FeatureData featureDataList, String disableSnapshotVersions, boolean isSnapshot, boolean disableAllTests, String gitDir, String prePath, String postPath) throws IOException, ClassNotFoundException {
        if (verRelease1 == verRelease3) {

            String[] vf = new String[2];
            if (subVersionsMax != null && verRelease1 == verRelease3 && subVersionsMax[0].split("\\.").length > 3) {
                vf[0] = subVersionsMax[0].substring(0, subVersionsMax[0].lastIndexOf("."));
                vf[1] = subVersionsMax[0].substring(subVersionsMax[0].lastIndexOf(".") + 1);
            } else {
                vf[0] = subVersionsMax[0];
                vf[1] = null;
            }

            File versionFolder = new File(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
            if (vf != null && versionFolder.exists()) {
                String versionsString = readFile(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
                if (versionsString != null) {
                    String[] versions = null;
                    if (vf[1] != null) {
                        versions = versionsString.substring(0, versionsString.indexOf(vf[1])).concat(vf[1]).split(",");
                    } else {
                        versions = versionsString.split(",");
                    }
                    for (String versionPart : versionRelease) {
                        if (versionPart.contains(".")) {
                            String[] versionNums = versionPart.split("\\.");
                            String lastPart = versionNums[versionNums.length - 1];
                            if (!lastPart.matches("[0-9]+")) {
                                for (String ver : versions) {
                                    if (lastPart.contains(ver) && !(ver.equals(lastPart) && isSnapshot)) {
                                        System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                        copyWithStreams(file, new File(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName), false);
                                        if (disableAllTests) {
                                            disableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.fileName);
                                        }
                                        String destFile = basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName;
                                        checkMethodInclusion(file, destFile, server, basedir, version, versionOrderDir, featureDataList, disableSnapshotVersions, gitDir, prePath, postPath, dest.activemodule);
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (verRelease1 <= verRelease3) {
                if (!(verRelease1 == verRelease3 && isSnapshot)) {
                    System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                    copyWithStreams(file, new File(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName), false);
                    if (disableAllTests) {
                        disableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.fileName);
                    }
                    String destFile = basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName;
                    checkMethodInclusion(file, destFile, server, basedir, version, versionOrderDir, featureDataList, disableSnapshotVersions, gitDir, prePath, postPath, dest.activemodule);
                }
            }
        }

    }

    private static ArrayList<FileData> checkFileForAnnotation(String file, String annotationName, String server, String gitDir, String prePath, String postPath, String activemodules) throws ClassNotFoundException {
        ArrayList<String> destinations = null;
        ArrayList<String> destinations2 = new ArrayList<>();
        String annotationLine = null;
        ArrayList<FileData> result = new ArrayList<FileData>();
        String packageName = "";
        File f = new File(file);
        String activemodule = null;
        String[] modules = activemodules.split(",");
        String pathB = null;

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.startsWith("package")) {
                    packageName = line.replaceAll("package ", "").replaceAll(";", "").trim();
                }
                if (line.contains(annotationName)) {
                    annotationLine = line;
                    destinations = new ArrayList<String>(Arrays.asList(annotationLine.split("\"")));
                    
                    for (String path : destinations) {
                        if(path.startsWith("&"))
                            path = path.replaceFirst("&", prePath);
                        if(path.contains("&")) {
                            path = path.replaceFirst("&", postPath);
                        }
                        
                        if (!path.contains(",") && path.contains("/" + server + "/")) {
                            
                            pathB = path;
                            if(path.contains("...")) {
                                if(modules.length>0) {
                                    activemodule = modules[0];
                                    path = path.replaceFirst("\\.\\.\\.", activemodule);
                                }

                            }
                                                    
                            if (path.contains("@")){
                                String[] commits = path.split("@")[1].split("-");
                                String dir = gitDir;
                                String distribute = null;
                                
                                for(String com : commits){
                                    ArrayList<String> commands=new ArrayList<String>();
                                    commands.add("bash");
                                    commands.add("-c");
                                    commands.add("cd " + dir + " ; git show --name-only " + com);
                                    ProcessBuilder pb = new ProcessBuilder(commands);
                                    Process p = pb.start();

                                    while (p.isAlive());
                                    String output = IOUtils.toString(p.getInputStream());
                               //     System.out.println("+++++++ " + output);
                                    p.destroy();
                                    if(!output.contains("commit")){
                                        distribute="false";
                                        break;
                                    }
                                }
                                String[] pathVersion = path.split("@");
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion[0], null, null,distribute, activemodule));
                            } else if (!path.contains("#") && !path.contains("*")) {
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), path, null, null,null, activemodule));
                            } else if (!path.contains("*")) {
                                String[] pathVersion = path.split("#");
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion[0], pathVersion[1], null,null, activemodule));
                            } else {
                                String[] pathVersion = path.split("\\*");
                                String[] pathVersion2 = pathVersion[0].split("#");
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion2[0], pathVersion2[1], pathVersion[1],null, activemodule));
                            }
                        }
                    }
                    
                    if(modules.length>1 && pathB!=null && pathB.contains("...")) {
                        for (int l=1; l<modules.length; l++) {
                            activemodule = modules[l];
                            String path = pathB.replaceFirst("\\.\\.\\.", modules[l]);

                           if (!path.contains(",") && path.contains("/" + server + "/")) {
                                if (path.contains("@")){
                                    String[] commits = path.split("@")[1].split("-");
                                    String dir = gitDir;
                                    String distribute = null;

                                    for(String com : commits){
                                        ArrayList<String> commands=new ArrayList<String>();
                                        commands.add("bash");
                                        commands.add("-c");
                                        commands.add("cd " + dir + " ; git show --name-only " + com);
                                        ProcessBuilder pb = new ProcessBuilder(commands);
                                        Process p = pb.start();

                                        while (p.isAlive());
                                        String output = IOUtils.toString(p.getInputStream());
                                   //     System.out.println("+++++++ " + output);
                                        p.destroy();
                                        if(!output.contains("commit")){
                                            distribute="false";
                                            break;
                                        }
                                    }
                                    String[] pathVersion = path.split("@");
                                    result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion[0], null, null,distribute, activemodule));
                                } else if (!path.contains("#") && !path.contains("*")) {
                                    result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), path, null, null,null, activemodule));
                                } else if (!path.contains("*")) {
                                    String[] pathVersion = path.split("#");
                                    result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion[0], pathVersion[1], null,null, activemodule));
                                } else {
                                    String[] pathVersion = path.split("\\*");
                                    String[] pathVersion2 = pathVersion[0].split("#");
                                    result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion2[0], pathVersion2[1], pathVersion[1],null, activemodule));
                                }
                            }
                        }
                    }
                    
                    break;
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return result;
    }

    private static ArrayList<FileData> checkFileForAnnotations(String file, String annotationName, String server, String gitDir, String prePath, String postPath, String activemodule) throws ClassNotFoundException {
        ArrayList<String> destinations = null;
        String annotationLine = null;
        ArrayList<FileData> result = new ArrayList<FileData>();
        String packageName = "";
        File f = new File(file);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.startsWith("package")) {
                    packageName = line.replaceAll("package ", "").replaceAll(";", "").trim();
                }
                if (line.contains(annotationName)) {
                    annotationLine = line;
                    destinations = new ArrayList<String>(Arrays.asList(annotationLine.split("\"")));
                    for (String path : destinations) {
                        
                        if(path.startsWith("&"))
                            path = path.replaceFirst("&", prePath);
                        if(path.contains("&")) {
                            path = path.replaceFirst("&", postPath);
                        }
                                                
                        if(path.contains("...")) {
                           path = path.replaceFirst("\\.\\.\\.", activemodule);
                        }
                                                
                        if (!path.contains(",") && path.contains("/" + server + "/")) {
                            if (path.contains("@")){
                                String[] commits = path.split("@")[1].split("-");
                                String dir = gitDir;
                                String distribute = "true";
                                
                                for(String com : commits){
                                    ArrayList<String> commands=new ArrayList<String>();
                                    commands.add("bash");
                                    commands.add("-c");
                                    commands.add("cd " + dir + " ; git show --name-only " + com);
                                    ProcessBuilder pb = new ProcessBuilder(commands);
                                    Process p = pb.start();

                                    while (p.isAlive());
                                    String output = IOUtils.toString(p.getInputStream());
                                //    System.out.println("+++++++ " + output);
                                    p.destroy();
                                    if(!output.contains("commit")){
                                        distribute="false";
                                        break;
                                    }
                                }
                                String[] pathVersion = path.split("@");
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion[0], null, null, lineNum, distribute, activemodule));
                            } else if(!path.contains("#") && !path.contains("*")) {
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), path, null, null, lineNum, null, activemodule));
                            } else if (!path.contains("*")) {
                                String[] pathVersion = path.split("#");
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion[0], pathVersion[1], null, lineNum, null, activemodule));
                            } else {
                                String[] pathVersion = path.split("\\*");
                                String[] pathVersion2 = pathVersion[0].split("#");
                                result.add(new FileData(f.getName(), packageName.replaceAll("\\.", "/"), pathVersion2[0], pathVersion2[1], pathVersion[1], lineNum, null, activemodule));
                            }
                        }
                    }

                    line = reader.readLine();
                    lineNum++;
                    if(line.contains("EATDPM") && line.contains("excludeDependencies")) {
                        String[] excludeDependencies = null;
                        if (line.lastIndexOf("excludeDependencies={\"") != -1) {
                            excludeDependencies = line.substring(line.lastIndexOf("excludeDependencies={\"") + 22, line.lastIndexOf("excludeDependencies={\"") + 22 + line.substring(line.lastIndexOf("excludeDependencies={\"") + 22).indexOf("\"}")).split(",");
                            result.get(result.size()-1).excudeDeps = excludeDependencies;
                        }
                    }
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return result;
    }

    private static ArrayList<FeatureData> checkFileForFeatures(String file, String annotationName) throws ClassNotFoundException {
        String annotationLine;
        ArrayList<FeatureData> result = new ArrayList<>();
        File f = new File(file);

        BufferedReader reader = null;
        try {
            reader = new BufferedReader(new FileReader(file));
            String line;
            int lineNum = 0;
            while ((line = reader.readLine()) != null) {
                lineNum++;
                if (line.contains(annotationName)) {
                    annotationLine = line;
                    String[] features = annotationLine.substring(annotationLine.lastIndexOf("feature={\"") + 10, annotationLine.indexOf("\"}")).split(",");
                    String[] minVersions = null;
                    if (annotationLine.lastIndexOf("minVersion={\"") != -1) {
                        minVersions = annotationLine.substring(annotationLine.lastIndexOf("minVersion={\"") + 13, annotationLine.lastIndexOf("minVersion={\"") + 13 + annotationLine.substring(annotationLine.lastIndexOf("minVersion={\"") + 13).indexOf("\"}")).split(",");
                    }
                    String[] maxVersions = null;
                    if (annotationLine.lastIndexOf("maxVersion={\"") != -1) {
                        maxVersions = annotationLine.substring(annotationLine.lastIndexOf("maxVersion={\"") + 13, annotationLine.lastIndexOf("maxVersion={\"") + 13 + annotationLine.substring(annotationLine.lastIndexOf("maxVersion={\"") + 13).indexOf("\"}")).split(",");
                    }
                    String[] resources = null;
                    if (annotationLine.lastIndexOf("resource={\"") != -1) {
                        resources = annotationLine.substring(annotationLine.lastIndexOf("resource={\"") + 11, annotationLine.lastIndexOf("resource={\"") + 11 + annotationLine.substring(annotationLine.lastIndexOf("resource={\"") + 11).indexOf("\"}")).split(",");
                    }
                    String[] params = null;
                    if (annotationLine.lastIndexOf("params={{") != -1) {
                        params = annotationLine.substring(annotationLine.lastIndexOf("params={{") + 9, annotationLine.lastIndexOf("params={{") + 9 + annotationLine.substring(annotationLine.lastIndexOf("params={{") + 9).indexOf("\"}}")).split("\"");
                    }
                    FeatureData fd = new FeatureData();
                    fd.lineNum = lineNum;
                    Collections.addAll(fd.feature, features);
                    if (minVersions != null) {
                        Collections.addAll(fd.minVersion, minVersions);
                    }
                    if (maxVersions != null) {
                        Collections.addAll(fd.maxVersion, maxVersions);
                    }
                    if (resources != null) {
                        Collections.addAll(fd.resource, resources);
                    }
                    if (params != null) {
                        Collections.addAll(fd.params, params);
                    }

                    result.add(fd);
                }
            }
        } catch (Exception ex) {
            ex.printStackTrace();
        } finally {
            if (reader != null) {
                try {
                    reader.close();
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }
        }

        return result;
    }

    private static String readFile(String file) throws FileNotFoundException, IOException {
        BufferedReader br = new BufferedReader(new FileReader(file));
        StringBuilder sb = new StringBuilder();
        try {
            String line = br.readLine();

            while (line != null) {
                sb.append(line);
                sb.append(System.lineSeparator());
                line = br.readLine();
            }
        } finally {
            br.close();
        }

        return sb.toString();
    }

    private static void copyWithStreams(File aSourceFile, File aTargetFile, boolean aAppend) {
        ensureTargetDirectoryExists(aTargetFile.getParentFile());
        InputStream inStream = null;
        OutputStream outStream = null;
        try {
            try {
                byte[] bucket = new byte[32 * 1024];
                inStream = new BufferedInputStream(new FileInputStream(aSourceFile));
                outStream = new BufferedOutputStream(new FileOutputStream(aTargetFile, aAppend));
                int bytesRead = 0;
                while (bytesRead != -1) {
                    bytesRead = inStream.read(bucket); //-1, 0, or more
                    if (bytesRead > 0) {
                        outStream.write(bucket, 0, bytesRead);
                    }
                }
            } finally {
                if (inStream != null) {
                    inStream.close();
                }
                if (outStream != null) {
                    outStream.close();
                }
            }
        } catch (FileNotFoundException ex) {
            ex.printStackTrace();
        } catch (IOException ex) {
            ex.printStackTrace();
        }
    }

    private static void ensureTargetDirectoryExists(File aTargetDir) {
        if (!aTargetDir.exists()) {
            aTargetDir.mkdirs();
        }
    }

    private static void procedure(FileData dest, String server, String basedir, String version, String versionOrderDir, int verRelease1, int verRelease3, String[] subVersionsMax, String[] versionRelease, boolean isSnapshot) throws IOException {
        boolean enable = false;
        
        if (verRelease1 == verRelease3) {

            String[] vf = new String[2];
            if (subVersionsMax != null && verRelease1 == verRelease3 && subVersionsMax[0].split("\\.").length > 3) {
                vf[0] = subVersionsMax[0].substring(0, subVersionsMax[0].lastIndexOf("."));
                vf[1] = subVersionsMax[0].substring(subVersionsMax[0].lastIndexOf(".") + 1);
            } else {
                vf[0] = subVersionsMax[0];
                vf[1] = null;
            }

            File versionFolder = new File(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
            if (vf != null && versionFolder.exists()) {
                String versionsString = readFile(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
                if (versionsString != null) {
                    String[] versions = null;
                    if (vf[1] != null) {
                        versions = versionsString.substring(0, versionsString.indexOf(vf[1])).concat(vf[1]).split(",");
                    } else {
                        versions = versionsString.split(",");
                    }
                    for (String versionPart : versionRelease) {
                        if (versionPart.contains(".")) {
                            String[] versionNums = versionPart.split("\\.");
                            String lastPart = versionNums[versionNums.length - 1];
                            if (!lastPart.matches("[0-9]+")) {
                                for (String ver : versions) {
                                    if (lastPart.contains(ver) && !(ver.equals(lastPart) && isSnapshot)) {
                                        System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                        enableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest, dest.fileName);
                                        enable = true;
                                    }
                                }
                            }
                        }
                    }
                }
            } else if (verRelease1 <= verRelease3) {
                if (!(verRelease1 == verRelease3 && isSnapshot)) {
                    System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                    enableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest, dest.fileName);
                    enable = true;
                }
            }
        }

        if(!enable && dest.excudeDeps!=null) {
            deleteMethodEAT(dest.lineNum, basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.excudeDeps);
        }
    }

    private static void checkMethodInclusion(File file, String destFile, String server, String basedir, String version, String versionOrderDir, FeatureData featureDataList, String disableSnapshotVersions, String gitDir, String prePath, String postPath, String activemodule) throws ClassNotFoundException, IOException {

        ArrayList<FileData> output = checkFileForAnnotations(file.getAbsolutePath(), "@ATTest", server, gitDir, prePath, postPath, activemodule);
        for (FileData dest : output) {
            if(dest.fileBaseDir.contains("..."))
                dest.fileBaseDir = dest.fileBaseDir.replaceFirst("\\.\\.\\.", activemodule);
            if(dest.commitExists!=null){
                if(dest.commitExists.equals("true")){
                    System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                    enableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest, dest.fileName);
                }
                continue;
            }

            boolean enable = false;
            
            if (dest.minVersion != null) {
                boolean isSnapshot = false;
                if (disableSnapshotVersions != null && disableSnapshotVersions.contains("true")) {
                    isSnapshot = version.contains("SNAPSHOT");
                }
                String[] versionRelease = version.split("-");
                int verRelease1 = 0;
                String[] verPart = versionRelease[0].split("\\.");
                int v11=0;
                            int v12=0;
                            int v13=0;
                            if (verPart.length > 2) {
                                verRelease1 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                                v11 = Integer.parseInt(verPart[0]);
                                v12 = Integer.parseInt(verPart[1]);
                                v13 = Integer.parseInt(verPart[2]);
                            }
                            String[] subVersions = dest.minVersion.split("-");
                            verPart = subVersions[0].split("\\.");
                            int verRelease2 = 0;
                            int v21=0;
                            int v22=0;
                            int v23=0;
                            if (verPart.length > 2) {
                                verRelease2 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                                v21 = Integer.parseInt(verPart[0]);
                                v22 = Integer.parseInt(verPart[1]);
                                v23 = Integer.parseInt(verPart[2]);
                            }

                            int verRelease3 = 0;
                            int v31=0;
                            int v32=0;
                            int v33=0;
                            String[] subVersionsMax = null;

                            if (dest.maxVersion != null) {
                                subVersionsMax = dest.maxVersion.split("-");
                                verPart = subVersionsMax[0].split("\\.");

                                if (verPart.length > 2) {
                                    verRelease3 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                                    v31 = Integer.parseInt(verPart[0]);
                                    v32 = Integer.parseInt(verPart[1]);
                                    v33 = Integer.parseInt(verPart[2]);
                                }
                            }


                if ((subVersions.length >= 1 && verRelease1 == verRelease2)) {

                    String[] vf = new String[2];
                    if (verRelease1 == verRelease2 && subVersions[0].split("\\.").length > 3) {
                        vf[0] = subVersions[0].substring(0, subVersions[0].lastIndexOf("."));
                        vf[1] = subVersions[0].substring(subVersions[0].lastIndexOf(".") + 1);
                    } else {
                        vf[0] = subVersions[0];
                        vf[1] = null;
                    }

                    File versionFolder = new File(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
                    if (vf != null && versionFolder.exists()) {
                        String versionsString = readFile(basedir + "/" + versionOrderDir + "/" + server + "/" + vf[0]);
                        if (versionsString != null) {
                            String[] versions = null;
                            if (vf[1] != null) {
                                versions = versionsString.substring(versionsString.indexOf(vf[1])).split(",");
                            } else {
                                versions = versionsString.split(",");
                            }
                            for (String versionPart : versionRelease) {
                                if (versionPart.contains(".")) {
                                    String[] versionNums = versionPart.split("\\.");
                                    String lastPart = versionNums[versionNums.length - 1];
                                    if (!lastPart.matches("[0-9]+")) {
                                        for (String ver : versions) {
                                            if (lastPart.contains(ver)) {
                                                if (!(ver.equals(lastPart) && isSnapshot)) {
                                                    if ((verRelease3 == 0 || verRelease1 < verRelease3)) {
                                                        System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                                        enableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest, dest.fileName);
                                                        enable = true;
                                                    } else if (verRelease1 == verRelease3) {
                                                        procedure(dest, server, basedir, version, versionOrderDir, verRelease1, verRelease3, subVersionsMax, versionRelease, isSnapshot);
                                                    }
                                                }
                                            }
                                        }
                                    } else {
                                        if (!(verRelease1 == verRelease2 && isSnapshot)) {
                                            if ((verRelease3 == 0 || verRelease1 < verRelease3)) {
                                                System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                                                enableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest, dest.fileName);
                                                enable = true;
                                            } else if (verRelease1 == verRelease3) {
                                                procedure(dest, server, basedir, version, versionOrderDir, verRelease1, verRelease3, subVersionsMax, versionRelease, isSnapshot);
                                            }
                                        }
                                    }
                                }
                            }
                        }
                    }
                } else if (verRelease2==0 || (verRelease1 >= verRelease2 && v11>=v21 && v12>=v22 && v13>=v23)) {
                    if (!(verRelease1 == verRelease2 && isSnapshot)) {
                        if (verRelease3 == 0 || verRelease1 < verRelease3) {
                            System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                            enableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest, dest.fileName);
                            enable = true;
                        } else if (verRelease1 == verRelease3) {
                            procedure(dest, server, basedir, version, versionOrderDir, verRelease1, verRelease3, subVersionsMax, versionRelease, isSnapshot);
                        }
                    }
                }
            } else {
                System.out.println(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName);
                enable = true;
                enableTests(basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest, dest.fileName);
            }
            
            if(!enable && dest.excudeDeps!=null) {
                deleteMethodEAT(dest.lineNum, basedir + "/" + dest.fileBaseDir + "/" + dest.packageName + "/" + dest.fileName, dest.excudeDeps);
            }
        }

        ArrayList<FeatureData> features = checkFileForFeatures(file.getAbsolutePath(), "@ATFeature");
        ArrayList<FeatureData> featureLineData = new ArrayList();
        for (FeatureData featureLine : features) {
            int fd = -1;
            int featuresOk = 0;
            for (int i = 0; i < featureLine.feature.size(); i++) {

                if (!featureDataList.feature.contains(featureLine.feature.get(i))) {
                    break;
                }

                fd = featureDataList.feature.indexOf(featureLine.feature.get(i));

                boolean phaseOK = false;

                while (fd != -1) {
                    phaseOK = false;

                    if (!featureLine.minVersion.isEmpty() && featureLine.minVersion.get(i) != null) {
                        String[] versionRelease = featureDataList.minVersion.get(fd).split("-");
                        int verRelease1 = 0;
                        String[] verPart = versionRelease[0].split("\\.");
                        if (verPart.length > 2) {
                            verRelease1 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                        } else if (verPart[0].compareTo("null") != 0) {
                            verRelease1 = Integer.parseInt(verPart[0]);
                        }

                        String[] subVersions = featureLine.minVersion.get(i).split("-");
                        verPart = subVersions[0].split("\\.");
                        int verRelease2 = 0;
                        if (verPart.length > 2) {
                            verRelease2 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                        } else if (verPart[0].compareTo("null") != 0) {
                            verRelease2 = Integer.parseInt(verPart[0]);
                        }

                        int verRelease3 = 0;

                        if (!featureLine.maxVersion.isEmpty() && featureLine.maxVersion.get(i) != null) {
                            String[] subVersionsMax = featureLine.maxVersion.get(i).split("-");
                            verPart = subVersionsMax[0].split("\\.");

                            if (verPart.length > 2) {
                                verRelease3 = Integer.parseInt(verPart[0] + verPart[1] + verPart[2]);
                            } else if (verPart[0].compareTo("null") != 0) {
                                verRelease3 = Integer.parseInt(verPart[0]);
                            }
                        }

                        if (subVersions.length > 1 && verRelease1 == verRelease2) {
                            File versionFolder = new File(basedir + "/" + versionOrderDir + "/" + featureLine.feature.get(i) + "/" + subVersions[0]);
                            if (versionFolder.exists()) {
                                String versionsString = readFile(basedir + "/" + versionOrderDir + "/" + featureLine.feature.get(i) + "/" + subVersions[0]);
                                if (versionsString != null && versionsString.contains(subVersions[1])) {
                                    String[] versions = versionsString.substring(versionsString.indexOf(subVersions[1])).split(",");
                                    for (String versionPart : versionRelease) {
                                        if (versionPart.contains(".")) {
                                            String[] versionNums = versionPart.split("\\.");
                                            String lastPart = versionNums[versionNums.length - 1];
                                            if (!lastPart.matches("[0-9]+")) {
                                                for (String ver : versions) {
                                                    if (lastPart.contains(ver) || lastPart.compareTo(ver) == 0) {
                                                        phaseOK = true;
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        } else if (verRelease1 >= verRelease2 && (verRelease3 == 0 || verRelease1 <= verRelease3)) {
                            phaseOK = true;
                        }
                    } else {
                        phaseOK = true;
                    }

                    if (!featureLine.minVersion.isEmpty() && !phaseOK) {
                        fd = featureDataList.feature.subList(fd + 1, featureDataList.feature.size()).indexOf(featureLine.feature.get(i));
                        continue;
                    }

                    if (!featureLine.resource.isEmpty() && featureLine.resource.get(i) != null) {
                        phaseOK = false;
                        if (featureLine.resource.get(i).compareTo(featureDataList.resource.get(fd)) == 0) {
                            phaseOK = true;
                        }
                    }

                    if (!featureLine.resource.isEmpty() && !phaseOK) {
                        fd = featureDataList.feature.subList(fd + 1, featureDataList.feature.size()).indexOf(featureLine.feature.get(i));
                        continue;
                    }

                    if (!featureLine.params.isEmpty() && featureLine.params.get(i) != null) {
                        String[] pms = featureLine.params.get(i).split(",");

                        for (String p : pms) {
                            if (!featureDataList.params.get(fd).contains(p)) {
                                phaseOK = false;
                                fd = featureDataList.feature.subList(fd + 1, featureDataList.feature.size()).indexOf(featureLine.feature.get(i));
                                continue;
                            }
                        }
                    }

                    if (phaseOK) {
                        featuresOk++;
                        fd = -1;
                        break;
                    } else {
                        fd = featureDataList.feature.subList(fd + 1, featureDataList.feature.size()).indexOf(featureLine.feature.get(i));
                    }
                }
            }

            if (featuresOk != featureLine.feature.size() && !featureDataList.feature.isEmpty()) {
                featureLineData.add(featureLine);
            }
        }

        if (!featureLineData.isEmpty()) {
            disableFeatureTests(destFile, featureLineData, destFile.substring(destFile.lastIndexOf("\\") + 1));
        }
    }

    public static void enableTests(String file, FileData fileData, String fileName) throws FileNotFoundException, IOException {

        List<String> lines = Files.readAllLines(Paths.get(file), Charset.defaultCharset());

        if (fileData != null) {

            for (int j = 0; j < lines.size(); j++) {
                if (lines.get(j).contains("/* @RunWith(Arquillian.class) */")) {
                    lines.set(j, lines.get(j).replaceAll("/* @RunWith(Arquillian.class) */", "@RunWith(Arquillian.class)"));
                }
                if (lines.get(j).contains("class") && lines.get(j).contains(fileName.replaceAll(".java", "")) && lines.get(j - 1).contains("@org.junit.Ignore")) {
                    lines.set(j - 1, lines.get(j - 1).replaceAll("@org.junit.Ignore", ""));
                    break;
                }
            }
            
            if (!lines.get(fileData.lineNum - 1).contains("@Test")) {
                lines.set(fileData.lineNum - 1, lines.get(fileData.lineNum - 1) + " @Test");
            }

            Files.write(Paths.get(file), lines, Charset.defaultCharset());
        }
    }

    public static void disableFeatureTests(String file, ArrayList<FeatureData> featureData, String fileName) throws FileNotFoundException, IOException {

        List<String> lines = Files.readAllLines(Paths.get(file), Charset.defaultCharset());

        if (featureData != null && featureData.size() != 0) {

            for (int j = 0; j < lines.size(); j++) {
                if (lines.get(j).contains("/* @RunWith(Arquillian.class) */")) {
                    lines.set(j, lines.get(j).replaceAll("/* @RunWith(Arquillian.class) */", "@RunWith(Arquillian.class)"));
                }
                if (lines.get(j).contains("class") && lines.get(j).contains(fileName.replaceAll(".java", "")) && lines.get(j - 1).contains("@org.junit.Ignore")) {
                    lines.set(j - 1, lines.get(j - 1).replaceAll("@org.junit.Ignore", ""));
                    break;
                }
            }

            for (FeatureData fd : featureData) {
                lines.set(fd.lineNum - 4, lines.get(fd.lineNum - 4).replaceAll("@Test", ""));
                lines.set(fd.lineNum - 3, lines.get(fd.lineNum - 3).replaceAll("@Test", ""));
                lines.set(fd.lineNum - 2, lines.get(fd.lineNum - 2).replaceAll("@Test", ""));
                lines.set(fd.lineNum - 1, lines.get(fd.lineNum - 1).replaceAll("@Test", ""));
                lines.set(fd.lineNum, lines.get(fd.lineNum).replaceAll("@Test", ""));
                lines.set(fd.lineNum + 1, lines.get(fd.lineNum + 1).replaceAll("@Test", ""));
                lines.set(fd.lineNum + 2, lines.get(fd.lineNum + 2).replaceAll("@Test", ""));
            }

            Files.write(Paths.get(file), lines, Charset.defaultCharset());
        }
    }

    public static void disableTests(String file, String fileName) throws FileNotFoundException, IOException {
        List<String> lines = Files.readAllLines(Paths.get(file), Charset.defaultCharset());
        boolean testExist = false;

        int i = 0;
        for (String line : lines) {
            if (line.contains("@Test")) {
                testExist = true;
            }
            line = line.replaceAll("@Test", "");
            lines.set(i, line);
            i++;
        }

        boolean ignoreExists = false;
        for (int j = 0; j < lines.size(); j++) {
            if (lines.get(j).contains("@Ignore")) {
                ignoreExists = true;
            }
            if (lines.get(j).contains("@RunWith(Arquillian.class)")) {
                lines.set(j, "/* " + lines.get(j) + " */");
            }
            if (testExist) {
                if (!ignoreExists && lines.get(j).contains(" class") && lines.get(j).contains(fileName.replaceAll(".java", ""))) {
                    lines.set(j - 1, "@org.junit.Ignore" + lines.get(j - 1));
                    break;
                }
            }
        }

        Files.write(Paths.get(file), lines, Charset.defaultCharset());
    }
    
    private static void deleteMethodEAT(int lineNum, String file, String[] excludeDependencies) throws IOException{
        List<String> lines = FileUtils.readLines(new File(file), "utf-8");
        
        if (excludeDependencies!=null) {
            for(String dep : excludeDependencies) {
                int lineN = 0;
                while(!lines.get(lineN).contains(dep)){
                    lineN++;
                }
                
                if (lines.get(lineN).contains(dep)){
                    lines.remove(lineN);
                    lineNum--;
                }
            }
        }
        
        while(lines.get(lineNum).compareTo("")!=0){
            lineNum--;
        }
        if(lines.get(lineNum).compareTo("")==0) {
            int up=0;
            int down=0;
            lines.remove(lineNum);
            lines.add(lineNum, "/**");
            lineNum++;
            
            while ((lines!=null && lines.size()>lineNum-1 && lines.get(lineNum)!=null && lines.get(lineNum).trim().compareTo("")!=0) || up==0 || up!=down) {
                String line = lines.get(lineNum);
                while(line.contains("{")) {
                    line = line.replaceFirst("\\{", "");
                    up++;
                }
                while(line.contains("}")) {
                    line = line.replaceFirst("\\}", "");
                    down++;
                }
                lineNum++;
            //    lines.remove(lineNum);
            }
            lines.remove(lineNum);
            lines.add(lineNum, "**/");
            
            FileWriter writer = new FileWriter(file); 
            for(String str: lines) {
              writer.write(str + "\n");
            }
            writer.close();
            
        }
    }
}

class FileData {

    protected String fileName;
    protected String packageName;
    protected String fileBaseDir;
    protected String minVersion;
    protected String maxVersion;
    protected String[] excudeDeps;
    protected int lineNum;
    protected String commitExists;
    protected String activemodule;

    public FileData(String fileName, String packageName, String fileBaseDir, String minVersion, String maxVersion, String commitExists, String activemodule) {
        this.fileName = fileName;
        this.packageName = packageName;
        this.fileBaseDir = fileBaseDir;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.commitExists = commitExists;
        this.activemodule = activemodule;
    }

    public FileData(String fileName, String packageName, String fileBaseDir, String minVersion, String maxVersion, int lineNum, String commitExists, String activemodule) {
        this.fileName = fileName;
        this.packageName = packageName;
        this.fileBaseDir = fileBaseDir;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.lineNum = lineNum;
        this.commitExists = commitExists;
        this.activemodule = activemodule;
    }

}

class FeatureData {

    protected ArrayList<String> feature;
    protected ArrayList<String> minVersion;
    protected ArrayList<String> maxVersion;
    protected ArrayList<String> resource;
    protected ArrayList<String> params;
    protected int lineNum;

    public FeatureData() {
        feature = new ArrayList();
        minVersion = new ArrayList();
        maxVersion = new ArrayList();
        resource = new ArrayList();
        params = new ArrayList();
    }

    public FeatureData(ArrayList<String> feature, ArrayList<String> minVersion, ArrayList<String> maxVersion, ArrayList<String> resource, ArrayList<String> params) {
        this.feature = feature;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.resource = resource;
        this.params = params;
    }

    public FeatureData(ArrayList<String> feature, ArrayList<String> minVersion, ArrayList<String> maxVersion, ArrayList<String> resource, ArrayList<String> params, int lineNum) {
        this.feature = feature;
        this.minVersion = minVersion;
        this.maxVersion = maxVersion;
        this.resource = resource;
        this.params = params;
        this.lineNum = lineNum;
    }

}

package it.serasoft.pdi.parser;

import it.serasoft.pdi.model.PDIProcessConnection;
import it.serasoft.pdi.model.PDIProcessParameterHolder;
import it.serasoft.pdi.utils.ConsoleOutputUtil;
import it.serasoft.pdi.utils.PDIMetadataPath;
import it.serasoft.pdi.utils.ResolvePDIInternalVariables;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


/**
 *  Copyright 2016 - Sergio Ramazzina : sergio.ramazzina@serasoft.it
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *  http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */


/**
 * Class Name   : ParseJobEntries.java
 * Package Name : it.serasoft.pdi.parse
 * <p>
 * Created By   : Sergio Ramazzina - sergio.ramazzina@serasoft.it
 * Creation Date: 24/11/16
 * Description  :
 */
public class ParseJob extends ParsePDIMetadata {

    private Logger l = LoggerFactory.getLogger(ParseJob.class);

    private String jobName;
    private String jobDesc;
    private String jobExtDesc;

    public ParseJob(File jobFile, int depth, boolean followSymlinks) {
        super(jobFile, depth, followSymlinks);
    }

    public void parse() {
        parse(null, null, null);
    }

    public void parse(String parentPDIProcName, File parentprocFileRef, String callerStepName) {

        try {
            PDIMetadataPath metadataPath = new PDIMetadataPath();

            XMLStreamReader xmlStreamReader = XMLInputFactory.newInstance().createXMLStreamReader(new FileInputStream(procFileRef));
            String elementName = null;
            int eventType = 0;

            while (xmlStreamReader.hasNext()) {

                eventType = xmlStreamReader.next();
                switch (eventType) {
                    case XMLStreamReader.START_ELEMENT:
                        elementName = xmlStreamReader.getLocalName();
                        metadataPath.push(elementName);

                        if (elementName.equals("entries")) {
                            parseEntries(xmlStreamReader, metadataPath);
                        } else if (elementName.equals("name") && metadataPath.path().equals("/job/name")) {
                            name = parseSimpleTextElementByName(xmlStreamReader, "name", metadataPath);
                            System.out.println("Analyzing job metadata - File: " + name
                                    + "\n| Filename: " + procFileRef.getName()
                                    + "\n| Path: " + procFileRef.getParent()
                                    + (parentPDIProcName != null ? "\n| Caller: " + parentPDIProcName : "")
                                    + (parentprocFileRef != null ? "\n| Caller Filename: " + parentprocFileRef.getName() : "")
                                    + (callerStepName != null ? "\n| Caller Step: " + callerStepName : ""));
                        } else if (elementName.equals("description") && metadataPath.path().equals("/job/description")) {
                            desc = parseSimpleTextElementByName(xmlStreamReader, "description", metadataPath);
                        } else if (elementName.equals("extended_description") && metadataPath.path().equals("/job/extended_description")) {
                            extDesc = parseSimpleTextElementByName(xmlStreamReader, "extended_description", metadataPath);
                        } else if (elementName.equals("parameters") && metadataPath.path().equals("/job/parameters")) {
                            parseParameters(xmlStreamReader, metadataPath);
                        } else if (elementName.equals("connection") && metadataPath.path().equals("/job/connection")) {
                            PDIProcessConnection conn = parseConnection(xmlStreamReader, metadataPath);
                            if (connections == null)
                                connections = new ArrayList<>();
                            if (conn != null) {
                                connections.add(conn);
                            }
                        }
                        break;

                    case XMLStreamConstants.END_ELEMENT:
                        elementName = xmlStreamReader.getLocalName();
                        metadataPath.pop();
                        if(elementName.equals("job")) {
                            printReport();
                        }
                        break;
                }
            }

        } catch (FileNotFoundException e1) {
            System.out.println("| WARNING - File " + procFileRef + " was not found as expected by PDI analyzer. Please check!"
                    + (parentPDIProcName != null ? "\n| | Caller: " + parentPDIProcName : "")
                    + (parentprocFileRef != null ? "\n| | Caller Filename: " + parentprocFileRef.getName() : "")
                    + (callerStepName != null ? "\n| | Caller Step: " + callerStepName : ""));

        } catch (XMLStreamException e2) {
            l.error(e2.getLocalizedMessage());
        } catch (Exception e) {
            l.error(e.getMessage());
        }
    }

    private void parseEntries(XMLStreamReader xmlStreamReader, PDIMetadataPath metadataPath) {

        int eventType = 0;
        boolean elementAnalyzed = false;
        String elementName = null;

        try {
            while (xmlStreamReader.hasNext() && !elementAnalyzed) {
                eventType = xmlStreamReader.next();
                switch (eventType) {
                    case XMLStreamReader.START_ELEMENT:
                        elementName = xmlStreamReader.getLocalName();
                        metadataPath.push(elementName);
                        if (elementName.equals("entry")) {
                            parseEntry(xmlStreamReader, metadataPath);
                        }
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        elementName = xmlStreamReader.getLocalName();
                        metadataPath.pop();
                        if (elementName.equals("entries"))
                            elementAnalyzed = true;
                        break;
                }

            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }

    }

    private void parseEntry(XMLStreamReader xmlStreamReader, PDIMetadataPath metadataPath){

        int eventType = 0;
        boolean elementAnalyzed = false;
        String elementName = null;
        String type = null;
        String name = null;
        String description = null;
        String procFileRefname = null;

        try {
            while (xmlStreamReader.hasNext() && !elementAnalyzed) {
                eventType = xmlStreamReader.next();
                switch (eventType) {
                    case XMLStreamReader.START_ELEMENT:
                        elementName = xmlStreamReader.getLocalName();
                        metadataPath.push(elementName);
                        if (elementName.equals("name")) {
                            name = readElementText(xmlStreamReader, metadataPath);
                            l.debug("Name: " + name);
                        } else if (elementName.equals("type")) {
                            type = readElementText(xmlStreamReader, metadataPath);
                            l.debug("Type: " + type);
                        } else if (elementName.equals("description")) {
                            description = readElementText(xmlStreamReader, metadataPath);
                            l.debug("Description: " + description);
                        } else if (elementName.equals("filename")) {
                            procFileRefname = readElementText(xmlStreamReader, metadataPath);
                            if (procFileRefname != null) {
                                procFileRefname = ResolvePDIInternalVariables.internalProcessDirectories(procFileRef.getParent(), procFileRefname);
                                l.debug("Filename: " + procFileRefname);
                                if (followSymlinks && type.equals("JOB")) {
                                    ParseJob parseJob = new ParseJob(new File(procFileRefname), depth + 1, false);
                                    parseJob.parse(jobName, procFileRef, name);
                                    if (linkedPDIMetadata == null) {
                                        linkedPDIMetadata = new ArrayList<>();
                                    }
                                    linkedPDIMetadata.add(parseJob);
                                } else if (followSymlinks && type.equals("TRANS")) {
                                    ParseTransformation parseTrans = new ParseTransformation(new File(procFileRefname), depth + 1, false);
                                    parseTrans.parse(jobName, procFileRef, name);
                                    if (linkedPDIMetadata == null) {
                                        linkedPDIMetadata = new ArrayList<>();
                                    }
                                    linkedPDIMetadata.add(parseTrans);
                                }
                            }
                        }
                        break;
                    case XMLStreamReader.END_ELEMENT:
                        metadataPath.pop();
                        elementName = xmlStreamReader.getLocalName();
                        if (elementName.equals("entry"))
                            elementAnalyzed = true;
                        break;
                }
            }
        } catch (XMLStreamException e) {
            e.printStackTrace();
        }


    }

}